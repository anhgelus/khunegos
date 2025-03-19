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
    }

    public void onPreyKilled() {
        preyKilled = true;
        //TODO: drop prey's heart
    }

    public void finish() {
        hunter.taskFinished(preyKilled);
        prey.taskFinished(!preyKilled);
    }

    public void onPreyDisconnection() {
        //
    }
}
