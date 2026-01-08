package de.weinschenk.lea.core;

import de.weinschenk.lea.api.LeaModule;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;

public class JarModuleLoader {

    public List<LoadedJar> loadFromDirectory(Path modulesDir) {
        if (!Files.exists(modulesDir)) {
            System.out.println("[Lea] modules dir does not exist: " + modulesDir.toAbsolutePath());
            return List.of();
        }

        List<Path> jars = listJars(modulesDir);
        if (jars.isEmpty()) {
            System.out.println("[Lea] no module jars found in: " + modulesDir.toAbsolutePath());
            return List.of();
        }

        List<LoadedJar> loaded = new ArrayList<>();
        for (Path jar : jars) {
            try {
                loaded.add(loadSingleJar(jar));
            } catch (Exception e) {
                System.out.println("[Lea] failed to load module jar: " + jar.getFileName());
                e.printStackTrace(System.out);
            }
        }
        return loaded;
    }

    private List<Path> listJars(Path modulesDir) {
        try (var stream = Files.list(modulesDir)) {
            return stream
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".jar"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to list modules dir: " + modulesDir, e);
        }
    }

    private LoadedJar loadSingleJar(Path jarPath) throws IOException {
        URL jarUrl = jarPath.toUri().toURL();

        // Parent = Core classloader (kennt lea-api)
        URLClassLoader cl = new URLClassLoader(new URL[]{jarUrl}, this.getClass().getClassLoader());

        ServiceLoader<LeaModule> sl = ServiceLoader.load(LeaModule.class, cl);

        List<LeaModule> modules = new ArrayList<>();
        for (LeaModule m : sl) {
            modules.add(m);
        }

        if (modules.isEmpty()) {
            cl.close();
            throw new IllegalStateException("No LeaModule found via ServiceLoader in " + jarPath.getFileName());
        }

        return new LoadedJar(jarPath, cl, modules);
    }

    public record LoadedJar(Path jar, URLClassLoader classLoader, List<LeaModule> modules) {}
}
