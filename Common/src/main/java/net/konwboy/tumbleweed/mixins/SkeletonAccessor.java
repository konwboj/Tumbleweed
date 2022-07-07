package net.konwboy.tumbleweed.mixins;

import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractSkeleton.class)
public interface SkeletonAccessor {

	@Invoker
	AbstractArrow invokeGetArrow(ItemStack stack, float power);
}
