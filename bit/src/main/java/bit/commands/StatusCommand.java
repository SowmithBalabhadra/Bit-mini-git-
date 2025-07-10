package bit.commands;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import bit.utils.FileHasher;

public class StatusCommand implements RunnableCommand {

    @Override
    public void run(String[] args) {
        try {
            Set<String> staged = new HashSet<>();
            Set<String> committed = new HashSet<>();
            Map<String, String> stagedHashes = new HashMap<>();

            // Read staged files from index
            Path indexPath = Paths.get(".bit/index");
            if (Files.exists(indexPath)) {
                for (String line : Files.readAllLines(indexPath)) {
                    String[] parts = line.split(" ", 2);
                    if (parts.length == 2) {
                        staged.add(parts[1]);
                        stagedHashes.put(parts[1], parts[0]);
                    }
                }
            }

            // Resolve HEAD to commit hash
            Path headPath = Paths.get(".bit/HEAD");
            if (Files.exists(headPath)) {
                String headContent = Files.readString(headPath).trim();
                if (headContent.startsWith("ref: ")) {
                    String refPathStr = headContent.substring(5); // remove "ref: "
                    Path refPath = Paths.get(".bit", refPathStr);
                    if (Files.exists(refPath)) {
                        headContent = Files.readString(refPath).trim(); // actual commit hash
                    } else {
                        System.out.println("❌ HEAD ref missing: " + refPath);
                        return;
                    }
                }

                if (headContent.length() >= 40) {
                    String dir = headContent.substring(0, 2);
                    String file = headContent.substring(2);
                    Path commitPath = Paths.get(".bit", "objects", dir, file);

                    if (Files.exists(commitPath)) {
                        List<String> lines = Files.readAllLines(commitPath);
                        boolean treeSection = false;
                        for (String line : lines) {
                            if (line.startsWith("tree")) {
                                treeSection = true;
                            } else if (treeSection && line.contains(" ")) {
                                String[] parts = line.trim().split(" ", 2);
                                if (parts.length == 2) {
                                    committed.add(parts[1]);
                                }
                            }
                        }
                    }
                }
            }

            // Scan working directory
            List<Path> allFiles = Files.walk(Paths.get("."))
                    .filter(p -> !Files.isDirectory(p))
                    .filter(p -> !p.startsWith(Paths.get(".bit")))
                    .collect(Collectors.toList());

            List<String> modified = new ArrayList<>();
            List<String> untracked = new ArrayList<>();

            for (Path file : allFiles) {
                String path = file.toString().replace("\\", "/");
                String currentHash = FileHasher.hashFile(file);

                if (staged.contains(path)) {
                    String stagedHash = stagedHashes.get(path);
                    if (!stagedHash.equals(currentHash)) {
                        modified.add(path);
                    }
                } else if (!committed.contains(path)) {
                    untracked.add(path);
                }
            }

            // Print output
            System.out.println("📦 Bit Status\n");

            if (!staged.isEmpty()) {
                System.out.println("🟢 Staged files:");
                staged.forEach(f -> System.out.println("   + " + f));
            }

            if (!modified.isEmpty()) {
                System.out.println("\n🟡 Modified (not staged):");
                modified.forEach(f -> System.out.println("   ~ " + f));
            }

            if (!untracked.isEmpty()) {
                System.out.println("\n🔴 Untracked files:");
                untracked.forEach(f -> System.out.println("   ? " + f));
            }

            if (staged.isEmpty() && modified.isEmpty() && untracked.isEmpty()) {
                System.out.println("✅ Working directory clean!");
            }

        } catch (IOException e) {
            System.out.println("❌ Error checking status: " + e.getMessage());
        }
    }
}
