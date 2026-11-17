package au.com.testplex.arcadeports;

import java.util.UUID;

/** Packet-only owner label kept separate from the uniquely named fake player profile. */
final class DragonEscapeGhostLabel {
    static final double HEIGHT_OFFSET = 1.65D;
    private static final int ENTITY_ID_BASE = Integer.MAX_VALUE;
    private static final int ENTITY_ID_MASK = 0x0fffffff;
    private static final int PLAYER_GHOST_ID_MIN = 1_800_000_000 - 0x1fffffff;

    private DragonEscapeGhostLabel() { }

    static String displayName(String ownerName, String fallbackName) {
        return ownerName == null || ownerName.isBlank() ? fallbackName : ownerName;
    }

    static int entityId(UUID ghostUuid) {
        return ENTITY_ID_BASE - (ghostUuid.hashCode() & ENTITY_ID_MASK);
    }

    static boolean usesSeparateEntityIdRange(int playerGhostId, int nameTagEntityId) {
        return playerGhostId >= PLAYER_GHOST_ID_MIN && playerGhostId < 1_800_000_000
                && nameTagEntityId > 1_800_000_000;
    }
}
