package world.anhgelus.khunegos.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import world.anhgelus.khunegos.listener.PlayerListeners;

@Mixin(ServerPlayerEntity.class)
public class PlayerMixin {
    @Inject(at = @At("RETURN"), method = "onDisconnect")
    public void disconnect(CallbackInfo ci) {
        final var player = (ServerPlayerEntity) (Object) this;
        PlayerListeners.disconnect(player.networkHandler, player.getServer());
    }
}
