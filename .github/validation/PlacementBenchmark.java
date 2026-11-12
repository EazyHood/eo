package org.eolang.maven;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public final class PlacementBenchmark {
    public static void main(String[] args) throws Exception {
        Logger.getRootLogger().setLevel(Level.OFF);
        Logger.getLogger("org.eolang").setLevel(Level.OFF);
        for (int size : new int[] {1000, 2000, 1000, 2000}) {
            Path root = Files.createTempDirectory("placement-benchmark-");
            Path home = root.resolve("resolve");
            Path dependency = home.resolve("example/binary/jar/1.0");
            Path classes = root.resolve("classes");
            Files.createDirectories(dependency);
            Files.createDirectories(classes);
            for (int index = 0; index < size; ++index) {
                Files.writeString(dependency.resolve("File" + index + ".class"), "binary");
            }
            try (TjsPlaced placed = new TjsPlaced(root.resolve("placed.json"))) {
                long start = System.nanoTime();
                new Placing(placed, home, classes, Set.of("**"), Set.of(), true).exec();
                long placement = System.nanoTime() - start;
                if (placed.classes().size() != size) {
                    throw new AssertionError("Not all files were placed");
                }
                start = System.nanoTime();
                new Unplacing(placed, classes, Set.of()).exec();
                long unplacement = System.nanoTime() - start;
                if (Files.exists(classes)) {
                    try (var files = Files.walk(classes)) {
                        if (files.anyMatch(Files::isRegularFile)) {
                            throw new AssertionError("Files remain after unplacing");
                        }
                    }
                }
                System.out.printf("size=%d place_ms=%.3f unplace_ms=%.3f%n", size, placement / 1e6, unplacement / 1e6);
            }
        }
    }
}
