package net.konwboy.tumbleweed.mixins;

import net.konwboy.tumbleweed.common.EntityTumbleweed;
import net.konwboy.tumbleweed.services.Services;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

// Works around vanilla's entity position networking problems (bug MC-170907)
@Mixin(ServerEntity.class)
public class ServerEntityMixin {

	@Final
	@Shadow
	private Entity entity;

	@Final
	@Shadow
	private VecDeltaCodec positionCodec;

	@Shadow
	private int tickCount;

	@Inject(at = @At("RETURN"), method = "<init>")
	private void ctor(ServerLevel level, Entity entity, int updateInterval, boolean trackDelta, Consumer<Packet<?>> broadcast, CallbackInfo ci){
		if (entity instanceof EntityTumbleweed) {
			tickCount = 30;
			positionCodec.setBase(quantize(positionCodec.decode(0, 0, 0)));
		}
	}

	@Inject(at = @At("HEAD"), method = "sendChanges")
	private void preTick(CallbackInfo ci) {
		if (entity instanceof EntityTumbleweed) {
			positionCodec.setBase(quantize(positionCodec.decode(0, 0, 0)));
		}
	}

	@Inject(at = @At("RETURN"), method = "addPairing")
	private void preSeen(ServerPlayer player, CallbackInfo ci) {
		if (entity instanceof EntityTumbleweed) {
			Services.NETWORK.sendPositionAnchor(player, (EntityTumbleweed) entity, positionCodec.decode(0, 0, 0));
		}
	}

	private static double quantize(double d) {
		return Mth.lfloor(d * 4096.0D) / 4096.0;
	}

	private static Vec3 quantize(Vec3 v) {
		return new Vec3(quantize(v.x), quantize(v.y), quantize(v.z));
	}
}
