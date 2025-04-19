package world.anhgelus.khunegos;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.gamerule.v1.rule.DoubleRule;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import world.anhgelus.khunegos.command.CommandHandler;
import world.anhgelus.khunegos.listener.PlayerListeners;
import world.anhgelus.khunegos.player.KPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Khunegos implements ModInitializer {
    public static final String MOD_ID = "khunegos";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String BASE_KEY = MOD_ID; // base key of all NBT things
    private static final Set<BlockPos> armorStandsToSpawn = new HashSet<>();
    private static final Set<ChunkPos> alreadySpawned = new HashSet<>();
    public static double KHUNEGOS_DURATION = 2f; // in day(s)
    public static final GameRules.Key<DoubleRule> KHUNEGOS_DURATION_RULE = GameRuleRegistry.register(
            MOD_ID + ":duration",
            GameRules.Category.MISC,
            GameRuleFactory.createDoubleRule(KHUNEGOS_DURATION, (server, rule) -> KHUNEGOS_DURATION = rule.get())
    );
    public static double KHUNEGOS_DELAY = 1.5f; // in day(s)
    public static final GameRules.Key<DoubleRule> KHUNEGOS_DELAY_RULE = GameRuleRegistry.register(
            MOD_ID + ":delay",
            GameRules.Category.MISC,
            GameRuleFactory.createDoubleRule(KHUNEGOS_DELAY, (server, rule) -> KHUNEGOS_DELAY = rule.get())
    );
    public static int MAX_RELATIVE_HEALTH = 5; // in heart(s)
    public static final GameRules.Key<GameRules.IntRule> MAX_HEALTH_RULE = GameRuleRegistry.register(
            MOD_ID + ":maxHealth",
            GameRules.Category.MISC,
            GameRuleFactory.createIntRule(MAX_RELATIVE_HEALTH + 10, 11, (server, rule) -> MAX_RELATIVE_HEALTH = rule.get() - 10)
    );
    public static int MIN_RELATIVE_HEALTH = -5; // in heart(s)
    public static final GameRules.Key<GameRules.IntRule> MIN_HEALTH_RULE = GameRuleRegistry.register(
            MOD_ID + ":minHealth",
            GameRules.Category.MISC,
            GameRuleFactory.createIntRule(MIN_RELATIVE_HEALTH + 10, 1, (server, rule) -> MIN_RELATIVE_HEALTH = rule.get() - 10)
    );
    public static int FIRST_START_PLAYERS = 4;
    public static final GameRules.Key<GameRules.IntRule> MIN_PLAYERS_RULE = GameRuleRegistry.register(
            MOD_ID + ":minPlayers",
            GameRules.Category.MISC,
            GameRuleFactory.createIntRule(FIRST_START_PLAYERS, 2, (server, rule) -> FIRST_START_PLAYERS = rule.get() + 1)
    );
    public static boolean ENABLED = false;
    public static final GameRules.Key<GameRules.BooleanRule> ENABLE_RULE = GameRuleRegistry.register(
            MOD_ID + ":enable",
            GameRules.Category.MISC,
            GameRuleFactory.createBooleanRule(ENABLED, (server, rule) -> ENABLED = rule.get())
    );

    public static void spawnArmorStand(BlockPos pos) {
        armorStandsToSpawn.add(pos);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Khunegos");

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            final var state = StateSaver.getServerState(server);
            armorStandsToSpawn.addAll(state.armorStandsToSpawn);
            KPlayer.Manager.loadPlayers(state);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            final var state = StateSaver.getServerState(server);
            state.armorStandsToSpawn = armorStandsToSpawn;
            KPlayer.Manager.savePlayers(state);
        });

        CommandRegistrationCallback.EVENT.register(CommandHandler::bootstrap);

        ServerPlayConnectionEvents.JOIN.register(PlayerListeners::join);

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
    }
}
