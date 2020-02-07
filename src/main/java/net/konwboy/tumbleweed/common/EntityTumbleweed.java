package net.konwboy.tumbleweed.common;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.client.renderer.Quaternion;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.Pose;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.item.minecart.AbstractMinecartEntity;
import net.minecraft.entity.item.minecart.MinecartEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.TrackedEntity;
import net.minecraft.world.World;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class EntityTumbleweed extends Entity implements IEntityAdditionalSpawnData {

	public static final int FADE_TIME = 4 * 20;
	private static final int DESPAWN_RANGE = 110;
	private static final float BASE_SIZE = 3/4f;
	private static final double WIND_X = -1/16f;
	private static final double WIND_Z = -1/16f;

	private static final DataParameter<Integer> SIZE = EntityDataManager.createKey(EntityTumbleweed.class, DataSerializers.VARINT);
	private static final DataParameter<Boolean> CUSTOM_WIND_ENABLED = EntityDataManager.createKey(EntityTumbleweed.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Float> CUSTOM_WIND_X = EntityDataManager.createKey(EntityTumbleweed.class, DataSerializers.FLOAT);
	private static final DataParameter<Float> CUSTOM_WIND_Z = EntityDataManager.createKey(EntityTumbleweed.class, DataSerializers.FLOAT);
	private static final DataParameter<Boolean> FADING = EntityDataManager.createKey(EntityTumbleweed.class, DataSerializers.BOOLEAN);

	private int age;
	public int fadeProgress;
	public boolean persistent;
	private double windMod;
	private int lifetime;
	private float angularX, angularZ;
	public float stretch = 1f, prevStretch = 1f;
	private boolean prevOnGround;
	private Vec3d prevMotion = Vec3d.ZERO;

	@OnlyIn(Dist.CLIENT)
	public float rot1, rot2, rot3;
	@OnlyIn(Dist.CLIENT)
	public Quaternion quat;
	@OnlyIn(Dist.CLIENT)
	public Quaternion prevQuat;

	public EntityTumbleweed(EntityType<?> type, World world) {
		super(type, world);

		this.entityCollisionReduction = 0.95f;
		this.preventEntitySpawning = true;

		setEntityId(getEntityId());

		if (this.world.isRemote) {
			initClient();
		}
	}

	@OnlyIn(Dist.CLIENT)
	private void initClient() {
		this.rot1 = 360f * world.rand.nextFloat();
		this.rot2 = 360f * world.rand.nextFloat();
		this.rot3 = 360f * world.rand.nextFloat();

		this.quat = new Quaternion(0, 0, 0, 1);
		this.prevQuat = new Quaternion(0, 0, 0, 1);
	}

	@Override
	protected void registerData() {
		this.dataManager.register(SIZE, 2);
		this.dataManager.register(CUSTOM_WIND_ENABLED, false);
		this.dataManager.register(CUSTOM_WIND_X, 0f);
		this.dataManager.register(CUSTOM_WIND_Z, 0f);
		this.dataManager.register(FADING, false);

		recalculateSize();
	}

	@Override
	protected void writeAdditional(CompoundNBT nbt) {
		nbt.putInt("Size", getSize());
		nbt.putBoolean("CustomWindEnabled", getCustomWindEnabled());
		nbt.putDouble("CustomWindX", getCustomWindX());
		nbt.putDouble("CustomWindZ", getCustomWindZ());
		nbt.putBoolean("Persistent", persistent);

		AxisAlignedBB bb = this.getBoundingBox();
		nbt.put("AABB", this.newDoubleNBTList(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ));
	}

	@Override
	protected void readAdditional(CompoundNBT nbt) {
		if (nbt.contains("Size"))
			this.dataManager.set(SIZE, nbt.getInt("Size"));

		this.dataManager.set(CUSTOM_WIND_ENABLED, nbt.getBoolean("CustomWindEnabled"));
		this.dataManager.set(CUSTOM_WIND_X, nbt.getFloat("CustomWindX"));
		this.dataManager.set(CUSTOM_WIND_Z, nbt.getFloat("CustomWindZ"));

		persistent = nbt.getBoolean("Persistent");

		// Fixes server-side collision glitches
		if (nbt.contains("AABB")) {
			ListNBT aabb = nbt.getList("AABB", 6);
			setBoundingBox(new AxisAlignedBB(aabb.getDouble(0), aabb.getDouble(1), aabb.getDouble(2), aabb.getDouble(3), aabb.getDouble(4), aabb.getDouble(5)));
		}
	}

	@Override
	public void notifyDataManagerChange(DataParameter<?> key) {
		super.notifyDataManagerChange(key);

		if (key.equals(SIZE))
			recalculateSize();
	}

	@Override
	public IPacket<?> createSpawnPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	public EntitySize getSize(Pose pose) {
		float mcSize = BASE_SIZE + this.getSize() * (1 / 8f);

		// Fixes client-side collision glitches
		if (world.isRemote)
			mcSize -= 1/2048f;

		return EntitySize.flexible(mcSize, mcSize);
	}

	@Override
	public AxisAlignedBB getCollisionBox(Entity entityIn) {
		return null;
	}

	@Override
	public boolean canBeCollidedWith() {
		return true;
	}

	@Override
	public boolean canBePushed() {
		return true;
	}

	@Override
	public void setEntityId(int id) {
		super.setEntityId(id);

		Random rand = new Random(id);

		this.windMod = 1.05 - 0.1 * rand.nextDouble();
		this.lifetime = 2 * 60 * 20 + rand.nextInt(200);
	}

	@Override
	public void tick() {
		super.tick();

		// Fixes some cases of rubber banding
		if (!world.isRemote && ticksExisted == 1)
		 	trackerHack();

		if (this.world.isRemote) {
			preTickClient();
		}

		if (this.getRidingEntity() != null) {
			this.setMotion(Vec3d.ZERO);
			return;
		}

		if (!this.isInWater())
			this.setMotion(getMotion().subtract(0, 0.012, 0));

		prevMotion = this.getMotion();
		prevOnGround = onGround;

		this.move(MoverType.SELF, getMotion());

		double windX = getCustomWindEnabled() ? getCustomWindX() : WIND_X * windMod;
		double windZ = getCustomWindEnabled() ? getCustomWindZ() : WIND_Z * windMod;

		if (this.isInWater()) {
			this.setMotion(getMotion().mul(0.95, 1, 0.95));
			this.setMotion(getMotion().add(0, 0.02, 0));
			windX = windZ = 0;
		} else if (windX != 0 || windZ != 0) {
			this.setMotion(windX, getMotion().y, windZ);
		}

		// Rotate
		if (this.world.isRemote) {
			tickClient();
		}

		// Bounce on ground
		if (this.onGround) {
			if (windX * windX + windZ * windZ >= 0.05 * 0.05) {
				this.setMotion(getMotion().x, Math.max(-prevMotion.y * 0.7, 0.24 - getSize() * 0.02), getMotion().z);
			} else {
				this.setMotion(getMotion().x, -prevMotion.y * 0.7, getMotion().z);
			}
		}

		// Friction
		this.setMotion(getMotion().mul(0.98, 0.98, 0.98));

		collideWithNearbyEntities();

		if (!this.world.isRemote) {
			// Age faster when stuck on a wall or in water
			this.age += (collidedHorizontally || isInWater()) ? 8 : 1;
			tryDespawn();
		}

		if (isFading()) {
			this.fadeProgress++;

			if (this.fadeProgress > FADE_TIME)
				remove();
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

		float motionAngleX = (float)-prevMotion.x / (getWidth() * 0.5f);
		float motionAngleZ = (float)prevMotion.z / (getWidth() * 0.5f);

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
		temp.multiply(quat);
		quat = temp;
	}

	private void trackerHack() {
		TrackedEntity entry = getTrackerEntry();
		try {
			int counter = (int)updateCounter.get(entry);
			if (entry != null && counter == 0)
				updateCounter.set(entry, counter + 30);
		} catch (IllegalAccessException e) {
		}
	}

	private void tryDespawn() {
		if (shouldPersist()) {
			this.age = 0;
			return;
		}

		PlayerEntity player = this.world.getClosestPlayer(this, -1);
		if (player != null && player.getDistanceSq(this) > DESPAWN_RANGE * DESPAWN_RANGE)
			this.remove();

		if (this.age > this.lifetime && fadeProgress == 0)
			this.dataManager.set(FADING, true);
	}

	@Override
	public boolean isInRangeToRenderDist(double distance) {
		return distance < 128 * 128;
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		if (this.isInvulnerableTo(source)) {
			return false;
		}

		if (this.isAlive() && !this.world.isRemote) {
			this.remove();

			SoundType sound = SoundType.PLANT;
			this.playSound(sound.getBreakSound(), (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);

			if (TumbleweedConfig.enableDrops)
				dropItem();
		}

		return true;
	}

	private void dropItem() {
		ItemStack item = TumbleweedConfig.getRandomItem();
		if (item != null) {
			ItemEntity entityitem = new ItemEntity(this.world, this.posX, this.posY, this.posZ, item);
			entityitem.setMotion(new Vec3d(0, 0.2, 0));
			entityitem.setDefaultPickupDelay();
			this.world.addEntity(entityitem);
		}
	}

	@Override
	public boolean hitByEntity(Entity entityIn) {
		return entityIn instanceof PlayerEntity && this.attackEntityFrom(DamageSource.causePlayerDamage((PlayerEntity) entityIn), 0.0F);
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState blockIn) {
		this.playSound(SoundEvents.BLOCK_GRASS_STEP, 0.15f,1.0f);
	}

	@Override
	public boolean canTrample(BlockState state, BlockPos pos, float fallDistance) {
		return world.rand.nextFloat() < 0.7F && world.getGameRules().getBoolean(GameRules.MOB_GRIEFING) && TumbleweedConfig.damageCrops;
	}

	@Override
	protected boolean shouldSetPosAfterLoading() {
		return false;
	}

	private void collideWithNearbyEntities() {
		List<Entity> list = this.world.getEntitiesInAABBexcluding(this, this.getBoundingBox().expand(0.2D, 0.0D, 0.2D), Entity::canBePushed);

		for (Entity entity : list) {
			if (!this.world.isRemote && entity instanceof AbstractMinecartEntity && ((AbstractMinecartEntity) entity).getMinecartType() == MinecartEntity.Type.RIDEABLE && entity.getMotion().x * entity.getMotion().x + entity.getMotion().z * entity.getMotion().z > 0.01D && entity.getPassengers().isEmpty() && this.getRidingEntity() == null) {
				this.startRiding(entity);
				this.setMotion(getMotion().add(0, 0.25, 0));
				this.velocityChanged = true;
			}

			entity.applyEntityCollision(this);
		}
	}

	public boolean isNotColliding() {
		return this.world.checkNoEntityCollision(this) && !this.world.getCollisionShapes(this, this.getBoundingBox()).findAny().isPresent() && !this.world.containsAnyLiquid(this.getBoundingBox());
	}

	public void setSize(int size) {
		this.dataManager.set(SIZE, size);
	}

	public int getSize() {
		return this.dataManager.get(SIZE);
	}

	public double getCustomWindX() {
		return this.dataManager.get(CUSTOM_WIND_X);
	}

	public double getCustomWindZ() {
		return this.dataManager.get(CUSTOM_WIND_Z);
	}

	public boolean getCustomWindEnabled() {
		return this.dataManager.get(CUSTOM_WIND_ENABLED);
	}

	public boolean isFading() {
		return this.dataManager.get(FADING);
	}

	public boolean shouldPersist() {
		return persistent || getRidingEntity() != null;
	}

	private static Field trackedEntityHashTable;
	private static Field entryFieldLazy;
	private static Field encodedPosX;
	private static Field encodedPosY;
	private static Field encodedPosZ;
	private static Field updateCounter;

	static {
		trackedEntityHashTable = fieldsOfType(ChunkManager.class, Int2ObjectMap.class)[0];
		trackedEntityHashTable.setAccessible(true);
		encodedPosX = fieldsOfType(TrackedEntity.class, long.class)[0];
		encodedPosX.setAccessible(true);
		encodedPosY = fieldsOfType(TrackedEntity.class, long.class)[1];
		encodedPosY.setAccessible(true);
		encodedPosZ = fieldsOfType(TrackedEntity.class, long.class)[2];
		encodedPosZ.setAccessible(true);

		// Field found by index, recheck on game updates
		updateCounter = fieldsOfType(TrackedEntity.class, int.class)[4];
		updateCounter.setAccessible(true);
	}

	private static Field[] fieldsOfType(Class inClass, Class type){
		return Arrays.stream(inClass.getDeclaredFields()).filter(f -> f.getType() == type).toArray(Field[]::new);
	}

	public TrackedEntity getTrackerEntry() {
		TrackedEntity entry = null;
		try {
			Object e = ((Int2ObjectMap<?>) trackedEntityHashTable.get(((ServerWorld) world).getChunkProvider().chunkManager)).get(getEntityId());
			if (entryFieldLazy == null) {
				entryFieldLazy = fieldsOfType(e.getClass(), TrackedEntity.class)[0];
				entryFieldLazy.setAccessible(true);
			}
			entry = (TrackedEntity)entryFieldLazy.get(e);
		} catch(IllegalAccessException e){
		}
		return entry;
	}

	@Override
	public void writeSpawnData(PacketBuffer buffer) {
		TrackedEntity entry = getTrackerEntry();

		try {
			buffer.writeLong((long)encodedPosX.get(entry));
			buffer.writeLong((long)encodedPosY.get(entry));
			buffer.writeLong((long)encodedPosZ.get(entry));
		} catch(IllegalAccessException e){
		}
	}

	@Override
	public void readSpawnData(PacketBuffer additionalData) {
		// Fixes some more cases of rubber banding
		this.serverPosX = additionalData.readLong();
		this.serverPosY = additionalData.readLong();
		this.serverPosZ = additionalData.readLong();
	}

}