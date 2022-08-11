package net.konwboy.tumbleweed.mixins;

import net.konwboy.tumbleweed.common.ShootTumbleweed;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSkeleton.class)
public class SkeletonMixin {

	@Inject(at = @At("TAIL"), method = "registerGoals")
	void registerGoals(CallbackInfo ci) {
		((MobAccessor)this).getGoalSelector().addGoal(5, new ShootTumbleweed((AbstractSkeleton)(Object)this));
	}

}
