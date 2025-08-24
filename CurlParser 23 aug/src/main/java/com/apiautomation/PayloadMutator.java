package com.apiautomation;

import com.google.gson.*;
import java.util.*;

public class PayloadMutator {

    // Generate mutation rules based on value type
    public static Map<String, String> generateMutationRules(String payload) {
        Map<String, String> rules = new HashMap<>();
        try {
            JsonElement jsonElement = JsonParser.parseString(payload);
            collectMutationRules(jsonElement, rules);
        } catch (JsonSyntaxException e) {
            System.err.println("⚠ Invalid JSON, cannot generate mutation rules.");
        }
        return rules;
    }

    private static void collectMutationRules(JsonElement element, Map<String, String> rules) {
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();
                if (value.isJsonPrimitive()) {
                    JsonPrimitive primitive = value.getAsJsonPrimitive();
                    if (primitive.isString()) rules.put(key, "number");
                    else if (primitive.isNumber()) rules.put(key, "string");
                    else rules.put(key, "null"); // boolean or others
                } else {
                    collectMutationRules(value, rules);
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) {
                collectMutationRules(item, rules);
            }
        }
    }

    // Mutate payload values according to rules
    public static String mutatePayload(String payload, Map<String, String> mutationRules) {
        try {
            JsonElement jsonElement = JsonParser.parseString(payload);
            mutateElement(jsonElement, mutationRules);
            return new GsonBuilder().setPrettyPrinting().create().toJson(jsonElement);
        } catch (JsonSyntaxException e) {
            System.err.println("⚠ Invalid JSON, cannot mutate payload.");
            return payload;
        }
    }

    private static void mutateElement(JsonElement element, Map<String, String> mutationRules) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String key = entry.getKey();
                if (mutationRules.containsKey(key)) {
                    switch (mutationRules.get(key)) {
                        case "string": obj.addProperty(key, "mutated_string"); break;
                        case "number": obj.addProperty(key, 0); break;
                        case "null": obj.add(key, JsonNull.INSTANCE); break;
                    }
                } else {
                    mutateElement(entry.getValue(), mutationRules);
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) {
                mutateElement(item, mutationRules);
            }
        }
    }
}
