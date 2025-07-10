package bit.commands;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;

public class CommitCommand implements RunnableCommand {
    public void run(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: bit save \"commit message\"");
            return;
        }

        try {
            String index = Files.readString(Paths.get(".bit/index"));
            String commit = "tree:\n" + index +
                            "message: " + args[0] + "\n" +
                            "time: " + Instant.now() + "\n";

            String commitId = Integer.toHexString(commit.hashCode());
            Files.writeString(Paths.get(".bit/objects/" + commitId), commit);
            Files.writeString(Paths.get(".bit/refs/heads/main"), commitId);
            System.out.println("Commit saved with id: " + commitId);
        } catch (IOException e) {
            System.out.println("Commit failed: " + e.getMessage());
        }
    }
}
