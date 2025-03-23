package world.anhgelus.khunegos.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import world.anhgelus.khunegos.timer.TimerAccess;

import static net.minecraft.server.command.CommandManager.literal;

public class TimerCommand {
    public static void bootstrap(CommandDispatcher<ServerCommandSource> dispatcher) {
        final var cmd = literal("timer")
                .requires(s -> s.hasPermissionLevel(3))
                .executes(context -> {
                    final var source = context.getSource();
                    final var timer = TimerAccess.getTimerFromOverworld(source.getServer());
                    source.sendFeedback(() -> Text.literal(timer.timer_toString()), false);
                    return Command.SINGLE_SUCCESS;
                });

        dispatcher.register(cmd);
    }
}
