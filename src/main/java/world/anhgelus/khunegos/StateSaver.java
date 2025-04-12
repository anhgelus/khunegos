package world.anhgelus.khunegos;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;
import world.anhgelus.khunegos.player.PlayerData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StateSaver extends PersistentState {
    private static final Type<StateSaver> type = new Type<>(
            StateSaver::new,
            StateSaver::createFromNbt,
            null
    );
    public Map<UUID, PlayerData> players = new HashMap<>();

    public static StateSaver createFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        final var state = new StateSaver();

        tag.getKeys().forEach(key -> {
            final var compound = tag.getCompound(key);
            state.players.put(UUID.fromString(key), PlayerData.from(compound));
        });

        return state;
    }

    public static StateSaver getServerState(MinecraftServer server) {
        final var world = server.getOverworld();
        final var persistentStateManager = world.getPersistentStateManager();

        final var state = persistentStateManager.getOrCreate(type, Khunegos.MOD_ID);

        state.markDirty();

        return state;
    }

    public static PlayerData getPlayerState(ServerPlayerEntity player) {
        return getPlayerState(player.server, player.getUuid());
    }

    public static PlayerData getPlayerState(MinecraftServer server, UUID uuid) {
        final var state = getServerState(server);
        return state.players.computeIfAbsent(uuid, u -> PlayerData.DEFAULT);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        players.forEach((uuid, d) -> {
            nbt.put(uuid.toString(), d.save());
        });

        return nbt;
    }
}

