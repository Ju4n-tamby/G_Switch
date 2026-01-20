package network;

import java.util.*;

/**
 * Utilitaire JSON minimaliste fait maison (sans librairie externe)
 * Supporte: String, Number, Boolean, null, Array, Object
 * 
 * Usage:
 *   String json = JsonUtils.builder()
 *       .put("type", "INPUT")
 *       .put("playerId", 1)
 *       .put("action", "GRAVITY_SWITCH")
 *       .build();
 *   
 *   Map<String, Object> data = JsonUtils.parse(json);
 */
public class JsonUtils {
    
    // ==================== BUILDER ====================
    
    public static JsonBuilder builder() {
        return new JsonBuilder();
    }
    
    public static class JsonBuilder {
        private final Map<String, Object> data = new LinkedHashMap<>();
        
        public JsonBuilder put(String key, Object value) {
            data.put(key, value);
            return this;
        }
        
        public JsonBuilder putArray(String key, List<?> array) {
            data.put(key, array);
            return this;
        }
        
        public JsonBuilder putObject(String key, Map<String, Object> obj) {
            data.put(key, obj);
            return this;
        }
        
        public String build() {
            return mapToJson(data);
        }
    }
    
    // ==================== SERIALISATION ====================
    
    public static String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            
            sb.append("\"").append(escapeString(entry.getKey())).append("\":");
            sb.append(valueToJson(entry.getValue()));
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    public static String valueToJson(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + escapeString((String) value) + "\"";
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof List) {
            return listToJson((List<?>) value);
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            return mapToJson(map);
        } else {
            return "\"" + escapeString(value.toString()) + "\"";
        }
    }
    
    public static String listToJson(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        
        for (Object item : list) {
            if (!first) sb.append(",");
            first = false;
            sb.append(valueToJson(item));
        }
        
        sb.append("]");
        return sb.toString();
    }
    
    private static String escapeString(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    // ==================== PARSING ====================
    
    public static Map<String, Object> parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        Parser parser = new Parser(json.trim());
        return parser.parseObject();
    }
    
    public static String getString(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultValue;
    }
    
    public static int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        try {
            return Integer.parseInt(val.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    public static double getDouble(Map<String, Object> map, String key, double defaultValue) {
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        try {
            return Double.parseDouble(val.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    public static boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object val = map.get(key);
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        if (val != null) {
            return Boolean.parseBoolean(val.toString());
        }
        return defaultValue;
    }
    
    @SuppressWarnings("unchecked")
    public static List<Object> getArray(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List) {
            return (List<Object>) val;
        }
        return new ArrayList<>();
    }
    
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getObject(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Map) {
            return (Map<String, Object>) val;
        }
        return new HashMap<>();
    }
    
    // ==================== PARSER INTERNE ====================
    
    private static class Parser {
        private final String json;
        private int pos = 0;
        
        Parser(String json) {
            this.json = json;
        }
        
        Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            
            if (pos >= json.length() || json.charAt(pos) != '{') {
                return map;
            }
            pos++; // skip '{'
            
            skipWhitespace();
            if (pos < json.length() && json.charAt(pos) == '}') {
                pos++;
                return map;
            }
            
            while (pos < json.length()) {
                skipWhitespace();
                
                // Parse key
                String key = parseString();
                
                skipWhitespace();
                if (pos >= json.length() || json.charAt(pos) != ':') break;
                pos++; // skip ':'
                
                skipWhitespace();
                
                // Parse value
                Object value = parseValue();
                map.put(key, value);
                
                skipWhitespace();
                if (pos >= json.length()) break;
                
                char c = json.charAt(pos);
                if (c == '}') {
                    pos++;
                    break;
                } else if (c == ',') {
                    pos++;
                }
            }
            
            return map;
        }
        
        List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++; // skip '['
            
            skipWhitespace();
            if (pos < json.length() && json.charAt(pos) == ']') {
                pos++;
                return list;
            }
            
            while (pos < json.length()) {
                skipWhitespace();
                list.add(parseValue());
                
                skipWhitespace();
                if (pos >= json.length()) break;
                
                char c = json.charAt(pos);
                if (c == ']') {
                    pos++;
                    break;
                } else if (c == ',') {
                    pos++;
                }
            }
            
            return list;
        }
        
        Object parseValue() {
            skipWhitespace();
            if (pos >= json.length()) return null;
            
            char c = json.charAt(pos);
            
            if (c == '"') {
                return parseString();
            } else if (c == '{') {
                return parseObject();
            } else if (c == '[') {
                return parseArray();
            } else if (c == 't' || c == 'f') {
                return parseBoolean();
            } else if (c == 'n') {
                return parseNull();
            } else if (c == '-' || Character.isDigit(c)) {
                return parseNumber();
            }
            
            return null;
        }
        
        String parseString() {
            if (pos >= json.length() || json.charAt(pos) != '"') {
                return "";
            }
            pos++; // skip opening quote
            
            StringBuilder sb = new StringBuilder();
            while (pos < json.length()) {
                char c = json.charAt(pos);
                if (c == '"') {
                    pos++;
                    break;
                } else if (c == '\\' && pos + 1 < json.length()) {
                    pos++;
                    char escaped = json.charAt(pos);
                    switch (escaped) {
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        default: sb.append(escaped);
                    }
                } else {
                    sb.append(c);
                }
                pos++;
            }
            return sb.toString();
        }
        
        Number parseNumber() {
            int start = pos;
            boolean isFloat = false;
            
            if (json.charAt(pos) == '-') pos++;
            
            while (pos < json.length()) {
                char c = json.charAt(pos);
                if (Character.isDigit(c)) {
                    pos++;
                } else if (c == '.' || c == 'e' || c == 'E') {
                    isFloat = true;
                    pos++;
                } else if (c == '+' || c == '-') {
                    pos++;
                } else {
                    break;
                }
            }
            
            String numStr = json.substring(start, pos);
            try {
                if (isFloat) {
                    return Double.parseDouble(numStr);
                } else {
                    return Long.parseLong(numStr);
                }
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        
        Boolean parseBoolean() {
            if (json.substring(pos).startsWith("true")) {
                pos += 4;
                return true;
            } else if (json.substring(pos).startsWith("false")) {
                pos += 5;
                return false;
            }
            return false;
        }
        
        Object parseNull() {
            if (json.substring(pos).startsWith("null")) {
                pos += 4;
            }
            return null;
        }
        
        void skipWhitespace() {
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
                pos++;
            }
        }
    }
}
