package net.konwboy.tumbleweed;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.konwboy.tumbleweed.common.EntityTumbleweed;
import net.konwboy.tumbleweed.services.AnchorMsg;
import net.konwboy.tumbleweed.services.INetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class FabricNetwork implements INetwork {

	public static void loadClient() {
		ClientPlayNetworking.registerGlobalReceiver(Constants.TUMBLEWEED_CHANNEL, (client, handler, buf, responseSender) -> {
			var msg = AnchorMsg.read(buf);
			client.execute(msg::handle);
		});
	}

	@Override
	public void sendPositionAnchor(ServerPlayer player, EntityTumbleweed entity, Vec3 anchor) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		new AnchorMsg(entity.getId(), anchor).write(buf);
		ServerPlayNetworking.send(player, Constants.TUMBLEWEED_CHANNEL, buf);
	}
}
