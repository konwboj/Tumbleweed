package net.konwboy.tumbleweed.services;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

public record AnchorMsg(int entityId, Vec3 anchor) {
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(entityId);
		buf.writeDouble(anchor.x);
		buf.writeDouble(anchor.y);
		buf.writeDouble(anchor.z);
	}

	public static AnchorMsg read(FriendlyByteBuf buf) {
		return new AnchorMsg(buf.readVarInt(), new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
	}

	public void handle() {
		var entity = Minecraft.getInstance().level.getEntity(entityId);
		if (entity != null)
			entity.syncPacketPositionCodec(anchor.x, anchor.y, anchor.z);
	}
}