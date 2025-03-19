package world.anhgelus.khunegos.player;

public class KhunegosTask {
    public final KhunegosPlayer hunter;
    public final KhunegosPlayer prey;

    private boolean preyKilled = false;

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
    }

    public void finish() {
        hunter.taskFinished(preyKilled);
        prey.taskFinished(!preyKilled);
    }

    public void onPreyDisconnection() {
        //
    }

    private static KhunegosTask planNextOne() {
        //TODO: implements
        return null;
    }

    public static void cancelOneNext() {
        //TODO: cancel one planned random next task
    }
}
