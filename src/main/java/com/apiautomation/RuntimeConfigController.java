package com.apiautomation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tells the UI whether to show local-only controls (e.g. Cursor API key input).
 * On Render, CURSOR_API_KEY is already in env — do not show the key field.
 */
@RestController
@RequestMapping("/api")
public class RuntimeConfigController {

    @GetMapping("/runtime-config")
    public Map<String, Object> runtimeConfig() {
        boolean serverHasCursorKey = hasEnv("CURSOR_API_KEY");
        // Show key input only for local/installer (no server key configured)
        boolean showCursorKeyInput = !serverHasCursorKey;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("showCursorKeyInput", showCursorKeyInput);
        out.put("aiKeyConfiguredOnServer", serverHasCursorKey);
        out.put("deploymentHint", serverHasCursorKey ? "cloud" : "local");
        return out;
    }

    private static boolean hasEnv(String name) {
        String v = System.getenv(name);
        return v != null && !v.isBlank();
    }
}
