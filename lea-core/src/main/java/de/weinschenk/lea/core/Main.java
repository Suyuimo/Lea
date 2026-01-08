package de.weinschenk.lea.core;

import de.weinschenk.lea.api.CommandRequest;

import java.nio.file.Path;
import java.util.Optional;

public class Main {

    public static void main(String[] args) {
        var registry = new ModuleRegistry();
        var ctx = new CoreBotContext(Path.of("lea-test.txt"));

        // Fake incoming message (bis Signal dran ist)
        String sender = "ALLOWLIST_NUMMER_1";
        String text = "minecraft start";

        onIncomingMessage(registry, ctx, sender, Optional.empty(), text);
    }

    static void onIncomingMessage(
            ModuleRegistry registry,
            CoreBotContext ctx,
            String sender,
            Optional<String> groupId,
            String rawText
    ) {
        var parsedOpt = CommandParser.parse(rawText);
        if (parsedOpt.isEmpty()) {
            ctx.log("Ignored message (not a command): " + rawText);
            return;
        }

        var parsed = parsedOpt.get();
        var req = new CommandRequest(sender, groupId, parsed.command(), parsed.args(), rawText);

        registry.get(parsed.moduleId())
                .ifPresentOrElse(
                        module -> module.onCommand(req, ctx),
                        () -> ctx.reply(req, "Unknown module: " + parsed.moduleId())
                );
    }
}
