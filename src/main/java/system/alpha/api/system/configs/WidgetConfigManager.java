package system.alpha.api.system.configs;

import com.google.gson.*;
import lombok.Getter;
import system.alpha.api.system.backend.ClientInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class WidgetConfigManager {
    @Getter private static final WidgetConfigManager instance = new WidgetConfigManager();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path CONFIG_FILE;

    private final Map<String, Map<String, Object>> widgetConfigs = new HashMap<>();

    private WidgetConfigManager() {
        this.CONFIG_FILE = Paths.get(ClientInfo.CONFIG_PATH_OTHER + "/widgets.json");
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        if (!Files.exists(CONFIG_FILE)) {
            return;
        }

        try {
            String json = Files.readString(CONFIG_FILE);
            JsonObject root = GSON.fromJson(json, JsonObject.class);

            if (root != null) {
                for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                    String widgetName = entry.getKey();
                    JsonObject widgetConfig = entry.getValue().getAsJsonObject();

                    Map<String, Object> configMap = new HashMap<>();
                    for (Map.Entry<String, JsonElement> configEntry : widgetConfig.entrySet()) {
                        String key = configEntry.getKey();
                        JsonElement value = configEntry.getValue();
                        configMap.put(key, parseJsonValue(value));
                    }

                    widgetConfigs.put(widgetName, configMap);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            JsonObject root = new JsonObject();

            for (Map.Entry<String, Map<String, Object>> widgetEntry : widgetConfigs.entrySet()) {
                String widgetName = widgetEntry.getKey();
                Map<String, Object> configMap = widgetEntry.getValue();

                JsonObject widgetJson = new JsonObject();
                for (Map.Entry<String, Object> configEntry : configMap.entrySet()) {
                    String key = configEntry.getKey();
                    Object value = configEntry.getValue();

                    if (value instanceof Boolean) {
                        widgetJson.addProperty(key, (Boolean) value);
                    } else if (value instanceof Number) {
                        widgetJson.addProperty(key, (Number) value);
                    } else if (value instanceof String) {
                        widgetJson.addProperty(key, (String) value);
                    } else if (value instanceof Float) {
                        widgetJson.addProperty(key, (Float) value);
                    } else if (value instanceof Double) {
                        widgetJson.addProperty(key, (Double) value);
                    } else if (value instanceof Integer) {
                        widgetJson.addProperty(key, (Integer) value);
                    }
                }

                root.add(widgetName, widgetJson);
            }

            Files.writeString(CONFIG_FILE, GSON.toJson(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setValue(String widgetName, String key, Object value) {
        Map<String, Object> config = widgetConfigs.computeIfAbsent(widgetName, k -> new HashMap<>());
        config.put(key, value);
        save();
    }

    public Object getValue(String widgetName, String key, Object defaultValue) {
        Map<String, Object> config = widgetConfigs.get(widgetName);
        if (config == null) {
            return defaultValue;
        }
        return config.getOrDefault(key, defaultValue);
    }

    public boolean getBoolean(String widgetName, String key, boolean defaultValue) {
        Object value = getValue(widgetName, key, defaultValue);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    public int getInt(String widgetName, String key, int defaultValue) {
        Object value = getValue(widgetName, key, defaultValue);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public float getFloat(String widgetName, String key, float defaultValue) {
        Object value = getValue(widgetName, key, defaultValue);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        } else if (value instanceof String) {
            try {
                return Float.parseFloat((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public String getString(String widgetName, String key, String defaultValue) {
        Object value = getValue(widgetName, key, defaultValue);
        if (value instanceof String) {
            return (String) value;
        } else if (value != null) {
            return value.toString();
        }
        return defaultValue;
    }

    private Object parseJsonValue(JsonElement element) {
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            } else if (primitive.isNumber()) {
                Number number = primitive.getAsNumber();
                if (number.toString().contains(".")) {
                    return number.doubleValue();
                } else {
                    return number.intValue();
                }
            } else if (primitive.isString()) {
                return primitive.getAsString();
            }
        }
        return null;
    }
}