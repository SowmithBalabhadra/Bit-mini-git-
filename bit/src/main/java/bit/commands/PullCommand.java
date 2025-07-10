package bit.commands;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import org.json.*;

public class PullCommand implements RunnableCommand {

    @Override
    public void run(String[] args) {
        try {
            // Step 1: Parse token and target folder
            String token = null;
            String targetDir = ".";

            for (String arg : args) {
                if (arg.startsWith("--token=")) {
                    token = arg.substring("--token=".length());
                } else if (!arg.startsWith("--")) {
                    targetDir = arg;
                }
            }

            if (token == null || token.isBlank()) {
                token = System.getenv("GITHUB_TOKEN");
            }
            if (token == null || token.isBlank()) {
                System.out.println("❌ GITHUB_TOKEN not set in environment or --token flag.");
                return;
            }

            // Step 2: Read remote config
            Path configPath = Paths.get(".bit/config");
            if (!Files.exists(configPath)) {
                System.out.println("❌ No remote config found.");
                return;
            }

            String remoteUrl = Files.readString(configPath).trim();
            if (!remoteUrl.startsWith("https://github.com/")) {
                System.out.println("❌ Invalid remote URL in config.");
                return;
            }

            String[] parts = remoteUrl.replace("https://github.com/", "").replace(".git", "").split("/");
            if (parts.length != 2) {
                System.out.println("❌ Remote URL format incorrect.");
                return;
            }

            String username = parts[0];
            String repo = parts[1];

            // Step 3: Download files recursively into the target folder
            pullDirectory(username, repo, "", token, targetDir);

            System.out.println("✅ Pull complete into: " + targetDir);

        } catch (Exception e) {
            System.out.println("❌ Pull failed: " + e.getMessage());
        }
    }

    private void pullDirectory(String username, String repo, String path, String token, String targetDir) throws IOException, JSONException {
        String apiUrl = String.format("https://api.github.com/repos/%s/%s/contents/%s", username, repo, URLEncoder.encode(path, "UTF-8"));
        JSONArray contents = getJsonArray(apiUrl, token);

        for (int i = 0; i < contents.length(); i++) {
            JSONObject file = contents.getJSONObject(i);
            String filePath = file.getString("path");
            String type = file.getString("type");

            Path targetPath = Paths.get(targetDir, filePath);

            if (type.equals("file")) {
                String downloadUrl = file.optString("download_url", null);
                if (downloadUrl == null) continue;

                Files.createDirectories(targetPath.getParent());
                try (InputStream in = new URL(downloadUrl).openStream()) {
                    Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("⬇️  Pulled: " + filePath);
                }
            } else if (type.equals("dir")) {
                pullDirectory(username, repo, filePath, token, targetDir);
            }
        }
    }

    private JSONArray getJsonArray(String apiUrl, String token) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            return new JSONArray(json.toString());
        }
    }
}