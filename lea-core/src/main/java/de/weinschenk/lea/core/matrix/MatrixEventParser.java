package de.weinschenk.lea.core.matrix;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public class MatrixEventParser {

    public record Incoming(String sender, String roomId, String message) {}

    public static List<Incoming> parseSync(JsonNode syncResponse) {
        List<Incoming> result = new ArrayList<>();

        JsonNode joinedRooms = syncResponse.path("rooms").path("join");
        if (joinedRooms.isMissingNode()) return result;

        joinedRooms.fields().forEachRemaining(roomEntry -> {
            String roomId = roomEntry.getKey();
            JsonNode events = roomEntry.getValue().path("timeline").path("events");

            for (JsonNode event : events) {
                if (!"m.room.message".equals(event.path("type").asText())) continue;
                if (!"m.text".equals(event.path("content").path("msgtype").asText())) continue;

                String sender = event.path("sender").asText(null);
                String body = event.path("content").path("body").asText(null);

                if (sender == null || body == null) continue;

                result.add(new Incoming(sender, roomId, body));
            }
        });

        return result;
    }
}