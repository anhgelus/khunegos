package world.anhgelus.khunegos;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StateSaver extends PersistentState {
    private static final Type<StateSaver> type = new Type<>(
            StateSaver::new,
            StateSaver::createFromNbt,
            null
    );
    public Map<UUID, Float> players = new HashMap<>();

    public static StateSaver createFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        final var state = new StateSaver();

        final var playersNbt = tag.getCompound("players");
        playersNbt.getKeys().forEach(key -> {
            final var compound = playersNbt.getCompound(key);
            state.players.put(UUID.fromString(key), compound.getFloat("health"));
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

    public static float getPlayerState(ServerPlayerEntity player) {
        return getPlayerState(player.server, player.getUuid());
    }

    public static float getPlayerState(MinecraftServer server, UUID uuid) {
        final var state = getServerState(server);
        return state.players.computeIfAbsent(uuid, u -> 0f);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        final var playersNbt = new NbtCompound();
        players.forEach((uuid, h) -> {
            NbtCompound playerNbt = new NbtCompound();

            playerNbt.putFloat("health", h);

            playersNbt.put(uuid.toString(), playerNbt);
        });

        return nbt;
    }
}

