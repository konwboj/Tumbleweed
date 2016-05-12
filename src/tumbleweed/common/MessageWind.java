package tumbleweed.common;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import tumbleweed.Tumbleweed;

public class MessageWind implements IMessage
{
	private float windX;
	private float windZ;

	public MessageWind()
	{
	}

	public MessageWind(float windX, float windZ)
	{
		this.windX = windX;
		this.windZ = windZ;
	}

	@Override
	public void fromBytes(ByteBuf buf)
	{
		this.windX = buf.readFloat();
		this.windZ = buf.readFloat();
	}

	@Override
	public void toBytes(ByteBuf buf)
	{
		buf.writeFloat(this.windX);
		buf.writeFloat(this.windZ);
	}

	public static class Handler implements IMessageHandler<MessageWind, IMessage>
	{
		@Override
		public IMessage onMessage(final MessageWind message, final MessageContext ctx)
		{
			if (ctx.side == Side.CLIENT)
			{
				Tumbleweed.windX = message.windX;
				Tumbleweed.windZ = message.windZ;
			}

			return null;
		}
	}
}