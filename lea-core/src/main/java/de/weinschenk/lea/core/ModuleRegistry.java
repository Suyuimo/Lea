package de.weinschenk.lea.core;

import de.weinschenk.lea.api.LeaModule;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

public class ModuleRegistry {

    private final Map<String, LeaModule> modules = new HashMap<>();

    public ModuleRegistry() {
        ServiceLoader<LeaModule> loader = ServiceLoader.load(LeaModule.class);
        for (LeaModule module : loader) {
            modules.put(module.id().toLowerCase(), module);
            System.out.println("[Lea] Loaded module: " + module.id());
        }
    }

    public Optional<LeaModule> get(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(modules.get(id.toLowerCase()));
    }
}
