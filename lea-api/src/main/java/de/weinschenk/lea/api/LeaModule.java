package de.weinschenk.lea.api;

public interface LeaModule {
    String id();
    void onCommand(CommandRequest request, BotContext context);
}
