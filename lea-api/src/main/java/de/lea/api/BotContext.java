package de.weinschenk.lea.api;

public interface BotContext {
    void reply(CommandRequest request, String message);
    void log(String message);
    void writeTestFile(String text);
}
