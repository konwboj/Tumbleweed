package net.konwboy.tumbleweed.services;

import net.konwboy.tumbleweed.common.EntityTumbleweed;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public interface INetwork {

	void sendPositionAnchor(ServerPlayer player, EntityTumbleweed entity, Vec3 anchor);
}
