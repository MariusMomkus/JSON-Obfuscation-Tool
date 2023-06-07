package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class JSONObfuscationTool {

    public static void main(String[] args) {
        try (InputStream inputStream = JSONObfuscationTool.class.getResourceAsStream("/input.json");
             OutputStream outputStream = new FileOutputStream(getResourceFilePath("output.json"));
             FileWriter mapFileWriter = new FileWriter(getResourceFilePath("map.txt"))) {

            // Read input JSON file
            String jsonData = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            // Obfuscate JSON and generate mapping
            JsonNode obfuscatedData = obfuscateJSON(jsonData);
            Map<String, String> mapping = generateMapping(obfuscatedData);

            // Write obfuscated JSON to output file
            writeJSONToFile(obfuscatedData, outputStream);

            // Write mapping to map file
            writeMappingToFile(mapping, mapFileWriter);

            System.out.println("JSON obfuscation completed successfully.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Obfuscates the input JSON by replacing field names and string values with their obfuscated counterparts. Returns the obfuscated JSON as a JsonNode.

    private static JsonNode obfuscateJSON(String jsonData) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode dataNode = mapper.readTree(jsonData);

        return obfuscateData(dataNode, mapper);
    }

    // Obfuscates the given JsonNode recursively. Replaces field names and string values with their obfuscated counterparts. Returns the obfuscated JsonNode.

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

    // Obfuscates the input string by replacing each character with its Unicode escape sequence representation. Returns the obfuscated string.

    private static String obfuscateString(String input) {
        StringBuilder obfuscatedString = new StringBuilder();
        for (char c : input.toCharArray()) {
            obfuscatedString.append(String.format("\\u%04x", (int) c));
        }
        return obfuscatedString.toString();
    }

    // Generates a mapping of original field names and their obfuscated counterparts from the obfuscated JSON. Returns the mapping as a Map.
    private static Map<String, String> generateMapping(JsonNode obfuscatedData) {
        Map<String, String> mapping = new HashMap<>();
        generateMappingRecursive(obfuscatedData, "", mapping);
        return mapping;
    }

    //Recursively generates a mapping of original field names and their obfuscated counterparts from the obfuscated JSON. Populates the mapping Map.

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


     //Writes the obfuscated JSON to the specified OutputStream.

    private static void writeJSONToFile(JsonNode data, OutputStream outputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        outputStream.write(json.getBytes());
    }

    // Writes the mapping of original field names and their obfuscated counterparts to the specified FileWriter.

    private static void writeMappingToFile(Map<String, String> mapping, FileWriter mapFileWriter) throws IOException {
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            mapFileWriter.write(entry.getKey() + " -> " + entry.getValue() + "\n");
        }
        mapFileWriter.close();
    }

     //Constructs the absolute file path of a resource file within the src/main/resources directory.

    private static String getResourceFilePath(String filename) throws IOException {
        Path resourceDirectory = Path.of("src", "main", "resources");
        return resourceDirectory.resolve(filename).toAbsolutePath().toString();
    }
}
