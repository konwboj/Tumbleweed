package net.konwboy.tumbleweed.client;

import net.konwboy.tumbleweed.common.EntityTumbleweed;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;

import java.nio.FloatBuffer;

public class RenderTumbleweed extends Render<EntityTumbleweed>
{
	private static final ResourceLocation TEXTURE = new ResourceLocation("tumbleweed", "textures/entity/tumbleweed.png");
	private static final FloatBuffer BUF_FLOAT_16 = BufferUtils.createFloatBuffer(16);
	private static final Matrix4f MATRIX = new Matrix4f();
	public static final Quaternion CURRENT = new Quaternion();

	private ModelTumbleweed tumbleweed;
	private int lastV = 0;

	public RenderTumbleweed(RenderManager manager)
	{
		super(manager);
		this.shadowSize = 0.4f;
		this.shadowOpaque = 0.8f;
		this.tumbleweed = new ModelTumbleweed();
		this.lastV = this.tumbleweed.getV();
	}

	@Override
	public void doRender(EntityTumbleweed entity, double x, double y, double z, float p_76986_8_, float partialTicks)
	{
		if (lastV != tumbleweed.getV())
		{
			this.tumbleweed = new ModelTumbleweed();
			this.lastV = tumbleweed.getV();
		}

		GlStateManager.pushMatrix();

		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		float alpha = 0.7f;
		float ageFade = 4f * 20f;
		if (entity.fadeAge > 1)
			alpha -= entity.fadeAge / ageFade;
		if (alpha < 0.0)
			alpha = 0f;

		GlStateManager.color(1.0f, 1.0f, 1.0f, alpha);
		GlStateManager.translate((float) x, (float) y + 0.25F, (float) z);

		BUF_FLOAT_16.clear();
		toMatrix(lerp(entity.prevQuat, entity.quat, partialTicks)).store(BUF_FLOAT_16);
		BUF_FLOAT_16.flip();
		GlStateManager.multMatrix(BUF_FLOAT_16);

		GlStateManager.rotate(entity.rot1, 1, 0, 0);
		GlStateManager.rotate(entity.rot2, 0, 1, 0);
		GlStateManager.rotate(entity.rot3, 0, 0, 1);

		float size = 1.0f + entity.getSize() / 8f;
		GlStateManager.scale(size, size, size);

		this.bindTexture(TEXTURE);
		this.tumbleweed.render(entity, 0, 0, 0, 0, 0, 0.0625F);

		GlStateManager.disableBlend();
		GlStateManager.enableLighting();
		GlStateManager.popMatrix();

		super.doRender(entity, x, y, z, p_76986_8_, partialTicks);
	}

	@Override
	protected ResourceLocation getEntityTexture(EntityTumbleweed entity)
	{
		return TEXTURE;
	}

	private static Matrix4f toMatrix(Quaternion quat)
	{
		final float xx = quat.x * quat.x;
		final float xy = quat.x * quat.y;
		final float xz = quat.x * quat.z;
		final float xw = quat.x * quat.w;
		final float yy = quat.y * quat.y;
		final float yz = quat.y * quat.z;
		final float yw = quat.y * quat.w;
		final float zz = quat.z * quat.z;
		final float zw = quat.z * quat.w;

		MATRIX.m00 = 1f - 2f * (yy + zz);
		MATRIX.m10 = 2f * (xy - zw);
		MATRIX.m20 = 2f * (xz + yw);
		MATRIX.m30 = 0f;
		MATRIX.m01 = 2f * (xy + zw);
		MATRIX.m11 = 1f - 2f * (xx + zz);
		MATRIX.m21 = 2f * (yz - xw);
		MATRIX.m31 = 0f;
		MATRIX.m02 = 2f * (xz - yw);
		MATRIX.m12 = 2f * (yz + xw);
		MATRIX.m22 = 1f - 2f * (xx + yy);
		MATRIX.m32 = 0f;
		MATRIX.m03 = 0f;
		MATRIX.m13 = 0f;
		MATRIX.m23 = 0f;
		MATRIX.m33 = 1f;

		MATRIX.transpose();

		return MATRIX;
	}

	private static Quaternion lerp(Quaternion start, Quaternion end, float alpha)
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

	public static class Factory implements IRenderFactory<EntityTumbleweed>
	{
		@Override
		public Render<? super EntityTumbleweed> createRenderFor(RenderManager manager)
		{
			return new RenderTumbleweed(manager);
		}
	}
}
