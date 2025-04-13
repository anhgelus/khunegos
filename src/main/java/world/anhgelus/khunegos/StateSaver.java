package world.anhgelus.khunegos;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import world.anhgelus.khunegos.player.PlayerData;

import java.util.*;

public class StateSaver extends PersistentState {
    public static final String PLAYERS_KEY = "players";
    public static final String ARMOR_STANDS_KEY = "armor_stands";
    private static final Type<StateSaver> type = new Type<>(
            StateSaver::new,
            StateSaver::createFromNbt,
            null
    );
    public Map<UUID, PlayerData> players = new HashMap<>();
    public Set<BlockPos> armorStandsToSpawn = new HashSet<>();

    public static StateSaver createFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        final var state = new StateSaver();

        final var playersNbt = tag.getCompound(PLAYERS_KEY);
        playersNbt.getKeys().forEach(key -> state.players.put(UUID.fromString(key), PlayerData.from(playersNbt.getCompound(key))));

        final var armorStandsNbt = tag.getCompound(ARMOR_STANDS_KEY);
        armorStandsNbt.getKeys().forEach(key -> state.armorStandsToSpawn.add(BlockPos.fromLong(armorStandsNbt.getLong(key))));

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
        final var playersNbt = new NbtCompound();
        players.forEach((uuid, d) -> playersNbt.put(uuid.toString(), d.save()));
        nbt.put(PLAYERS_KEY, playersNbt);

        final var armorStandsNbt = new NbtCompound();
        armorStandsToSpawn.forEach((pos) -> armorStandsNbt.putLong(pos.toShortString(), pos.asLong()));
        nbt.put(ARMOR_STANDS_KEY, armorStandsNbt);

        return nbt;
    }
}

