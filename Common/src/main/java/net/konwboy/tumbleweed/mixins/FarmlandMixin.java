package net.konwboy.tumbleweed.mixins;

import net.konwboy.tumbleweed.common.EntityTumbleweed;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmBlock.class)
public class FarmlandMixin {

	@Inject(at = @At("HEAD"), method = "fallOn")
	private void fallOn(Level level, BlockState block, BlockPos pos, Entity entity, float height, CallbackInfo ci){
		if (!level.isClientSide && entity instanceof EntityTumbleweed && ((EntityTumbleweed) entity).canTumbleweedTrample(block, pos, height))
			FarmBlock.turnToDirt(entity, block, level, pos);
	}

}
