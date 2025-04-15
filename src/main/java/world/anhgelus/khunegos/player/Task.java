package world.anhgelus.khunegos.player;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import world.anhgelus.khunegos.Khunegos;
import world.anhgelus.khunegos.timer.TickAccess;
import world.anhgelus.khunegos.timer.TickTask;
import world.anhgelus.khunegos.timer.TimerAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents the task to complete during Khunegos for hunter and prey
 */
public class Task {
    public final KhunegosPlayer hunter;
    public final KhunegosPlayer prey;
    private final TickTask task;
    private final MinecraftServer server;
    private final long duration;
    private final TickAccess.Ticker ticker;
    private boolean preyKilled = false;
    private boolean finished = false;

    private Task(MinecraftServer server, KhunegosPlayer hunter, KhunegosPlayer prey) {
        this.hunter = hunter;
        this.prey = prey;
        this.server = server;
        // assign tasks
        hunter.assignTask(this);
        prey.assignTask(this);
        // create timer to finish and to starts new task
        final var d = MathHelper.nextFloat(server.getOverworld().getRandom(), 0, 1);
        duration = MathHelper.floor(20 * (Khunegos.KHUNEGOS_DURATION + d));
        final var timer = TimerAccess.getTimerFromOverworld(server);
        task = new TickTask(() -> {
            final var next = finish();
            if (next != null) Manager.addTask(next);
        }, duration * 1000L);
        timer.timer_runTask(task);

        server.getPlayerManager().broadcast(Text.of("A new Khunegos starts! Check your inventory."), false);

        hunter.giveBook();
        prey.giveBook();

        ticker = () -> {
            if (prey.isConnected()) return;
            if (hunter.getCoords().equals(prey.getCoords())) onPreyKilled();
        };
        TickAccess.getTickFromOverworld(server).tick_add(ticker);
    }

    /**
     * @return ticks before run
     */
    public long getTicksBeforeEnd() {
        return task.getTickingBeforeRun();
    }

    public Incoming onPreyKilled() {
        preyKilled = true;
        final var in = finish();
        task.cancel();
        return in;
    }

    @Nullable
    private Incoming finish() {
        if (finished) {
            Khunegos.LOGGER.warn("Khunegos already finished");
            return null;
        }
        hunter.taskFinished(preyKilled);
        prey.taskFinished(!preyKilled);
        finished = true;
        TickAccess.getTickFromOverworld(server).tick_remove(ticker);
        // start new one
        if (Manager.canServerStartsNewTask(server)) return new Incoming(server, false);
        return null;
    }

    public boolean isFinished() {
        return finished;
    }

    public String toString() {
        return String.format(
                "KhunegosTask{duration=%d, hunter=%s, prey=%s} (finished=%b, tick task=%s)",
                duration, hunter.getUuid().toString(), prey.getUuid().toString(), finished, task
        );
    }

    /**
     * Manage all tasks
     */
    public static class Manager {
        private static List<Incoming> khunegosTaskList = new ArrayList<>();

        /**
         * @return copy of the khunegos task list
         */
        public static List<Incoming> getTasks() {
            return List.copyOf(khunegosTaskList);
        }

        /**
         * @return true if a task was removed, false otherwise
         */
        public static boolean removeRandomTask(Random rand) {
            final var list = khunegosTaskList
                    .stream()
                    .filter(i -> !i.isKhunegosTask())
                    .collect(Collectors.toCollection(ArrayList::new));
            if (list.isEmpty()) return false;
            final var rem = list.get(rand.nextBetween(0, list.size() - 1));
            return khunegosTaskList.remove(rem);
        }

        public static void updateIncomingTasks(MinecraftServer server) {
            if (!canServerStartsNewTask(server)) return;
            if (!removeRandomTask(server.getOverworld().getRandom()))
                Khunegos.LOGGER.warn("Failed to remove a random task");
        }

        public static boolean canServerStartsNewTask(MinecraftServer server) {
            return canServerStartsNewTask(server, false);
        }

        public static boolean canServerStartsNewTask(MinecraftServer server, boolean bl) {
            var size = server.getPlayerManager().getPlayerList().size();
            if (bl) size++;
            return size - 2 >= getTasks().size() / 2;
        }

        public static void addTask(Incoming incoming) {
            if (incoming == null) return;
            khunegosTaskList.add(incoming);
            removeAllFinishedTask();
        }

