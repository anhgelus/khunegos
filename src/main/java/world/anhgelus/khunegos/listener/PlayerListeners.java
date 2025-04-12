package world.anhgelus.khunegos.listener;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
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
        khunegosPlayer.setConnected(true);
        khunegosPlayer.onRespawn(handler.player);
        if (khunegosPlayer.getRole() != KhunegosPlayer.Role.NONE) {
            final var task = khunegosPlayer.getTask();
            assert task != null;
            if (khunegosPlayer.getRole() == KhunegosPlayer.Role.PREY) task.onPreyReconnection();
            return;
        }
        // setup khunegos
        final var rand = server.getOverworld().getRandom();
        if (next == -1) next = 4 + MathHelper.nextInt(rand, -1, 1);
        final var playersConnected = server.getPlayerManager().getPlayerList().size() + 1;
        if (firstStarted) {
            if (MathHelper.nextInt(rand, 0, 1) == 1) return;
            if (KhunegosTask.Manager.canServerStartsNewTask(server))
                KhunegosTask.Manager.addTask(new KhunegosTask.Incoming(server, false));
            else logger.info("Cannot start a new task (not enough players)");
            return;
        }
        logger.info("first not started, {}", playersConnected);
//        if (playersConnected < MathHelper.nextInt(rand, -1, 1) + 3) return;
        if (playersConnected < 2) return;
        // create first khunegos
        KhunegosTask.Manager.addTask(new KhunegosTask.Incoming(server, true));
        firstStarted = true;
    }

    public static void disconnect(ServerPlayNetworkHandler handler, MinecraftServer server) {
        final var khunegosPlayer = getKhunegosPlayer(handler.player);
        khunegosPlayer.setConnected(false);
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
    }

    public static void afterRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        // use UUID to get (prevents creating a new one if not registered)
        final var khunegosPlayer = getKhunegosPlayer(oldPlayer.getUuid());
        if (khunegosPlayer == null) return;
        khunegosPlayer.onRespawn(newPlayer);
    }

    public static ActionResult clickOnEntity(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        if (!(entity instanceof ArmorStandEntity armorStand)) return ActionResult.PASS;
        // check armor stand validity
        if (armorStand.getEquippedStack(EquipmentSlot.MAINHAND).getItem() != Items.NETHER_STAR)
            return ActionResult.PASS;
        // now, send FAIL to prevent player to pick armor stand's thing
        // check validity of nether star
        ItemStack is;
        if (hand == Hand.MAIN_HAND) is = player.getInventory().getMainHandStack();
        else return ActionResult.FAIL;
        if (!is.isOf(Items.NETHER_STAR)) return ActionResult.FAIL; // fail to prevent player to pick armor stand's thing
        final var nbt = is.get(DataComponentTypes.CUSTOM_DATA);
        if (nbt == null) return ActionResult.FAIL;
        if (!nbt.contains(KhunegosPlayer.PLAYER_KEY)) return ActionResult.FAIL;
        player.getInventory().removeOne(is);
        getKhunegosPlayer((ServerPlayerEntity) player).onDeposeHeart();
        return ActionResult.SUCCESS;
    }
}
