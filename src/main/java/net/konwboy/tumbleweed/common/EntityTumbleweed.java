package net.konwboy.tumbleweed.common;

import com.google.common.base.Predicate;
import net.konwboy.tumbleweed.Tumbleweed;
import net.konwboy.tumbleweed.client.RenderTumbleweed;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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

	private static final int FADE_AFTER = 2 * 60 * 20;
	public static final int MAX_FADE = 4 * 20;
	private static final int DESPAWN_RANGE = 110;
	private static final float BASE_SIZE = 0.75f;

	private static final DataParameter<Integer> SIZE = EntityDataManager.createKey(EntityTumbleweed.class, DataSerializers.VARINT);
	private static final DataParameter<Boolean> CUSTOM_WIND_ENABLED = EntityDataManager.createKey(EntityTumbleweed.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Float> CUSTOM_WIND_X = EntityDataManager.createKey(EntityTumbleweed.class, DataSerializers.FLOAT);
	private static final DataParameter<Float> CUSTOM_WIND_Z = EntityDataManager.createKey(EntityTumbleweed.class, DataSerializers.FLOAT);
	private static final DataParameter<Boolean> CAN_DESPAWN = EntityDataManager.createKey(EntityTumbleweed.class, DataSerializers.BOOLEAN);

	private int age;
	public int fadeAge;
	public float distanceWalkedModified;
	public float distanceWalkedOnStepModified;
	private int nextStepDistance;
	private int currentSize;
	private int groundTicks;
	public float rot1, rot2, rot3;
	private float windModX, windModZ;

	@SideOnly(Side.CLIENT)
	public Quaternion quat;
	@SideOnly(Side.CLIENT)
	public Quaternion prevQuat;

	public EntityTumbleweed(World world) {
		super(world);

		setSize(BASE_SIZE, BASE_SIZE);
		this.entityCollisionReduction = 0.95f;
		this.preventEntitySpawning = true;
		this.windModX = 1.2f - 0.4f * worldObj.rand.nextFloat();
		this.windModZ = 1.2f - 0.4f * worldObj.rand.nextFloat();

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
		this.dataManager.register(SIZE, -2 + worldObj.rand.nextInt(5));
		this.dataManager.register(CUSTOM_WIND_ENABLED, false);
		this.dataManager.register(CUSTOM_WIND_X, 0f);
		this.dataManager.register(CUSTOM_WIND_Z, 0f);
		this.dataManager.register(CAN_DESPAWN, true);
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tagCompound) {
		tagCompound.setInteger("Size", getSize());
		tagCompound.setBoolean("CustomWindEnabled", getCustomWindEnabled());
		tagCompound.setFloat("CustomWindX", getCustomWindX());
		tagCompound.setFloat("CustomWindZ", getCustomWindZ());
		tagCompound.setBoolean("CanDespawn", getCanDespawn());
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
			this.dataManager.set(CAN_DESPAWN, tagCompound.getBoolean("CanDespawn"));
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

		float size = BASE_SIZE + this.getSize() * (1 / 8f);
		if (this.currentSize != this.getSize()) {
			this.currentSize = this.getSize();
			this.setSize(size, size);
		}

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
		this.moveEntity(this.motionX, this.motionY, this.motionZ);

		float windX = getCustomWindEnabled() ? getCustomWindX() : Tumbleweed.windX * windModX;
		float windZ = getCustomWindEnabled() ? getCustomWindZ() : Tumbleweed.windZ * windModZ;
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

			double div = 5d * size - groundTicks / 5d;
			double rotX = 2d * Math.PI * this.motionX / div;
			double rotZ = -2d * Math.PI * this.motionZ / div;

			this.prevQuat = this.quat;
			RenderTumbleweed.CURRENT.setFromAxisAngle((new Vector4f(1, 0, 0, (float) rotZ)));
			Quaternion.mul(this.quat, RenderTumbleweed.CURRENT, this.quat);
			RenderTumbleweed.CURRENT.setFromAxisAngle((new Vector4f(0, 0, 1, (float) rotX)));
			Quaternion.mul(this.quat, RenderTumbleweed.CURRENT, this.quat);
		}

		// Bounce on ground
		if (this.onGround) {
			if (Math.abs(windX) >= 0.05 || Math.abs(windZ) >= 0.05)
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
			despawnEntity();
		}

		if (this.fadeAge > 0) {
			this.fadeAge++;

			if (this.fadeAge > MAX_FADE)
				setDead();
		}
	}

	private void despawnEntity() {
		if (!getCanDespawn()) {
			this.age = 0;
			return;
		}

		Entity entity = this.worldObj.getClosestPlayerToEntity(this, DESPAWN_RANGE);
		if (entity == null)
			this.setDead();

		if (this.age > FADE_AFTER)
			this.startFading();
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
	public void moveEntity(double x, double y, double z) {
		if (this.noClip) {
			this.setEntityBoundingBox(this.getEntityBoundingBox().offset(x, y, z));
			this.resetPositionToBB();
		} else {
			double d0 = this.posX;
			double d1 = this.posY;
			double d2 = this.posZ;

			if (this.isInWeb) {
				this.isInWeb = false;
				x *= 0.25D;
				y *= 0.05D;
				z *= 0.25D;
				this.motionX = 0.0D;
				this.motionY = 0.0D;
				this.motionZ = 0.0D;
			}

			double d3 = x;
			double d4 = y;
			double d5 = z;

			List<AxisAlignedBB> list1 = this.worldObj.getCollisionBoxes(this, this.getEntityBoundingBox().addCoord(x, y, z));

			for (AxisAlignedBB axisalignedbb1 : list1)
				y = axisalignedbb1.calculateYOffset(this.getEntityBoundingBox(), y);

			this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0D, y, 0.0D));

			for (AxisAlignedBB axisalignedbb2 : list1)
				x = axisalignedbb2.calculateXOffset(this.getEntityBoundingBox(), x);

			this.setEntityBoundingBox(this.getEntityBoundingBox().offset(x, 0.0D, 0.0D));

			for (AxisAlignedBB axisalignedbb13 : list1)
				z = axisalignedbb13.calculateZOffset(this.getEntityBoundingBox(), z);

			this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0D, 0.0D, z));

			this.resetPositionToBB();
			this.isCollidedHorizontally = d3 != x || d5 != z;
			this.isCollidedVertically = d4 != y;
			this.onGround = this.isCollidedVertically && d4 < 0.0D;
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

			this.updateFallState(y, this.onGround, state, blockpos);

			if (d3 != x)
				this.motionX = 0.0D;

			if (d5 != z)
				this.motionZ = 0.0D;

			if (d4 != y) {
				block.onLanded(this.worldObj, this);

				if (block == Blocks.FARMLAND) {
					if (!worldObj.isRemote && worldObj.rand.nextFloat() < 0.7F) {
						if (!worldObj.getGameRules().getBoolean("mobGriefing"))
							return;

						worldObj.setBlockState(blockpos, Blocks.DIRT.getDefaultState());
					}
				}
			}

			double d15 = this.posX - d0;
			double d16 = this.posY - d1;
			double d17 = this.posZ - d2;

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
	}

	private void collideWithNearbyEntities() {
		List list = this.worldObj.getEntitiesInAABBexcluding(this, this.getEntityBoundingBox().expand(0.2D, 0.0D, 0.2D), new Predicate<Entity>() {
			public boolean apply(Entity p_apply_1_) {
				return p_apply_1_.canBePushed();
			}
		});

		if (!list.isEmpty())
			for (int i = 0; i < list.size(); ++i) {
				Entity entity = (Entity) list.get(i);
				collision(entity);
			}
	}

	private void collision(Entity entity) {
		if (!isPassenger(entity) && this.getRidingEntity() != entity) {
			if (!this.noClip && !entity.noClip) {
				if (!this.worldObj.isRemote && entity instanceof EntityMinecart && ((EntityMinecart) entity).getType() == EntityMinecart.Type.RIDEABLE && entity.motionX * entity.motionX + entity.motionZ * entity.motionZ > 0.01D && entity.getPassengers().isEmpty() && this.getRidingEntity() == null) {
					this.startRiding(entity);
					this.motionY += 0.25;
					this.velocityChanged = true;
				} else {
					double dx = this.posX - entity.posX;
					double dz = this.posZ - entity.posZ;
					double dmax = MathHelper.abs_max(dx, dz);

					if (dmax >= 0.01D) {
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
			}
		}
	}

	public void startFading() {
		if (this.fadeAge > 0)
			return;

		this.fadeAge = 1;

		if (!this.worldObj.isRemote)
			if (!this.isDead)
				for (EntityPlayer other : this.worldObj.playerEntities)
					if (other != null && other.worldObj == this.worldObj) {
						double dx = this.posX - other.posX;
						double dy = this.posY - other.posY;
						double dz = this.posZ - other.posZ;
						double distance = dx * dx + dy * dy + dz * dz;

						if (distance < 64D * 64D)
							Tumbleweed.network.sendTo(new MessageFade(this.getEntityId()), (EntityPlayerMP) other);
					}
	}

	public boolean isNotColliding() {
		return this.worldObj.checkNoEntityCollision(this.getEntityBoundingBox(), this) && this.worldObj.getCollisionBoxes(this, this.getEntityBoundingBox()).isEmpty() && !this.worldObj.containsAnyLiquid(this.getEntityBoundingBox());
	}

	public int getSize() {
		return this.dataManager.get(SIZE);
	}

	public float getCustomWindX() {
		return this.dataManager.get(CUSTOM_WIND_X);
	}

	public float getCustomWindZ() {
		return this.dataManager.get(CUSTOM_WIND_Z);
	}

	public boolean getCanDespawn() {
		return this.dataManager.get(CAN_DESPAWN);
	}

	public boolean getCustomWindEnabled() {
		return this.dataManager.get(CUSTOM_WIND_ENABLED);
	}
}