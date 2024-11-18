package world.anhgelus.khunegos;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
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
        command.then(literal("reveal").then(CommandManager.argument("target", EntityArgumentType.player())).executes(context -> {
            final var prisoner = getPrisonerFromContext(context);
            if (prisoner == null) return 2;
            final var target = context.getArgument("target", ServerPlayerEntity.class);
            final var source = context.getSource();
            if (target == null) {
                source.sendMessage(Text.of("Target is null (not online?)"));
                return 3;
            }
            final var task = prisoner.getHunterTasks()
                    .stream()
                    .filter(t -> t.linked == target.getUuid() && !t.isRunning())
                    .findFirst();
            if (task.isEmpty()) {
                source.sendMessage(Text.of("Impossible to find task"));
                return 4;
            }
            assert target.getDisplayName() != null;
            source.getServer().getPlayerManager().broadcast(
                    Text.empty().append(prisoner.player().getDisplayName())
                            .append(Text.of(" is tracking "))
                            .append(Text.of(target.getDisplayName())),
                    false);
            prisoner.revealTask(task.get());
            return Command.SINGLE_SUCCESS;
        }));
        command.then(literal("tasks").executes(context -> {
            final var prisoner = getPrisonerFromContext(context);
            if (prisoner == null) return 2;

            final var source = context.getSource();
            final var server = source.getServer();
            final var txt = Text.empty().append(Text.of("Your tasks:\n"));
            prisoner.getHunterTasks().forEach(task -> {
                if (!task.isRunning()) return;
                final var player = server.getPlayerManager().getPlayer(task.linked);
                if (player == null) throw new IllegalStateException("Player is null (not online?)");
                txt.append(Text.of("\n- kill ")).append(player.getDisplayName());
            });
            source.sendMessage(txt);
            return Command.SINGLE_SUCCESS;
        }));

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

    private @Nullable Prisoner getPrisonerFromContext(CommandContext<ServerCommandSource> context) {
        final var source = context.getSource();
        final var player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.of("You must be a player to execute this command!"));
            return null;
        }
        return Prisoner.from(player);
    }
}
