package de.weinschenk.lea.core;

import de.weinschenk.lea.api.LeaModule;

import java.nio.file.Path;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ModuleRegistry {

    private static final Logger log = LoggerFactory.getLogger(ModuleRegistry.class);    

    private final Map<String, LeaModule> modules = new HashMap<>();
    private final List<JarModuleLoader.LoadedJar> loadedJars = new ArrayList<>();

    public ModuleRegistry(Path modulesDir) {
        var loader = new JarModuleLoader();
        loadedJars.addAll(loader.loadFromDirectory(modulesDir));

        for (var jar : loadedJars) {
            for (LeaModule module : jar.modules()) {
                String id = module.id().toLowerCase();

                if (modules.containsKey(id)) {
                    log.warn("Duplicate module id '{}', ignoring {}", id, jar.jar().getFileName());
                    continue;
                }

                modules.put(id, module);
                log.info("Loaded module '{}' from {}", module.id(), jar.jar().getFileName());
            }
        }
    }

    public Optional<LeaModule> get(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(modules.get(id.toLowerCase()));
    }
}
