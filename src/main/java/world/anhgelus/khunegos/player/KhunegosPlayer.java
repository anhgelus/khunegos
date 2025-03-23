package world.anhgelus.khunegos.player;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import world.anhgelus.khunegos.Khunegos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KhunegosPlayer {
    public static final Identifier HEALTH_MODIFIER = Identifier.of(Khunegos.MOD_ID, "health_modifier");
    public static final String PLAYER_KEY = Khunegos.BASE_KEY + "_player"; // UUID of player
    public static final String BOOK_KEY = Khunegos.BASE_KEY + "_book"; // is a khunegos book

    private ServerPlayerEntity player;
    private Role role = Role.NONE;
    private float healthModifier = 0;
    /**
     * Is null if {@link #role} == Role.NONE.
     * If not, throw an {@link IllegalStateException}
     */
    @Nullable
    private KhunegosTask task = null;

    public KhunegosPlayer(ServerPlayerEntity player) {
        this.player = player;
    }

    public KhunegosPlayer(ServerPlayerEntity player, float healthModifier) {
        this.player = player;
        this.healthModifier = healthModifier;
    }

    public void onDeath(boolean killedByPlayer) {
        if (!killedByPlayer || role != Role.PREY) return;
        // save uuid in nbt
        final var nbt = new NbtCompound();
        nbt.putUuid(PLAYER_KEY, player.getUuid());
        // create itemstack
        final var is = new ItemStack(Items.NETHER_STAR);
        is.set(DataComponentTypes.CUSTOM_NAME, player.getName());
        is.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        // drop
        player.dropItem(is, true, false);
    }

    public void onRespawn(ServerPlayerEntity newPlayer) {
        this.player = newPlayer;
        updateHealth();
    }

    public void onDeposeHeart() {
        healthModifier += 2;
        updateHealth();
    }

    public void assignTask(@NotNull KhunegosTask task) {
        this.task = task;
        if (task.hunter == this) role = Role.HUNTER;
        else role = Role.PREY;
    }

    public void taskFinished(boolean success) {
        if (!success) {
            healthModifier -= 2;
            updateHealth();
        }
        this.task = null;
        role = Role.NONE;
    }

    public void giveBook() {
        // prevents giving same book
        if (player.getInventory().contains(is -> {
            final var nbt = is.get(DataComponentTypes.CUSTOM_DATA);
            return nbt != null && nbt.contains(BOOK_KEY) && nbt.copyNbt().getBoolean(BOOK_KEY);
        })) return;
        final var is = new ItemStack(Items.BOOK);
        final var nbt = new NbtCompound();
        nbt.putBoolean(BOOK_KEY, true);
        is.set(DataComponentTypes.CUSTOM_NAME, Text.of("Khunegos"));
        is.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        player.giveOrDropStack(is);
    }

    /**
     * Task is null if {@link #getRole()} is {@link Role} none
     *
     * @return Current task
     * @throws IllegalStateException if task == null and if {@link Role} is not none
     */
    public @Nullable KhunegosTask getTask() {
        if (task == null && role != Role.NONE) throw new IllegalStateException("No task assigned to KhunegosPlayer");
        return task;
    }

    public Role getRole() {
        return role;
    }

    public int getMaxHearts() {
        return MathHelper.floor((20 + healthModifier) / 2);
    }

    public BlockPos getCoords() {
        return player.getBlockPos();
    }

    public String getCoordsString() {
        final var coords = getCoords();
        return "x=" + coords.getX() + " y=" + coords.getY() + " z=" + coords.getZ();
    }

    public WrittenBookContentComponent getBookContent() {
        final var rawContent = new ArrayList<RawFilteredPair<Text>>();
        if (role == Role.NONE) {
            rawContent.add(RawFilteredPair.of(Text.of("You are not in a Khunegos.")));
        } else {
            final var role = getRole() == Role.HUNTER ? "hunter" : "prey";
            rawContent.add(RawFilteredPair.of(Text.of("You are a " + role)));
            if (getRole() == Role.HUNTER) {
                rawContent.add(RawFilteredPair.of(Text.of("")));
                assert task != null; // is valid because role != none
                rawContent.add(RawFilteredPair.of(Text.of(task.prey.getCoordsString())));
            }
        }
        return new WrittenBookContentComponent(
                RawFilteredPair.of("Khunegos"),
                player.getName().getString(),
                0,
                rawContent,
                false // I don't know what this do
        );
    }

    private void updateHealth() {
        healthModifier = MathHelper.clamp(healthModifier, Khunegos.MIN_RELATIVE_HEALTH, Khunegos.MAX_RELATIVE_HEALTH);
        final var attr = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (attr == null) throw new IllegalStateException("No health attribute assigned to KhunegosPlayer");
        if (attr.hasModifier(HEALTH_MODIFIER)) attr.removeModifier(HEALTH_MODIFIER);
        final var modifier = new EntityAttributeModifier(HEALTH_MODIFIER, healthModifier, EntityAttributeModifier.Operation.ADD_VALUE);
        attr.addTemporaryModifier(modifier);
    }

    public UUID getUuid() {
        return player.getUuid();
    }

    public String toString() {
        final var sb = new StringBuilder();
        sb.append("KhunegosPlayer{")
                .append("role=").append(role)
                .append(", player=").append(player)
                .append(", task=").append(task)
                .append("}").append(role);
        return sb.toString();
    }

    public enum Role {
        HUNTER,
        PREY,
        NONE
    }

    public static class Manager {
        private static final Map<UUID, KhunegosPlayer> players = new HashMap<>();

        public static KhunegosPlayer getKhunegosPlayer(ServerPlayerEntity player) {
            return players.computeIfAbsent(player.getUuid(), k -> new KhunegosPlayer(player));
        }

        @Nullable
        public static KhunegosPlayer getKhunegosPlayer(UUID uuid) {
            return players.get(uuid);
        }
    }
}
