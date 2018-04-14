package net.konwboy.tumbleweed.common;

import com.google.common.base.Predicate;
import net.konwboy.tumbleweed.client.RenderTumbleweed;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockWall;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector4f;

import java.util.List;

public class EntityTumbleweed extends Entity {

	public static final int FADE_TIME = 4 * 20;
	private static final int DESPAWN_RANGE = 110;
	private static final float BASE_SIZE = 0.75f;
	private static final double WIND_X = -0.07;
	private static final double WIND_Z = -0.07;

	private static final DataParameter<Integer> SIZE = EntityDataManager.createKey(EntityTumbleweed.class, DataSerializers.VARINT);
	private static final DataParameter<Boolean> CUSTOM_WIND_ENABLED = EntityDataManager.createKey(EntityTumbleweed.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Float> CUSTOM_WIND_X = EntityDataManager.createKey(EntityTumbleweed.class, DataSerializers.FLOAT);
	private static final DataParameter<Float> CUSTOM_WIND_Z = EntityDataManager.createKey(EntityTumbleweed.class, DataSerializers.FLOAT);
	private static final DataParameter<Boolean> FADING = EntityDataManager.createKey(EntityTumbleweed.class, DataSerializers.BOOLEAN);

	private int age;
	public int fadeAge;
	private boolean canDespawn;
	private double windMod;
	private int lifetime;

	private float distanceWalkedModified;
	private float distanceWalkedOnStepModified;
	private int nextStepDistance;
	private int groundTicks;

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
		this.canDespawn = true;

		if (this.worldObj.isRemote) {
			this.rot1 = 360f * worldObj.rand.nextFloat();
			this.rot2 = 360f * worldObj.rand.nextFloat();
			this.rot3 = 360f * worldObj.rand.nextFloat();

			this.quat = new Quaternion();
			this.prevQuat = new Quaternion();
		}
	}

	@Override
	protected void entityInit() {
		rand.setSeed(getEntityId());

		this.dataManager.register(SIZE, rand.nextInt(5) - 2);
		this.dataManager.register(CUSTOM_WIND_ENABLED, false);
		this.dataManager.register(CUSTOM_WIND_X, 0f);
		this.dataManager.register(CUSTOM_WIND_Z, 0f);
		this.dataManager.register(FADING, false);

		this.windMod = 1.2 - 0.4 * rand.nextDouble();
		this.lifetime = 2 * 60 * 20 + rand.nextInt(200);

		updateSize();
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tagCompound) {
		tagCompound.setInteger("Size", getSize());
		tagCompound.setBoolean("CustomWindEnabled", getCustomWindEnabled());
		tagCompound.setDouble("CustomWindX", getCustomWindX());
		tagCompound.setDouble("CustomWindZ", getCustomWindZ());
		tagCompound.setBoolean("CanDespawn", canDespawn);
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tagCompound) {
		if (tagCompound.hasKey("Size"))
			this.dataManager.set(SIZE, tagCompound.getInteger("Size"));

		if (tagCompound.hasKey("CustomWindEnabled"))
			this.dataManager.set(CUSTOM_WIND_ENABLED, tagCompound.getBoolean("CustomWindEnabled"));

		if (tagCompound.hasKey("CustomWindX"))
			this.dataManager.set(CUSTOM_WIND_X, tagCompound.getFloat("CustomWindX"));

		if (tagCompound.hasKey("CustomWindZ"))
			this.dataManager.set(CUSTOM_WIND_Z, tagCompound.getFloat("CustomWindZ"));

		if (tagCompound.hasKey("CanDespawn"))
			canDespawn = tagCompound.getBoolean("CanDespawn");
	}

	@Override
	public void notifyDataManagerChange(DataParameter<?> key) {
		super.notifyDataManagerChange(key);

		if (key == SIZE)
			updateSize();
	}

