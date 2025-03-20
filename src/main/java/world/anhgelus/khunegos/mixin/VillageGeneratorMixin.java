package world.anhgelus.khunegos.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.structure.StructureLiquidSettings;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.structure.pool.SinglePoolElement;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SinglePoolElement.class)
public abstract class VillageGeneratorMixin {
    @Shadow
    @Final
    protected Either<Identifier, StructureTemplate> location;

    @Unique
    private boolean spawned = false;

    @Inject(method = "generate", at = @At("RETURN"))
    public void gen(
            StructureTemplateManager structureTemplateManager,
            StructureWorldAccess world,
            StructureAccessor structureAccessor,
            ChunkGenerator chunkGenerator,
            BlockPos pos,
            BlockPos pivot,
            BlockRotation rotation,
            BlockBox box,
            Random random,
            StructureLiquidSettings liquidSettings,
            boolean keepJigsaws,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValue()) return;
        if (spawned) return;
        final var possibleLoc = this.location.left();
        if (possibleLoc.isEmpty()) return;
        final var identifier = possibleLoc.get();
        if (!identifier.getPath().contains("town_centers/plains_meeting_point")) return;
        // it is the center of the village
        final var armorStand = new ArmorStandEntity(world.toServerWorld(), pos.getX(), pos.getY(), pos.getZ());
        spawned = true;
        // invulnerable, on ground, cannot move, without gravity, no base plate
        armorStand.setInvulnerable(true);
        armorStand.setHideBasePlate(true);
        armorStand.setShowArms(true);
        armorStand.setOnGround(true);
        armorStand.setNoDrag(true);
        armorStand.setNoGravity(true);
        // give simple stuff
        armorStand.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        armorStand.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
        //TODO: define specific khunegos armor stand
    }
}
