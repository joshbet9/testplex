package au.com.testplex.arcadeports;

import java.util.UUID;

/** Packet-level player-to-Villager disguise lifecycle, kept free of PacketEvents types. */
interface SneakyAssassinsDisguiseBridge {
    boolean isAvailable();
    void setDisguised(UUID playerId, boolean disguised);
    void unregister();
}
