package net.konwboy.tumbleweed;

import net.konwboy.tumbleweed.common.EntityTumbleweed;
import net.konwboy.tumbleweed.services.AnchorMsg;
import net.konwboy.tumbleweed.services.INetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ForgeNetwork implements INetwork {

	private static final String PROTOCOL_VERSION = "1";

	private static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
			Constants.TUMBLEWEED_CHANNEL,
			() -> PROTOCOL_VERSION,
			PROTOCOL_VERSION::equals,
			PROTOCOL_VERSION::equals
	);

	public static void load() {
		INSTANCE.registerMessage(0, AnchorMsg.class, AnchorMsg::write, AnchorMsg::read, (msg, ctx) -> {
			ctx.get().enqueueWork(msg::handle);
			ctx.get().setPacketHandled(true);
		});
	}

	@Override
	public void sendPositionAnchor(ServerPlayer player, EntityTumbleweed entity, Vec3 anchor) {
		INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new AnchorMsg(entity.getId(), anchor));
	}

}
