package de.weinschenk.lea.core;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ConfigLoader {

    @SuppressWarnings("unchecked")
    public static LeaConfig load(Path configFile) {
        if (!Files.exists(configFile)) {
            throw new RuntimeException("Config file not found: " + configFile.toAbsolutePath());
        }

        try (InputStream in = Files.newInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Object rootObj = yaml.load(in);

            if (!(rootObj instanceof Map<?, ?> root)) {
                throw new RuntimeException("Invalid YAML root (expected mapping).");
            }

            Object leaObj = root.get("lea");
            if (!(leaObj instanceof Map<?, ?> lea)) {
                throw new RuntimeException("Missing or invalid 'lea' section in config.");
            }

            // ---- lea.modulesDir ----
            String modulesDir = asString(lea.get("modulesDir"), "lea.modulesDir");

            // ---- lea.allowlist ----
            Object allowObj = lea.get("allowlist");
            if (!(allowObj instanceof Map<?, ?> allow)) {
                throw new RuntimeException("Missing or invalid 'lea.allowlist' section.");
            }

            Set<String> senders = asStringSet(allow.get("senders"), "lea.allowlist.senders");
            Set<String> groups  = asStringSet(allow.get("groups"), "lea.allowlist.groups");
            LeaConfig.Allowlist allowlist = new LeaConfig.Allowlist(senders, groups);

            // ---- lea.matrix ----
            Object matrixObj = lea.get("matrix");
            if (!(matrixObj instanceof Map<?, ?> matrix)) {
                throw new RuntimeException("Missing or invalid 'lea.matrix' section.");
            }

            String homeserverUrl = asString(matrix.get("homeserverUrl"), "lea.matrix.homeserverUrl");
            String accessToken   = asString(matrix.get("accessToken"),   "lea.matrix.accessToken");
            String userId        = asString(matrix.get("userId"),         "lea.matrix.userId");
            LeaConfig.Matrix matrixCfg = new LeaConfig.Matrix(homeserverUrl, accessToken, userId);

            // ---- final ----
            return new LeaConfig(modulesDir, allowlist, matrixCfg);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load config: " + configFile, e);
        }
    }

    private static String asString(Object v, String path) {
        if (v == null) throw new RuntimeException("Missing config key: " + path);
        if (!(v instanceof String s) || s.isBlank()) {
            throw new RuntimeException("Invalid config value at " + path + " (expected non-empty string).");
        }
        return s;
    }

    private static Set<String> asStringSet(Object v, String path) {
        if (v == null) return Set.of(); // allow empty lists
        if (!(v instanceof List<?> list)) {
            throw new RuntimeException("Invalid config value at " + path + " (expected list).");
        }

        Set<String> out = new LinkedHashSet<>();
        for (Object item : list) {
            if (!(item instanceof String s) || s.isBlank()) {
                throw new RuntimeException("Invalid entry in " + path + " (expected non-empty string).");
            }
            out.add(s);
        }
        return Collections.unmodifiableSet(out);
    }
}

