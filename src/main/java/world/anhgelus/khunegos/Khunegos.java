package world.anhgelus.khunegos;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Khunegos implements ModInitializer {
    public static final String MOD_ID = "khunegos";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final float KHUNEGOS_DURATION = 1f; // in day(s)
    public static final float KHUNEGOS_BASE_DELAY = 1f; // in day(s)
    public static final int KHUNEGOS_MAX_RELATIVE_HEALTH = 5; // in heart(s)
    public static final int KHUNEGOS_MIN_RELATIVE_HEALTH = -5; // in heart(s)

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Khunegos");
    }
}
