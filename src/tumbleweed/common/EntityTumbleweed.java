package tumbleweed.common;

import com.google.common.base.Predicate;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemSpade;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector4f;
import tumbleweed.Tumbleweed;

import java.util.List;

public class EntityTumbleweed extends Entity implements IEntityAdditionalSpawnData
{
	private static final float BASE_SIZE = 0.75f;

	private int age;
	public int fadeAge;
	public float distanceWalkedModified;
	public float distanceWalkedOnStepModified;
	private int nextStepDistance;
	private int currentSize;
	private boolean canDespawn;

	@SideOnly(Side.CLIENT)
	public Quaternion quat;
	@SideOnly(Side.CLIENT)
	public Quaternion prevQuat;
	@SideOnly(Side.CLIENT)
	private Quaternion current;

	public EntityTumbleweed(World world)
	{
		super(world);

		this.setSize(BASE_SIZE, BASE_SIZE);
		this.entityCollisionReduction = 0.95f;
		this.preventEntitySpawning = true;
		this.canDespawn = true;
		this.renderDistanceWeight = 3;

		if (this.worldObj.isRemote)
		{
			this.quat = new Quaternion();
			this.prevQuat = new Quaternion();
			this.current = new Quaternion();
		}
	}

	@Override
	protected void entityInit()
	{
		this.dataWatcher.addObject(5, 1 - (3 - worldObj.rand.nextInt(5)));
		this.dataWatcher.addObject(6, 1.0f + (0.2f - 0.4f * worldObj.rand.nextFloat()));
		this.dataWatcher.addObject(7, 1.0f + (0.2f - 0.4f * worldObj.rand.nextFloat()));
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tagCompound)
	{
		tagCompound.setInteger("Size", getSize());
		tagCompound.setFloat("WindModX", getWindModX());
		tagCompound.setFloat("WindModZ", getWindModZ());
		tagCompound.setBoolean("CanDespawn", canDespawn);
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tagCompound)
	{
		if (tagCompound.hasKey("Size"))
			this.dataWatcher.updateObject(5, tagCompound.getInteger("Size"));

		if (tagCompound.hasKey("WindModX"))
			this.dataWatcher.updateObject(6, tagCompound.getFloat("WindModX"));

		if (tagCompound.hasKey("WindModZ"))
			this.dataWatcher.updateObject(7, tagCompound.getFloat("WindModZ"));

		if (tagCompound.hasKey("CanDespawn"))
			this.canDespawn = tagCompound.getBoolean("CanDespawn");
	}

	@Override
	public AxisAlignedBB getCollisionBox(Entity entityIn)
	{
		return null;
	}

	@Override
	public boolean canBeCollidedWith()
	{
		return true;
	}

	@Override
	public boolean canBePushed()
	{
		return true;
	}