	private void updateSize() {
		float mcSize = BASE_SIZE + this.getSize() * (1 / 8f);
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
	public void onUpdate() {
		super.onUpdate();

		if (this.getRidingEntity() != null) {
			this.motionX = 0;
			this.motionY = 0;
			this.motionZ = 0;
			return;
		}

		if (!this.isInWater())
			this.motionY -= 0.012;

		double x = this.motionX;
		double y = this.motionY;
		double z = this.motionZ;

		boolean ground = onGround;
		this.moveEntity(MoverType.SELF, this.motionX, this.motionY, this.motionZ);

		double windX = getCustomWindEnabled() ? getCustomWindX() : WIND_X * windMod;
		double windZ = getCustomWindEnabled() ? getCustomWindZ() : WIND_Z * windMod;

		if (this.isInWater()) {
			this.motionY += 0.02;
			this.motionX *= 0.95;
			this.motionZ *= 0.95;
		} else if (windX != 0 || windZ != 0) {
			this.motionX = windX;
			this.motionZ = windZ;
		}

		// Rotate
		if (this.worldObj.isRemote) {
			groundTicks--;

			if ((!ground && onGround) || isInWater())
				groundTicks = 10;
			else if (getCustomWindEnabled())
				groundTicks = 5;

			double div = 5d * width - groundTicks / 5d;
			double rotX = 2d * Math.PI * this.motionX / div;
			double rotZ = -2d * Math.PI * this.motionZ / div;

			this.prevQuat = this.quat;
			RenderTumbleweed.TEMP_QUAT.setFromAxisAngle((new Vector4f(1, 0, 0, (float) rotZ)));
			Quaternion.mul(this.quat, RenderTumbleweed.TEMP_QUAT, this.quat);
			RenderTumbleweed.TEMP_QUAT.setFromAxisAngle((new Vector4f(0, 0, 1, (float) rotX)));
			Quaternion.mul(this.quat, RenderTumbleweed.TEMP_QUAT, this.quat);
		}

		// Bounce on ground
		if (this.onGround) {
			if (windX * windX + windZ * windZ >= 0.05 * 0.05)
				this.motionY = Math.max(-y * 0.7, 0.24 - getSize() * 0.02);
			else
				this.motionY = -y * 0.7;
		}

		// Bounce on walls
		if (this.isCollidedHorizontally) {
			this.motionX = -x * 0.4;
			this.motionZ = -z * 0.4;
		}

		this.motionX *= 0.98;
		this.motionY *= 0.98;
		this.motionZ *= 0.98;

		if (Math.abs(this.motionX) < 0.005)
			this.motionX = 0.0;

		if (Math.abs(this.motionY) < 0.005)
			this.motionY = 0.0;

		if (Math.abs(this.motionZ) < 0.005)
			this.motionZ = 0.0;

		collideWithNearbyEntities();

		if (!this.worldObj.isRemote) {
			this.age++;
			tryDespawn();
		}

		if (isFading()) {
			this.fadeAge++;

			if (this.fadeAge > FADE_TIME)
				setDead();
		}
	}

	private void tryDespawn() {
		if (!canDespawn) {
			this.age = 0;
			return;
		}

		Entity entity = this.worldObj.getClosestPlayerToEntity(this, DESPAWN_RANGE);
		if (entity == null)
			this.setDead();

		if (this.age > this.lifetime && fadeAge == 0)
			this.dataManager.set(FADING, true);
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		if (this.isEntityInvulnerable(source)) {
			return false;
		}

		if (!this.isDead && !this.worldObj.isRemote) {
			this.setDead();
			this.setBeenAttacked();

			SoundType sound = SoundType.PLANT;
			this.playSound(sound.getBreakSound(), (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);

			ItemStack item = Config.getRandomItem();
			if (item != null) {
				EntityItem entityitem = new EntityItem(this.worldObj, this.posX, this.posY, this.posZ, item);
				entityitem.motionX = 0;
				entityitem.motionY = 0.2D;
				entityitem.motionZ = 0;
				entityitem.setDefaultPickupDelay();
				this.worldObj.spawnEntityInWorld(entityitem);
			}
		}

		return true;
	}

	@Override
	public boolean hitByEntity(Entity entityIn) {
		return entityIn instanceof EntityPlayer && this.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer) entityIn), 0.0F);
	}

	@Override
	public void moveEntity(MoverType mover, double velX, double velY, double velZ) {
		if (this.noClip) {
			this.setEntityBoundingBox(this.getEntityBoundingBox().offset(velX, velY, velZ));
			this.resetPositionToBB();
			return;
		}

		double startX = this.posX;
		double startY = this.posY;
		double startZ = this.posZ;

		if (this.isInWeb) {
			this.isInWeb = false;
			velX *= 0.25D;
			velY *= 0.05D;
			velZ *= 0.25D;
			this.motionX = 0.0D;
			this.motionY = 0.0D;
			this.motionZ = 0.0D;
		}

		double startVelX = velX;
		double startVelY = velY;
		double startVelZ = velZ;

		List<AxisAlignedBB> list1 = this.worldObj.getCollisionBoxes(this, this.getEntityBoundingBox().addCoord(velX, velY, velZ));

		for (AxisAlignedBB axisalignedbb1 : list1)
			velY = axisalignedbb1.calculateYOffset(this.getEntityBoundingBox(), velY);

		this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0D, velY, 0.0D));

		for (AxisAlignedBB axisalignedbb2 : list1)
			velX = axisalignedbb2.calculateXOffset(this.getEntityBoundingBox(), velX);

		this.setEntityBoundingBox(this.getEntityBoundingBox().offset(velX, 0.0D, 0.0D));

		for (AxisAlignedBB axisalignedbb13 : list1)
			velZ = axisalignedbb13.calculateZOffset(this.getEntityBoundingBox(), velZ);

		this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0D, 0.0D, velZ));

		this.resetPositionToBB();
		this.isCollidedHorizontally = startVelX != velX || startVelZ != velZ;
		this.isCollidedVertically = startVelY != velY;
		this.onGround = this.isCollidedVertically && startVelY < 0.0D;
		this.isCollided = this.isCollidedHorizontally || this.isCollidedVertically;
		int i = MathHelper.floor_double(this.posX);
		int j = MathHelper.floor_double(this.posY - 0.2D);
		int k = MathHelper.floor_double(this.posZ);
		BlockPos blockpos = new BlockPos(i, j, k);
		IBlockState state = this.worldObj.getBlockState(blockpos);
		Block block = state.getBlock();

		if (state.getBlock() == Blocks.AIR) {
			IBlockState state1 = this.worldObj.getBlockState(blockpos.down());
			Block block1 = state1.getBlock();

			if (block1 instanceof BlockFence || block1 instanceof BlockWall || block1 instanceof BlockFenceGate) {
				state = state1;
				blockpos = blockpos.down();
			}
		}

		this.updateFallState(velY, this.onGround, state, blockpos);

		if (startVelX != velX)
			this.motionX = 0.0D;

		if (startVelZ != velZ)
			this.motionZ = 0.0D;

		if (startVelY != velY) {
			block.onLanded(this.worldObj, this);

			if (block == Blocks.FARMLAND) {
				if (!worldObj.isRemote && worldObj.rand.nextFloat() < 0.7F) {
					if (!worldObj.getGameRules().getBoolean("mobGriefing"))
						return;

					worldObj.setBlockState(blockpos, Blocks.DIRT.getDefaultState());
				}
			}
		}

		double d15 = this.posX - startX;
		double d16 = this.posY - startY;
		double d17 = this.posZ - startZ;

		if (block != Blocks.LADDER)
			d16 = 0.0D;

		if (this.onGround)
			block.onEntityWalk(this.worldObj, blockpos, this);

		this.distanceWalkedModified = (float) ((double) this.distanceWalkedModified + (double) MathHelper.sqrt_double(d15 * d15 + d17 * d17) * 0.6D);
		this.distanceWalkedOnStepModified = (float) ((double) this.distanceWalkedOnStepModified + (double) MathHelper.sqrt_double(d15 * d15 + d16 * d16 + d17 * d17) * 0.6D);

		if (this.distanceWalkedOnStepModified > (float) this.nextStepDistance && state.getMaterial() != Material.AIR) {
			this.nextStepDistance = (int) this.distanceWalkedOnStepModified + 1;

			if (this.isInWater()) {
				float f = MathHelper.sqrt_double(this.motionX * this.motionX * 0.2D + this.motionY * this.motionY + this.motionZ * this.motionZ * 0.2D) * 0.35F;

				if (f > 1.0F)
					f = 1.0F;

				this.playSound(this.getSwimSound(), f, 1.0F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.4F);
			}

			if (!state.getMaterial().isLiquid()) {
				SoundType sound = SoundType.PLANT;
				this.playSound(sound.getStepSound(), sound.getVolume() * 0.15F, sound.getPitch());
			}
		}

		try {
			this.doBlockCollisions();
		} catch (Throwable throwable) {
			CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Checking entity block collision");
			CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being checked for collision");
			this.addEntityCrashInfo(crashreportcategory);
			throw new ReportedException(crashreport);
		}
	}

	private void collideWithNearbyEntities() {
		List<Entity> list = this.worldObj.getEntitiesInAABBexcluding(this, this.getEntityBoundingBox().expand(0.2D, 0.0D, 0.2D), new Predicate<Entity>() {
			public boolean apply(Entity p_apply_1_) {
				return p_apply_1_.canBePushed();
			}
		});

		for (Entity e : list)
			collision(e);
	}

	private void collision(Entity entity) {
		if (isPassenger(entity) || this.getRidingEntity() == entity)
			return;

		if (this.noClip || entity.noClip)
			return;

		if (!this.worldObj.isRemote && entity instanceof EntityMinecart && ((EntityMinecart) entity).getType() == EntityMinecart.Type.RIDEABLE && entity.motionX * entity.motionX + entity.motionZ * entity.motionZ > 0.01D && entity.getPassengers().isEmpty() && this.getRidingEntity() == null) {
			this.startRiding(entity);
			this.motionY += 0.25;
			this.velocityChanged = true;
		} else {
			double dx = this.posX - entity.posX;
			double dz = this.posZ - entity.posZ;
			double dmax = MathHelper.abs_max(dx, dz);

			if (dmax < 0.01D)
				return;

			dmax = (double) MathHelper.sqrt_double(dmax);
			dx /= dmax;
			dz /= dmax;
			double d3 = 1.0D / dmax;

			if (d3 > 1.0D)
				d3 = 1.0D;

			dx *= d3;
			dz *= d3;
			dx *= 0.05D;
			dz *= 0.05D;
			dx *= (double) (1.0F - entity.entityCollisionReduction);
			dz *= (double) (1.0F - entity.entityCollisionReduction);

			if (entity.getPassengers().isEmpty()) {
				entity.motionX += -dx;
				entity.motionZ += -dz;
			}

			if (this.getPassengers().isEmpty()) {
				this.motionX += dx;
				this.motionZ += dz;
			}
		}
	}

	public boolean isNotColliding() {
		return this.worldObj.checkNoEntityCollision(this.getEntityBoundingBox(), this) && this.worldObj.getCollisionBoxes(this, this.getEntityBoundingBox()).isEmpty() && !this.worldObj.containsAnyLiquid(this.getEntityBoundingBox());
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

}