package bit.commands;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class MergeCommand implements RunnableCommand {

    @Override
    public void run(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: bit merge <branch>");
            return;
        }

        String currentBranch = getCurrentBranch();
        String targetBranch = args[0];

        Path currentPath = Paths.get(".bit/refs/heads/" + currentBranch);
        Path targetPath = Paths.get(".bit/refs/heads/" + targetBranch);

        if (!Files.exists(targetPath)) {
            System.out.println("❌ Branch '" + targetBranch + "' does not exist.");
            return;
        }

        try {
            String currentCommit = Files.readString(currentPath).trim();
            String targetCommit = Files.readString(targetPath).trim();

            Map<String, String> currentTree = readTree(currentCommit);
            Map<String, String> targetTree = readTree(targetCommit);

            Map<String, String> mergedTree = new HashMap<>(currentTree);

            boolean conflict = false;

            for (var entry : targetTree.entrySet()) {
                String file = entry.getKey();
                String newHash = entry.getValue();

                if (!currentTree.containsKey(file)) {
                    mergedTree.put(file, newHash); // added file
                } else if (!currentTree.get(file).equals(newHash)) {
                    // conflict: file changed in both branches
                    conflict = true;
                    System.out.println("⚠️ Conflict in file: " + file);
                    resolveConflict(file, currentTree.get(file), newHash, currentBranch, targetBranch);

                    mergedTree.put(file, Files.readString(Paths.get(".bit/objects/" + file + ".merge")).trim());
                }
            }

            if (conflict) {
                System.out.println("❗ Conflicts were resolved manually.");
            }

            // Save merged tree as new commit
            String mergedCommitHash = saveMergedTree(mergedTree, currentCommit, targetCommit);
            Files.writeString(currentPath, mergedCommitHash);

            System.out.println("✅ Merge complete. New commit: " + mergedCommitHash);

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private String getCurrentBranch() {
        try {
            return Files.readString(Paths.get(".bit/HEAD"))
                        .trim().replace("refs/heads/", "");
        } catch (IOException e) {
            throw new RuntimeException("Cannot read HEAD.");
        }
    }

    private Map<String, String> readTree(String commitHash) throws IOException {
        Path objectPath = Paths.get(".bit/objects/" + commitHash);
        if (!Files.exists(objectPath)) return new HashMap<>();

        Map<String, String> tree = new HashMap<>();
        List<String> lines = Files.readAllLines(objectPath);
        for (String line : lines) {
            if (!line.isBlank()) {
                String[] parts = line.split(" ");
                tree.put(parts[1], parts[0]);  // filename → hash
            }
        }
        return tree;
    }

    private void resolveConflict(String file, String baseHash, String otherHash, String currentBranch, String targetBranch) throws IOException {
    Path baseFile = Paths.get(".bit/objects/" + baseHash);
    Path otherFile = Paths.get(".bit/objects/" + otherHash);
    String baseContent = Files.readString(baseFile);
    String otherContent = Files.readString(otherFile);

    System.out.println("----- ⚔️ Conflict in " + file + " -----");
    System.out.println("[" + currentBranch + "] version:\n" + baseContent);
    System.out.println("[" + targetBranch + "] version:\n" + otherContent);

    System.out.println("Choose version to keep:");
    System.out.println("[1] " + currentBranch);
    System.out.println("[2] " + targetBranch);
    System.out.println("[3] Manual edit");

    Scanner sc = new Scanner(System.in);
    int choice = -1;
    try {
        choice = Integer.parseInt(sc.nextLine().trim());
    } catch (NumberFormatException ignored) {}

    String finalContent;

    switch (choice) {
        case 1:
            finalContent = baseContent;
            break;
        case 2:
            finalContent = otherContent;
            break;
        case 3:
            System.out.println("Enter manual resolution (end with --- on a new line):");
            StringBuilder builder = new StringBuilder();
            String line;
            while (!(line = sc.nextLine()).equals("---")) {
                builder.append(line).append("\n");
            }
            finalContent = builder.toString();
            break;
        default:
            finalContent = baseContent;
            break;
    }

    Path mergePath = Paths.get(".bit/objects/" + file + ".merge");
    Files.writeString(mergePath, finalContent);
}


    private String saveMergedTree(Map<String, String> tree, String parent1, String parent2) throws IOException {
        StringBuilder builder = new StringBuilder();
        for (var entry : tree.entrySet()) {
            builder.append(entry.getValue()).append(" ").append(entry.getKey()).append("\n");
        }

        String treeContent = builder.toString();
        String hash = Integer.toHexString(treeContent.hashCode());

        Path objectPath = Paths.get(".bit/objects/" + hash);
        Files.writeString(objectPath, treeContent);

        return hash;
    }
}
