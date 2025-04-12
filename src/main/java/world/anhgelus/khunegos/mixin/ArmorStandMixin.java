package world.anhgelus.khunegos.mixin;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import world.anhgelus.khunegos.player.KhunegosTask;

@Mixin(ArmorStandEntity.class)
public class ArmorStandMixin {
    @Inject(method = "onBreak", at = @At("RETURN"))
    public void onBreak(ServerWorld world, DamageSource damageSource, CallbackInfo ci) {
        KhunegosTask.Manager.onArmorStandKilled((ArmorStandEntity) (Object) this);
    }
}
