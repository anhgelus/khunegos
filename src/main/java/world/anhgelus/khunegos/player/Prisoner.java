package world.anhgelus.khunegos.player;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import world.anhgelus.khunegos.Khunegos;

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

    public void modifyHealth(float health) {
        if (health == 0) return;
        final var instance = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (instance == null) return;

        final var previous = instance.getModifier(Khunegos.HEALTH_MODIFIER_ID);
        if (previous != null) {
            health += (float) previous.value();
        }
        instance.removeModifier(Khunegos.HEALTH_MODIFIER_ID);

        if (health <= -20) {
            player.changeGameMode(GameMode.SPECTATOR);
            final var server = player.getServer();
            if (server == null) throw new IllegalStateException("Server is null");
            server.sendMessage(Text.of(name + " lost all their hearts!"));
            return;
        } else if (health >= 10) {
            //TODO: starts Katarros if total health >= 15
        }

        final var playerHealthModifier = new EntityAttributeModifier(
                Khunegos.HEALTH_MODIFIER_ID,
                health,
                EntityAttributeModifier.Operation.ADD_VALUE
        );
        instance.addPersistentModifier(playerHealthModifier);
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
