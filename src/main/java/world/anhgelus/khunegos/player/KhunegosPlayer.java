package world.anhgelus.khunegos.player;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import world.anhgelus.khunegos.Khunegos;

public class KhunegosPlayer {
    public enum Role {
        HUNTER,
        PREY,
        NONE
    }

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

    public void onRespawn(ServerPlayerEntity newPlayer) {
        this.player = newPlayer;
        //TODO: update attribute
    }

    public void assignTask(@NotNull KhunegosTask task) {
        this.task = task;
        if (task.hunter == this) role = Role.HUNTER;
        else role = Role.PREY;
    }

    public void taskFinished(boolean success) {
        if (!success) {
            healthModifier -= 2;
            // it will be in update attribute
            // healthModifier = MathHelper.clamp(healthModifier, Khunegos.MIN_RELATIVE_HEALTH, Khunegos.MAX_RELATIVE_HEALTH);
            //TODO: update attribute
        }
        this.task = null;
        role = Role.NONE;
    }

    /**
     * Task is null if {@link #getRole()} is {@link Role} none
     * @throws IllegalStateException if task == null and if {@link Role} is not none
     * @return Current task
     */
    public @Nullable KhunegosTask getTask() {
        if (task == null && role != Role.NONE) throw new IllegalStateException("No task assigned to KhunegosPlayer");
        return task;
    }

    public Role getRole() {
        return role;
    }

    public int getMaxHealth() {
        return (int) Math.floor((20 + healthModifier) / 2);
    }
}
