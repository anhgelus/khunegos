package world.anhgelus.khunegos.player;

public class KhunegosTask {
    public final KhunegosPlayer hunter;
    public final KhunegosPlayer prey;

    private boolean preyKilled = false;

    public KhunegosTask(KhunegosPlayer hunter, KhunegosPlayer prey) {
        this.hunter = hunter;
        this.prey = prey;
    }

    public void onPreyKilled() {
        preyKilled = true;
        //TODO: drop prey's heart
    }

    public void finish() {
        hunter.taskFinished(preyKilled);
        prey.taskFinished(!preyKilled);
    }
}
