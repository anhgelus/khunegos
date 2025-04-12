package world.anhgelus.khunegos;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import world.anhgelus.khunegos.command.CommandHandler;
import world.anhgelus.khunegos.listener.PlayerListeners;
import world.anhgelus.khunegos.player.KhunegosPlayer;
import world.anhgelus.khunegos.player.KhunegosTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Khunegos implements ModInitializer {
    public static final String MOD_ID = "khunegos";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final float KHUNEGOS_DURATION = 2f; // in day(s)
    public static final float KHUNEGOS_BASE_DELAY = 1.5f; // in day(s)
    public static final int MAX_RELATIVE_HEALTH = 5; // in heart(s)
    public static final int MIN_RELATIVE_HEALTH = -5; // in heart(s)
    public static final String BASE_KEY = MOD_ID; // base key of all NBT things
    public static final String ARMOR_STAND_KEY = BASE_KEY + "_armor_stand"; // base key of all NBT things

    private static final Set<BlockPos> armorStandsToSpawn = new HashSet<>();
    private static final Set<ChunkPos> alreadySpawned = new HashSet<>();

    public static void spawnArmorStand(BlockPos pos) {
        armorStandsToSpawn.add(pos);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Khunegos");

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

        UseEntityCallback.EVENT.register(PlayerListeners::clickOnEntity);


        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            final var toDelete = new ArrayList<BlockPos>();
            armorStandsToSpawn.forEach(pos -> {
                final var cPos = entity.getChunkPos();
                if (alreadySpawned.contains(cPos)) return;
                if (!(cPos.getStartX() <= pos.getX() &&
                        pos.getX() <= cPos.getEndX() &&
                        cPos.getStartZ() <= pos.getZ() &&
                        pos.getZ() <= cPos.getEndZ())) return;
                alreadySpawned.add(cPos);
                final var armorStand = new ArmorStandEntity(world, pos.getX(), pos.getY() + 25, pos.getZ());
                // invulnerable, on ground, cannot move, without gravity, no base plate
                armorStand.setInvulnerable(true);
                armorStand.setHideBasePlate(true);
                armorStand.setShowArms(true);
                armorStand.setOnGround(true);
                armorStand.setNoDrag(true);
                // give simple stuff
                armorStand.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                armorStand.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHER_STAR));
                // armor stand is protected by the item in its main hand
                world.spawnEntity(armorStand);
                toDelete.add(pos);
            });
            toDelete.forEach(armorStandsToSpawn::remove);
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, world) -> {
            if (entity instanceof ArmorStandEntity armorStand) KhunegosTask.Manager.onArmorStandKilled(armorStand);
        });
    }
}
