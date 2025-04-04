package world.anhgelus.khunegos;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.SharedConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import world.anhgelus.khunegos.command.CommandHandler;
import world.anhgelus.khunegos.listener.PlayerListeners;
import world.anhgelus.khunegos.player.KhunegosPlayer;

public class Khunegos implements ModInitializer {
    public static final String MOD_ID = "khunegos";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final float KHUNEGOS_DURATION = 1f; // in day(s)
    public static final float KHUNEGOS_BASE_DELAY = 1f; // in day(s)
    public static final int MAX_RELATIVE_HEALTH = 5; // in heart(s)
    public static final int MIN_RELATIVE_HEALTH = -5; // in heart(s)
    public static final String BASE_KEY = MOD_ID; // base key of all NBT things

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Khunegos");

        SharedConstants.isDevelopment = true;

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            final var state = StateSaver.getServerState(server);
            KhunegosPlayer.Manager.loadPlayers(state);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            final var state = StateSaver.getServerState(server);
            KhunegosPlayer.Manager.savePlayers(state);
        });

        CommandRegistrationCallback.EVENT.register(CommandHandler::bootstrap);

        ServerPlayConnectionEvents.JOIN.register(PlayerListeners::join);
        ServerPlayConnectionEvents.DISCONNECT.register(PlayerListeners::disconnect);

        ServerLivingEntityEvents.AFTER_DEATH.register(PlayerListeners::afterDeath);
        ServerPlayerEvents.AFTER_RESPAWN.register(PlayerListeners::afterRespawn);

        UseItemCallback.EVENT.register(PlayerListeners::useItem);
    }
}
