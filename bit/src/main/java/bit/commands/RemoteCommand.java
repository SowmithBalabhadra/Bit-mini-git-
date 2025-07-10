package bit.commands;

import java.io.IOException;
import java.nio.file.*;

public class RemoteCommand implements RunnableCommand {
    public void run(String[] args) {
        if (args.length != 3 || !args[0].equals("add") || !args[1].equals("origin")) {
            System.out.println("Usage: bit remote add origin <url>");
            return;
        }

        try {
            Files.writeString(Paths.get(".bit/config"), args[2]);
            System.out.println("Remote origin set to: " + args[2]);
        } catch (IOException e) {
            System.out.println("Failed to set origin: " + e.getMessage());
        }
    }
}
