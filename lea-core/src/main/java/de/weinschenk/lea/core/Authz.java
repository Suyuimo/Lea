package de.weinschenk.lea.core;

import java.util.Optional;

public class Authz {

    private final LeaConfig config;

    public Authz(LeaConfig config) {
        this.config = config;
    }

    public boolean isAllowed(String sender, Optional<String> groupId) {
        if (!config.isSenderAllowed(sender)) return false;

        // DM: ok, wenn Sender erlaubt
        if (groupId.isEmpty()) return true;

        // Group: Sender + Gruppe müssen erlaubt sein
        return config.isGroupAllowed(groupId.get());
    }
}

