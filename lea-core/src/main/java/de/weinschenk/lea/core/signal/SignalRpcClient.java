package de.weinschenk.lea.core.signal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;

public class SignalRpcClient {

    private static final Logger log = LoggerFactory.getLogger(SignalRpcClient.class);

    private final HttpClient http;
    private final ObjectMapper om = new ObjectMapper();

    private final URI rpcUrl;
    private final URI eventsUrl;
    private final String account;

    public SignalRpcClient(String rpcUrl, String eventsUrl, String account) {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.rpcUrl = URI.create(rpcUrl);
        this.eventsUrl = URI.create(eventsUrl);
        this.account = account;
    }

    public void sendToRecipient(String recipientE164, String message) {
        ObjectNode params = om.createObjectNode();
        params.put("message", message);
        params.putArray("recipient").add(recipientE164);

        // In Multi-Account mode wäre account ein param; wir setzen es trotzdem dazu (schadet nicht)
        params.put("account", account);

        call("send", params);
    }

    public void sendToGroup(String groupIdBase64, String message) {
        ObjectNode params = om.createObjectNode();
        params.put("groupId", groupIdBase64);
        params.put("message", message);
        params.put("account", account);

        call("send", params);
    }

    /** Startet einen SSE-Reader Thread; callback bekommt rohe JSON payloads der "receive" Events. */
    public Thread startEventsLoop(Consumer<JsonNode> onReceiveEvent) {
        Thread t = Thread.ofVirtual().name("signal-events").start(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    log.info("Connecting SSE events stream: {}", eventsUrl);
                    HttpRequest req = HttpRequest.newBuilder(eventsUrl)
                            .timeout(Duration.ofMinutes(0)) // stream
                            .header("Accept", "text/event-stream")
                            .GET()
                            .build();

                    HttpResponse<java.io.InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
                    if (resp.statusCode() != 200) {
                        log.error("SSE connect failed: HTTP {}", resp.statusCode());
                        Thread.sleep(2000);
                        continue;
                    }

                    try (BufferedReader br = new BufferedReader(new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
                        String line;
                        StringBuilder data = new StringBuilder();

                        while ((line = br.readLine()) != null) {
                            // SSE: leere Zeile = event Ende
                            if (line.isEmpty()) {
                                if (!data.isEmpty()) {
                                    handleSseData(data.toString(), onReceiveEvent);
                                    data.setLength(0);
                                }
                                continue;
                            }

                            // nur data: interessiert uns
                            if (line.startsWith("data:")) {
                                data.append(line.substring(5).trim());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("SSE loop error, reconnecting…", e);
                    try { Thread.sleep(2000); } catch (InterruptedException ie) { return; }
                }
            }
        });
        return t;
    }

    private void handleSseData(String json, Consumer<JsonNode> onReceiveEvent) {
        try {
            JsonNode node = om.readTree(json);
            // signal-cli events sind JSON-RPC notifications, method meistens "receive" :contentReference[oaicite:1]{index=1}
            if (node.has("method") && "receive".equals(node.get("method").asText())) {
                onReceiveEvent.accept(node);
            } else {
                log.debug("Ignoring SSE event: {}", node.path("method").asText());
            }
        } catch (Exception e) {
            log.warn("Failed to parse SSE data: {}", json, e);
        }
    }

    private JsonNode call(String method, ObjectNode params) {
        try {
            ObjectNode req = om.createObjectNode();
            req.put("jsonrpc", "2.0");
            req.put("id", UUID.randomUUID().toString());
            req.put("method", method);
            req.set("params", params);

            HttpRequest httpReq = HttpRequest.newBuilder(rpcUrl)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(req.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = http.send(httpReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                throw new RuntimeException("RPC HTTP " + resp.statusCode() + ": " + resp.body());
            }

            JsonNode res = om.readTree(resp.body());
            if (res.has("error")) {
                throw new RuntimeException("RPC error: " + res.get("error").toString());
            }
            return res;
        } catch (Exception e) {
            throw new RuntimeException("RPC call failed: " + method, e);
        }
    }
}

