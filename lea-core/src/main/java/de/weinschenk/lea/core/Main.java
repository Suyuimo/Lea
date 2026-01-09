package de.weinschenk.lea.core;

import de.weinschenk.lea.api.CommandRequest;

import java.nio.file.Path;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        // === Config laden ===
        Path configPath = Path.of("..", "config", "lea.yml").normalize();
        LeaConfig config = ConfigLoader.load(configPath);

        Authz authz = new Authz(config);

        // === Module laden ===
        Path modulesDir = Path.of(config.modulesDir()).normalize();
        ModuleRegistry registry = new ModuleRegistry(modulesDir);

        // === Bot Context ===
        CoreBotContext ctx = new CoreBotContext(Path.of("lea-test.txt"));

        // === Fake Message (Debug) ===
        String sender = "ALLOWLIST_NUMMER_1";
        Optional<String> groupId = Optional.empty();
        String text = "minecraft start";

        onIncomingMessage(authz, registry, ctx, sender, groupId, text);
    }

    static void onIncomingMessage(
            Authz authz,
            ModuleRegistry registry,
            CoreBotContext ctx,
            String sender,
            Optional<String> groupId,
            String rawText
    ) {

        if (!authz.isAllowed(sender, groupId)) {
            ctx.log("Unauthorized message ignored. sender=" + sender +
                    " groupId=" + groupId.orElse("<dm>"));
            return;
        }

        var parsedOpt = CommandParser.parse(rawText);
        if (parsedOpt.isEmpty()) {
            ctx.log("Ignored message (not a command): " + rawText);
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

        registry.get(parsed.moduleId())
                .ifPresentOrElse(
                        module -> module.onCommand(req, ctx),
                        () -> ctx.reply(req, "Unknown module: " + parsed.moduleId())
                );
    }
}

