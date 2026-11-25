
package com.warcontry.auth;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WarContryAuthManager {

    private static final Set<UUID> AUTHENTICATED_PLAYERS =
            ConcurrentHashMap.newKeySet();

    private static final ConcurrentHashMap<UUID, String> PENDING_2FA =
            new ConcurrentHashMap<>();

    private WarContryAuthManager() {
    }

    public static void authenticate(UUID playerUuid) {
        PENDING_2FA.remove(playerUuid);
        AUTHENTICATED_PLAYERS.add(playerUuid);
    }

    public static boolean isAuthenticated(UUID playerUuid) {
        return AUTHENTICATED_PLAYERS.contains(playerUuid);
    }

    public static void unauthenticate(UUID playerUuid) {
        AUTHENTICATED_PLAYERS.remove(playerUuid);
        PENDING_2FA.remove(playerUuid);
    }

    public static void setPending2FA(UUID playerUuid, String challengeId) {
        if (challengeId == null || challengeId.isBlank()) {
            throw new IllegalArgumentException("Identifiant 2FA invalide.");
        }

        PENDING_2FA.put(playerUuid, challengeId);
    }

    public static String getPending2FA(UUID playerUuid) {
        return PENDING_2FA.get(playerUuid);
    }

    public static void clearPending2FA(UUID playerUuid) {
        PENDING_2FA.remove(playerUuid);
    }
}
