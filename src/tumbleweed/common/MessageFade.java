package tumbleweed.common;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

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