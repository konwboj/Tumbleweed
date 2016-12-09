package net.konwboy.tumbleweed.common;

import com.google.common.collect.Sets;
import net.konwboy.tumbleweed.Tumbleweed;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Set;

public class CommonEventHandler
{
	private static final int MOB_COUNT_DIV = (int) Math.pow(17.0D, 2.0D);
	private int ticks;

	@SubscribeEvent
	public void onTick(TickEvent.WorldTickEvent event)
	{
		World world = event.world;

		if (event.phase == TickEvent.Phase.END && world.provider.getDimension() == 0)
		{
			if (this.ticks % (10 * 20) == 0)
				trySpawn(world);

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
		if (event.getModID().equals(References.MOD_ID))
			Config.load();
	}

	private void trySpawn(World world)
	{
		Set<ChunkPos> eligibleChunksForSpawning = Sets.newHashSet();
		int chunks = 0;

		for (EntityPlayer entityplayer : world.playerEntities)
			if (!entityplayer.isSpectator())
			{
				int playerX = MathHelper.floor_double(entityplayer.posX / 16.0D);
				int playerZ = MathHelper.floor_double(entityplayer.posZ / 16.0D);

				for (int x = -8; x <= 8; ++x)
					for (int z = -8; z <= 8; ++z)
					{
						boolean flag = x == -8 || x == 8 || z == -8 || z == 8;
						ChunkPos chunkcoordintpair = new ChunkPos(x + playerX, z + playerZ);

						if (!eligibleChunksForSpawning.contains(chunkcoordintpair))
						{
							++chunks;

							if (!flag && world.getWorldBorder().contains(chunkcoordintpair))
								eligibleChunksForSpawning.add(chunkcoordintpair);
						}
					}
			}

		BlockPos spawnPoint = world.getSpawnPoint();
		int current = world.countEntities(EntityTumbleweed.class);
		int max = 35 * chunks / MOB_COUNT_DIV;

		for (ChunkPos chunkcoordintpair : eligibleChunksForSpawning)
		{
			if (current > max)
				break;

			if (world.rand.nextFloat() < 0.4f)
			{
				BlockPos blockpos = getRandomChunkPosition(world, chunkcoordintpair.chunkXPos, chunkcoordintpair.chunkZPos);
				BlockPos deadBush = null;
				int r = 4;

				for (int x = -r; x < r; x++)
					for (int y = -r; y < r; y++)
						for (int z = -r; z < r; z++)
						{
							BlockPos check = new BlockPos(blockpos.getX() + x, blockpos.getY() + y, blockpos.getZ() + z);
							Block block = world.getBlockState(check).getBlock();
							if (block == Blocks.DEADBUSH && world.getLight(check) > 8)
							{
								deadBush = check;
								break;
							}
						}

				if (deadBush != null)
				{
					int x = deadBush.getX();
					int y = deadBush.getY();
					int z = deadBush.getZ();

					if (!world.isAnyPlayerWithinRangeAt((double) x, (double) y, (double) z, 24.0D) && spawnPoint.distanceSq((double) x, (double) y, (double) z) >= 24.0 * 24.0)
					{
						Biome biome = world.getBiome(deadBush);
						if (biome.getTemperature() > 1.8f && biome.getRainfall() == 0f)
						{
							EntityTumbleweed entity = new EntityTumbleweed(world);
							entity.setLocationAndAngles((double) x + 0.5, (double) y + 0.5, (double) z + 0.5, 0.0F, 0.0F);

							if (entity.isNotColliding())
							{
								current++;
								world.spawnEntityInWorld(entity);
							}
						}
					}
				}
			}
		}
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