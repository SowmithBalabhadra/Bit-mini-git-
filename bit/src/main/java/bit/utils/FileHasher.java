package bit.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileHasher {
    public static String hashFile(Path path) throws IOException {
        byte[] content = Files.readAllBytes(path);
        return Integer.toHexString(new String(content).hashCode());
    }
}
