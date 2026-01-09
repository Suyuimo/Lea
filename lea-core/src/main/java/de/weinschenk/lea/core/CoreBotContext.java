package de.weinschenk.lea.core;

import de.weinschenk.lea.api.BotContext;
import de.weinschenk.lea.api.CommandRequest;
import de.weinschenk.lea.core.signal.SignalRpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class CoreBotContext implements BotContext {

    private static final Logger log = LoggerFactory.getLogger(CoreBotContext.class);

    private final Path testFile;
    private final SignalRpcClient signal;

    public CoreBotContext(Path testFile, SignalRpcClient signal) {
        this.testFile = testFile;
        this.signal = signal;
    }

    @Override
    public void reply(CommandRequest request, String message) {
        try {
            if (request.groupId().isPresent()) {
                String groupId = request.groupId().get();
                signal.sendToGroup(groupId, message);
                log.info("Sent reply to groupId={}", groupId);
            } else {
                String recipient = request.sender();
                signal.sendToRecipient(recipient, message);
                log.info("Sent reply to recipient={}", recipient);
            }
        } catch (Exception e) {
            log.error("Failed to send reply. toSender={} groupId={}",
                    request.sender(),
                    request.groupId().orElse("<dm>"),
                    e
            );
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

