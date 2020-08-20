package net.konwboy.tumbleweed.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.konwboy.tumbleweed.Tumbleweed;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber
public class TumbleweedSpawner {

	private static final int TRY_SPAWN_TICKS = 10 * 20;
	private static final int MOB_COUNT_DIV = 17 * 17;
	private static final int SEARCH_RADIUS = 2;
	private static final int SPAWN_ATTEMPTS = 10;

	private static void trySpawn(ServerWorld world) {
		Set<ChunkPos> eligibleChunksForSpawning = Sets.newHashSet();

		for (PlayerEntity entityplayer : world.getPlayers()) {
			if (entityplayer.isSpectator())
				continue;

			int playerX = MathHelper.floor(entityplayer.getPosX() / 16d);
			int playerZ = MathHelper.floor(entityplayer.getPosZ() / 16d);

			for (int x = 8; x >= -8; x--) {
				for (int z = 8; z >= -8; z--) {
					boolean corner = x == -8 || x == 8 || z == -8 || z == 8;
					ChunkPos chunk = new ChunkPos(x + playerX, z + playerZ);

					if (eligibleChunksForSpawning.contains(chunk))
						continue;

					if (corner || !world.getWorldBorder().contains(chunk))
						continue;

					if (!isEntityProcessing(world, chunk.x * 16, chunk.z * 16))
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

		BlockPos worldSpawn = new BlockPos(world.getWorldInfo().getSpawnX(), world.getWorldInfo().getSpawnY(), world.getWorldInfo().getSpawnZ());
		long current = world.getEntities().filter(e -> e.getType() == Tumbleweed.TUMBLEWEED).count();
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
					BlockPos check = world.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, new BlockPos(start.getX() + x, 0, start.getZ() + z));
					BlockState state = world.getBlockState(check);
					Block block = state.getBlock();

					if (TumbleweedConfig.spawningBlocks.contains(block.getRegistryName()) && world.canBlockSeeSky(check)) {
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

				if (!world.getBlockState(new BlockPos(x, y - 1, z)).isSolid())
					continue;

				if (world.isPlayerWithin(x, y, z, 32) || worldSpawn.distanceSq(new Vec3i(x, y, z)) < 24.0 * 24.0)
					continue;

				EntityTumbleweed entity = Tumbleweed.TUMBLEWEED.create(world);
				entity.setSize(world.rand.nextInt(5) - 2);
				entity.setLocationAndAngles((double) x + 0.5, (double) y + 0.5 + 0.5 * world.rand.nextDouble(), (double) z + 0.5, 0.0F, 0.0F);

				if (entity.isNotColliding()) {
					current++;
					packSpawned++;
					world.addEntity(entity);
				}

				if (packSpawned == packSize)
					break;
			}
		}
	}

	private static boolean isValidBiome(Biome biome) {
		boolean rightType = BiomeDictionary.hasType(biome, BiomeDictionary.Type.DRY) || BiomeDictionary.hasType(biome, BiomeDictionary.Type.SANDY);
		return TumbleweedConfig.biomeWhitelist.isEmpty() && rightType || TumbleweedConfig.biomeWhitelist.contains(biome.getRegistryName());
	}

	private static BlockPos getRandomSurfacePosition(World world, int chunkX, int chunkZ) {
		Chunk chunk = world.getChunk(chunkX, chunkZ);
		int x = chunkX * 16 + world.rand.nextInt(16 - SEARCH_RADIUS) + SEARCH_RADIUS;
		int z = chunkZ * 16 + world.rand.nextInt(16 - SEARCH_RADIUS) + SEARCH_RADIUS;
		int y = chunk.getTopBlockY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);

		return new BlockPos(x, y, z);
	}

	private static boolean isEntityProcessing(World world, double posX, double posZ) {
		return world.getChunkProvider().isChunkLoaded(new ChunkPos(MathHelper.floor(posX) >> 4, MathHelper.floor(posZ) >> 4));
	}

	@SubscribeEvent
	public static void onTick(TickEvent.WorldTickEvent event) {
		ServerWorld world = (ServerWorld) event.world;

		if (event.phase != TickEvent.Phase.END)
			return;

		if (world.getGameTime() % TRY_SPAWN_TICKS == 7 && world.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING)) {
			world.getProfiler().startSection("spawn_tumbleweed");
			trySpawn(world);
			world.getProfiler().endSection();
		}

		// De-spawn to prevent piling up in non entity-processing chunks
		world.getEntities()
			.filter(e -> e.getType() == Tumbleweed.TUMBLEWEED)
			.map(t -> (EntityTumbleweed)t)
			.filter(t -> !t.shouldPersist() && t.ticksExisted > 0 && !isEntityProcessing(world, t.getPosX(), t.getPosZ()))
			.forEach(EntityTumbleweed::remove);
	}

}