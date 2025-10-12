package world.anhgelus.khunegos.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;

public record PlayerData(float healthModifier, boolean mustClear) {
    public static final String HEALTH_MODIFIER_KEY = "health_modifier";
    public static final String MUST_CLEAR_KEY = "must_clear";
    public static final PlayerData DEFAULT;
    public static final Codec<PlayerData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf(HEALTH_MODIFIER_KEY).forGetter(PlayerData::healthModifier),
            Codec.BOOL.fieldOf(MUST_CLEAR_KEY).forGetter(PlayerData::mustClear)
    ).apply(i, PlayerData::new));

    static {
        DEFAULT = new PlayerData(0, false);
    }

    public static PlayerData from(KPlayer player) {
        return new PlayerData(player.getHealthModifier(), player.isMustClear());
    }

    public @NotNull String toString() {
        return String.format("PlayerData{health modifier=%s, must clear=%s}", healthModifier, mustClear);
    }
}