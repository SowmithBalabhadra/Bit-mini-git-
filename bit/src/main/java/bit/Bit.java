package bit;

import bit.commands.*;
import java.util.*;

public class Bit {
    private static final Map<String, RunnableCommand> commands = Map.ofEntries(
        Map.entry("init", new InitCommand()),
        Map.entry("add", new AddCommand()),
        Map.entry("commit", new CommitCommand()),
        Map.entry("status", new StatusCommand()),
        Map.entry("remote", new RemoteCommand()),
        Map.entry("push", new PushCommand()),
        Map.entry("merge", new MergeCommand()),
        Map.entry("undo", new UndoCommand()),
        Map.entry("pull", new PullCommand()
)
    );

    private static final Map<String, String> aliases = Map.ofEntries(
        Map.entry("start", "init"),
        Map.entry("stage", "add"),
        Map.entry("save", "commit"),
        Map.entry("check", "status"),
        Map.entry("upload", "push"),
        Map.entry("revert", "undo")
    );

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: bit <command> [args]");
            return;
        }

        String command = aliases.getOrDefault(args[0], args[0]);
        RunnableCommand runnable = commands.get(command);

        if (runnable != null) {
 runnable.run(Arrays.copyOfRange(args, 1, args.length));
        } else {
            System.out.println("❌ Unknown command: " + args[0]);
        }
    }
}
