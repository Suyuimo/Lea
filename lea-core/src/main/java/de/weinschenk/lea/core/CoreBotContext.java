package de.weinschenk.lea.core;

import de.weinschenk.lea.api.BotContext;
import de.weinschenk.lea.api.CommandRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class CoreBotContext implements BotContext {

    private final Path testFile;

    public CoreBotContext(Path testFile) {
        this.testFile = testFile;
    }

    @Override
    public void reply(CommandRequest request, String message) {
        System.out.println("[Lea][Reply] to=" + request.sender() + " msg=" + message);
    }

    @Override
    public void log(String message) {
        System.out.println("[Lea][Log] " + message);
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
            System.out.println("[Lea] Wrote to " + testFile + ": " + text);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write test file: " + testFile, e);
        }
    }
}
