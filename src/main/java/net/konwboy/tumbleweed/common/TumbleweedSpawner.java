package net.konwboy.tumbleweed.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TumbleweedSpawner {

	private static final int TRY_SPAWN_TICKS = 10 * 20;
	private static final int MOB_COUNT_DIV = 17 * 17;
	private static final int SEARCH_RADIUS = 2;
	private static final int SPAWN_ATTEMPTS = 10;

	private static void trySpawn(WorldServer world) {
		Set<ChunkPos> eligibleChunksForSpawning = Sets.newHashSet();

		for (EntityPlayer entityplayer : world.playerEntities) {
			if (entityplayer.isSpectator())
				continue;

			int playerX = MathHelper.floor(entityplayer.posX / 16.0D);
			int playerZ = MathHelper.floor(entityplayer.posZ / 16.0D);

			for (int x = 8; x >= -8; x--) {
				for (int z = 8; z >= -8; z--) {
					boolean corner = x == -8 || x == 8 || z == -8 || z == 8;
					ChunkPos chunk = new ChunkPos(x + playerX, z + playerZ);

					if (eligibleChunksForSpawning.contains(chunk))
						continue;

					if (corner || !world.getWorldBorder().contains(chunk))
						continue;

					PlayerChunkMapEntry playerChunk = world.getPlayerChunkMap().getEntry(chunk.x, chunk.z);
					if (playerChunk == null || !playerChunk.isSentToPlayers())
						continue;

					Biome biome = world.getBiome(chunk.getBlock(8, 0, 8));
					if (!isValidBiome(biome))
						continue;

					eligibleChunksForSpawning.add(chunk);
				}
			}
		}

		List<ChunkPos> chunkList = Lists.newArrayList(eligibleChunksForSpawning);
		Collections.shuffle(chunkList);

		BlockPos worldSpawn = world.getSpawnPoint();
		int current = world.countEntities(net.konwboy.tumbleweed.common.EntityTumbleweed.class);
		int max = MathHelper.ceil(TumbleweedConfig.maxPerPlayer * eligibleChunksForSpawning.size() / (double) MOB_COUNT_DIV);

		for (ChunkPos chunk : chunkList) {
			if (current > max)
				break;

			if (world.rand.nextDouble() > TumbleweedConfig.spawnChance)
				continue;

			BlockPos start = getRandomSurfacePosition(world, chunk.x, chunk.z);
			BlockPos spawner = null;

			for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++)
				for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
					BlockPos check = world.getHeight(new BlockPos(start.getX() + x, 0, start.getZ() + z));
					IBlockState state = world.getBlockState(check);
					Block block = state.getBlock();
					TumbleweedConfig.Metadata meta = new TumbleweedConfig.Metadata(block.getRegistryName(), block.getMetaFromState(state));

					if (TumbleweedConfig.Logic.spawningBlocks.contains(meta) && world.canBlockSeeSky(check)) {
						spawner = check;
						break;
					}
				}

			if (spawner == null)
				continue;

			int packSize = 1 + (world.rand.nextFloat() < 0.2f ? 1 : 0);
			int packSpawned = 0;

			for (int i = 0; i < SPAWN_ATTEMPTS; i++) {
				int x = spawner.getX() + world.rand.nextInt(5) - world.rand.nextInt(5);
				int y = spawner.getY() + world.rand.nextInt(2) - world.rand.nextInt(2);
				int z = spawner.getZ() + world.rand.nextInt(5) - world.rand.nextInt(5);

				if (!isEntityProcessing(world, x, z))
					continue;

				if (!world.isSideSolid(new BlockPos(x, y - 1, z), EnumFacing.UP))
					continue;

				if (world.isAnyPlayerWithinRangeAt(x, y, z, 32) || worldSpawn.distanceSq(x, y, z) < 24.0 * 24.0)
					continue;

				net.konwboy.tumbleweed.common.EntityTumbleweed entity = new net.konwboy.tumbleweed.common.EntityTumbleweed(world);
				entity.setSize(world.rand.nextInt(5) - 2);
				entity.setLocationAndAngles((double) x + 0.5, (double) y + 0.5 + 0.5 * world.rand.nextDouble(), (double) z + 0.5, 0.0F, 0.0F);

				if (entity.isNotColliding()) {
					current++;
					packSpawned++;
					world.spawnEntity(entity);
				}

				if (packSpawned == packSize)
					break;
			}
		}
	}

	private static boolean isValidBiome(Biome biome) {
		boolean rightType = BiomeDictionary.hasType(biome, BiomeDictionary.Type.DRY) || BiomeDictionary.hasType(biome, BiomeDictionary.Type.SANDY);
		return TumbleweedConfig.Logic.biomeWhitelist.isEmpty() && rightType || TumbleweedConfig.Logic.biomeWhitelist.contains(biome.getRegistryName());
	}

	private static BlockPos getRandomSurfacePosition(World world, int chunkX, int chunkZ) {
		Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
		int x = chunkX * 16 + world.rand.nextInt(16 - SEARCH_RADIUS) + SEARCH_RADIUS;
		int z = chunkZ * 16 + world.rand.nextInt(16 - SEARCH_RADIUS) + SEARCH_RADIUS;
		int y = chunk.getHeight(new BlockPos(x, 0, z));

		return new BlockPos(x, y, z);
	}

	private static boolean isEntityProcessing(World world, double posX, double posZ) {
		int x = MathHelper.floor(posX);
		int z = MathHelper.floor(posZ);
		return world.isAreaLoaded(new BlockPos(x - 32, 0, z - 32), new BlockPos(x + 32, 0, z + 32));
	}

	@SubscribeEvent
	public void onTick(TickEvent.WorldTickEvent event) {
		WorldServer world = (WorldServer) event.world;

		if (event.phase != TickEvent.Phase.END)
			return;

		if (world.getTotalWorldTime() % TRY_SPAWN_TICKS == 7) {
			world.profiler.startSection("spawn_tumbleweed");
			trySpawn(world);
			world.profiler.endSection();
		}
	}

	@SubscribeEvent
	public void onCanUpdate(EntityEvent.CanUpdate event) {
		Entity entity = event.getEntity();
		World world = entity.getEntityWorld();

		// De-spawn to prevent piling up in non entity-processing chunks
		if (!world.isRemote && entity instanceof EntityTumbleweed && entity.ticksExisted > 0 && !isEntityProcessing(world, entity.posX, entity.posZ) && !((EntityTumbleweed) entity).persistent && !entity.isRiding())
			world.removeEntity(entity);
	}

}