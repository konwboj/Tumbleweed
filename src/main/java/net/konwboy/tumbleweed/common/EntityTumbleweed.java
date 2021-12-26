package net.konwboy.tumbleweed.common;

import com.mojang.math.Quaternion;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.Random;

public class EntityTumbleweed extends Entity implements IEntityAdditionalSpawnData {

	public static final int FADE_TIME = 4 * 20;
	private static final int DESPAWN_RANGE = 110;
	private static final float BASE_SIZE = 3/4f;
	private static final double WIND_X = -1/16f;
	private static final double WIND_Z = -1/16f;

	private static final EntityDataAccessor<Integer> SIZE = SynchedEntityData.defineId(EntityTumbleweed.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> CUSTOM_WIND_ENABLED = SynchedEntityData.defineId(EntityTumbleweed.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Float> CUSTOM_WIND_X = SynchedEntityData.defineId(EntityTumbleweed.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> CUSTOM_WIND_Z = SynchedEntityData.defineId(EntityTumbleweed.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> FADING = SynchedEntityData.defineId(EntityTumbleweed.class, EntityDataSerializers.BOOLEAN);

	private int age;
	public int fadeProgress;
	public boolean persistent;
	private double windMod;
	private int lifetime;
	private float angularX, angularZ;
	public float stretch = 1f, prevStretch = 1f;
	private boolean prevOnGround;
	private Vec3 prevMotion = Vec3.ZERO;

	@OnlyIn(Dist.CLIENT)
	public float rot1, rot2, rot3;
	@OnlyIn(Dist.CLIENT)
	public Quaternion quat;
	@OnlyIn(Dist.CLIENT)
	public Quaternion prevQuat;

	public EntityTumbleweed(EntityType<?> type, Level world) {
		super(type, world);

		//this.pushthrough = 0.95f;
		this.blocksBuilding = true;

		setId(getId());

		if (this.level.isClientSide) {
			initClient();
		}
	}

	@OnlyIn(Dist.CLIENT)
	private void initClient() {
		this.rot1 = 360f * level.random.nextFloat();
		this.rot2 = 360f * level.random.nextFloat();
		this.rot3 = 360f * level.random.nextFloat();

		this.quat = new Quaternion(0, 0, 0, 1);
		this.prevQuat = new Quaternion(0, 0, 0, 1);
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(SIZE, 2);
		this.entityData.define(CUSTOM_WIND_ENABLED, false);
		this.entityData.define(CUSTOM_WIND_X, 0f);
		this.entityData.define(CUSTOM_WIND_Z, 0f);
		this.entityData.define(FADING, false);

		refreshDimensions();
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag nbt) {
		nbt.putInt("Size", getSize());
		nbt.putBoolean("CustomWindEnabled", getCustomWindEnabled());
		nbt.putDouble("CustomWindX", getCustomWindX());
		nbt.putDouble("CustomWindZ", getCustomWindZ());
		nbt.putBoolean("Persistent", persistent);

		AABB bb = this.getBoundingBox();
		nbt.put("AABB", this.newDoubleList(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ));
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag nbt) {
		if (nbt.contains("Size"))
			this.entityData.set(SIZE, nbt.getInt("Size"));

		this.entityData.set(CUSTOM_WIND_ENABLED, nbt.getBoolean("CustomWindEnabled"));
		this.entityData.set(CUSTOM_WIND_X, nbt.getFloat("CustomWindX"));
		this.entityData.set(CUSTOM_WIND_Z, nbt.getFloat("CustomWindZ"));

		persistent = nbt.getBoolean("Persistent");

		// Fixes server-side collision glitches
		if (nbt.contains("AABB")) {
			ListTag aabb = nbt.getList("AABB", 6);
			setBoundingBox(new AABB(aabb.getDouble(0), aabb.getDouble(1), aabb.getDouble(2), aabb.getDouble(3), aabb.getDouble(4), aabb.getDouble(5)));
		}
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);

		if (key.equals(SIZE))
			refreshDimensions();
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		float mcSize = BASE_SIZE + this.getSize() * (1 / 8f);

		// Fixes client-side collision glitches
		if (level.isClientSide)
			mcSize -= 1/2048f;

		return EntityDimensions.scalable(mcSize, mcSize);
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public boolean isPushable() {
		return true;
	}

	@Override
	public void setId(int id) {
		super.setId(id);

		Random rand = new Random(id);

		this.windMod = 1.05 - 0.1 * rand.nextDouble();
		this.lifetime = 2 * 60 * 20 + rand.nextInt(200);
	}

	@Override
	public void tick() {
		super.tick();

		// Fixes some cases of rubber banding
		if (!level.isClientSide && tickCount == 1)
		 	trackerHack();

		if (this.level.isClientSide) {
			preTickClient();
		}

		if (this.getVehicle() != null) {
			this.setDeltaMovement(Vec3.ZERO);
			return;
		}

		if (!this.isInWater())
			this.setDeltaMovement(getDeltaMovement().subtract(0, 0.012, 0));

		prevMotion = this.getDeltaMovement();
		prevOnGround = onGround;

		this.move(MoverType.SELF, getDeltaMovement());

		double windX = getCustomWindEnabled() ? getCustomWindX() : WIND_X * windMod;
		double windZ = getCustomWindEnabled() ? getCustomWindZ() : WIND_Z * windMod;

		if (this.isInWater()) {
			this.setDeltaMovement(getDeltaMovement().multiply(0.95, 1, 0.95));
			this.setDeltaMovement(getDeltaMovement().add(0, 0.02, 0));
			windX = windZ = 0;
		} else if (windX != 0 || windZ != 0) {
			this.setDeltaMovement(windX, getDeltaMovement().y, windZ);
		}

		// Rotate
		if (this.level.isClientSide) {
			tickClient();
		}

		// Bounce on ground
		if (this.onGround) {
			if (windX * windX + windZ * windZ >= 0.05 * 0.05) {
				this.setDeltaMovement(getDeltaMovement().x, Math.max(-prevMotion.y * 0.7, 0.24 - getSize() * 0.02), getDeltaMovement().z);
			} else {
				this.setDeltaMovement(getDeltaMovement().x, -prevMotion.y * 0.7, getDeltaMovement().z);
			}
		}

		// Friction
		this.setDeltaMovement(getDeltaMovement().multiply(0.98, 0.98, 0.98));

		collideWithNearbyEntities();

		if (!this.level.isClientSide) {
			// Age faster when stuck on a wall or in water
			this.age += (horizontalCollision || isInWater()) ? 8 : 1;
			tryDespawn();
		}

		if (isFading()) {
			this.fadeProgress++;

			if (this.fadeProgress > FADE_TIME) {
				remove(RemovalReason.DISCARDED);
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	private void preTickClient() {
		prevStretch = stretch;
		stretch *= 1.2f;
		if (stretch > 1f) stretch = 1f;

		this.prevQuat = new Quaternion(this.quat);
	}

	@OnlyIn(Dist.CLIENT)
	private void tickClient() {
		if (prevOnGround != onGround)
			stretch *= 0.75f;

		float motionAngleX = (float)-prevMotion.x / (getBbWidth() * 0.5f);
		float motionAngleZ = (float)prevMotion.z / (getBbWidth() * 0.5f);

		if (onGround) {
			angularX = motionAngleX;
			angularZ = motionAngleZ;
		}

		if (isInWater()) {
			angularX += motionAngleX * 0.2f;
			angularZ += motionAngleZ * 0.2f;
		}

		float resistance = isInWater() ? 0.9f : 0.96f;
		angularX *= resistance;
		angularZ *= resistance;

		Quaternion temp = new Quaternion(angularZ, 0, angularX, false);
		temp.mul(quat);
		quat = temp;
	}

	private void trackerHack() {
		ServerEntity entry = getTrackerEntry();
		if (entry != null && entry.tickCount == 0)
			entry.tickCount += 30;
	}

	private void tryDespawn() {
		if (shouldPersist()) {
			this.age = 0;
			return;
		}

		Player player = this.level.getNearestPlayer(this, -1);
		if (player != null && player.distanceToSqr(this) > DESPAWN_RANGE * DESPAWN_RANGE)
			this.remove(RemovalReason.UNLOADED_WITH_PLAYER);

		if (this.age > this.lifetime && fadeProgress == 0)
			this.entityData.set(FADING, true);
	}

	public boolean shouldPersist() {
		return persistent || getVehicle() != null;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 128 * 128;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (this.isInvulnerableTo(source)) {
			return false;
		}

		if (this.isAlive() && !this.level.isClientSide) {
			this.remove(RemovalReason.KILLED);

			SoundType sound = SoundType.GRASS;
			this.playSound(sound.getBreakSound(), (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);

			if (TumbleweedConfig.enableDrops && (!TumbleweedConfig.dropOnlyByPlayer || source.getEntity() instanceof Player))
				dropItem();
		}

		return true;
	}

	private void dropItem() {
		ItemStack item = DropList.getRandomItem(getLevel());
		if (item != null) {
			ItemEntity itemEntity = new ItemEntity(this.level, this.getX(), this.getY(), this.getZ(), item);
			itemEntity.setDeltaMovement(new Vec3(0, 0.2, 0));
			itemEntity.setDefaultPickUpDelay();
			this.level.addFreshEntity(itemEntity);
		}
	}

	@Override
	public boolean skipAttackInteraction(Entity entityIn) {
		return entityIn instanceof Player && this.hurt(DamageSource.playerAttack((Player) entityIn), 0.0F);
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState blockIn) {
		this.playSound(SoundEvents.GRASS_STEP, 0.15f,1.0f);
	}

	@Override
	public boolean canTrample(BlockState state, BlockPos pos, float fallDistance) {
		return level.random.nextFloat() < 0.7F && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && TumbleweedConfig.damageCrops;
	}

	@Override
	protected boolean repositionEntityAfterLoad() {
		return false;
	}

	private void collideWithNearbyEntities() {
		List<Entity> list = this.level.getEntities(this, this.getBoundingBox().expandTowards(0.2D, 0.0D, 0.2D), Entity::isPushable);

		for (Entity entity : list) {
			if (!this.level.isClientSide && entity instanceof AbstractMinecart && ((AbstractMinecart) entity).getMinecartType() == Minecart.Type.RIDEABLE && entity.getDeltaMovement().x * entity.getDeltaMovement().x + entity.getDeltaMovement().z * entity.getDeltaMovement().z > 0.01D && entity.getPassengers().isEmpty() && this.getVehicle() == null) {
				this.startRiding(entity);
				this.setDeltaMovement(getDeltaMovement().add(0, 0.25, 0));
				this.hurtMarked = true;
			}

			entity.push(this);
		}
	}

	public boolean isNotColliding() {
		return this.level.isUnobstructed(this) && noBlockCollision() && !this.level.containsAnyLiquid(this.getBoundingBox());
	}

	private boolean noBlockCollision(){
		for(VoxelShape voxelshape : this.level.getBlockCollisions(this, this.getBoundingBox())) {
			if (!voxelshape.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	public void setSize(int size) {
		this.entityData.set(SIZE, size);
	}

	public int getSize() {
		return this.entityData.get(SIZE);
	}

	public double getCustomWindX() {
		return this.entityData.get(CUSTOM_WIND_X);
	}

	public double getCustomWindZ() {
		return this.entityData.get(CUSTOM_WIND_Z);
	}

	public boolean getCustomWindEnabled() {
		return this.entityData.get(CUSTOM_WIND_ENABLED);
	}

	public boolean isFading() {
		return this.entityData.get(FADING);
	}

	public ServerEntity getTrackerEntry() {
		return ((ServerLevel) level).getChunkSource().chunkMap.entityMap.get(getId()).serverEntity;
	}

	@Override
	public void writeSpawnData(FriendlyByteBuf buffer) {
		ServerEntity entry = getTrackerEntry();
		buffer.writeLong(entry.xp);
		buffer.writeLong(entry.yp);
		buffer.writeLong(entry.zp);
	}

	@Override
	public void readSpawnData(FriendlyByteBuf additionalData) {
		// Fixes some more cases of rubber banding
		this.setPacketCoordinates(
				additionalData.readLong() / 4096.0D,
				additionalData.readLong() / 4096.0D,
				additionalData.readLong() / 4096.0D
		);
	}

	@Override
	public Packet<?> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

}