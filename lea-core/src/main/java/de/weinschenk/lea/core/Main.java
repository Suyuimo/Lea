package de.weinschenk.lea.core;

import de.weinschenk.lea.api.CommandRequest;
import de.weinschenk.lea.core.matrix.MatrixClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {

        // === Config laden ===
        Path configPath = Path.of("config", "lea.yml").normalize();
        LeaConfig config = ConfigLoader.load(configPath);
        log.info("Loaded config from {}", configPath.toAbsolutePath());

        // === Auth ===
        Authz authz = new Authz(config);

        // === Module laden ===
        Path modulesDir = Path.of(config.modulesDir()).normalize();
        ModuleRegistry registry = new ModuleRegistry(modulesDir);
        log.info("Modules directory: {}", modulesDir.toAbsolutePath());

        // === Matrix Client ===
        if (config.matrix() == null) {
            throw new IllegalStateException("Missing lea.matrix in config.");
        }
        MatrixClient matrix = new MatrixClient(
                config.matrix().homeserverUrl(),
                config.matrix().accessToken()
        );

        // === Bot Context ===
        CoreBotContext ctx = new CoreBotContext(Path.of("lea-test.txt"), matrix);

        // === Sync Loop starten ===
        matrix.startEventsLoop(in -> {
            // Eigene Nachrichten ignorieren
            if (config.matrix().userId().equals(in.sender())) {
                log.debug("Ignoring self-sent message from {}", in.sender());
                return;
            }

            log.info("Incoming message sender={} roomId={} text={}",
                    in.sender(), in.roomId(), in.message());

            onIncomingMessage(authz, registry, ctx, in.sender(), Optional.of(in.roomId()), in.message());
        });

        log.info("Lea is running. Listening for Matrix events…");

        // Prozess am Leben halten
        Thread.currentThread().join();
    }

    static void onIncomingMessage(
            Authz authz,
            ModuleRegistry registry,
            CoreBotContext ctx,
            String sender,
            Optional<String> roomId,
            String rawText
    ) {
        // === Authorization ===
        if (!authz.isAllowed(sender, roomId)) {
            log.warn("Unauthorized message ignored. sender={} roomId={}",
                    sender, roomId.orElse("<unknown>"));
            return;
        }

        // === Ping ===
        if (rawText.trim().equalsIgnoreCase("ping")) {
            ctx.reply(new CommandRequest(sender, roomId, "ping", List.of(), rawText), "Ja, ich bin online.");
            return;
        }

        // === Parsing ===
        var parsedOpt = CommandParser.parse(rawText);
        if (parsedOpt.isEmpty()) {
            log.debug("Ignored message (not a command): {}", rawText);
            return;
        }

        var parsed = parsedOpt.get();

        CommandRequest req = new CommandRequest(
                sender,
                roomId,
                parsed.command(),
                parsed.args(),
                rawText
        );

        // === Dispatch ===
        registry.get(parsed.moduleId())
                .ifPresentOrElse(
                        module -> {
                            log.info("Dispatching to module={} command={} args={}",
                                    parsed.moduleId(), parsed.command(), parsed.args());
                            module.onCommand(req, ctx);
                        },
                        () -> ctx.reply(req, "Unknown module: " + parsed.moduleId())
                );
    }
}
