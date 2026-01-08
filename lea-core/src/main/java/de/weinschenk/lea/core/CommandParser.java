package de.weinschenk.lea.core;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class CommandParser {

    private CommandParser() {}

    public record ParsedCommand(String moduleId, String command, List<String> args) {}

    public static Optional<ParsedCommand> parse(String message) {
        if (message == null) return Optional.empty();
        String trimmed = message.trim();
        if (trimmed.isEmpty()) return Optional.empty();

        String[] parts = trimmed.split("\\s+");
        if (parts.length < 2) return Optional.empty();

        String moduleId = parts[0].toLowerCase();
        String command = parts[1].toLowerCase();

        List<String> args = (parts.length > 2)
                ? Arrays.asList(Arrays.copyOfRange(parts, 2, parts.length))
                : List.of();

        return Optional.of(new ParsedCommand(moduleId, command, args));
    }
}

