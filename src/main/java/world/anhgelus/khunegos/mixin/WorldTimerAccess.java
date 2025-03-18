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
    public void dds_runTask(TimerAccess.TickTask task) {
        tasks.add(task);
    }

    @Override
    public void dds_cancel() {
        tasks.stream().filter(TickTask::isRunning).forEach(TickTask::cancel);
    }

    @Override
    public List<TickTask> dds_getTasks() {
        return tasks.stream().filter(TickTask::isRunning).toList();
    }
}
