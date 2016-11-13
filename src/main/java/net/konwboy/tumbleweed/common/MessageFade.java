package net.konwboy.tumbleweed.common;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public class MessageFade implements IMessage
{
	private int eid;

	public MessageFade()
	{
	}

	public MessageFade(int eid)
	{
		this.eid = eid;
	}

	@Override
	public void fromBytes(ByteBuf buf)
	{
		this.eid = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf)
	{
		buf.writeInt(this.eid);
	}

	public static class Handler implements IMessageHandler<MessageFade, IMessage>
	{
		@Override
		public IMessage onMessage(final MessageFade message, final MessageContext ctx)
		{
			if (ctx.side == Side.CLIENT)
			{
				Entity entity = Minecraft.getMinecraft().theWorld.getEntityByID(message.eid);
				if (entity != null && entity instanceof EntityTumbleweed)
					((EntityTumbleweed) entity).startFading();
			}

			return null;
		}
	}
}