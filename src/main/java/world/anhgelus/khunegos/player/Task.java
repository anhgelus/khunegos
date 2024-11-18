package world.anhgelus.khunegos.player;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

public class Task {
    public final Role role;
    public final UUID linked;
    public final Prisoner prisoner;
    public boolean win;
    private boolean running = true;

    public Task(Role role, Prisoner prisoner, UUID linked) {
        this.prisoner = prisoner;
        this.role = role;
        this.linked = linked;
        win = role == Role.PREY;
    }

    public enum Role {
        PREY,
        HUNTER,
    }

    public void sendTask(ServerPlayerEntity player) {
        if (role == Role.HUNTER)
            player.sendMessage(Text.of("You are an hunter, find your prey and kill them! You can find their coordinates in your hunt's book."));
        else
            player.sendMessage(Text.of("You are a prey, survive!"));
    }

    public void finishTask() {
        if (!running) throw new IllegalStateException("Cannot finish task an already finished task");
        Text message;
        float healthAttribute = 0;
        if (role == Role.HUNTER) {
            if (win) {
                message = Text.of("You killed your prey! You gain one heart!");
                healthAttribute = 2.0f;
                running = false;
                prisoner.removeTask(this);
            } else {
                message = Text.of("You didn't kill your prey! You lose two hearts!");
                healthAttribute = -4.0f;
            }
        } else if (role == Role.PREY) {
            // if the player is not linked, task is not important
            if (prisoner.uuid == linked) {
                running = false;
                return;
            }

            final var server = prisoner.player().getServer();
            if (server == null) throw new IllegalStateException("Server is null");
            final var hunter = server.getPlayerManager().getPlayer(linked);
            if (hunter == null) throw new IllegalStateException("Player is null (not online?)");

            // check if linked hunter's task is running
            for (final var task : Prisoner.from(hunter).getHunterTasks())
                if (task.linked == prisoner.uuid && task.running) return;

            final var name = hunter.getDisplayName();
            assert name != null;
            if (win) {
                message = Text.of("You survived! Your hunter was "+name.getString());
            } else {
                message = Text.of("You were killed by your hunter and you lose five hearts! They were "+
                        name.getString());
                healthAttribute = -10.0f;
            }
            running = false;
            prisoner.removeTask(this);
        } else {
            throw new IllegalStateException("Unexpected value: " + role);
        }
        prisoner.player().sendMessage(message);
        prisoner.modifyHealth(healthAttribute);
    }

    public boolean isRunning() {
        return running;
    }
}
