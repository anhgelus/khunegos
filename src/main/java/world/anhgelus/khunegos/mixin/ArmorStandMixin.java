package world.anhgelus.khunegos.mixin;

import net.minecraft.entity.decoration.ArmorStandEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import world.anhgelus.khunegos.player.DeposeHeart;

@Mixin(ArmorStandEntity.class)
public class ArmorStandMixin implements DeposeHeart {
    @Unique
    private boolean deposeHeart = false;

    @Override
    public void khunegos_makeDeposeHeart() {
        this.deposeHeart = true;
    }

    @Override
    public boolean khunegos_isDeposeHeart() {
        return this.deposeHeart;
    }
}
