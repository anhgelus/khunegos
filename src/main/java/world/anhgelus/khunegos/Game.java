package world.anhgelus.khunegos;

import net.minecraft.server.MinecraftServer;
import world.anhgelus.khunegos.player.Prisoner;
import world.anhgelus.khunegos.player.Task;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class Game {
    private Timer timer = new Timer();
    private boolean started = false;
    private final MinecraftServer server;

    public Game(MinecraftServer server) {
        this.server = server;
    }

    /**
     * @throws IllegalStateException if game is already started
     */
    public void start() {
        if (started) throw new IllegalStateException("Game is already started");
        started = true;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                assignTask();
            }
        }, 60*60*20L, 20*60*20L);
        // reset day, weather, borders and tp all player to 0 0
    }

    public void assignTask() {
        final var players = server.getPlayerManager().getPlayerList();
        final var s = players.size();
        final var rand = new Random();
        while (!players.isEmpty() && (s%2 == 1 && players.size() > 1)) {
            // hunter
            var n = rand.nextInt(players.size());
            final var hunter = Prisoner.from(players.get(n));
            players.remove(n);
            // prey
            n = rand.nextInt(players.size());
            final var prey = Prisoner.from(players.get(n));
            players.remove(n);
            // tasks
            hunter.addTask(new Task(Task.Role.HUNTER, prey.uuid));
            prey.addTask(new Task(Task.Role.PREY, hunter.uuid));
        }
        if (s%2 == 1) {
            final var prisoner = Prisoner.from(players.getFirst());
            prisoner.addTask(new Task(Task.Role.PREY, prisoner.uuid));
        }
    }

    /**
     * @throws IllegalStateException if game did not start
     */
    public void stop() {
        if (!started) throw new IllegalStateException("Game did not start");
        started = false;
        timer.cancel();
        timer = new Timer();
    }
}
