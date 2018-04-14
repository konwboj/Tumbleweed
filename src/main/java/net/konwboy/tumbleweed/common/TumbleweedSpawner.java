package net.konwboy.tumbleweed.common;

import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Set;

public class TumbleweedSpawner {

	private static final int TRY_SPAWN_TICKS = 10 * 20;
	private static final int MOB_COUNT_DIV = 17 * 17;

	@SubscribeEvent
	public void onTick(TickEvent.WorldTickEvent event) {
		World world = event.world;

		if (event.phase != TickEvent.Phase.END)
			return;

		if (world.getTotalWorldTime() % TRY_SPAWN_TICKS == 0)
			trySpawn(world);
	}

	private void trySpawn(World world) {
		Set<ChunkPos> eligibleChunksForSpawning = Sets.newHashSet();
		int chunks = 0;

		for (EntityPlayer entityplayer : world.playerEntities) {
			if (entityplayer.isSpectator())
				continue;

			int playerX = MathHelper.floor_double(entityplayer.posX / 16.0D);
			int playerZ = MathHelper.floor_double(entityplayer.posZ / 16.0D);

			for (int x = -8; x <= 8; ++x)
				for (int z = -8; z <= 8; ++z) {
					boolean flag = x == -8 || x == 8 || z == -8 || z == 8;
					ChunkPos chunk = new ChunkPos(x + playerX, z + playerZ);

					if (eligibleChunksForSpawning.contains(chunk))
						continue;

					++chunks;

					if (!flag && world.getWorldBorder().contains(chunk))
						eligibleChunksForSpawning.add(chunk);
				}
		}

		BlockPos spawnPoint = world.getSpawnPoint();
		int current = world.countEntities(EntityTumbleweed.class);
		int max = 35 * chunks / MOB_COUNT_DIV;

		for (ChunkPos chunk : eligibleChunksForSpawning) {
			if (current > max)
				break;

			if (world.rand.nextDouble() < Config.getSpawnChance()) {
				BlockPos blockpos = getRandomChunkPosition(world, chunk.chunkXPos, chunk.chunkZPos);
				BlockPos spawner = null;
				int r = 4;

				for (int x = -r; x < r; x++)
					for (int y = -r; y < r; y++)
						for (int z = -r; z < r; z++) {
							BlockPos check = new BlockPos(blockpos.getX() + x, blockpos.getY() + y, blockpos.getZ() + z);
							IBlockState state = world.getBlockState(check);
							Block block = state.getBlock();
							Config.Metadata meta = new Config.Metadata(block.getRegistryName(), block.getMetaFromState(state));

							if (Config.getSpawningBlocks().contains(meta) && world.canBlockSeeSky(check)) {
								spawner = check;
								break;
							}
						}

				if (spawner != null) {
					int x = spawner.getX();
					int y = spawner.getY();
					int z = spawner.getZ();
					Biome biome = world.getBiome(spawner);

					if ((Config.getBiomeWhitelist().isEmpty() && BiomeDictionary.hasType(biome, BiomeDictionary.Type.DRY)) || Config.getBiomeWhitelist().contains(biome.getRegistryName())) {
						if (!world.isAnyPlayerWithinRangeAt((double) x, (double) y, (double) z, 24.0D) && spawnPoint.distanceSq((double) x, (double) y, (double) z) >= 24.0 * 24.0) {
							EntityTumbleweed entity = new EntityTumbleweed(world);
							entity.setLocationAndAngles((double) x + 0.5, (double) y + 0.5, (double) z + 0.5, 0.0F, 0.0F);

							if (entity.isNotColliding()) {
								current++;
								world.spawnEntityInWorld(entity);
							}
						}
					}
				}
			}
		}
	}

	private static BlockPos getRandomChunkPosition(World worldIn, int x, int z) {
		Chunk chunk = worldIn.getChunkFromChunkCoords(x, z);
		int i = x * 16 + worldIn.rand.nextInt(16);
		int j = z * 16 + worldIn.rand.nextInt(16);
		int k = chunk.getHeight(new BlockPos(i, 0, j)) + 1;

		return new BlockPos(i, k, j);
	}
}