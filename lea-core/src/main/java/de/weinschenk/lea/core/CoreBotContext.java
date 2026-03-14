package de.weinschenk.lea.core;

import de.weinschenk.lea.api.BotContext;
import de.weinschenk.lea.api.CommandRequest;
import de.weinschenk.lea.core.matrix.MatrixClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class CoreBotContext implements BotContext {

    private static final Logger log = LoggerFactory.getLogger(CoreBotContext.class);

    private final Path testFile;
    private final MatrixClient matrix;

    public CoreBotContext(Path testFile, MatrixClient matrix) {
        this.testFile = testFile;
        this.matrix = matrix;
    }

    @Override
    public void reply(CommandRequest request, String message) {
        try {
            String roomId = request.groupId().orElseThrow(
                    () -> new IllegalStateException("No roomId in request — cannot reply"));
            matrix.sendToRoom(roomId, message);
            log.info("Sent reply to roomId={}", roomId);
        } catch (Exception e) {
            log.error("Failed to send reply. sender={} roomId={}",
                    request.sender(),
                    request.groupId().orElse("<unknown>"),
                    e);
        }
    }

    @Override
    public void log(String message) {
        log.info(message);
    }

    @Override
    public void writeTestFile(String text) {
        try {
            Files.writeString(
                    testFile,
                    text + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
            log.info("Wrote to {}: {}", testFile.toAbsolutePath(), text);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write test file: " + testFile.toAbsolutePath(), e);
        }
    }
}
