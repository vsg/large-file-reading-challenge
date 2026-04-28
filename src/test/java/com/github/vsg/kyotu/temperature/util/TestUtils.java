package com.github.vsg.kyotu.temperature.util;

import java.nio.file.Path;

public class TestUtils {

    public static Path resourceFile(String fileName) {
        try {
            return Path.of(TestUtils.class.getClassLoader().getResource(fileName).toURI());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
}
