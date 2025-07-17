package io.github.anonventions.applydcu.config;

import java.io.*;
import java.util.*;

/**
 * Simple YAML parser that works without external dependencies
 * Supports basic YAML features needed for configuration
 */
public class SimpleYamlParser {
    
    public static Map<String, Object> parseYaml(File file) throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            List<String> currentPath = new ArrayList<>();
            
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                
                // Skip comments and empty lines
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                
                // Calculate indentation level
                int indent = getIndentLevel(line);
                
                // Adjust path based on indentation
                while (currentPath.size() > indent / 2) {
                    currentPath.remove(currentPath.size() - 1);
                }
                
                // Parse key-value pair
                int colonIndex = trimmed.indexOf(':');
                if (colonIndex > 0) {
                    String key = trimmed.substring(0, colonIndex).trim();
                    String value = trimmed.substring(colonIndex + 1).trim();
                    
                    if (value.isEmpty()) {
                        // This is a parent key
                        currentPath.add(key);
                    } else {
                        // This is a value
                        List<String> fullPath = new ArrayList<>(currentPath);
                        fullPath.add(key);
                        
                        // Parse value
                        Object parsedValue = parseValue(value);
                        setNestedValue(result, fullPath, parsedValue);
                    }
                }
                
                // Handle list items
                if (trimmed.startsWith("- ")) {
                    String value = trimmed.substring(2).trim();
                    Object parsedValue = parseValue(value);
                    
                    // Get or create list at current path
                    Object current = getNestedValue(result, currentPath);
                    List<Object> list;
                    if (current instanceof List) {
                        list = (List<Object>) current;
                    } else {
                        list = new ArrayList<>();
                        setNestedValue(result, currentPath, list);
                    }
                    list.add(parsedValue);
                }
            }
        }
        
        return result;
    }
    
    private static int getIndentLevel(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                count++;
            } else {
                break;
            }
        }
        return count;
    }
    
    private static Object parseValue(String value) {
        // Remove quotes if present
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
            (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        
        // Parse boolean
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        
        // Parse number
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException e) {
            // Not a number, return as string
        }
        
        return value;
    }
    
    @SuppressWarnings("unchecked")
    private static void setNestedValue(Map<String, Object> map, List<String> path, Object value) {
        Map<String, Object> current = map;
        
        for (int i = 0; i < path.size() - 1; i++) {
            String key = path.get(i);
            if (!current.containsKey(key)) {
                current.put(key, new LinkedHashMap<String, Object>());
            }
            current = (Map<String, Object>) current.get(key);
        }
        
        current.put(path.get(path.size() - 1), value);
    }
    
    @SuppressWarnings("unchecked")
    private static Object getNestedValue(Map<String, Object> map, List<String> path) {
        Object current = map;
        
        for (String key : path) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(key);
            } else {
                return null;
            }
        }
        
        return current;
    }
}