        private static void removeAllFinishedTask() {
            khunegosTaskList = khunegosTaskList
                    .stream()
                    .dropWhile(t -> t.isKhunegosTask() && t.getTask().orElseThrow().isFinished())
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        /**
         * @throws IllegalArgumentException if {@link Incoming#isKhunegosTask()} is true for the given task
         */
        public static void removeTask(Incoming incoming) {
            if (incoming.isKhunegosTask()) throw new IllegalArgumentException("Cannot cancel KhunegosTask");
            incoming.cancel();
            khunegosTaskList.remove(incoming);
        }

        /**
         * @throws IllegalArgumentException if cannot find an incoming task with the given task
         * @throws IllegalArgumentException if {@link Incoming#isKhunegosTask()} is true for the given task
         */
        public static void removeTask(Task task) {
            final var in = khunegosTaskList.stream().filter(i -> i.getTask().orElseThrow() == task).findFirst().orElse(null);
            if (in == null) throw new IllegalArgumentException("Cannot remove a non-existent task");
            removeTask(in);
        }
    }

    /**
     * Represents an incoming {@link Task} delayed
     */
    public static class Incoming {
        public final TickTask delayTask;
        @Nullable
        private Task task = null;

        public Incoming(Random rand, MinecraftServer server, boolean first) {
            if (!Manager.canServerStartsNewTask(server) && !first)
                throw new IllegalStateException("Cannot start a new Khunegos task");
            final var m = MathHelper.floor(5 * Khunegos.KHUNEGOS_BASE_DELAY);
            final var t = MathHelper.nextInt(rand, -m, m);
            // if first, delay is in [0, 5*alpha[, else is 20(alpha + t) where t is in [-5 alpha; 5 alpha]
            final var delay = first ? (t + m) % m : MathHelper.floor(20 * (Khunegos.KHUNEGOS_BASE_DELAY + t));
//            final var delay = 1;
            delayTask = new TickTask(() -> {
                final var players = new ArrayList<>(server.getPlayerManager().getPlayerList());
                final var khunegosHunter = getRandomPlayer(players, rand, true);
                if (khunegosHunter == null) {
                    Khunegos.LOGGER.error("Cannot find a valid player for being a hunter");
                    return;
                }
                final var khunegosPrey = getRandomPlayer(players, rand, false);
                if (khunegosPrey == null) {
                    Khunegos.LOGGER.error("Cannot find a valid player for being a prey");
                    return;
                }
                task = new Task(server, khunegosHunter, khunegosPrey);
            }, delay * 1000L);
            TimerAccess.getTimerFromOverworld(server).timer_runTask(delayTask);
        }

        public Incoming(MinecraftServer server, boolean first) {
            this(server.getOverworld().getRandom(), server, first);
        }

        /**
         * @param players  list of players to use
         * @param rand     random to use
         * @param isHunter if the player could be a hunter (if false, player could be a prey)
         * @return the player or null if impossible to find a valid player
         */
        @Nullable
        private KhunegosPlayer getRandomPlayer(List<ServerPlayerEntity> players, Random rand, boolean isHunter) {
            if (players.isEmpty()) return null;
            var p = players.get(MathHelper.nextInt(rand, 0, players.size() - 1));
            players.remove(p);
            var pk = KhunegosPlayer.Manager.getKhunegosPlayer(p);
            while (!players.isEmpty() && !validPlayer(pk, isHunter)) {
                p = players.get(MathHelper.nextInt(rand, 0, players.size() - 1));
                players.remove(p);
                pk = KhunegosPlayer.Manager.getKhunegosPlayer(p);
            }
            // verify validity
            return validPlayer(pk, isHunter) ? pk : null;
        }

        private boolean validPlayer(KhunegosPlayer player, boolean hunter) {
            return hunter ? player.getMaxHearts() < 10 + Khunegos.MAX_RELATIVE_HEALTH && player.getTask().isEmpty() :
                    player.getMaxHearts() > 10 + Khunegos.MIN_RELATIVE_HEALTH && player.getTask().isEmpty();
        }

        public boolean isKhunegosTask() {
            if (!delayTask.isRunning() && task == null) {
                Khunegos.LOGGER.error("Task in incoming is null");
                return false;
            }
            return !delayTask.isRunning() && !getTask().orElseThrow().isFinished();
        }

        public Optional<Task> getTask() {
            return task == null ? Optional.empty() : Optional.of(task);
        }

        /**
         * @throws IllegalStateException if {@link #isKhunegosTask()} is true
         */
        public void cancel() {
            if (isKhunegosTask()) throw new IllegalStateException("Cannot cancel KhunegosTask");
            delayTask.cancel();
            Manager.removeTask(this);
        }

        public String toString() {
            return String.format("Incoming(is khunegos task=%b, tick delay task=%s, khunegos task=%s)", isKhunegosTask(), delayTask, task);
        }
    }
}
