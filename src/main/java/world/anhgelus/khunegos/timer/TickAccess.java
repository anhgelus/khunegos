package world.anhgelus.khunegos.timer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import java.util.Collection;

public interface TickAccess {
    /**
     * Get the tick linked to the overworld
     *
     * @param server Current server
     * @return TickAccess linked to the overworld
     */
    static TickAccess getTickFromOverworld(MinecraftServer server) {
        final var tick = (TickAccess) server.getWorld(World.OVERWORLD);
        if (tick == null)
            throw new NullPointerException("Impossible to get TickAccess from the overworld (it is null)");
        return tick;
    }

    void tick_add(Ticker ticker);

    void tick_remove(Ticker ticker);

    Collection<Ticker> tick_get();

    String tick_toString();

    @FunctionalInterface
    interface Ticker {
        /**
         * Tick the task
         */
        void tick();
    }
}
