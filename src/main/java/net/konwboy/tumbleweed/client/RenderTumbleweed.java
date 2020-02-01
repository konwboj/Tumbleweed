package net.konwboy.tumbleweed.client;

import net.konwboy.tumbleweed.Tumbleweed;
import net.konwboy.tumbleweed.common.EntityTumbleweed;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Quaternion;

public class RenderTumbleweed extends Render<EntityTumbleweed> {

	private static final ResourceLocation TEXTURE = new ResourceLocation(Tumbleweed.MOD_ID, "textures/entity/tumbleweed.png");

	private ModelTumbleweed tumbleweed;
	private int lastV = 0;

	public RenderTumbleweed(RenderManager manager) {
		super(manager);
		this.shadowSize = 0.4f;
		this.shadowOpaque = 0.8f;
		this.tumbleweed = new ModelTumbleweed(0);
		this.lastV = this.tumbleweed.getV();
	}

	@Override
	public void doRender(EntityTumbleweed entity, double x, double y, double z, float yaw, float partialTicks) {
		if (lastV != tumbleweed.getV()) {
			this.tumbleweed = new ModelTumbleweed(0);
			this.lastV = tumbleweed.getV();
		}

		GlStateManager.pushMatrix();

		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		float alpha = 1f - entity.fadeAge / (float) EntityTumbleweed.FADE_TIME;
		alpha *= 0.7f;

		this.shadowOpaque = alpha;

		GlStateManager.color(1f, 1f, 1f, alpha);
		GlStateManager.translate(x, y + entity.height * 0.3f, z);

		double scaleY = 1d - Math.sin(Math.max(entity.groundTicks - 7 - partialTicks, 0) / 3f * Math.PI) * 0.25f;
		float size = 1.0f + entity.getSize() / 8f;

		GlStateManager.scale(size, size, size);
		GlStateManager.scale(1f, scaleY, 1f);
		GlStateManager.rotate(lerp(entity.prevQuat, entity.quat, partialTicks));

		GlStateManager.rotate(entity.rot1, 1, 0, 0);
		GlStateManager.rotate(entity.rot2, 0, 1, 0);
		GlStateManager.rotate(entity.rot3, 0, 0, 1);

		this.bindTexture(TEXTURE);
		this.tumbleweed.render(entity, 0, 0, 0, 0, 0, 0.0625F);

		GlStateManager.disableBlend();
		GlStateManager.enableLighting();
		GlStateManager.popMatrix();

		super.doRender(entity, x, y, z, yaw, partialTicks);
	}

	@Override
	protected ResourceLocation getEntityTexture(EntityTumbleweed entity) {
		return TEXTURE;
	}

	public static Quaternion lerp(Quaternion start, Quaternion end, float alpha) {
		Quaternion result = new Quaternion();
		final float d = start.x * end.x + start.y * end.y + start.z * end.z + start.w * end.w;
		float absDot = d < 0.f ? -d : d;

		float scale0 = 1f - alpha;
		float scale1 = alpha;

		if ((1 - absDot) > 0.1) {
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

	public static class Factory implements IRenderFactory<EntityTumbleweed> {

		@Override
		public Render<? super EntityTumbleweed> createRenderFor(RenderManager manager) {
			return new RenderTumbleweed(manager);
		}
	}
}
