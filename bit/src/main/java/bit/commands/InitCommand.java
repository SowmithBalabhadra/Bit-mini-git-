package bit.commands;

import java.io.IOException;
import java.nio.file.*;

public class InitCommand implements RunnableCommand {
    public void run(String[] args) {
        try {
            Files.createDirectories(Paths.get(".bit/objects"));
            Files.createDirectories(Paths.get(".bit/refs/heads"));
            Files.writeString(Paths.get(".bit/HEAD"), "ref: refs/heads/main\n");
            System.out.println("Initialized empty Bit repository.");
        } catch (IOException e) {
            System.err.println("Init failed: " + e.getMessage());
        }
    }
}
