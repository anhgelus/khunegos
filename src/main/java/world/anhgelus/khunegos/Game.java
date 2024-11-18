package world.anhgelus.khunegos;

import net.minecraft.server.MinecraftServer;
import world.anhgelus.khunegos.player.Prisoner;
import world.anhgelus.khunegos.player.Task;

import java.util.*;

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
                // finish previous tasks
                final var hunterTasks = new HashSet<Task>();
                final var preyTasks = new HashSet<Task>();
                Prisoner.getPrisoners().forEach(prisoner -> {
                    // hunter tasks
                    final var hs = new HashSet<>(prisoner.getHunterTasks());
                    hs.removeIf(t -> !t.isRunning());
                    hunterTasks.addAll(hs);
                    // prey tasks
                    final var ps = new HashSet<>(prisoner.getPreyTasks());
                    ps.removeIf(t -> !t.isRunning());
                    preyTasks.addAll(ps);
                });
                hunterTasks.forEach(Task::finishTask);
                preyTasks.forEach(Task::finishTask);
                // assign new tasks
                assignTasks();
            }
        }, 60*60*20L, 20*60*20L);
        // reset day, weather, borders and tp all player to 0 0
    }

    public void assignTasks() {
        final var players = new ArrayList<>(server.getPlayerManager().getPlayerList());
        final var rand = new Random();
        while (players.size() > 1) {
            // hunter
            var n = rand.nextInt(players.size());
            final var hunter = Prisoner.from(players.get(n));
            players.remove(n);
            // prey
            n = rand.nextInt(players.size());
            final var prey = Prisoner.from(players.get(n));
            players.remove(n);
            // tasks
            hunter.addTask(new Task(Task.Role.HUNTER, hunter, prey.uuid));
            prey.addTask(new Task(Task.Role.PREY, prey, hunter.uuid));
        }
        if (players.size()%2 == 1) {
            final var prisoner = Prisoner.from(players.getFirst());
            prisoner.addTask(new Task(Task.Role.PREY, prisoner, prisoner.uuid));
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
