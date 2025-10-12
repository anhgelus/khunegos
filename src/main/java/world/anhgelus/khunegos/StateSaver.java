package world.anhgelus.khunegos;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import world.anhgelus.khunegos.player.PlayerData;

import java.util.*;

public class StateSaver extends PersistentState {
    public static final String PLAYERS_KEY = "players";
    public static final String ARMOR_STANDS_KEY = "armor_stands";
    public static final Codec<StateSaver> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, PlayerData.CODEC).fieldOf(PLAYERS_KEY).forGetter(StateSaver::getPlayers),
            BlockPos.CODEC.listOf().fieldOf(ARMOR_STANDS_KEY).forGetter(StateSaver::getArmorStandsToSpawn)
    ).apply(i, StateSaver::new));
    private static final PersistentStateType<StateSaver> type = new PersistentStateType<>(
            Khunegos.MOD_ID, StateSaver::new, CODEC, null
    );

    public Map<UUID, PlayerData> players = new HashMap<>();
    public Set<BlockPos> armorStandsToSpawn = new HashSet<>();

    public StateSaver(Map<String, PlayerData> players, List<BlockPos> armorStandsToSpawn) {
        players.forEach((s, playerData) -> this.players.put(UUID.fromString(s), playerData));
        this.armorStandsToSpawn.addAll(armorStandsToSpawn);
    }

    public StateSaver() {
    }

    public static StateSaver getServerState(MinecraftServer server) {
        final var world = server.getOverworld();
        final var persistentStateManager = world.getPersistentStateManager();

        final var state = persistentStateManager.getOrCreate(type);

        state.markDirty();

        return state;
    }

    public static PlayerData getPlayerState(ServerPlayerEntity player) {
        return getPlayerState(player.getEntityWorld().getServer(), player.getUuid());
    }

    public static PlayerData getPlayerState(MinecraftServer server, UUID uuid) {
        final var state = getServerState(server);
        return state.players.computeIfAbsent(uuid, u -> PlayerData.DEFAULT);
    }

    public Map<String, PlayerData> getPlayers() {
        final var np = new HashMap<String, PlayerData>();
        players.forEach((uuid, d) -> {
            np.put(uuid.toString(), d);
        });
        return np;
    }

    public List<BlockPos> getArmorStandsToSpawn() {
        return new ArrayList<>(armorStandsToSpawn);
    }
}

