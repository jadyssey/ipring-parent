package org.ipring.jacg.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class JavaSourceScanUtils {

    private static final String JAVA_SUFFIX = ".java";

    private JavaSourceScanUtils() {
    }

    public static List<Path> findAllJavaRoots(Path root) throws Exception {
        List<Path> roots = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isDirectory)
                    .filter(path -> "java".equalsIgnoreCase(path.getFileName().toString()))
                    .forEach(path -> {
                        Path parent = path.getParent();
                        if (parent == null || parent.getFileName() == null) {
                            return;
                        }
                        if (!"main".equalsIgnoreCase(parent.getFileName().toString())) {
                            return;
                        }
                        Path grand = parent.getParent();
                        if (grand == null || grand.getFileName() == null) {
                            return;
                        }
                        if (!"src".equalsIgnoreCase(grand.getFileName().toString())) {
                            return;
                        }
                        String normalized = JavaParseTextUtils.normalizePathSlash(path.toString());
                        if (normalized.contains("/target/") || normalized.contains("/build/")) {
                            return;
                        }
                        roots.add(path.toAbsolutePath().normalize());
                    });
        }
        roots.sort(Comparator.comparing(Path::toString));
        return roots;
    }

    public static List<Path> collectJavaFiles(Path javaRoot) throws Exception {
        List<Path> javaFiles = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(javaRoot)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(JAVA_SUFFIX))
                    .forEach(javaFiles::add);
        }
        javaFiles.sort(Comparator.comparing(Path::toString));
        return javaFiles;
    }

    public static void collectJavaFiles(File root, List<File> output, List<String> skipDirNames) {
        if (root == null || !root.exists()) {
            return;
        }
        if (root.isDirectory()) {
            if (shouldSkipDir(root, skipDirNames)) {
                return;
            }
            for (File file : listFiles(root)) {
                collectJavaFiles(file, output, skipDirNames);
            }
            return;
        }
        if (root.getName().endsWith(JAVA_SUFFIX)) {
            output.add(root);
        }
    }

    public static boolean shouldSkipDir(File dir, List<String> skipDirNames) {
        String name = JavaParseTextUtils.trimToEmpty(dir.getName());
        for (String skip : skipDirNames) {
            if (Objects.equals(skip, name)) {
                return true;
            }
        }
        return false;
    }

    public static File[] listFiles(File dir) {
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return new File[0];
        }
        List<File> sorted = new ArrayList<>();
        java.util.Collections.addAll(sorted, files);
        sorted.sort(Comparator.comparing(File::getName));
        return sorted.toArray(new File[0]);
    }
}
