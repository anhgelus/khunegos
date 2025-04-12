package world.anhgelus.khunegos.player;

import net.minecraft.nbt.NbtCompound;

public class PlayerData {
    public static final String HEALTH_MODIFIER_KEY = "health_modifier";
    public static final String MUST_CLEAR_KEY = "must_clear";
    public static final PlayerData DEFAULT;

    static {
        DEFAULT = new PlayerData();
        DEFAULT.mustClear = false;
        DEFAULT.healthModifier = 0;
    }

    public float healthModifier = 0;
    public boolean mustClear = false;

    public static PlayerData from(NbtCompound nbt) {
        final var data = new PlayerData();
        data.healthModifier = nbt.getFloat(HEALTH_MODIFIER_KEY);
        data.mustClear = nbt.getBoolean(MUST_CLEAR_KEY);
        return data;
    }

    public static PlayerData from(KhunegosPlayer player) {
        final var data = new PlayerData();
        data.healthModifier = player.getHealthModifier();
        data.mustClear = player.isMustClear();
        return data;
    }

    public NbtCompound save(NbtCompound nbt) {
        nbt.putFloat(HEALTH_MODIFIER_KEY, healthModifier);
        nbt.putBoolean(MUST_CLEAR_KEY, mustClear);
        return nbt;
    }

    public NbtCompound save() {
        return save(new NbtCompound());
    }
}