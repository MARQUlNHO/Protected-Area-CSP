package me.marquinho.protectedareaserver.util;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

@SuppressWarnings("unchecked")
public class SimpleYaml {

    private Map<String, Object> data;

    public SimpleYaml() {
        this.data = new LinkedHashMap<>();
    }

    private SimpleYaml(Map<String, Object> data) {
        this.data = data != null ? data : new LinkedHashMap<>();
    }


    public static SimpleYaml load(File file) {
        if (!file.exists()) return new SimpleYaml();
        try (InputStream is = new FileInputStream(file)) {
            Yaml yaml = new Yaml();
            Map<String, Object> map = yaml.load(is);
            return new SimpleYaml(map);
        } catch (Exception e) {
            e.printStackTrace();
            return new SimpleYaml();
        }
    }

    public void save(File file) throws IOException {
        Files.createDirectories(file.getParentFile().toPath());
        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opts.setPrettyFlow(true);
        Yaml yaml = new Yaml(opts);
        try (Writer w = new FileWriter(file)) {
            yaml.dump(data, w);
        }
    }


    public boolean contains(String key) {
        return get(key) != null;
    }

    public Object getRaw(String key) {
        return get(key);
    }

    public String getString(String key) {
        return getString(key, null);
    }

    public String getString(String key, String def) {
        Object val = get(key);
        return val != null ? val.toString() : def;
    }

    public int getInt(String key) {
        return getInt(key, 0);
    }

    public int getInt(String key, int def) {
        Object val = get(key);
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    public boolean getBoolean(String key, boolean def) {
        Object val = get(key);
        if (val instanceof Boolean b) return b;
        if (val instanceof String s) return Boolean.parseBoolean(s);
        return def;
    }

    public List<String> getStringList(String key) {
        Object val = get(key);
        if (val instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object o : list) if (o != null) result.add(o.toString());
            return result;
        }
        return new ArrayList<>();
    }

    public SimpleYaml getSection(String key) {
        Object val = get(key);
        if (val instanceof Map<?, ?> map) {
            Map<String, Object> cast = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) cast.put(e.getKey().toString(), e.getValue());
            return new SimpleYaml(cast);
        }
        return null;
    }

    public Set<String> getKeys() {
        return new LinkedHashSet<>(data.keySet());
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }


    public void set(String key, Object value) {
        String[] parts = key.split("\\.", 2);
        if (parts.length == 1) {
            if (value == null) {
                data.remove(key);
            } else {
                data.put(key, value);
            }
        } else {
            Object child = data.get(parts[0]);
            Map<String, Object> childMap;
            if (child instanceof Map<?, ?> m) {
                childMap = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : m.entrySet()) childMap.put(e.getKey().toString(), e.getValue());
            } else {
                if (value == null) return;
                childMap = new LinkedHashMap<>();
            }
            new SimpleYaml(childMap).set(parts[1], value);
            if (value == null && childMap.isEmpty()) {
                data.remove(parts[0]);
            } else {
                data.put(parts[0], childMap);
            }
        }
    }


    private Object get(String key) {
        String[] parts = key.split("\\.", 2);
        Object val = data.get(parts[0]);
        if (parts.length == 1) return val;
        if (val instanceof Map<?, ?> map) {
            Map<String, Object> child = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) child.put(e.getKey().toString(), e.getValue());
            return new SimpleYaml(child).get(parts[1]);
        }
        return null;
    }
}
