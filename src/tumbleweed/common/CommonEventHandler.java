package tumbleweed.common;

import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import tumbleweed.Tumbleweed;

import java.util.Set;

public class CommonEventHandler
{
	private static final int MOB_COUNT_DIV = (int) Math.pow(17.0D, 2.0D);
	private int ticks;

	@SubscribeEvent
	public void onTick(TickEvent.WorldTickEvent event)
	{
		WorldServer world = (WorldServer) event.world;

		if (event.phase == TickEvent.Phase.END && world.provider.getDimensionId() == 0)
		{
			if (this.ticks % 200 == 0)
			{
				Set<ChunkCoordIntPair> eligibleChunksForSpawning = Sets.newHashSet();
				int i = 0;

				for (EntityPlayer entityplayer : world.playerEntities)
				{
					if (!entityplayer.isSpectator())
					{
						int j = MathHelper.floor_double(entityplayer.posX / 16.0D);
						int k = MathHelper.floor_double(entityplayer.posZ / 16.0D);
						int l = 8;

						for (int i1 = -l; i1 <= l; ++i1)
						{
							for (int j1 = -l; j1 <= l; ++j1)
							{
								boolean flag = i1 == -l || i1 == l || j1 == -l || j1 == l;
								ChunkCoordIntPair chunkcoordintpair = new ChunkCoordIntPair(i1 + j, j1 + k);

								if (!eligibleChunksForSpawning.contains(chunkcoordintpair))
								{
									++i;

									if (!flag && world.getWorldBorder().contains(chunkcoordintpair))
										eligibleChunksForSpawning.add(chunkcoordintpair);
								}
							}
						}
					}
				}

				BlockPos spawnPoint = world.getSpawnPoint();
				int j4 = world.countEntities(EntityTumbleweed.class);
				int k4 = 35 * i / MOB_COUNT_DIV;

				for (ChunkCoordIntPair chunkcoordintpair : eligibleChunksForSpawning)
				{
					if (j4 > k4)
						break;

					if (world.rand.nextFloat() < 0.4f)
					{
						BlockPos blockpos = getRandomChunkPosition(world, chunkcoordintpair.chunkXPos, chunkcoordintpair.chunkZPos);
						BlockPos deadBush = null;
						int r = 4;

						for (int x = -r; x < r; x++)
						{
							for (int y = -r; y < r; y++)
							{
								for (int z = -r; z < r; z++)
								{
									BlockPos check = new BlockPos(blockpos.getX() + x, blockpos.getY() + y, blockpos.getZ() + z);
									Block block = world.getBlockState(check).getBlock();
									if (block == Blocks.deadbush && world.getLight(check) > 8)
									{
										deadBush = check;
										break;
									}
								}
							}
						}

						if (deadBush != null)
						{
							int x = deadBush.getX();
							int y = deadBush.getY();
							int z = deadBush.getZ();

							if (!world.isAnyPlayerWithinRangeAt((double) x, (double) y, (double) z, 24.0D) && spawnPoint.distanceSq((double) x, (double) y, (double) z) >= 24.0 * 24.0)
							{
								BiomeGenBase biome = world.getBiomeGenForCoords(deadBush);
								if (biome.temperature > 1.8f && biome.rainfall == 0f)
								{
									EntityTumbleweed entity = new EntityTumbleweed(world);
									entity.setLocationAndAngles((double) x + 0.5, (double) y + 0.5, (double) z + 0.5, 0.0F, 0.0F);

									if (entity.isNotColliding())
									{
										j4++;
										world.spawnEntityInWorld(entity);
									}
								}
							}
						}
					}
				}
			}

			if (ticks % (2 * 60 * 20) == 0)
			{
				if (world.rand.nextBoolean())
					Tumbleweed.windX *= -1;

				if (world.rand.nextBoolean())
					Tumbleweed.windZ *= -1;

				Tumbleweed.network.sendToAll(new MessageWind(Tumbleweed.windX, Tumbleweed.windZ));
			}

			this.ticks++;
		}

	}

	@SubscribeEvent
	public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event)
	{
		Tumbleweed.network.sendTo(new MessageWind(Tumbleweed.windX, Tumbleweed.windZ), (EntityPlayerMP) event.player);
	}

	@SubscribeEvent
	public void onConfigChangedEvent(ConfigChangedEvent.OnConfigChangedEvent event)
	{
		if (event.modID.equals(References.MOD_ID))
			Config.load();
	}

	private static BlockPos getRandomChunkPosition(World worldIn, int x, int z)
	{
		Chunk chunk = worldIn.getChunkFromChunkCoords(x, z);
		int i = x * 16 + worldIn.rand.nextInt(16);
		int j = z * 16 + worldIn.rand.nextInt(16);
		int k = chunk.getHeight(new BlockPos(i, 0, j)) + 1;

		return new BlockPos(i, k, j);
	}
}