package bit.commands;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.stream.Collectors;

public class AddCommand implements RunnableCommand {

    private Set<PathMatcher> ignoreMatchers;

    @Override
    public void run(String[] args) {
        try {
            loadIgnorePatterns();

            List<Path> filesToAdd = new ArrayList<>();

            if (args.length == 0 || (args.length == 1 && args[0].equals("."))) {
                // Add all files recursively
                Files.walk(Paths.get("."))
                        .filter(Files::isRegularFile)
                        .filter(path -> !isIgnored(path))
                        .filter(path -> !path.startsWith(".bit")) // Don't add internal files
                        .forEach(filesToAdd::add);
            } else {
                // Add specific files
                for (String fileArg : args) {
                    Path path = Paths.get(fileArg);
                    if (Files.isRegularFile(path) && !isIgnored(path)) {
                        filesToAdd.add(path);
                    }
                }
            }

            // Prepare .bit structure
            Files.createDirectories(Paths.get(".bit/objects"));
            Path indexPath = Paths.get(".bit/index");
            if (!Files.exists(indexPath)) {
                Files.createFile(indexPath);
            }

            try (BufferedWriter indexWriter = Files.newBufferedWriter(indexPath, StandardOpenOption.APPEND)) {
                for (Path file : filesToAdd) {
                    byte[] content = Files.readAllBytes(file);
                    String hash = hash(content);

                    Path objectPath = Paths.get(".bit/objects", hash);
                    Files.write(objectPath, content); // Save object file

                    String filePath = file.toString().replace("\\", "/"); // Normalize path

                    indexWriter.write(hash + " " + filePath);
                    indexWriter.newLine();

                    System.out.println("➕ Staged: " + filePath);
                }
            }

        } catch (IOException e) {
            System.out.println("❌ Error while adding files: " + e.getMessage());
        }
    }

    private String hash(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hashed = digest.digest(content);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 not available");
        }
    }

    private void loadIgnorePatterns() {
        ignoreMatchers = new HashSet<>();
        Path ignorePath = Paths.get(".bitignore");

        if (!Files.exists(ignorePath)) return;

        try {
            List<String> patterns = Files.readAllLines(ignorePath).stream()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .collect(Collectors.toList());

            for (String pattern : patterns) {
                String glob = "glob:" + pattern;
                ignoreMatchers.add(FileSystems.getDefault().getPathMatcher(glob));
            }
        } catch (IOException e) {
            System.out.println("⚠️ Failed to read .bitignore: " + e.getMessage());
        }
    }

    private boolean isIgnored(Path path) {
        Path relativePath = Paths.get("").toAbsolutePath().relativize(path.toAbsolutePath());
        for (PathMatcher matcher : ignoreMatchers) {
            if (matcher.matches(relativePath)) {
                return true;
            }
        }
        return false;
    }
}
