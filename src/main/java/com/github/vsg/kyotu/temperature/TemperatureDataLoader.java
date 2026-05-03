package com.github.vsg.kyotu.temperature;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.github.vsg.kyotu.temperature.exception.InvalidDataFormatException;

@Component
public class TemperatureDataLoader {

    private static class Chunk {
        
        private final byte[] array;
        private final int begin;
        private final int end;
        private final int hashCode;

        public Chunk(byte[] array, int begin, int end) {
            this.array = array;
            this.begin = begin;
            this.end = end;
            this.hashCode = calcHashCode(array, begin, end);
        }

        private int calcHashCode(byte[] array, int begin, int end) {
            int result = 1;
            for (int i = begin; i < end; i++) {
                result = 31 * result + array[i];
            }
            return result;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Chunk other = (Chunk) obj;
            if (this.hashCode != other.hashCode) return false;
            return Arrays.equals(this.array, this.begin, this.end, other.array, other.begin, other.end);
        }
        
    }
    
    private static class SumAndCount {

        private double sum;
        private int count;
        
        public void add(double value) {
            sum += value;
            count++;
        }
        
        public double average() {
            return (count > 0) ? sum / count : 0;
        }
        
        public static SumAndCount combine(SumAndCount a, SumAndCount b) {
            SumAndCount result = new SumAndCount();
            result.sum = a.sum + b.sum;
            result.count = a.count + b.count;
            return result;
        }
        
        public static void accumulate(SumAndCount acc, SumAndCount x) {
            acc.sum += x.sum;
            acc.count += x.count;
        }

        public static <T> Collector<SumAndCount, SumAndCount, T> collectingAndThen(
                Function<SumAndCount, T> finisher) {
            return Collector.of(
                    SumAndCount::new, 
                    SumAndCount::accumulate, 
                    SumAndCount::combine, 
                    finisher);
        }
        
    }
    
    private static record CityYear(String city, String year) {
        
        public CityYear(Chunk chunk) {
            int pos = chunk.begin;
            while (chunk.array[pos] != ';') pos++;
            
            String city = new String(chunk.array, chunk.begin, pos - chunk.begin);
            String year = new String(chunk.array, pos + 1, chunk.end - pos - 1);
            
            this(city, year);
        }
        
    }
    
    private static final int BLOCK_SIZE = 10_000_000;

    public Map<String, Map<String, Double>> loadCityYearAverages(Path path) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            return blocks(file, BLOCK_SIZE).stream().parallel()
                    .flatMap(block -> processBlock(block).entrySet().stream())
                    .collect(Collectors.groupingByConcurrent(
                            e -> e.getKey().city(),
                            Collectors.groupingBy(
                                    e -> e.getKey().year(),
                                    Collectors.mapping(
                                            e -> e.getValue(),
                                            SumAndCount.collectingAndThen(
                                                  SumAndCount::average)))));
        }
    }

    private List<MappedByteBuffer> blocks(RandomAccessFile file, int blockSize) throws IOException {
        long fileSize = file.length();
        List<MappedByteBuffer> result = new ArrayList<>();
        long pos = 0;
        while (pos < fileSize) {
            long begin = pos;
            pos = Math.min(fileSize, pos + blockSize);
            if (pos < fileSize) {
                file.seek(pos);
                while (pos < fileSize && file.read() != '\n') {
                    pos++;
                }
                if (pos < fileSize) pos++;
            }
            result.add(file.getChannel().map(MapMode.READ_ONLY, begin, pos - begin));
        }
        return result;
    }

    private Map<CityYear, SumAndCount> processBlock(MappedByteBuffer block) {
        int length = block.remaining();
        byte[] array = new byte[length];
        block.get(array);
        
        // Example: 
        //   Warszawa;2018-09-20 18:44:42.468;39.02
        
        Map<Chunk, SumAndCount> summary = new HashMap<>();
        
        try {
            int pos = 0;
            while (pos < length) {
                int beginCity = pos;
                while (array[pos] != ';') pos++;
                pos++;
                while (array[pos] != '-') pos++;
                Chunk cityYear = new Chunk(array, beginCity, pos);
                while (array[pos] != ';') pos++;
                pos++;
                
                int beginTemperature = pos;
                while (pos < length && array[pos] != '\r' && array[pos] != '\n') pos++;
                double temperature = TemperatureParser.parse(array, beginTemperature, pos);
                while (pos < length && (array[pos] == '\r' || array[pos] == '\n')) pos++;
    
                summary.computeIfAbsent(cityYear, k -> new SumAndCount()).add(temperature);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new InvalidDataFormatException("Failed to parse data block", e);
        }
        
        return summary.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> new CityYear(e.getKey()), 
                        e -> e.getValue()));
    }
    
}
