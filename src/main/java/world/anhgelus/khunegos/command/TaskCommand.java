package world.anhgelus.khunegos.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import world.anhgelus.khunegos.player.KhunegosTask;

import static net.minecraft.server.command.CommandManager.literal;

public class TaskCommand {
    public static void bootstrap(CommandDispatcher<ServerCommandSource> dispatcher) {
        final var cmd = literal("task")
                .requires(s -> s.hasPermissionLevel(3))
                .executes(context -> {
                    final var source = context.getSource();
                    final var tasks = KhunegosTask.Manager.getTasks();
                    tasks.forEach(task -> source.sendFeedback(() -> Text.literal(task.toString()), false));
                    return Command.SINGLE_SUCCESS;
                });

        dispatcher.register(cmd);
    }
}
