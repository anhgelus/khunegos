package world.anhgelus.khunegos.player;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import world.anhgelus.khunegos.Khunegos;
import world.anhgelus.khunegos.timer.TickTask;
import world.anhgelus.khunegos.timer.TimerAccess;

public class KhunegosTask {
    public static class Incoming {
        public final TickTask delayTask;
        public KhunegosTask task;

        public Incoming(Random rand, MinecraftServer server) {
            this.delayTask = new TickTask(() -> {
                task = new KhunegosTask(null, null);
            }, MathHelper.nextInt(rand, 0, MathHelper.floor(5* Khunegos.KHUNEGOS_DURATION)));
            TimerAccess.getTimerFromOverworld(server).timer_runTask(delayTask);
        }

        public Incoming(MinecraftServer server) {
            this(server.getOverworld().getRandom(), server);
        }

        public boolean isKhunegosTask() {
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
