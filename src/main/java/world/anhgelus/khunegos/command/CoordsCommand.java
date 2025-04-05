package world.anhgelus.khunegos.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import world.anhgelus.khunegos.player.KhunegosPlayer;

import static net.minecraft.server.command.CommandManager.literal;

public class CoordsCommand {
    public static void bootstrap(CommandDispatcher<ServerCommandSource> dispatcher) {
        final var cmd = literal("coords")
                .executes(context -> {
                    final var source = context.getSource();
                    if (!source.isExecutedByPlayer()) return 2;
                    final var player = source.getPlayer();
                    assert player != null;
                    final var khunegos = KhunegosPlayer.Manager.getKhunegosPlayer(player);
                    if (khunegos.getRole() != KhunegosPlayer.Role.HUNTER) {
                        source.sendFeedback(() -> Text.of("Your are not an hunter."), false);
                        return Command.SINGLE_SUCCESS;
                    }
                    final var task = khunegos.getTask();
                    assert task != null; // valid because role == hunter
                    source.sendFeedback(() -> {
                        return Text.of("Your prey's coords: " + task.prey.getCoordsString());
                    }, false);
                    return Command.SINGLE_SUCCESS;
                });

        dispatcher.register(cmd);
    }
}
