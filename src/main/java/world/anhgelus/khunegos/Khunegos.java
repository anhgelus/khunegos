package world.anhgelus.khunegos;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import world.anhgelus.khunegos.player.KhunegosPlayer;
import world.anhgelus.khunegos.player.KhunegosTask;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Khunegos implements ModInitializer {
    public static final String MOD_ID = "khunegos";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final float KHUNEGOS_DURATION = 1f; // in day(s)
    public static final float KHUNEGOS_BASE_DELAY = 1f; // in day(s)
    public static final int MAX_RELATIVE_HEALTH = 5; // in heart(s)
    public static final int MIN_RELATIVE_HEALTH = -5; // in heart(s)

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Khunegos");

        final var next = new AtomicInteger(-1);
        final var started = new AtomicBoolean(false);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            final var khunegosPlayer = getKhunegosPlayer(handler.player);
            khunegosPlayer.onRespawn(handler.player);
            // setup khunegos
            if (next.get() == -1) next.set(4 + MathHelper.nextInt(server.getOverworld().getRandom(), -1, 1));
            if (started.get()){
                //TODO: handle multiple khunegos
                return;
            }
            // create first khunegos
            if (server.getPlayerManager().getPlayerList().size() < next.get()) return;
            KhunegosTask.Manager.addTask(new KhunegosTask.Incoming(server, true));
            started.set(true);
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
            // remove old task and add new planned
            KhunegosTask.Manager.addTask(task.onPreyKilled());
            KhunegosTask.Manager.removeTask(task);
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // use UUID to get (prevents creating a new one if not registered)
            final var khunegosPlayer = getKhunegosPlayer(oldPlayer.getUuid());
            if (khunegosPlayer == null) return;
            khunegosPlayer.onRespawn(newPlayer);
        });
    }

    private KhunegosPlayer getKhunegosPlayer(ServerPlayerEntity player) {
        return KhunegosPlayer.Manager.getKhunegosPlayer(player);
    }

    @Nullable
    private KhunegosPlayer getKhunegosPlayer(UUID uuid) {
        return KhunegosPlayer.Manager.getKhunegosPlayer(uuid);
    }
}
