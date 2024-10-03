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
    }

    public void sendTask(ServerPlayerEntity player) {
        final var sb = new StringBuilder();
        if (role == Role.HUNTER) {
            sb.append("You are an hunter, find your prey and kill them!");
        } else {
            sb.append("You are a prey, survive!");
        }
        player.sendMessage(Text.of(sb.toString()));
    }

    public void finishTask(Prisoner prisoner) {
        prisoner.removeTask(this);
        Text message;
        float healthAttribute = 0;
        switch (role) {
            case HUNTER:
                if (win) {
                    message = Text.of("You killed your prey! You gain one heart!");
                    healthAttribute = 2.0f;
                } else {
                    message = Text.of("You didn't kill your prey! You lose two hearts!");
                    healthAttribute = -4.0f;
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
                    message = Text.of("You were killed by your hunter and you lose five hearts! They were "+
                            name.getString());
                    healthAttribute = -10.0f;
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + role);
        }
        prisoner.player().sendMessage(message);
        prisoner.modifyHealth(healthAttribute);
    }
}
