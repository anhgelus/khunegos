package world.anhgelus.khunegos.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static world.anhgelus.khunegos.player.KhunegosPlayer.Manager.getKhunegosPlayer;

public class BookCommand {
    public static void bootstrap(CommandDispatcher<ServerCommandSource> dispatcher) {
        final var cmd = literal("book").requires(s -> s.hasPermissionLevel(3)).then(
                argument("player", EntityArgumentType.player())
                        .executes(context -> {
                            final var source = context.getSource();
                            final var target = EntityArgumentType.getPlayer(context, "player");
                            final var p = source.getPlayerOrThrow();
                            return giveBook(p, target);
                        })
        ).executes(context -> {
            final var source = context.getSource();
            final var p = source.getPlayerOrThrow();
            return giveBook(p, p);
        });

        dispatcher.register(cmd);
    }

    private static int giveBook(ServerPlayerEntity player, ServerPlayerEntity target) {
        final var is = new ItemStack(Items.WRITTEN_BOOK);
        is.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, getKhunegosPlayer(target).getBookContent());
        player.giveOrDropStack(is);
        return Command.SINGLE_SUCCESS;
    }
}
