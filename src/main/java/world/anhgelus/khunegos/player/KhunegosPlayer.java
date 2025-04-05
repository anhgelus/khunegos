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
import world.anhgelus.khunegos.StateSaver;

import java.util.*;

public class KhunegosPlayer {
    public static final Identifier HEALTH_MODIFIER = Identifier.of(Khunegos.MOD_ID, "health_modifier");
    public static final String PLAYER_KEY = Khunegos.BASE_KEY + "_player"; // UUID of player
    private final UUID uuid;
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
        this.uuid = player.getUuid();
    }

    public KhunegosPlayer(UUID uuid, float healthModifier) {
        Khunegos.LOGGER.info("Creating KhunegosPlayer with health modifier");
        this.uuid = uuid;
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
        if (!newPlayer.getUuid().equals(uuid)) throw new IllegalArgumentException("Player does not have the same UUID");
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
        final var is = new ItemStack(Items.WRITTEN_BOOK);
        is.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, getBookContent());
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
        return String.format("%d %d %d", coords.getX(), coords.getY(), coords.getZ());
    }

    public WrittenBookContentComponent getBookContent() {
        final var rawContent = new ArrayList<RawFilteredPair<Text>>();
        if (role == Role.NONE) {
            rawContent.add(RawFilteredPair.of(Text.of("You are not in a Khunegos.")));
        } else {
            assert task != null; // is valid because role != none
            final var role = getRole() == Role.HUNTER ? "§cHunter" : "§2Prey";
            final var sb = new StringBuilder();
            sb.append("You are a §o§l").append(role).append("§r.\n\n");
            final var cal = Calendar.getInstance(Locale.FRANCE);
            final var minBeforeEnd = task.getTicksBeforeEnd() / (60 * 20);
            var endHour = (cal.get(Calendar.HOUR_OF_DAY) + minBeforeEnd / 60) % 24;
            var minuteEndHour = cal.get(Calendar.MINUTE) + minBeforeEnd;
            while (minuteEndHour >= 60) {
                endHour = (endHour + 1) % 64;
                minuteEndHour %= 60;
            }
            sb.append("End at §l")
                    .append(endHour)
                    .append(":")
                    .append(minuteEndHour)
                    .append(" §r(")
                    .append(TimeZone.getDefault().getDisplayName().split("/")[1])
                    .append(" timezone).")
                    .append("\n\n");
            sb.append("Use §l/coords§r to get your prey's coords");
            rawContent.add(RawFilteredPair.of(Text.of(sb.toString())));
        }
        return new WrittenBookContentComponent(
                RawFilteredPair.of("Khunegos"),
                "Khunegos",
                0,
                rawContent,
                true // I don't know what this do
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

        public static void loadPlayers(StateSaver state) {
            state.players.forEach((uuid, data) -> players.put(uuid, new KhunegosPlayer(uuid, data)));
        }

        public static void savePlayers(StateSaver state) {
            players.forEach((u, khunegosPlayer) -> {
                state.players.put(u, khunegosPlayer.healthModifier);
            });
        }
    }
}
