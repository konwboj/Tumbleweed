package tumbleweed.client;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import tumbleweed.common.EntityTumbleweed;

import java.nio.FloatBuffer;

public class RenderTumbleweed extends Render
{
	private static final ResourceLocation texture = new ResourceLocation("tumbleweed", "textures/entity/tumbleweed.png");

	private ModelTumbleweed tumbleweed;
	private int lastV = 0;

	public RenderTumbleweed()
	{
		this.tumbleweed = new ModelTumbleweed();
		this.lastV = this.tumbleweed.getV();
	}

	@Override
	public void doRender(Entity entity, double x, double y, double z, float p_76986_8_, float partialTicks)
	{
		if (lastV != tumbleweed.getV())
		{
			this.tumbleweed = new ModelTumbleweed();
			this.lastV = tumbleweed.getV();
		}

		EntityTumbleweed tumbleweed = (EntityTumbleweed) entity;

		GL11.glPushMatrix();

		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		float alpha = 0.7f;
		float ageFade = 4f * 20f;
		if (tumbleweed.fadeAge > 1)
			alpha -= tumbleweed.fadeAge / ageFade;
		if (alpha < 0.0)
			alpha = 0f;

		GL11.glColor4f(1.0f, 1.0f, 1.0f, alpha);
		GL11.glTranslatef((float) x, (float) y + 0.25F, (float) z);

		FloatBuffer buffer = BufferUtils.createFloatBuffer(4 * 4);
		toMatrix(slerp(tumbleweed.prevQuat, tumbleweed.quat, partialTicks)).store(buffer);
		buffer.flip();
		GL11.glMultMatrix(buffer);

		float size = 1.0f + tumbleweed.getSize() * (1 / 8f);
		GL11.glScalef(size, size, size);

		this.bindTexture(texture);
		this.tumbleweed.render(entity, 0, 0, 0, 0, 0, 0.0625F);

		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glPopMatrix();
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity)
	{
		return texture;
	}

	private static Matrix4f toMatrix(Quaternion quat)
	{
		Matrix4f matrix = new Matrix4f();

		final float xx = quat.x * quat.x;
		final float xy = quat.x * quat.y;
		final float xz = quat.x * quat.z;
		final float xw = quat.x * quat.w;
		final float yy = quat.y * quat.y;
		final float yz = quat.y * quat.z;
		final float yw = quat.y * quat.w;
		final float zz = quat.z * quat.z;
		final float zw = quat.z * quat.w;

		matrix.m00 = 1 - 2 * (yy + zz);
		matrix.m10 = 2 * (xy - zw);
		matrix.m20 = 2 * (xz + yw);
		matrix.m30 = 0;
		matrix.m01 = 2 * (xy + zw);
		matrix.m11 = 1 - 2 * (xx + zz);
		matrix.m21 = 2 * (yz - xw);
		matrix.m31 = 0;
		matrix.m02 = 2 * (xz - yw);
		matrix.m12 = 2 * (yz + xw);
		matrix.m22 = 1 - 2 * (xx + yy);
		matrix.m32 = 0;
		matrix.m03 = 0;
		matrix.m13 = 0;
		matrix.m23 = 0;
		matrix.m33 = 1;

		matrix.transpose();

		return matrix;
	}

	private static Quaternion slerp(Quaternion start, Quaternion end, float alpha)
	{
		Quaternion result = new Quaternion();
		final float d = start.x * end.x + start.y * end.y + start.z * end.z + start.w * end.w;
		float absDot = d < 0.f ? -d : d;

		float scale0 = 1f - alpha;
		float scale1 = alpha;

		if ((1 - absDot) > 0.1)
		{
			final float angle = (float) Math.acos(absDot);
			final float invSinTheta = 1f / (float) Math.sin(angle);

			scale0 = ((float) Math.sin((1f - alpha) * angle) * invSinTheta);
			scale1 = ((float) Math.sin((alpha * angle)) * invSinTheta);
		}

		if (d < 0.f)
			scale1 = -scale1;

		result.x = (scale0 * start.x) + (scale1 * end.x);
		result.y = (scale0 * start.y) + (scale1 * end.y);
		result.z = (scale0 * start.z) + (scale1 * end.z);
		result.w = (scale0 * start.w) + (scale1 * end.w);

		return result;
	}
}
