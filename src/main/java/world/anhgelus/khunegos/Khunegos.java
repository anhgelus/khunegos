package world.anhgelus.khunegos;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import world.anhgelus.khunegos.player.Prisoner;

import java.util.HashMap;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.literal;

public class Khunegos implements ModInitializer {
    public static final Identifier HEALTH_MODIFIER_ID = Identifier.of("khunegos_health_modifier");
    public static final String MOD_ID = "khunegos";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static boolean karratos = false;

    private final Map<ServerPlayerEntity, DamageSource> damageSources = new HashMap<>();
    private Game game;

    @Override
    public void onInitialize() {
        final var command = literal("khunegos");
        command.then(literal("start").requires(source -> source.hasPermissionLevel(1)).executes(context -> {
            game.start();
            return Command.SINGLE_SUCCESS;
        }));
        command.then(literal("stop").requires(source -> source.hasPermissionLevel(1)).executes(context -> {
            game.stop();
            return Command.SINGLE_SUCCESS;
        }));
        command.then(literal("reveal").executes(context -> {
            final var source = context.getSource();
            final var player = source.getPlayer();
            if (player == null) {
                source.sendError(Text.of("You must be a player to execute this command"));
                return 1;
            }
            final var prisoner = Prisoner.from(player);
            // reveal player with given id
            return Command.SINGLE_SUCCESS;
        }));
        // command to get tasks

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            game = new Game(server);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Prisoner.from(handler.getPlayer()));

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            damageSources.put(player, source);
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            Prisoner.from(oldPlayer).playerDies(newPlayer, damageSources.get(oldPlayer));
        });
    }

    public static boolean isKarratos() {
        return karratos;
    }

    public static void enableKarratos() {
        karratos = false;
    }
}
