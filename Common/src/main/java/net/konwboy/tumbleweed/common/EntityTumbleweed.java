package net.konwboy.tumbleweed.common;

import com.mojang.math.Quaternion;
import net.konwboy.tumbleweed.services.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

public class EntityTumbleweed extends Entity {

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
	private boolean prevVerticalCol;
	private Vec3 prevMotion = Vec3.ZERO;
	private int despawnCounter;

	public float rot1, rot2, rot3;
	public Quaternion quat;
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

		return EntityDimensions.scalable(
			level.isClientSide ? mcSize - 1/1024f : mcSize, // Fixes client-side collision glitches
			mcSize
		);
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public boolean isPushable() {
		return true;
	}

	// Called in the constructor and from recreateFromPacket
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
				this.setDeltaMovement(getDeltaMovement().x, Math.max(-prevMotion.y * 0.7, 0.24 - Math.abs(getSize()) * 0.02), getDeltaMovement().z);
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

			if (this.age > this.lifetime && fadeProgress == 0)
				this.entityData.set(FADING, true);

			tryDespawn();
		}

		if (isFading()) {
			this.fadeProgress++;

			if (this.fadeProgress > FADE_TIME) {
				remove(RemovalReason.DISCARDED);
			}
		}
	}

	private void preTickClient() {
		prevStretch = stretch;
		stretch *= 1.2f;
		if (stretch > 1f) stretch = 1f;

		this.prevQuat = new Quaternion(this.quat);
	}

	private void tickClient() {
		if (!prevVerticalCol && verticalCollision) {
			stretch *= 0.70f;
		}

		prevVerticalCol = verticalCollision;

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

	private void tryDespawn() {
		if (shouldPersist()) {
			this.age = 0;
			return;
		}

		// Despawn if no player nearby
		Player player = this.level.getNearestPlayer(this, -1);
		if (player != null && player.distanceToSqr(this) > DESPAWN_RANGE * DESPAWN_RANGE)
			this.remove(RemovalReason.UNLOADED_WITH_PLAYER);
	}

	public boolean shouldPersist() {
		// Don't despawn if explicitly persistent or in a vehicle (minecart)
		return persistent || getVehicle() != null;
	}

	public void tickDespawn() {
		// De-spawn to prevent piling up in non entity-processing chunks
		if (!shouldPersist() && tickCount > 0 && Spawner.isNonEntityProcessing((ServerLevel) level, blockPosition())) {
			despawnCounter++;
		} else {
			despawnCounter = 0;
		}

		if (despawnCounter > 20) {
			discard();
		}
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 128 * 128;
	}

	// Enable naming with name tags with setting persistence
	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		var stack = player.getItemInHand(hand);
		if (stack.getItem() == Items.NAME_TAG && stack.hasCustomHoverName())
		{
			if (!level.isClientSide)
			{
				setCustomName(stack.getHoverName());
				persistent = true;
				stack.shrink(1);
			}

			return InteractionResult.sidedSuccess(level.isClientSide);
		} else
		{
			return InteractionResult.PASS;
		}
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

			Entity killer = source.getEntity();

			if (killer instanceof Player)
				((Player)killer).awardStat(Stats.ENTITY_KILLED.get(getType()));

			if (Services.CONFIG.enableDrops() && (killer instanceof Player || !Services.CONFIG.dropOnlyByPlayer()))
				dropFromLootTable(source);
		}

		return true;
	}

	@Nullable
	@Override
	public ItemEntity spawnAtLocation(ItemStack itemStack, float yOffset) {
		ItemEntity item = super.spawnAtLocation(itemStack, yOffset);
		if (item == null) return null;
		item.setDeltaMovement(0, 0.2, 0);
		return item;
	}

	// Copied from LivingEntity
	protected void dropFromLootTable(DamageSource damageSource) {
		LootTable loottable = this.level.getServer().getLootTables().get(getType().getDefaultLootTable());
		LootContext.Builder lootcontext$builder = this.createLootContext(damageSource);
		LootContext ctx = lootcontext$builder.create(LootContextParamSets.ENTITY);
		loottable.getRandomItems(ctx).forEach(this::spawnAtLocation);
	}

	protected LootContext.Builder createLootContext(DamageSource damageSource) {
		return new LootContext.Builder((ServerLevel) this.level).
			withRandom(this.random).
			withParameter(LootContextParams.THIS_ENTITY, this).
			withParameter(LootContextParams.ORIGIN, this.position()).
			withParameter(LootContextParams.DAMAGE_SOURCE, damageSource).
			withOptionalParameter(LootContextParams.KILLER_ENTITY, damageSource.getEntity()).
			withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, damageSource.getDirectEntity());
	}

	@Override
	public boolean skipAttackInteraction(Entity entityIn) {
		return entityIn instanceof Player && this.hurt(DamageSource.playerAttack((Player) entityIn), 0.0F);
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState blockIn) {
		this.playSound(SoundEvents.GRASS_STEP, 0.15f,1.0f);
	}

	// This is handled by Forge but not Fabric so a mixin is used (see FarmlandMixin)
	public boolean canTumbleweedTrample(BlockState state, BlockPos pos, float fallDistance) {
		return level.random.nextFloat() < 0.7F &&
				level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) &&
				Services.CONFIG.damageCrops();
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
		for (VoxelShape voxelshape : this.level.getBlockCollisions(this, this.getBoundingBox())) {
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

	@Override
	public Packet<?> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}
}