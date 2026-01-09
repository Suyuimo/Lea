package de.weinschenk.lea.core;

import de.weinschenk.lea.api.CommandRequest;
import de.weinschenk.lea.core.signal.SignalEventParser;
import de.weinschenk.lea.core.signal.SignalRpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Optional;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {

        // === Config laden ===
        Path configPath = Path.of("..", "config", "lea.yml").normalize();
        LeaConfig config = ConfigLoader.load(configPath);
        log.info("Loaded config from {}", configPath.toAbsolutePath());

        // === Auth ===
        Authz authz = new Authz(config);

        // === Module laden ===
        Path modulesDir = Path.of(config.modulesDir()).normalize();
        ModuleRegistry registry = new ModuleRegistry(modulesDir);
        log.info("Modules directory: {}", modulesDir.toAbsolutePath());

        // === Signal Client ===
        if (config.signal() == null) {
            throw new IllegalStateException("Missing lea.signal in config.");
        }
        SignalRpcClient signal = new SignalRpcClient(
                config.signal().rpcUrl(),
                config.signal().eventsUrl(),
                config.signal().account()
        );

        // === Bot Context ===
        CoreBotContext ctx = new CoreBotContext(Path.of("lea-test.txt"), signal);

        // === Events loop starten ===
        signal.startEventsLoop(event -> {
            SignalEventParser.parseReceive(event).ifPresentOrElse(in -> {
                // Safety: ignore messages from ourselves (optional, aber praktisch)
                if (config.signal().account() != null && config.signal().account().equals(in.sender())) {
                    log.debug("Ignoring self-sent message from {}", in.sender());
                    return;
                }

                log.info("Incoming message sender={} groupId={} text={}",
                        in.sender(),
                        in.groupId().orElse("<dm>"),
                        in.message()
                );

                onIncomingMessage(authz, registry, ctx, in.sender(), in.groupId(), in.message());

            }, () -> log.debug("Ignored non-dataMessage receive event"));
        });

        log.info("Lea is running. Listening for Signal events…");

        // Prozess am Leben halten
        Thread.currentThread().join();
    }

    static void onIncomingMessage(
            Authz authz,
            ModuleRegistry registry,
            CoreBotContext ctx,
            String sender,
            Optional<String> groupId,
            String rawText
    ) {
        // === Authorization ===
        if (!authz.isAllowed(sender, groupId)) {
            log.warn("Unauthorized message ignored. sender={} groupId={}",
                    sender, groupId.orElse("<dm>"));
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
                groupId,
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
