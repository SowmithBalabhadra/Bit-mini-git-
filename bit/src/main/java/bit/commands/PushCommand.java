package bit.commands;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Base64;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
public class PushCommand implements RunnableCommand {

    @Override
    public void run(String[] args) {
        try {
            // Step 0: Get token from CLI or environment
            String token = null;
            for (String arg : args) {
                if (arg.startsWith("--token=")) {
                    token = arg.substring("--token=".length());
                    break;
                }
            }
            if (token == null || token.isBlank()) {
                token = System.getenv("GITHUB_TOKEN");
            }
            if (token == null || token.isBlank()) {
                System.out.println("❌ Error: GitHub token is not provided via --token or GITHUB_TOKEN env variable.");
                return;
            }

            // Step 1: Read the remote origin from config
            String remoteUrl = Files.readString(Paths.get(".bit/config")).trim();
            if (!remoteUrl.startsWith("https://github.com/") || !remoteUrl.endsWith(".git")) {
                System.out.println("Invalid remote URL in .bit/config");
                return;
            }

            // Step 2: Parse username and repo
            String[] parts = remoteUrl.replace("https://github.com/", "").replace(".git", "").split("/");
            if (parts.length != 2) {
                System.out.println("Remote URL parsing failed.");
                return;
            }
            String username = parts[0];
            String repo = parts[1];

            // Step 3: Read index file for all staged files
            Path indexPath = Paths.get(".bit/index");
            if (!Files.exists(indexPath)) {
                System.out.println("Nothing to upload. No index found.");
                return;
            }

            // Step 4: Upload each file using GitHub API
            for (String line : Files.readAllLines(indexPath)) {
                String[] split = line.trim().split(" ", 2);
                if (split.length != 2) continue;

                String hash = split[0];
                String filePath = split[1];
                Path objectPath = Paths.get(".bit/objects/" + hash);
                if (!Files.exists(objectPath)) continue;

                byte[] contentBytes = Files.readAllBytes(objectPath);
                String contentBase64 = Base64.getEncoder().encodeToString(contentBytes);

              String cleanedPath = filePath.replace("\\", "/").replaceFirst("^\\./", ""); // remove leading ./ if any
              String encodedPath = URLEncoder.encode(cleanedPath, StandardCharsets.UTF_8)
                               .replace("+", "%20"); // GitHub prefers %20 over +


                String apiUrl = String.format(
                    "https://api.github.com/repos/%s/%s/contents/%s",
                    username,
                    repo,
                    encodedPath
                );
                uploadToGitHub(apiUrl, token, contentBase64, "bit upload: " + filePath);
            }

            System.out.println("✅ Upload complete!");
        } catch (IOException e) {
            System.out.println("Upload failed: " + e.getMessage());
        }
    }

private void uploadToGitHub(String apiUrl, String token, String base64Content, String message) throws IOException {
    // Step 1: Check if file exists to get its SHA
    String existingSha = null;
    HttpURLConnection checkConn = (HttpURLConnection) new URL(apiUrl).openConnection();
    checkConn.setRequestMethod("GET");
    checkConn.setRequestProperty("Authorization", "Bearer " + token);
    checkConn.setRequestProperty("Accept", "application/vnd.github.v3+json");

    int checkResponseCode = checkConn.getResponseCode();
    if (checkResponseCode == 200) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(checkConn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            String json = sb.toString();

            int shaIndex = json.indexOf("\"sha\":\"");
            if (shaIndex != -1) {
                int shaStart = shaIndex + 7;
                int shaEnd = json.indexOf("\"", shaStart);
                existingSha = json.substring(shaStart, shaEnd);
            }
        }
    }

    // Step 2: Upload with PUT and optional SHA
    HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
    conn.setRequestMethod("PUT");
    conn.setRequestProperty("Authorization", "Bearer " + token);
    conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setDoOutput(true);

    String jsonBody = String.format(
        "{\n" +
        "  \"message\": \"%s\",\n" +
        "  \"content\": \"%s\"%s\n" +
        "}",
        message,
        base64Content,
        (existingSha != null ? ",\n  \"sha\": \"" + existingSha + "\"" : "")
    );

    try (OutputStream os = conn.getOutputStream()) {
        os.write(jsonBody.getBytes());
    }

    int responseCode = conn.getResponseCode();
    if (responseCode == 201 || responseCode == 200) {
        System.out.println("✔️ Uploaded: " + apiUrl);
    } else {
        String error = new String(conn.getErrorStream().readAllBytes());
        System.out.println("❌ Failed to upload " + apiUrl + ": " + error);
    }
}

}
