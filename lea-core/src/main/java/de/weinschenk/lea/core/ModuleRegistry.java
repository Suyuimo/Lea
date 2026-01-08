package de.weinschenk.lea.core;

import de.weinschenk.lea.api.LeaModule;

import java.nio.file.Path;
import java.util.*;

public class ModuleRegistry {

    private final Map<String, LeaModule> modules = new HashMap<>();
    private final List<JarModuleLoader.LoadedJar> loadedJars = new ArrayList<>();

    public ModuleRegistry(Path modulesDir) {
        var loader = new JarModuleLoader();
        loadedJars.addAll(loader.loadFromDirectory(modulesDir));

        for (var jar : loadedJars) {
            for (LeaModule module : jar.modules()) {
                String id = module.id().toLowerCase();

                if (modules.containsKey(id)) {
                    System.out.println("[Lea] WARNING: duplicate module id '" + id
                            + "'; keeping first, ignoring from " + jar.jar().getFileName());
                    continue;
                }

                modules.put(id, module);
                System.out.println("[Lea] Loaded module: " + module.id() + " from " + jar.jar().getFileName());
            }
        }
    }

    public Optional<LeaModule> get(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(modules.get(id.toLowerCase()));
    }
}
