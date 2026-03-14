package de.weinschenk.lea.core.matrix;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class MatrixClient {

    private static final Logger log = LoggerFactory.getLogger(MatrixClient.class);
    private static final int SYNC_TIMEOUT_MS = 30_000;

    private final HttpClient http;
    private final ObjectMapper om = new ObjectMapper();

    private final String homeserverUrl;
    private final String accessToken;

    public MatrixClient(String homeserverUrl, String accessToken) {
        this.homeserverUrl = homeserverUrl.replaceAll("/+$", "") + "/_matrix/client/v3";
        this.accessToken = accessToken;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void sendToRoom(String roomId, String message) {
        String txnId = UUID.randomUUID().toString();
        String encodedRoomId = URLEncoder.encode(roomId, StandardCharsets.UTF_8);
        String url = homeserverUrl + "/rooms/" + encodedRoomId + "/send/m.room.message/" + txnId;

        ObjectNode body = om.createObjectNode();
        body.put("msgtype", "m.text");
        body.put("body", message);

        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                throw new RuntimeException("Send failed: HTTP " + resp.statusCode() + ": " + resp.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send message to room " + roomId, e);
        }
    }

    /** Startet einen Sync-Loop (Long Polling). Der Consumer wird pro eingehender Nachricht aufgerufen. */
    public Thread startEventsLoop(Consumer<MatrixEventParser.Incoming> onMessage) {
        return Thread.ofVirtual().name("matrix-sync").start(() -> {
            String since = null;

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    since = syncOnce(since, onMessage);
                } catch (Exception e) {
                    log.error("Sync error, retrying…", e);
                    try { Thread.sleep(2000); } catch (InterruptedException ie) { return; }
                }
            }
        });
    }

    private String syncOnce(String since, Consumer<MatrixEventParser.Incoming> onMessage) throws Exception {
        String url = homeserverUrl + "/sync?timeout=" + SYNC_TIMEOUT_MS;
        if (since != null) {
            url += "&since=" + URLEncoder.encode(since, StandardCharsets.UTF_8);
        }

        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .timeout(Duration.ofMillis(SYNC_TIMEOUT_MS + 10_000))
                .GET()
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Sync failed: HTTP " + resp.statusCode() + ": " + resp.body());
        }

        JsonNode root = om.readTree(resp.body());
        String nextBatch = root.path("next_batch").asText(null);

        // Beim ersten Sync (since == null) Events ignorieren — nur next_batch merken
        if (since != null) {
            List<MatrixEventParser.Incoming> incoming = MatrixEventParser.parseSync(root);
            for (MatrixEventParser.Incoming msg : incoming) {
                try {
                    onMessage.accept(msg);
                } catch (Exception e) {
                    log.error("Error handling incoming message from {}", msg.sender(), e);
                }
            }
        }

        return nextBatch;
    }
}
