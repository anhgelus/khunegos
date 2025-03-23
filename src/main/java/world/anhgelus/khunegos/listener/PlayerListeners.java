package world.anhgelus.khunegos.listener;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.slf4j.Logger;
import world.anhgelus.khunegos.Khunegos;
import world.anhgelus.khunegos.player.KhunegosPlayer;
import world.anhgelus.khunegos.player.KhunegosTask;

import static world.anhgelus.khunegos.player.KhunegosPlayer.Manager.getKhunegosPlayer;

public class PlayerListeners {
    private final static Logger logger = Khunegos.LOGGER;

    private static int next = -1;
    private static boolean firstStarted = false;

    public static void join(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        final var khunegosPlayer = getKhunegosPlayer(handler.player);
        khunegosPlayer.onRespawn(handler.player);
        // setup khunegos
        final var rand = server.getOverworld().getRandom();
        if (next == -1) next = 4 + MathHelper.nextInt(rand, -1, 1);
        final var playersConnected = server.getPlayerManager().getPlayerList().size();
        if (firstStarted) {
            if (MathHelper.nextInt(rand, 0, 1) == 1) return;
            if (KhunegosTask.Manager.canServerStartsNewTask(server))
                KhunegosTask.Manager.addTask(new KhunegosTask.Incoming(server, false));
            else logger.info("Cannot start a new task (not enough players)");
            return;
        }
        if (playersConnected < next) return;
        // create first khunegos
        KhunegosTask.Manager.addTask(new KhunegosTask.Incoming(server, true));
        firstStarted = true;
    }

    public static void disconnect(ServerPlayNetworkHandler handler, MinecraftServer server) {
        final var khunegosPlayer = getKhunegosPlayer(handler.player);
        final var role = khunegosPlayer.getRole();
        if (role == KhunegosPlayer.Role.NONE) KhunegosTask.Manager.updateIncomingTasks(server);
        final var task = khunegosPlayer.getTask();
        assert task != null; // true because role != none
        if (role == KhunegosPlayer.Role.PREY) task.onPreyDisconnection();
    }

    public static void afterDeath(LivingEntity entity, DamageSource damageSource) {
        if (!(entity instanceof ServerPlayerEntity player)) return;
        final var khunegosPlayer = getKhunegosPlayer(player);
        if (!(damageSource.getAttacker() instanceof ServerPlayerEntity killer)) {
            khunegosPlayer.onDeath(false);
            return;
        }
        if (khunegosPlayer.getRole() != KhunegosPlayer.Role.PREY) {
            khunegosPlayer.onDeath(true);
            return;
        }
        final var task = khunegosPlayer.getTask();
        assert task != null; // is always valid because task is never null if role == prey
        khunegosPlayer.onDeath(task.hunter == getKhunegosPlayer(killer)); // checks if it's the right player
        // remove old task and add new planned
        KhunegosTask.Manager.addTask(task.onPreyKilled());
        KhunegosTask.Manager.removeTask(task);
    }

    public static void afterRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        // use UUID to get (prevents creating a new one if not registered)
        final var khunegosPlayer = getKhunegosPlayer(oldPlayer.getUuid());
        if (khunegosPlayer == null) return;
        khunegosPlayer.onRespawn(newPlayer);
    }

    public static ActionResult useItem(PlayerEntity player, World world, Hand hand) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
        // check validity of book
        ItemStack is;
        if (hand == Hand.MAIN_HAND) is = player.getInventory().getMainHandStack();
        else return ActionResult.PASS;
        if (!is.isOf(Items.BOOK)) return ActionResult.PASS;
        final var nbt = is.get(DataComponentTypes.CUSTOM_DATA);
        if (nbt == null) return ActionResult.PASS;
        if (!nbt.contains(Khunegos.KEY)) return ActionResult.PASS;
        if (!nbt.copyNbt().getBoolean(Khunegos.KEY)) return ActionResult.PASS;
        // modify book content
        is.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, getKhunegosPlayer(serverPlayer).getBookContent());
        return ActionResult.SUCCESS;
    }
}
