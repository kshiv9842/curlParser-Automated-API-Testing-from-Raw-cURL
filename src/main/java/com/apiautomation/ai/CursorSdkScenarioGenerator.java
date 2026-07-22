package com.apiautomation.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Calls the local Node AI Agents bridge. Falls back to empty if unavailable.
 */
public final class CursorSdkScenarioGenerator implements AiScenarioGenerator {

    private final Gson gson = new Gson();
    private final Path bridgeScript;
    private final String apiKeyOverride;

    public CursorSdkScenarioGenerator() {
        this(null);
    }

    public CursorSdkScenarioGenerator(String apiKeyOverride) {
        this.bridgeScript = resolveBridgeScript();
        this.apiKeyOverride = apiKeyOverride;
    }

    @Override
    public GenerationOutcome propose(ContextPack pack) {
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return new GenerationOutcome(List.of(), "AI_AGENT",
                    "AI agent key not configured — paste Cursor API key in the UI or set CURSOR_API_KEY");
        }
        if (bridgeScript == null || !Files.isRegularFile(bridgeScript)) {
            return new GenerationOutcome(List.of(), "AI_AGENT", "AI bridge script not found");
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("node", bridgeScript.toAbsolutePath().toString());
            pb.environment().put("CURSOR_API_KEY", apiKey);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(gson.toJson(pack));
                writer.flush();
            }

            boolean finished = process.waitFor(180, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new GenerationOutcome(List.of(), "AI_AGENT", "AI agent bridge timed out");
            }

            StringBuilder out = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line).append('\n');
                }
            }

            if (process.exitValue() != 0) {
                return new GenerationOutcome(List.of(), "AI_AGENT",
                        "AI bridge exit " + process.exitValue() + ": " + trim(out.toString(), 400));
            }

            List<AiScenarioSpec> specs = parseScenarios(out.toString());
            return new GenerationOutcome(specs, "AI_AGENT", "AI Agents proposed " + specs.size() + " scenarios");
        } catch (Exception e) {
            return new GenerationOutcome(List.of(), "AI_AGENT", "AI bridge error: " + e.getMessage());
        }
    }

    private String resolveApiKey() {
        if (apiKeyOverride != null && !apiKeyOverride.isBlank()) {
            return apiKeyOverride.trim();
        }
        String env = System.getenv("CURSOR_API_KEY");
        return env == null ? "" : env.trim();
    }

    private List<AiScenarioSpec> parseScenarios(String raw) {
        String json = extractJson(raw);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray arr = root.has("scenarios") ? root.getAsJsonArray("scenarios") : new JsonArray();
        List<AiScenarioSpec> list = new ArrayList<>();
        for (JsonElement el : arr) {
            list.add(gson.fromJson(el, AiScenarioSpec.class));
        }
        return list;
    }

    private static String extractJson(String raw) {
        String t = raw.trim();
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return t.substring(start, end + 1);
        }
        return t;
    }

    private static Path resolveBridgeScript() {
        Path p1 = Path.of("ai-bridge", "generate.mjs");
        if (Files.isRegularFile(p1)) {
            return p1.toAbsolutePath();
        }
        Path p2 = Path.of(System.getProperty("user.dir"), "ai-bridge", "generate.mjs");
        if (Files.isRegularFile(p2)) {
            return p2;
        }
        // when running from jar in target/
        Path p3 = Path.of("..", "ai-bridge", "generate.mjs");
        if (Files.isRegularFile(p3)) {
            return p3.toAbsolutePath().normalize();
        }
        return p1;
    }

    private static String trim(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
