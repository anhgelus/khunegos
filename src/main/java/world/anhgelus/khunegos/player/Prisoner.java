package world.anhgelus.khunegos.player;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

public class Prisoner {
    private static final Map<UUID, Prisoner> prisoners = new HashMap<>();

    public final UUID uuid;
    public final String name;
    private ServerPlayerEntity player;

    private Prisoner(ServerPlayerEntity player) {
        this.uuid = player.getUuid();
        this.player = player;
        assert player.getDisplayName() != null;
        name = player.getDisplayName().getString();
        prisoners.put(player.getUuid(), this);
    }

    public ServerPlayerEntity player() {
        return player;
    }

    public void playerDies(ServerPlayerEntity newPlayer, DamageSource source) {
        player = newPlayer;
    }

    private void updatePlayer(ServerPlayerEntity player) {
        this.player = player;
    }

    public static Prisoner from(ServerPlayerEntity player) {
        final var found = prisoners.computeIfAbsent(player.getUuid(), u -> new Prisoner(player));
        if (found.player() != player) {
            found.updatePlayer(player);
        }
        return found;
    }

    public static Collection<Prisoner> prisoners() {
        return prisoners.values();
    }
}
