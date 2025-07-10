package bit.commands;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class UndoCommand implements RunnableCommand {

    @Override
    public void run(String[] args) {
        if (args.length != 1 || (!args[0].equals("commit") && !args[0].equals("stage"))) {
            System.out.println("Usage: bit undo <commit|stage>");
            return;
        }

        switch (args[0]) {
            case "commit":
                undoLastCommit();
                break;
            case "stage":
                undoLastStage();
                break;
        }
    }

    private void undoLastCommit() {
        try {
            Path headPath = Paths.get(".bit/HEAD");
            String branch = Files.readString(headPath).trim().replace("refs/heads/", "");
            Path branchPath = Paths.get(".bit/refs/heads/" + branch);
            String currentCommit = Files.readString(branchPath).trim();

            Path logPath = Paths.get(".bit/logs/" + branch);
            if (!Files.exists(logPath)) {
                System.out.println("❌ No commit log found. Cannot undo.");
                return;
            }

            List<String> lines = Files.readAllLines(logPath);
            if (lines.size() < 2) {
                System.out.println("❌ Not enough history to undo.");
                return;
            }

            String previousCommit = lines.get(lines.size() - 2).split(" ")[0];
            Files.writeString(branchPath, previousCommit);
            Files.write(logPath, lines.subList(0, lines.size() - 1));

            System.out.println("✅ Reverted to previous commit: " + previousCommit);
        } catch (IOException e) {
            System.out.println("❌ Error undoing commit: " + e.getMessage());
        }
    }

    private void undoLastStage() {
        try {
            Path indexPath = Paths.get(".bit/index");
            if (!Files.exists(indexPath)) {
                System.out.println("❌ No staged files.");
                return;
            }

            List<String> lines = Files.readAllLines(indexPath);
            if (lines.isEmpty()) {
                System.out.println("❌ Index is already empty.");
                return;
            }

            Files.write(indexPath, lines.subList(0, lines.size() - 1));
            System.out.println("✅ Removed last staged file.");
        } catch (IOException e) {
            System.out.println("❌ Error undoing stage: " + e.getMessage());
        }
    }
}
