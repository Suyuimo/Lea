package de.weinschenk.lea.api;

import java.util.List;
import java.util.Optional;

public record CommandRequest(
        String sender,
        Optional<String> groupId,
        String command,
        List<String> args,
        String rawMessage
) {}
