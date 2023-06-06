package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;



public class JSONObfuscationTool {

    public static void main(String[] args) {
        String inputFilePath = "./input.json";
        String outputFilePath = "org/example/output.json";
        String mapFilePath = "org/example/map.txt";
//        String inputFilePath = "C:/Users/Marius.Momkus/IdeaProjects/ToolT/src/main/java/org/example/data/input.json";
//        String outputFilePath = "C:/Users/Marius.Momkus/IdeaProjects/ToolT/src/main/java/org/example/data/output.json";
//        String mapFilePath = "C:/Users/Marius.Momkus/IdeaProjects/ToolT/src/main/java/org/example/data/map.txt";
        try {
            // Read input JSON file
            String jsonData = new String(Files.readAllBytes(Paths.get(inputFilePath)), StandardCharsets.UTF_8);

            // Obfuscate JSON and generate mapping
            JsonNode obfuscatedData = obfuscateJSON(jsonData);
            Map<String, String> mapping = generateMapping(obfuscatedData);

            // Write obfuscated JSON to output file
            writeJSONToFile(obfuscatedData, outputFilePath);

            // Write mapping to map file
            writeMappingToFile(mapping, mapFilePath);

            System.out.println("JSON obfuscation completed successfully.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static JsonNode obfuscateJSON(String jsonData) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode dataNode = mapper.readTree(jsonData);

        return obfuscateData(dataNode, mapper);
    }

    private static JsonNode obfuscateData(JsonNode data, ObjectMapper mapper) {
        if (data.isObject()) {
            Map<String, JsonNode> obfuscatedData = new HashMap<>();
            data.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();

                String obfuscatedKey = obfuscateString(key);
                JsonNode obfuscatedValue = obfuscateData(value, mapper);

                obfuscatedData.put(obfuscatedKey, obfuscatedValue);
            });

            return mapper.valueToTree(obfuscatedData);

        } else if (data.isArray()) {
            JsonNode[] obfuscatedData = new JsonNode[data.size()];
            for (int i = 0; i < data.size(); i++) {
                JsonNode element = data.get(i);
                obfuscatedData[i] = obfuscateData(element, mapper);
            }

            return mapper.valueToTree(obfuscatedData);

        } else if (data.isTextual()) {
            String obfuscatedString = obfuscateString(data.asText());
            return mapper.valueToTree(obfuscatedString);
        }

        return data;
    }

    private static String obfuscateString(String input) {
        StringBuilder obfuscatedString = new StringBuilder();
        for (char c : input.toCharArray()) {
            obfuscatedString.append(String.format("\\u%04x", (int) c));
        }
        return obfuscatedString.toString();
    }

    private static Map<String, String> generateMapping(JsonNode obfuscatedData) {
        Map<String, String> mapping = new HashMap<>();
        generateMappingRecursive(obfuscatedData, "", mapping);
        return mapping;
    }

    private static void generateMappingRecursive(JsonNode data, String prefix, Map<String, String> mapping) {
        if (data.isObject()) {
            data.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();

                String newPrefix = prefix.isEmpty() ? key : prefix + "." + key;
                generateMappingRecursive(value, newPrefix, mapping);
            });

        } else if (data.isArray()) {
            for (int i = 0; i < data.size(); i++) {
                JsonNode element = data.get(i);
                String newPrefix = prefix + "[" + i + "]";
                generateMappingRecursive(element, newPrefix, mapping);
            }

        } else if (data.isTextual()) {
            mapping.put(prefix, data.asText());
        }
    }

    private static void writeJSONToFile(JsonNode data, String filePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(json);
        }
    }

    private static void writeMappingToFile(Map<String, String> mapping, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            for (Map.Entry<String, String> entry : mapping.entrySet()) {
                writer.write(entry.getKey() + " -> " + entry.getValue() + "\n");
            }
        }
    }
}