	@Override
	public void onUpdate()
	{
		super.onUpdate();

		float size = BASE_SIZE + this.currentSize * (1 / 8f);
		if (this.currentSize != this.getSize())
		{
			this.currentSize = this.getSize();
			this.setSize(size, size);
		}

		if (this.ridingEntity != null)
		{
			this.motionX = 0;
			this.motionY = 0;
			this.motionZ = 0;
		} else
		{
			if (!this.isInWater())
				this.motionY -= 0.012;

			double x = this.motionX;
			double y = this.motionY;
			double z = this.motionZ;

			this.moveEntity(this.motionX, this.motionY, this.motionZ);

			if (this.isInWater())
			{
				this.motionY += 0.01;
				this.motionX *= 0.95;
				this.motionZ *= 0.95;
			} else
			{
				float windX = getWindX();
				float windZ = getWindZ();
				if (windX != 0 || windZ != 0)
				{
					this.motionX = windX;
					this.motionZ = windZ;
				}
			}

			if (this.worldObj.isRemote)
			{
				double rotX = 360d * (-this.motionZ / (5 * size));
				double rotZ = 360d * (this.motionX / (5 * size));

				this.prevQuat = this.quat;
				this.current.setFromAxisAngle((new Vector4f(1, 0, 0, (float) Math.toRadians(rotX))));
				Quaternion.mul(this.quat, this.current, this.quat);
				this.current.setFromAxisAngle((new Vector4f(0, 0, 1, (float) Math.toRadians(rotZ))));
				Quaternion.mul(this.quat, this.current, this.quat);
			}

			if (this.onGround)
			{
				if (Math.abs(this.getWindX()) >= 0.05 || Math.abs(this.getWindZ()) >= 0.05)
					this.motionY = Math.max(-y * 0.7, 0.24 - getSize() * 0.02);
				else
					this.motionY = -y * 0.7;
			}

			if (this.isCollidedHorizontally)
			{
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

			if (!this.worldObj.isRemote)
			{
				this.age++;
				despawnEntity();
			}

			if (this.fadeAge > 0)
			{
				this.fadeAge++;

				if (this.fadeAge > 4 * 20)
					setDead();
			}
		}
	}

	private void despawnEntity()
	{
		if (!this.canDespawn)
		{
			this.age = 0;
		} else
		{
			Entity entity = this.worldObj.getClosestPlayerToEntity(this, -1.0D);

			if (entity != null)
			{
				double d0 = entity.posX - this.posX;
				double d1 = entity.posY - this.posY;
				double d2 = entity.posZ - this.posZ;
				double d3 = d0 * d0 + d1 * d1 + d2 * d2;

				if (d3 > 110 * 110)
				{
					this.setDead();
				}
			}

			if (this.age > 2 * 60 * 20)
			{
				this.startFading();
			}
		}
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount)
	{
		if (!this.worldObj.isRemote && (!(source.getEntity() instanceof EntityPlayer) || ((EntityPlayer) source.getEntity()).capabilities.isCreativeMode))
		{
			Block.SoundType sound = Block.soundTypeGrass;
			this.playSound(sound.getBreakSound(), (sound.getVolume() + 1.0F) / 2.0F, sound.getFrequency() * 0.8F);

			EntityItem entityitem = new EntityItem(this.worldObj, this.posX, this.posY, this.posZ, Config.getRandomItem());
			entityitem.motionX = 0;
			entityitem.motionY = 0.2D;
			entityitem.motionZ = 0;
			entityitem.setDefaultPickupDelay();
			this.worldObj.spawnEntityInWorld(entityitem);

			this.kill();

			return true;
		}

		return false;
	}

	@Override
	public void moveEntity(double x, double y, double z)
	{
		if (this.noClip)
		{
			this.setEntityBoundingBox(this.getEntityBoundingBox().offset(x, y, z));
			this.resetPositionToBB();
		} else
		{
			double d0 = this.posX;
			double d1 = this.posY;
			double d2 = this.posZ;

			if (this.isInWeb)
			{
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

			List<AxisAlignedBB> list1 = this.worldObj.getCollidingBoundingBoxes(this, this.getEntityBoundingBox().addCoord(x, y, z));

			for (AxisAlignedBB axisalignedbb1 : list1)
				y = axisalignedbb1.calculateYOffset(this.getEntityBoundingBox(), y);

			this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0D, y, 0.0D));

			for (AxisAlignedBB axisalignedbb2 : list1)
				x = axisalignedbb2.calculateXOffset(this.getEntityBoundingBox(), x);

			this.setEntityBoundingBox(this.getEntityBoundingBox().offset(x, 0.0D, 0.0D));

			for (AxisAlignedBB axisalignedbb13 : list1)
				z = axisalignedbb13.calculateZOffset(this.getEntityBoundingBox(), z);

			this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0D, 0.0D, z));

			this.worldObj.theProfiler.endSection();
			this.worldObj.theProfiler.startSection("rest");
			this.resetPositionToBB();
			this.isCollidedHorizontally = d3 != x || d5 != z;
			this.isCollidedVertically = d4 != y;
			this.onGround = this.isCollidedVertically && d4 < 0.0D;
			this.isCollided = this.isCollidedHorizontally || this.isCollidedVertically;
			int i = MathHelper.floor_double(this.posX);
			int j = MathHelper.floor_double(this.posY - 0.2D);
			int k = MathHelper.floor_double(this.posZ);
			BlockPos blockpos = new BlockPos(i, j, k);
			Block block1 = this.worldObj.getBlockState(blockpos).getBlock();

