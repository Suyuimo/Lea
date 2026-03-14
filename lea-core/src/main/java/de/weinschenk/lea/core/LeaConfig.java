package de.weinschenk.lea.core;

import java.util.Set;

public record LeaConfig(
        String modulesDir,
        Allowlist allowlist,
        Matrix matrix
) {
    public record Allowlist(Set<String> senders, Set<String> groups) {}

    public record Matrix(String homeserverUrl, String accessToken, String userId) {}

    public boolean isSenderAllowed(String sender) {
        return sender != null
                && allowlist != null
                && allowlist.senders() != null
                && allowlist.senders().contains(sender);
    }

    public boolean isGroupAllowed(String roomId) {
        return roomId != null
                && allowlist != null
                && allowlist.groups() != null
                && allowlist.groups().contains(roomId);
    }
}

