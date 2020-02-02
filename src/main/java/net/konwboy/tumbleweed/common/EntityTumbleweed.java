package net.konwboy.tumbleweed.common;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IntHashMap;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector4f;

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
	public int fadeAge;
	public boolean persistent;
	private double windMod;
	private int lifetime;
	private float angularX, angularZ;
	public float stretch = 1f, prevStretch = 1f;

	@SideOnly(Side.CLIENT)
	public float rot1, rot2, rot3;
	@SideOnly(Side.CLIENT)
	public Quaternion quat;
	@SideOnly(Side.CLIENT)
	public Quaternion prevQuat;

	public EntityTumbleweed(World world) {
		super(world);

		this.entityCollisionReduction = 0.95f;
		this.preventEntitySpawning = true;

		setEntityId(getEntityId());

		if (this.world.isRemote) {
			this.rot1 = 360f * world.rand.nextFloat();
			this.rot2 = 360f * world.rand.nextFloat();
			this.rot3 = 360f * world.rand.nextFloat();

			this.quat = new Quaternion();
			this.prevQuat = new Quaternion();
		}
	}

	@Override
	protected void entityInit() {
		this.dataManager.register(SIZE, 2);
		this.dataManager.register(CUSTOM_WIND_ENABLED, false);
		this.dataManager.register(CUSTOM_WIND_X, 0f);
		this.dataManager.register(CUSTOM_WIND_Z, 0f);
		this.dataManager.register(FADING, false);

		updateSize();
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbt) {
		nbt.setInteger("Size", getSize());
		nbt.setBoolean("CustomWindEnabled", getCustomWindEnabled());
		nbt.setDouble("CustomWindX", getCustomWindX());
		nbt.setDouble("CustomWindZ", getCustomWindZ());
		nbt.setBoolean("Persistent", persistent);

		AxisAlignedBB bb = this.getEntityBoundingBox();
		nbt.setTag("AABB", this.newDoubleNBTList(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ));
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbt) {
		if (nbt.hasKey("Size"))
			this.dataManager.set(SIZE, nbt.getInteger("Size"));

		this.dataManager.set(CUSTOM_WIND_ENABLED, nbt.getBoolean("CustomWindEnabled"));
		this.dataManager.set(CUSTOM_WIND_X, nbt.getFloat("CustomWindX"));
		this.dataManager.set(CUSTOM_WIND_Z, nbt.getFloat("CustomWindZ"));

		persistent = nbt.getBoolean("Persistent");

		// Fixes server-side collision glitches
		if (nbt.hasKey("AABB")) {
			NBTTagList aabb = nbt.getTagList("AABB", 6);
			setEntityBoundingBox(new AxisAlignedBB(aabb.getDoubleAt(0), aabb.getDoubleAt(1), aabb.getDoubleAt(2), aabb.getDoubleAt(3), aabb.getDoubleAt(4), aabb.getDoubleAt(5)));
		}
	}

	@Override
	public void notifyDataManagerChange(DataParameter<?> key) {
		super.notifyDataManagerChange(key);

		if (key == SIZE)
			updateSize();
	}

	private void updateSize() {
		float mcSize = BASE_SIZE + this.getSize() * (1 / 8f);

		// Fixes client-side collision glitches
		if (world.isRemote)
			mcSize -= 1/2048f;

		this.setSize(mcSize, mcSize);
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
	public void onUpdate() {
		super.onUpdate();

		// Fixes some cases of rubber banding
		if (!world.isRemote && ticksExisted == 1)
			trackerHack();

		if (this.world.isRemote) {
			prevStretch = stretch;
			stretch *= 1.2f;
			if (stretch > 1f) stretch = 1f;

			this.prevQuat = new Quaternion(this.quat);
		}

		if (this.getRidingEntity() != null) {
			this.motionX = 0;
			this.motionY = 0;
			this.motionZ = 0;
			return;
		}

		if (!this.isInWater())
			this.motionY -= 0.012;

		double prevMotionX = this.motionX;
		double prevMotionY = this.motionY;
		double prevMotionZ = this.motionZ;
		boolean prevOnGround = onGround;

		this.move(MoverType.SELF, motionX, motionY, motionZ);

		double windX = getCustomWindEnabled() ? getCustomWindX() : WIND_X * windMod;
		double windZ = getCustomWindEnabled() ? getCustomWindZ() : WIND_Z * windMod;

		if (this.isInWater()) {
			this.motionY += 0.02;
			this.motionX *= 0.95;
			this.motionZ *= 0.95;

			windX = windZ = 0;
		} else if (windX != 0 || windZ != 0) {
			this.motionX = windX;
			this.motionZ = windZ;
		}

		// Rotate
		if (this.world.isRemote) {
			if (prevOnGround != onGround)
				stretch *= 0.75f;

			float motionAngleX = (float)-prevMotionX / (width * 0.5f);
			float motionAngleZ = (float)prevMotionZ / (width * 0.5f);

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

			Quaternion temp = new Quaternion();
			temp.setFromAxisAngle(new Vector4f(1, 0, 0, angularZ));
			Quaternion.mul(temp, quat, quat);
			temp.setFromAxisAngle(new Vector4f(0, 0, 1, angularX));
			Quaternion.mul(temp, quat, quat);
		}

		// Bounce on ground
		if (this.onGround) {
			if (windX * windX + windZ * windZ >= 0.05 * 0.05) {
				this.motionY = Math.max(-prevMotionY * 0.7, 0.24 - getSize() * 0.02);
			} else {
				this.motionY = -prevMotionY * 0.7;
			}
		}

		this.motionX *= 0.98;
		this.motionY *= 0.98;
		this.motionZ *= 0.98;

		collideWithNearbyEntities();

		if (!this.world.isRemote) {
			this.age += (collidedHorizontally || isInWater()) ? 8 : 1;
			tryDespawn();
		}

		if (isFading()) {
			this.fadeAge++;

			if (this.fadeAge > FADE_TIME)
				setDead();
		}
	}

	private void trackerHack() {
		EntityTrackerEntry entry = getTrackerEntry();
		if (entry != null && entry.updateCounter == 0)
			entry.updateCounter += 30;
	}

	private void tryDespawn() {
		if (persistent) {
			this.age = 0;
			return;
		}

		EntityPlayer player = this.world.getClosestPlayerToEntity(this, -1);
		if (player != null && player.getDistanceSq(this) > DESPAWN_RANGE * DESPAWN_RANGE)
			this.setDead();

		if (this.age > this.lifetime && fadeAge == 0)
			this.dataManager.set(FADING, true);
	}

	@Override
	public boolean isInRangeToRenderDist(double distance) {
		return distance < 128 * 128;
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		if (this.isEntityInvulnerable(source)) {
			return false;
		}

		if (!this.isDead && !this.world.isRemote) {
			this.setDead();

			SoundType sound = SoundType.PLANT;
			this.playSound(sound.getBreakSound(), (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);

			if (TumbleweedConfig.enableDrops)
				dropItem();
		}

		return true;
	}

	private void dropItem() {
		ItemStack item = TumbleweedConfig.Logic.getRandomItem();
		if (item != null) {
			EntityItem entityitem = new EntityItem(this.world, this.posX, this.posY, this.posZ, item);
			entityitem.motionX = 0;
			entityitem.motionY = 0.2D;
			entityitem.motionZ = 0;
			entityitem.setDefaultPickupDelay();
			this.world.spawnEntity(entityitem);
		}
	}

	@Override
	public boolean hitByEntity(Entity entityIn) {
		return entityIn instanceof EntityPlayer && this.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer) entityIn), 0.0F);
	}

	@Override
	protected void playStepSound(BlockPos pos, Block blockIn) {
		this.playSound(SoundEvents.BLOCK_GRASS_STEP, 0.15f,1.0f);
	}

	@Override
	public boolean canTrample(World world, Block block, BlockPos pos, float fallDistance) {
		return world.rand.nextFloat() < 0.7F && world.getGameRules().getBoolean("mobGriefing") && TumbleweedConfig.damageCrops;
	}

	@Override
	protected boolean shouldSetPosAfterLoading() {
		return false;
	}

	private void collideWithNearbyEntities() {
		List<Entity> list = this.world.getEntitiesInAABBexcluding(this, this.getEntityBoundingBox().expand(0.2D, 0.0D, 0.2D), Entity::canBePushed);

		for (Entity entity : list) {
			if (!this.world.isRemote && entity instanceof EntityMinecart && ((EntityMinecart) entity).getType() == EntityMinecart.Type.RIDEABLE && entity.motionX * entity.motionX + entity.motionZ * entity.motionZ > 0.01D && entity.getPassengers().isEmpty() && this.getRidingEntity() == null) {
				this.startRiding(entity);
				this.motionY += 0.25;
				this.velocityChanged = true;
			}

			entity.applyEntityCollision(this);
		}
	}

	public boolean isNotColliding() {
		return this.world.checkNoEntityCollision(this.getEntityBoundingBox(), this) && this.world.getCollisionBoxes(this, this.getEntityBoundingBox()).isEmpty() && !this.world.containsAnyLiquid(this.getEntityBoundingBox());
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

	private static Field trackedEntityHashTable;
	private static Field encodedPosX;
	private static Field encodedPosY;
	private static Field encodedPosZ;

	static {
		trackedEntityHashTable = fieldsOfType(EntityTracker.class, IntHashMap.class)[0];
		trackedEntityHashTable.setAccessible(true);
		encodedPosX = fieldsOfType(EntityTrackerEntry.class, long.class)[0];
		encodedPosX.setAccessible(true);
		encodedPosY = fieldsOfType(EntityTrackerEntry.class, long.class)[1];
		encodedPosY.setAccessible(true);
		encodedPosZ = fieldsOfType(EntityTrackerEntry.class, long.class)[2];
		encodedPosZ.setAccessible(true);
	}

	private static Field[] fieldsOfType(Class inClass, Class type){
		return Arrays.stream(inClass.getDeclaredFields()).filter(f -> f.getType() == type).toArray(Field[]::new);
	}

	public EntityTrackerEntry getTrackerEntry() {
		EntityTrackerEntry entry = null;
		try {
			entry = ((IntHashMap<EntityTrackerEntry>) trackedEntityHashTable.get(((WorldServer) world).getEntityTracker())).lookup(getEntityId());
		} catch(IllegalAccessException e){
		}
		return entry;
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		EntityTrackerEntry entry = getTrackerEntry();

		try {
			buffer.writeLong((long)encodedPosX.get(entry));
			buffer.writeLong((long)encodedPosY.get(entry));
			buffer.writeLong((long)encodedPosZ.get(entry));
		} catch(IllegalAccessException e){
		}
	}

	@Override
	public void readSpawnData(ByteBuf additionalData) {
		// Fixes some more cases of rubber banding
		this.serverPosX = additionalData.readLong();
		this.serverPosY = additionalData.readLong();
		this.serverPosZ = additionalData.readLong();
	}

}