			if (block1.getMaterial() == Material.air)
			{
				Block block = this.worldObj.getBlockState(blockpos.down()).getBlock();

				if (block instanceof BlockFence || block instanceof BlockWall || block instanceof BlockFenceGate)
				{
					block1 = block;
					blockpos = blockpos.down();
				}
			}

			this.updateFallState(y, this.onGround, block1, blockpos);

			if (d3 != x)
				this.motionX = 0.0D;

			if (d5 != z)
				this.motionZ = 0.0D;

			if (d4 != y)
			{
				block1.onLanded(this.worldObj, this);

				if (block1 == Blocks.farmland)
				{
					if (!worldObj.isRemote && worldObj.rand.nextFloat() < 0.7F)
					{
						if (!worldObj.getGameRules().getBoolean("mobGriefing"))
							return;

						worldObj.setBlockState(blockpos, Blocks.dirt.getDefaultState());
					}
				}
			}

			double d15 = this.posX - d0;
			double d16 = this.posY - d1;
			double d17 = this.posZ - d2;

			if (block1 != Blocks.ladder)
				d16 = 0.0D;

			if (this.onGround)
				block1.onEntityCollidedWithBlock(this.worldObj, blockpos, this);

			this.distanceWalkedModified = (float) ((double) this.distanceWalkedModified + (double) MathHelper.sqrt_double(d15 * d15 + d17 * d17) * 0.6D);
			this.distanceWalkedOnStepModified = (float) ((double) this.distanceWalkedOnStepModified + (double) MathHelper.sqrt_double(d15 * d15 + d16 * d16 + d17 * d17) * 0.6D);

			if (this.distanceWalkedOnStepModified > (float) this.nextStepDistance && block1.getMaterial() != Material.air)
			{
				this.nextStepDistance = (int) this.distanceWalkedOnStepModified + 1;

				if (this.isInWater())
				{
					float f = MathHelper.sqrt_double(this.motionX * this.motionX * 0.2D + this.motionY * this.motionY + this.motionZ * this.motionZ * 0.2D) * 0.35F;

					if (f > 1.0F)
						f = 1.0F;

					this.playSound(this.getSwimSound(), f, 1.0F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.4F);
				}

				if (!block1.getMaterial().isLiquid())
				{
					Block.SoundType sound = Block.soundTypeGrass;
					this.playSound(sound.getStepSound(), sound.getVolume() * 0.15F, sound.getFrequency());
				}
			}

			try
			{
				this.doBlockCollisions();
			} catch (Throwable throwable)
			{
				CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Checking entity block collision");
				CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being checked for collision");
				this.addEntityCrashInfo(crashreportcategory);
				throw new ReportedException(crashreport);
			}

