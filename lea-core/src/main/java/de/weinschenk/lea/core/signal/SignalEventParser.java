package de.weinschenk.lea.core.signal;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

public class SignalEventParser {

    public record Incoming(String sender, Optional<String> groupId, String message) {}

    public static Optional<Incoming> parseReceive(JsonNode event) {
        // event: {"method":"receive","params":{"envelope":{... "dataMessage":{"message":"...","groupInfo":{"groupId":"..."}}}}}
        JsonNode env = event.path("params").path("envelope");
        if (env.isMissingNode()) return Optional.empty();

        String sender = env.path("sourceNumber").asText(null);
        if (sender == null) sender = env.path("source").asText(null);

        JsonNode dm = env.path("dataMessage");
        if (dm.isMissingNode()) return Optional.empty();

        String msg = dm.path("message").asText(null);
        if (msg == null) return Optional.empty();

        // groupId kann je nach Version/Typ in groupInfo stecken
        String groupId = dm.path("groupInfo").path("groupId").asText(null);

        return Optional.of(new Incoming(sender, Optional.ofNullable(groupId), msg));
    }
}

