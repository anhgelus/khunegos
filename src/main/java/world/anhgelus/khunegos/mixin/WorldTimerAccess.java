package world.anhgelus.khunegos.mixin;

import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import world.anhgelus.khunegos.timer.TimerAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

@Mixin(ServerWorld.class)
public class WorldTimerAccess implements TimerAccess {
    @Unique
    private final List<TickTask> tasks = new ArrayList<>();

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        tasks.stream().filter(TickTask::isRunning).forEach(TickTask::tick);
    }

    @Override
    public void timer_runTask(TimerAccess.TickTask task) {
        tasks.add(task);
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
}
