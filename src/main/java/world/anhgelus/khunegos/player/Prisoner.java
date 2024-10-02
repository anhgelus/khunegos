package world.anhgelus.khunegos.player;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

public class Prisoner {
    private static final Map<UUID, Prisoner> prisoners = new HashMap<>();

    public final UUID uuid;
    public final String name;
    private final Set<Task> tasks = new HashSet<>();
    private ServerPlayerEntity player;

    private Prisoner(ServerPlayerEntity player) {
        this.uuid = player.getUuid();
        this.player = player;
        assert player.getDisplayName() != null;
        name = player.getDisplayName().getString();
        prisoners.put(player.getUuid(), this);
    }

    public ServerPlayerEntity player() {
        return player;
    }

    public Set<Task> tasks() {
        return tasks;
    }

    public void addTask(Task task) {
        tasks.add(task);
        task.sendTask(player);
    }

    public void removeTask(Task task) {
        tasks.remove(task);
    }

    public void playerDies(ServerPlayerEntity newPlayer, DamageSource source) {
        if (source.getAttacker() instanceof final ServerPlayerEntity attacker) {
            tasks.forEach(task -> {
                if (task.role != Task.Role.PREY || task.linked != attacker.getUuid()) return;
                task.win = false;
                from(attacker).tasks.forEach(t -> {
                    if (t.role == Task.Role.HUNTER && t.linked == newPlayer.getUuid()) {
                        t.win = true;
                    }
                });
            });
        }
        player = newPlayer;
    }

    private void updatePlayer(ServerPlayerEntity player) {
        this.player = player;
    }

    public static Prisoner from(ServerPlayerEntity player) {
        final var found = prisoners.computeIfAbsent(player.getUuid(), u -> new Prisoner(player));
        if (found.player() != player) {
            found.updatePlayer(player);
        }
        return found;
    }

    public static Collection<Prisoner> prisoners() {
        return prisoners.values();
    }
}
