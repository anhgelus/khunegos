package world.anhgelus.khunegos.mixin;

import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import world.anhgelus.khunegos.Khunegos;
import world.anhgelus.khunegos.timer.TickAccess;
import world.anhgelus.khunegos.timer.TimerAccess;

import java.util.*;
import java.util.function.BooleanSupplier;

@Mixin(ServerWorld.class)
public class WorldTickAccess implements TimerAccess, TickAccess {
    @Unique
    private final List<TickTask> tasks = new ArrayList<>();
    @Unique
    private final List<TickTask> tasksToRun = new ArrayList<>();
    @Unique
    private final Set<Ticker> tickers = new HashSet<>();
    @Unique
    private final Set<Ticker> tickersToAdd = new HashSet<>();
    @Unique
    private final Set<Ticker> tickersToRemove = new HashSet<>();

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        // tasks
        tasks.stream().filter(TickTask::isRunning).forEach(t -> {
            try {
                t.tick();
            } catch (Exception e) {
                Khunegos.LOGGER.error(
                        "Caught exception during tick (task)",
                        new TimerAccess.TimerException("An error occurred while running TickTask", t, e)
                );
            }
        });
        tasks.addAll(tasksToRun);
        tasksToRun.clear();
        // tickers
        tickers.forEach(t -> {
            try {
                t.tick();
            } catch (Exception e) {
                Khunegos.LOGGER.error(
                        "Caught exception during tick (ticker)",
                        new TickAccess.TickerException("An error occurred while running Ticker", e)
                );
            }
        });
        tickers.removeAll(tickersToRemove);
        tickersToRemove.clear();
        tickers.addAll(tickersToAdd);
        tickersToAdd.clear();
    }

    @Override
    public void timer_runTask(TimerAccess.TickTask task) {
        tasksToRun.add(task);
    }

    @Override
    public void timer_cancel() {
        tasks.stream().filter(TickTask::isRunning).forEach(TickTask::cancel);
    }

    @Override
    public List<TickTask> timer_getTasks() {
        return tasks.stream().filter(TickTask::isRunning).toList();
    }

    @Override
    public String timer_toString() {
        final var sb = new StringBuilder();
        sb.append("WorldTimerAccess(")
                .append("number of tasks=").append(tasks.size());
        tasks.forEach(task -> sb.append(", ").append(task.toString()));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public void tick_add(Ticker ticker) {
        tickersToAdd.add(ticker);
    }

    @Override
    public void tick_remove(Ticker ticker) {
        tickersToRemove.remove(ticker);
    }

    @Override
    public Collection<Ticker> tick_get() {
        return tickers;
    }

    @Override
    public String tick_toString() {
        final var sb = new StringBuilder();
        sb.append("WorldTickAccess(")
                .append("number of tasks=").append(tickers.size());
        tickers.forEach(t -> sb.append(", ").append(t.toString()));
        sb.append(")");
        return sb.toString();
    }
}
