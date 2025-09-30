package com.apiautomation.Utils;

import com.google.gson.*;
import java.util.Random;

public class SqlInjectionHelper {

    private static final Random random = new Random();
    
    // SQL injection patterns for different field types (non-harmful for testing)
    private static final String[] STRING_INJECTIONS = {
        "test' OR '1'='1",
        "user' AND 1=1 --",
        "name' OR 1=1#",
        "value' OR 'x'='x",
        "data' OR 1=1 LIMIT 1 --",
        "input' OR 'a'='a",
        "field' OR 1=1 --"
    };
    
    private static final String[] EMAIL_INJECTIONS = {
        "test@example.com' OR '1'='1",
        "user@domain.com' OR 1=1#",
        "email@test.com' AND 1=1 --",
        "admin@test.com' OR 'a'='a",
        "user@example.com' OR 1=1 --"
    };
    
    private static final String[] NUMERIC_INJECTIONS = {
        "1 OR 1=1",
        "1' OR '1'='1",
        "1 AND 1=1",
        "1 OR 1=1#",
        "1' OR 1=1 --",
        "1' OR 'x'='x",
        "1 OR 'a'='a"
    };
    
    private static final String[] BOOLEAN_INJECTIONS = {
        "true OR 1=1",
        "false' OR '1'='1",
        "1 OR 1=1",
        "0' OR 1=1 --",
        "true OR 'a'='a",
        "false OR 1=1"
    };

    // Public helper to mutate JSON values with comprehensive SQL injection patterns
    public static String injectSqlIntoJsonValues(String body) {
        try {
            JsonElement root = JsonParser.parseString(body);
            mutateSqlValues(root);
            return new GsonBuilder().setPrettyPrinting().create().toJson(root);
        } catch (Exception e) {
            return body; // if not JSON, keep original
        }
    }

    private static void mutateSqlValues(JsonElement element) {
        if (element == null || element.isJsonNull()) return;

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (String key : obj.keySet()) {
                JsonElement value = obj.get(key);
                if (value != null && value.isJsonPrimitive()) {
                    String injection = getInjectionForField(key, value);
                    if (injection != null) {
                        obj.addProperty(key, injection);
                    }
                } else {
                    mutateSqlValues(value);
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (JsonElement item : arr) {
                mutateSqlValues(item);
            }
        }
    }
    
    // Determine appropriate SQL injection based on field name and value type
    private static String getInjectionForField(String key, JsonElement value) {
        String lowerKey = key.toLowerCase();
        
        // Email-specific injections
        if (lowerKey.contains("email") || lowerKey.contains("mail")) {
            return EMAIL_INJECTIONS[random.nextInt(EMAIL_INJECTIONS.length)];
        }
        
        // Numeric field injections
        if (lowerKey.contains("id") || lowerKey.contains("count") || lowerKey.contains("number") || 
            lowerKey.contains("age") || lowerKey.contains("price") || lowerKey.contains("amount")) {
            return NUMERIC_INJECTIONS[random.nextInt(NUMERIC_INJECTIONS.length)];
        }
        
        // Boolean field injections
        if (lowerKey.contains("active") || lowerKey.contains("enabled") || lowerKey.contains("status") ||
            lowerKey.contains("verified") || lowerKey.contains("confirmed")) {
            return BOOLEAN_INJECTIONS[random.nextInt(BOOLEAN_INJECTIONS.length)];
        }
        
        // Check if current value is numeric
        if (value.isJsonPrimitive()) {
            JsonPrimitive primitive = value.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                return NUMERIC_INJECTIONS[random.nextInt(NUMERIC_INJECTIONS.length)];
            }
        }
        
        // Default to string injections for other fields
        return STRING_INJECTIONS[random.nextInt(STRING_INJECTIONS.length)];
    }
    
    // Additional method for specific injection types
    public static String injectSpecificSqlType(String body, String injectionType) {
        try {
            JsonElement root = JsonParser.parseString(body);
            mutateSqlValuesWithType(root, injectionType);
            return new GsonBuilder().setPrettyPrinting().create().toJson(root);
        } catch (Exception e) {
            return body;
        }
    }
    
    private static void mutateSqlValuesWithType(JsonElement element, String injectionType) {
        if (element == null || element.isJsonNull()) return;

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (String key : obj.keySet()) {
                JsonElement value = obj.get(key);
                if (value != null && value.isJsonPrimitive()) {
                    String injection = getSpecificInjection(injectionType);
                    if (injection != null) {
                        obj.addProperty(key, injection);
                    }
                } else {
                    mutateSqlValuesWithType(value, injectionType);
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (JsonElement item : arr) {
                mutateSqlValuesWithType(item, injectionType);
            }
        }
    }
    
    private static String getSpecificInjection(String type) {
        switch (type.toLowerCase()) {
            case "union":
                return "1' UNION SELECT 'test','data' --";
            case "boolean":
                return "1' OR '1'='1";
            case "time":
                return "1' OR SLEEP(1) --";
            case "error":
                return "1' AND (SELECT COUNT(*) FROM information_schema.tables) --";
            case "stacked":
                return "1'; SELECT 'test' --";
            default:
                return STRING_INJECTIONS[random.nextInt(STRING_INJECTIONS.length)];
        }
    }
}
