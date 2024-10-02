package world.anhgelus.khunegos.player;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

public class Task {
    public final Role role;
    public final UUID linked;
    public boolean win;

    public Task(Role role, UUID linked) {
        this.role = role;
        this.linked = linked;
        win = role == Role.PREY;
    }

    public enum Role {
        PREY,
        HUNTER,
        NONE
    }

    public static Task NONE = new Task(Role.NONE, null);

    public void sendTask(ServerPlayerEntity player) {
        if (role == Role.NONE) return;
        final var sb = new StringBuilder();
        if (role == Role.HUNTER) {
            sb.append("You are an hunter, find your prey and kill them!");
        } else {
            sb.append("You are a prey, survive!");
        }
        player.sendMessage(Text.of(sb.toString()));
    }

    public void finishTask(Prisoner prisoner) {
        if (role == Role.NONE) throw new IllegalStateException("Cannot finish a task with none role");
        prisoner.removeTask(this);
        Text message;
        switch (role) {
            case HUNTER:
                if (win) {
                    message = Text.of("You killed your prey! You gain one heart!");
                } else {
                    message = Text.of("You didn't kill your prey! You lose two hearts!");
                }
                break;
            case PREY:
                final var server = prisoner.player().getServer();
                if (server == null) throw new IllegalStateException("Server is null");
                final var hunter = server.getPlayerManager().getPlayer(linked);
                if (hunter == null) throw new IllegalStateException("Player is null (not online?)");
                final var name = hunter.getDisplayName();
                assert name != null;
                if (win) {
                    message = Text.of("You survived! Your hunter was "+name.getString());
                } else {
                    message = Text.of("You were killed by your hunter! They were "+name.getString());
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + role);
        }
        prisoner.player().sendMessage(message);
    }
}
