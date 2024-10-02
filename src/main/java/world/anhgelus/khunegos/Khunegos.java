package world.anhgelus.khunegos;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import world.anhgelus.khunegos.player.Prisoner;

import java.util.HashMap;
import java.util.Map;

public class Khunegos implements ModInitializer {
    public static final Identifier HEALTH_MODIFIER_ID = Identifier.of("khunegos_health_modifier");

    private final Map<ServerPlayerEntity, DamageSource> damageSources = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Prisoner.from(handler.getPlayer()));

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            damageSources.put(player, source);
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            Prisoner.from(oldPlayer).playerDies(newPlayer, damageSources.get(oldPlayer));
        });
    }
}
