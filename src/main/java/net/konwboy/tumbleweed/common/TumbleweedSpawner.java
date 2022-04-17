package net.konwboy.tumbleweed.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.konwboy.tumbleweed.Tumbleweed;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.util.Mth;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Mod.EventBusSubscriber
public class TumbleweedSpawner {

	private static final int TRY_SPAWN_TICKS = 10 * 20;
	private static final int MOB_COUNT_DIV = 17 * 17;
	private static final int SEARCH_RADIUS = 2;
	private static final int SPAWN_ATTEMPTS = 10;

	private static void trySpawn(ServerLevel world) {
		Set<ChunkPos> eligibleChunksForSpawning = Sets.newHashSet();

		for (Player entityplayer : world.players()) {
			if (entityplayer.isSpectator())
				continue;

			int playerX = Mth.floor(entityplayer.getX() / 16d);
			int playerZ = Mth.floor(entityplayer.getZ() / 16d);

			for (int x = 8; x >= -8; x--) {
				for (int z = 8; z >= -8; z--) {
					boolean corner = x == -8 || x == 8 || z == -8 || z == 8;
					ChunkPos chunk = new ChunkPos(x + playerX, z + playerZ);

					if (eligibleChunksForSpawning.contains(chunk))
						continue;

					if (corner || !world.getWorldBorder().isWithinBounds(chunk))
						continue;

					if (!isEntityProcessing(world, chunk.x * 16, chunk.z * 16))
						continue;

					Optional<ResourceKey<Biome>> biome = world
							.getBiome(new BlockPos(chunk.getMinBlockX() + 8, 0, chunk.getMinBlockZ() + 8))
							.unwrapKey();

					if (biome.isEmpty() || !isValidBiome(biome.get()))
						continue;

					eligibleChunksForSpawning.add(chunk);
				}
			}
		}

		List<ChunkPos> chunkList = Lists.newArrayList(eligibleChunksForSpawning);
		Collections.shuffle(chunkList);

		BlockPos worldSpawn = new BlockPos(world.getLevelData().getXSpawn(), world.getLevelData().getYSpawn(), world.getLevelData().getZSpawn());
		long current = world.getEntities(Tumbleweed.TUMBLEWEED, e -> true).size();
		int max = Mth.ceil(TumbleweedConfig.maxPerPlayer * eligibleChunksForSpawning.size() / (double) MOB_COUNT_DIV);

		for (ChunkPos chunk : chunkList) {
			if (current > max)
				break;

			if (world.random.nextDouble() > TumbleweedConfig.spawnChance)
				continue;

			BlockPos start = getRandomSurfacePosition(world, chunk.x, chunk.z);
			BlockPos spawner = null;

			for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++)
				for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
					BlockPos check = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(start.getX() + x, 0, start.getZ() + z));
					BlockState state = world.getBlockState(check);
					Block block = state.getBlock();

					if (TumbleweedConfig.spawningBlocks.contains(block.getRegistryName()) && world.canSeeSkyFromBelowWater(check)) {
						spawner = check;
						break;
					}
				}

			if (spawner == null)
				continue;

			int packSize = 1 + (world.random.nextFloat() < 0.2f ? 1 : 0);
			int packSpawned = 0;

			for (int i = 0; i < SPAWN_ATTEMPTS; i++) {
				int x = spawner.getX() + world.random.nextInt(5) - world.random.nextInt(5);
				int y = spawner.getY() + world.random.nextInt(2) - world.random.nextInt(2);
				int z = spawner.getZ() + world.random.nextInt(5) - world.random.nextInt(5);

				if (!isEntityProcessing(world, x, z))
					continue;

				if (!world.getBlockState(new BlockPos(x, y - 1, z)).canOcclude())
					continue;

				if (world.hasNearbyAlivePlayer(x, y, z, 32) || worldSpawn.distSqr(new Vec3i(x, y, z)) < 24.0 * 24.0)
					continue;

				EntityTumbleweed entity = Tumbleweed.TUMBLEWEED.create(world);
				entity.setSize(world.random.nextInt(5) - 2);
				entity.moveTo((double) x + 0.5, (double) y + 0.5 + 0.5 * world.random.nextDouble(), (double) z + 0.5, 0.0F, 0.0F);

				if (entity.isNotColliding()) {
					current++;
					packSpawned++;
					world.addFreshEntity(entity);
				}

				if (packSpawned == packSize)
					break;
			}
		}
	}

	private static boolean isValidBiome(ResourceKey<Biome> biome) {
		boolean rightType = BiomeDictionary.hasType(biome, BiomeDictionary.Type.DRY) || BiomeDictionary.hasType(biome, BiomeDictionary.Type.SANDY);
		return TumbleweedConfig.biomeWhitelist.isEmpty() && rightType || TumbleweedConfig.biomeWhitelist.contains(biome.location());
	}

	private static BlockPos getRandomSurfacePosition(Level world, int chunkX, int chunkZ) {
		LevelChunk chunk = world.getChunk(chunkX, chunkZ);
		int x = chunkX * 16 + world.random.nextInt(16 - SEARCH_RADIUS) + SEARCH_RADIUS;
		int z = chunkZ * 16 + world.random.nextInt(16 - SEARCH_RADIUS) + SEARCH_RADIUS;
		int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

		return new BlockPos(x, y, z);
	}

	private static boolean isEntityProcessing(ServerLevel world, double posX, double posZ) {
		return world.isPositionEntityTicking(new BlockPos(posX, 64, posZ));
	}

	@SubscribeEvent
	public static void onTick(TickEvent.WorldTickEvent event) {
		ServerLevel world = (ServerLevel) event.world;

		if (event.phase != TickEvent.Phase.END)
			return;

		if (world.getGameTime() % TRY_SPAWN_TICKS == 7 && world.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
			world.getProfiler().push("spawn_tumbleweed");
			trySpawn(world);
			world.getProfiler().pop();
		}

		// De-spawn to prevent piling up in non entity-processing chunks
		world.getEntities(Tumbleweed.TUMBLEWEED, t -> !t.shouldPersist() && t.tickCount > 0 && !isEntityProcessing(world, t.getX(), t.getZ()))
			.forEach(t -> t.remove(Entity.RemovalReason.DISCARDED));
	}

}