package de.weinschenk.lea.core;

import java.util.Set;

public record LeaConfig(
        String modulesDir,
        Allowlist allowlist,
        Signal signal
) {
    public record Allowlist(Set<String> senders, Set<String> groups) {}

    public record Signal(String rpcUrl, String eventsUrl, String account) {}
    
    public record LeaConfig(String modulesDir, Allowlist allowlist, Signal signal) { ... }

    public boolean isSenderAllowed(String sender) {
        return sender != null
                && allowlist != null
                && allowlist.senders() != null
                && allowlist.senders().contains(sender);
    }

    public boolean isGroupAllowed(String groupId) {
        return groupId != null
                && allowlist != null
                && allowlist.groups() != null
                && allowlist.groups().contains(groupId);
    }
}

