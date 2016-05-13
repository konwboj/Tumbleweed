package tumbleweed.common;

import com.google.common.collect.Sets;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import tumbleweed.Tumbleweed;

import java.util.List;
import java.util.Set;

public class CommonEventHandler
{
	private static final int MOB_COUNT_DIV = (int) Math.pow(17.0D, 2.0D);
	private int ticks;

	@SubscribeEvent
	public void onTick(TickEvent.WorldTickEvent event)
	{
		WorldServer world = (WorldServer) event.world;

		if (event.phase == TickEvent.Phase.END && world.provider.dimensionId == 0)
		{
			if (this.ticks % 200 == 0)
			{
				Set<ChunkCoordIntPair> eligibleChunksForSpawning = Sets.newHashSet();
				int i = 0;

				for (EntityPlayer entityplayer : (List<EntityPlayer>) world.playerEntities)
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

								if (!flag)
								{
									eligibleChunksForSpawning.add(chunkcoordintpair);
								}
							}
						}
					}
				}

				ChunkCoordinates chunkcoordinates = world.getSpawnPoint();
				int j4 = world.countEntities(EntityTumbleweed.class);
				int k4 = 25 * i / MOB_COUNT_DIV;

				for (ChunkCoordIntPair chunkcoordintpair1 : eligibleChunksForSpawning)
				{
					if (j4 > k4)
						break;

					if (world.rand.nextFloat() < 0.4f)
					{
						ChunkCoordinates blockpos = getRandomChunkPosition(world, chunkcoordintpair1.chunkXPos, chunkcoordintpair1.chunkZPos);
						ChunkCoordinates deadBush = null;
						int r = 4;

						for (int x = -r; x < r; x++)
						{
							for (int y = -r; y < r; y++)
							{
								for (int z = -r; z < r; z++)
								{
									ChunkCoordinates check = new ChunkCoordinates(blockpos.posX + x, blockpos.posY + y, blockpos.posZ + z);
									Block block = world.getBlock(check.posX, check.posY, check.posZ);
									if (block == Blocks.deadbush && world.getFullBlockLightValue(check.posX, check.posY, check.posZ) > 8)
									{
										deadBush = check;
										break;
									}
								}
							}
						}

						if (deadBush != null)
						{
							int x = deadBush.posX;
							int y = deadBush.posY;
							int z = deadBush.posZ;

							float f3 = x - (float) chunkcoordinates.posX;
							float f4 = y - (float) chunkcoordinates.posY;
							float f5 = z - (float) chunkcoordinates.posZ;
							float f6 = f3 * f3 + f4 * f4 + f5 * f5;

							if (world.getClosestPlayer((double) x, (double) y, (double) z, 24.0D) == null && f6 >= 24.0 * 24.0)
							{
								BiomeGenBase biome = world.getBiomeGenForCoords(deadBush.posX, deadBush.posZ);
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

	private static ChunkCoordinates getRandomChunkPosition(World worldIn, int x, int z)
	{
		Chunk chunk = worldIn.getChunkFromChunkCoords(x, z);
		int chunkX = worldIn.rand.nextInt(16);
		int chunkZ = worldIn.rand.nextInt(16);
		int i = x * 16 + chunkX;
		int j = z * 16 + chunkZ;
		int k = chunk.getHeightValue(chunkX, chunkZ) + 1;

		return new ChunkCoordinates(i, k, j);
	}
}