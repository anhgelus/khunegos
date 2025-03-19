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
                task = new KhunegosTask(khunegosHunter, khunegosPrey);
            }, MathHelper.nextInt(rand, 0, MathHelper.floor(5* Khunegos.KHUNEGOS_DURATION)) * 1000L);
            TimerAccess.getTimerFromOverworld(server).timer_runTask(delayTask);
        }

        public Incoming(MinecraftServer server) {
            this(server.getOverworld().getRandom(), server);
        }

        private boolean validPlayer(KhunegosPlayer player, boolean hunter) {
            if (hunter) return player.getMaxHealth() < 15 && player.getTask() == null;
            return player.getMaxHealth() >= 5 && player.getTask() == null;
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

    private boolean preyKilled = false;
    private boolean finished = false;

    public KhunegosTask(KhunegosPlayer hunter, KhunegosPlayer prey) {
        this.hunter = hunter;
        this.prey = prey;
        // assign tasks
        hunter.assignTask(this);
        prey.assignTask(this);
        //TODO: handle planning of end
        //TODO: broadcast starts
        //TODO: give books
    }

    public void onPreyKilled() {
        preyKilled = true;
        finish();
    }

    public void finish() {
        hunter.taskFinished(preyKilled);
        prey.taskFinished(!preyKilled);
        finished = true;
    }

    public boolean isFinished() {
        return finished;
    }

    public void onPreyDisconnection() {
        //
    }

    private static Incoming planNextOne() {
        //TODO: implements
        return null;
    }
}