			this.worldObj.theProfiler.endSection();
		}
	}

	private void resetPositionToBB()
	{
		this.posX = (this.getEntityBoundingBox().minX + this.getEntityBoundingBox().maxX) / 2.0D;
		this.posY = this.getEntityBoundingBox().minY;
		this.posZ = (this.getEntityBoundingBox().minZ + this.getEntityBoundingBox().maxZ) / 2.0D;
	}

	private void collideWithNearbyEntities()
	{
		List list = this.worldObj.getEntitiesInAABBexcluding(this, this.getEntityBoundingBox().expand(0.2D, 0.0D, 0.2D), new Predicate<Entity>()
		{
			public boolean apply(Entity p_apply_1_)
			{
				return p_apply_1_.canBePushed();
			}
		});

		if (!list.isEmpty())
			for (int i = 0; i < list.size(); ++i)
			{
				Entity entity = (Entity) list.get(i);
				collision(entity);
			}
	}

	private void collision(Entity entity)
	{
		if (this.riddenByEntity != entity && this.ridingEntity != entity)
		{
			if (!this.noClip && !entity.noClip)
			{
				if (!this.worldObj.isRemote && entity instanceof EntityMinecart && ((EntityMinecart) entity).getMinecartType() == EntityMinecart.EnumMinecartType.RIDEABLE && entity.motionX * entity.motionX + entity.motionZ * entity.motionZ > 0.01D && entity.riddenByEntity == null && this.ridingEntity == null)
				{
					this.mountEntity(entity);
					this.motionY += 0.25;
					this.velocityChanged = true;
				} else
				{
					double d0 = this.posX - entity.posX;
					double d1 = this.posZ - entity.posZ;
					double d2 = MathHelper.abs_max(d0, d1);

					if (d2 >= 0.01D)
					{
						d2 = (double) MathHelper.sqrt_double(d2);
						d0 /= d2;
						d1 /= d2;
						double d3 = 1.0D / d2;

						if (d3 > 1.0D)
							d3 = 1.0D;

						d0 *= d3;
						d1 *= d3;
						d0 *= 0.05D;
						d1 *= 0.05D;
						d0 *= (double) (1.0F - entity.entityCollisionReduction);
						d1 *= (double) (1.0F - entity.entityCollisionReduction);

						if (entity.riddenByEntity == null)
						{
							entity.motionX += -d0;
							entity.motionZ += -d1;
						}

						if (this.riddenByEntity == null)
						{
							this.motionX += d0;
							this.motionZ += d1;
						}
					}
				}
			}
		}
	}

	@Override
	public boolean interactFirst(EntityPlayer playerIn)
	{
		if (!this.worldObj.isRemote && playerIn.getHeldItem() != null && playerIn.getHeldItem().getItem() instanceof ItemSpade)
		{
			this.motionY += 0.2;
			this.motionZ += 0.15;
			this.velocityChanged = true;
		}

		return true;
	}

	@Override
	public void writeSpawnData(ByteBuf buffer)
	{
		buffer.writeDouble(this.posX);
		buffer.writeDouble(this.posY);
		buffer.writeDouble(this.posZ);

		buffer.writeDouble(this.motionX);
		buffer.writeDouble(this.motionY);
		buffer.writeDouble(this.motionZ);
	}

	@Override
	public void readSpawnData(ByteBuf additionalData)
	{
		setPosition(additionalData.readDouble(), additionalData.readDouble(), additionalData.readDouble());
		setVelocity(additionalData.readDouble(), additionalData.readDouble(), additionalData.readDouble());
	}

	public void startFading()
	{
		this.fadeAge = 1;

		if (!this.worldObj.isRemote)
		{
			if (!this.isDead)
				for (EntityPlayer other : this.worldObj.playerEntities)
				{
					if (other != null && other.worldObj == this.worldObj)
					{
						double d0 = this.posX - other.posX;
						double d1 = this.posY - other.posY;
						double d2 = this.posZ - other.posZ;

						if (d0 * d0 + d1 * d1 + d2 * d2 < 64D * 64D)
							Tumbleweed.network.sendTo(new MessageFade(this.getEntityId()), (EntityPlayerMP) other);
					}
				}
		}
	}

	public boolean isNotColliding()
	{
		return this.worldObj.checkNoEntityCollision(this.getEntityBoundingBox(), this) && this.worldObj.getCollidingBoundingBoxes(this, this.getEntityBoundingBox()).isEmpty() && !this.worldObj.isAnyLiquid(this.getEntityBoundingBox());
	}

	public float getWindX()
	{
		return Tumbleweed.windX * getWindModX();
	}

	public float getWindZ()
	{
		return Tumbleweed.windZ * getWindModZ();
	}

	public int getSize()
	{
		return this.dataWatcher.getWatchableObjectInt(5);
	}

	public float getWindModX()
	{
		return this.dataWatcher.getWatchableObjectFloat(6);
	}

	public float getWindModZ()
	{
		return this.dataWatcher.getWatchableObjectFloat(7);
	}
}