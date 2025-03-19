package world.anhgelus.khunegos.player;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import world.anhgelus.khunegos.Khunegos;
import world.anhgelus.khunegos.timer.TickTask;
import world.anhgelus.khunegos.timer.TimerAccess;

import java.util.ArrayList;

public class KhunegosTask {
    public static class Incoming {
        public final TickTask delayTask;
        public KhunegosTask task;

        public Incoming(Random rand, MinecraftServer server) {
            this.delayTask = new TickTask(() -> {
                final var players = new ArrayList<>(server.getPlayerManager().getPlayerList());;
                // get hunter
                var hunter = players.get(MathHelper.nextInt(rand, players.size(), players.size() - 1));
                players.remove(hunter);
                var khunegosHunter = KhunegosPlayer.Manager.getKhunegosPlayer(hunter);
                while (players.size() >= 2 && !validPlayer(khunegosHunter, true)) {
                    hunter = players.get(MathHelper.nextInt(rand, players.size(), players.size() - 1));
                    players.remove(hunter);
                    khunegosHunter = KhunegosPlayer.Manager.getKhunegosPlayer(hunter);
                }
                // verify validity
                if (!validPlayer(khunegosHunter, true) || players.size() < 2) {
                    Khunegos.LOGGER.error("Cannot find a valid player for being a hunter");
                    return;
                }
                // get prey
                var prey = players.get(MathHelper.nextInt(rand, players.size(), players.size() - 1));
                players.remove(prey);
                var khunegosPrey = KhunegosPlayer.Manager.getKhunegosPlayer(prey);
                while (players.size() >= 2 && !validPlayer(khunegosPrey, false)) {
                    prey = players.get(MathHelper.nextInt(rand, players.size(), players.size() - 1));
                    players.remove(prey);
                    khunegosPrey = KhunegosPlayer.Manager.getKhunegosPlayer(prey);
                }
                // verify validity
                if (!validPlayer(khunegosPrey, false) || players.size() < 2) {
                    Khunegos.LOGGER.error("Cannot find a valid player for being a prey");
                    return;
                }
                task = new KhunegosTask(server, khunegosHunter, khunegosPrey);
            }, MathHelper.nextInt(rand, 0, MathHelper.floor(5* Khunegos.KHUNEGOS_BASE_DELAY)) * 1000L);
            TimerAccess.getTimerFromOverworld(server).timer_runTask(delayTask);
        }

        public Incoming(MinecraftServer server) {
            this(server.getOverworld().getRandom(), server);
        }

        private boolean validPlayer(KhunegosPlayer player, boolean hunter) {
            return hunter ? player.getMaxHealth() < 15 && player.getTask() == null :
                    player.getMaxHealth() >= 5 && player.getTask() == null;
        }

        public boolean isKhunegosTask() {
            if (!delayTask.isRunning() && task == null) {
                Khunegos.LOGGER.error("Task in incoming is null");
                return false;
            }
            return !delayTask.isRunning() && !task.isFinished();
        }

        /**
         * @throws IllegalStateException if {@link #isKhunegosTask()} is true
         */
        public void cancel() {
            if (isKhunegosTask()) throw new IllegalStateException("Cannot cancel KhunegosTask");
            delayTask.cancel();
        }
    }

    public final KhunegosPlayer hunter;
    public final KhunegosPlayer prey;

    private final TickTask task;

    private boolean preyKilled = false;
    private boolean finished = false;

    private KhunegosTask(MinecraftServer server, KhunegosPlayer hunter, KhunegosPlayer prey) {
        this.hunter = hunter;
        this.prey = prey;
        // assign tasks
        hunter.assignTask(this);
        prey.assignTask(this);

        final var d = MathHelper.nextFloat(server.getOverworld().getRandom(), 0, 1);
        final var duration = MathHelper.floor(20*(Khunegos.KHUNEGOS_DURATION + d));

        final var timer = TimerAccess.getTimerFromOverworld(server);
        task = new TickTask(this::finish, duration*1000L);
        timer.timer_runTask(task);
        //TODO: broadcast starts
        //TODO: give books
    }

    public void onPreyKilled() {
        preyKilled = true;
        finish();
        task.cancel();
    }

    private void finish() {
        if (finished) {
            Khunegos.LOGGER.warn("Khunegos already finished");
            return;
        }
        hunter.taskFinished(preyKilled);
        prey.taskFinished(!preyKilled);
        finished = true;
        //TODO: plan next one
    }

    public boolean isFinished() {
        return finished;
    }

    public void onPreyDisconnection() {
        //
    }
}
