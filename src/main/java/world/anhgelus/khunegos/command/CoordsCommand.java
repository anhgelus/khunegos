package world.anhgelus.khunegos.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import world.anhgelus.khunegos.player.KPlayer;

import static net.minecraft.server.command.CommandManager.literal;

public class CoordsCommand {
    public static void bootstrap(CommandDispatcher<ServerCommandSource> dispatcher) {
        final var cmd = literal("coords")
                .executes(context -> {
                    final var source = context.getSource();
                    if (!source.isExecutedByPlayer()) return 2;
                    final var player = source.getPlayer();
                    assert player != null;
                    final var khunegos = KPlayer.Manager.getKhunegosPlayer(player);
                    if (khunegos.getRole() != KPlayer.Role.HUNTER) {
                        source.sendFeedback(() -> Text.of("Your are not an hunter."), false);
                        return Command.SINGLE_SUCCESS;
                    }
                    if (!khunegos.canUseCommandCoords()) {
                        source.sendFeedback(() -> Text.of("You must wait before getting the new coords"), false);
                        return Command.SINGLE_SUCCESS;
                    }
                    final var task = khunegos.getTask().orElseThrow();
                    source.sendFeedback(() -> Text.of(
                            "Your prey's coords: " + task.prey.getCoordsString() + " (" + task.prey.getWorld().asString() + ")"
                    ), false);
                    khunegos.useCommandCoords();
                    return Command.SINGLE_SUCCESS;
                });

        dispatcher.register(cmd);
    }
}
