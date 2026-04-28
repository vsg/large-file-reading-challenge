package com.github.vsg.kyotu.temperature.storage;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.github.vsg.kyotu.temperature.storage.exception.InvalidDataFormatException;

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

        @Override
        public String toString() {
            return new String(array, begin, end-begin);
        }
        
    }
    
    // a simpler alternative to DoubleSummaryStatistics
    private static class SumAndCount {

        private double sum;
        private int count;
        
        public SumAndCount(double sum, int count) {
            this.sum = sum;
            this.count = count;
        } 
        
        public static SumAndCount combine(SumAndCount a, SumAndCount b) {
            return new SumAndCount(a.sum + b.sum, a.count + b.count);
        }
        
        public void add(double value) {
            sum += value;
            count++;
        }
        
        public double average() {
            return (count > 0) ? sum / count : 0;
        }

    }
    
    private static final int BLOCK_SIZE = 10_000_000;

    public Map<String, Map<Integer, Double>> loadCityYearAverages(Path path) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            Map<String, SumAndCount> summaries = blocks(file, BLOCK_SIZE).stream().parallel()
                    .flatMap(block -> processBlock(block).entrySet().stream())
                    .collect(Collectors.toConcurrentMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            SumAndCount::combine));

            // Parsing of city and year is delayed until "city;year" strings are deduplicated
            
            return summaries.entrySet().stream().parallel()
                    .collect(Collectors.groupingByConcurrent(
                            entry -> parseCity(entry.getKey()),
                            Collectors.toMap(
                                    entry -> parseYear(entry.getKey()),
                                    entry -> entry.getValue().average())));
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

    private Map<String, SumAndCount> processBlock(MappedByteBuffer block) {
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

                summary.computeIfAbsent(cityYear, k -> new SumAndCount(0, 0)).add(temperature);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new InvalidDataFormatException("Block parsing failed", e);
        }
        
        return summary.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toString(), 
                        entry -> entry.getValue()));
    }

    private static String parseCity(String cityYear) {
        return cityYear.substring(0, cityYear.lastIndexOf(';'));
    }

    private static Integer parseYear(String cityYear) {
        return Integer.parseInt(cityYear.substring(cityYear.lastIndexOf(';') + 1));
    }
    
    public static void main(String[] args) throws Exception {
        Path path = Paths.get(args[0]);
        long begin = System.currentTimeMillis();
        new TemperatureDataLoader().loadCityYearAverages(path);
        long end = System.currentTimeMillis();
        System.out.println(String.format("%d ms", end-begin));
    }
    
}
