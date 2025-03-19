package world.anhgelus.khunegos;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import world.anhgelus.khunegos.player.KhunegosPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Khunegos implements ModInitializer {
    public static final String MOD_ID = "khunegos";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final float KHUNEGOS_DURATION = 1f; // in day(s)
    public static final float KHUNEGOS_BASE_DELAY = 1f; // in day(s)
    public static final int MAX_RELATIVE_HEALTH = 5; // in heart(s)
    public static final int MIN_RELATIVE_HEALTH = -5; // in heart(s)

    private static final Map<UUID, KhunegosPlayer> players = new HashMap<>();

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Khunegos");
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            final var khunegosPlayer = getKhunegosPlayer(handler.player);
            khunegosPlayer.onRespawn(handler.player);
            //TODO: handle launch of Khunegos
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            final var khunegosPlayer = getKhunegosPlayer(player);
            if (!damageSource.isOf(DamageTypes.PLAYER_ATTACK) && !damageSource.isOf(DamageTypes.PLAYER_EXPLOSION)) {
                khunegosPlayer.onDeath(false);
                return;
            }
            khunegosPlayer.onDeath(true);
            if (khunegosPlayer.getRole() != KhunegosPlayer.Role.PREY) return;
            final var task = khunegosPlayer.getTask();
            assert task != null; // is always valid because task is never null if role == prey
            task.onPreyKilled();
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // use UUID to get (prevents creating a new one if not registered)
            final var khunegosPlayer = getKhunegosPlayer(oldPlayer.getUuid());
            if (khunegosPlayer == null) return;
            khunegosPlayer.onRespawn(newPlayer);
        });
    }

    private static KhunegosPlayer getKhunegosPlayer(ServerPlayerEntity player) {
        return players.computeIfAbsent(player.getUuid(), k -> new KhunegosPlayer(player));
    }

    @Nullable
    private static KhunegosPlayer getKhunegosPlayer(UUID uuid) {
        return players.get(uuid);
    }
}
