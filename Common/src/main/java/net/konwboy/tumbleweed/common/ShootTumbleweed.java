package net.konwboy.tumbleweed.common;

import net.konwboy.tumbleweed.mixins.SkeletonAccessor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumSet;
import java.util.List;

public class ShootTumbleweed extends Goal {

	private final AbstractSkeleton mob;
	private Entity target;
	private int lookTime;
	private static final float MAX_DISTANCE = 18;
	private static final float MIN_DISTANCE = 9;

	public ShootTumbleweed(AbstractSkeleton mob) {
		this.mob = mob;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (mob.getRandom().nextFloat() > 0.003F) {
			return false;
		} else {
            Entity tumbleweed = findTarget(mob.level().getEntitiesOfClass(EntityTumbleweed.class, mob.getBoundingBox().inflate(MAX_DISTANCE, 3.0D, MAX_DISTANCE), e -> true), mob, mob.getX(), mob.getEyeY(), mob.getZ());

			if (tumbleweed == null) {
				return false;
			} else {
				target = tumbleweed;
				return true;
			}
		}
	}

	private static Entity findTarget(List<? extends Entity> list, LivingEntity from, double x, double y, double z) {
		double min = -1.0D;
		Entity ent = null;

		for (Entity test : list) {
			double dist = test.distanceToSqr(x, y, z);
			if (dist > MIN_DISTANCE * MIN_DISTANCE && dist < MAX_DISTANCE * MAX_DISTANCE && from.hasLineOfSight(test) && (min == -1.0D || dist < min)) {
				ent = test;
				min = dist;
			}
		}

		return ent;
	}

	@Override
	public boolean canContinueToUse() {
		return lookTime > 0;
	}

	@Override
	public void start() {
		lookTime = 70;
	}

	@Override
	public void stop() {
		target = null;
		mob.setAggressive(false);
		mob.stopUsingItem();
		lookTime = 0;
	}

	// Copied from AbstractSkeleton, made the target parameter an Entity
	private void performRangedAttack(Entity target) {
		ItemStack arrowItem = mob.getProjectile(mob.getItemInHand(ProjectileUtil.getWeaponHoldingHand(mob, Items.BOW)));
		AbstractArrow arrow = ((SkeletonAccessor) mob).invokeGetArrow(arrowItem, 0.1f);
		double dX = target.getX() - mob.getX();
		double dY = target.getY(0.3D) - arrow.getY();
		double dZ = target.getZ() - mob.getZ();
		double dist = Math.sqrt(dX * dX + dZ * dZ);
		arrow.shoot(dX, dY + dist * 0.2F, dZ, 1.6F, 1);
		mob.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (mob.getRandom().nextFloat() * 0.4F + 0.8F));
        mob.level().addFreshEntity(arrow);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		lookTime--;

		if (lookTime == 60)
			mob.setAggressive(true);

		if (target.isAlive()) {
			if (lookTime == 50)
				mob.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.mob, Items.BOW));

			if (lookTime == 40) {
				performRangedAttack(target);
				mob.stopUsingItem();
			}

			this.mob.getLookControl().setLookAt(target.getX(), mob.getEyeY(), target.getZ());
		}

		if (lookTime == 30) {
			mob.setAggressive(false);
		}
	}
}
