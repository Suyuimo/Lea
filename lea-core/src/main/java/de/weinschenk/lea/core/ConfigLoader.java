package de.weinschenk.lea.core;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ConfigLoader {

    @SuppressWarnings("unchecked")
    public static LeaConfig load(Path configFile) {

        if (!Files.exists(configFile)) {
            throw new RuntimeException("Config file not found: " + configFile.toAbsolutePath());
        }

        try (InputStream in = Files.newInputStream(configFile)) {

            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(in);

            if (root == null || !root.containsKey("lea")) {
                throw new RuntimeException("Missing 'lea' root node in config");
            }

            // SnakeYAML → Map → Record Mapping
            Yaml mapper = new Yaml();
            String dumped = mapper.dump(root.get("lea"));

            return mapper.loadAs(dumped, LeaConfig.class);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load config: " + configFile, e);
        }
    }
}

