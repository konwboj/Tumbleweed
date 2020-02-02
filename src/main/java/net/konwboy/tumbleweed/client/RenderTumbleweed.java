package net.konwboy.tumbleweed.client;

import com.mojang.blaze3d.platform.GlStateManager;
import net.konwboy.tumbleweed.Tumbleweed;
import net.konwboy.tumbleweed.common.EntityTumbleweed;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.Quaternion;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import org.lwjgl.opengl.GL11;

public class RenderTumbleweed extends EntityRenderer<EntityTumbleweed> {

	private static final ResourceLocation TEXTURE = new ResourceLocation(Tumbleweed.MOD_ID, "textures/entity/tumbleweed.png");

	private ModelTumbleweed tumbleweed;
	private int lastV = 0;

	public RenderTumbleweed(EntityRendererManager manager) {
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

		this.shadowSize = entity.getWidth() * 0.5f;

		GlStateManager.pushMatrix();

		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		float alpha = 1f - entity.fadeProgress / (float) EntityTumbleweed.FADE_TIME;
		alpha *= 0.7f;

		this.shadowOpaque = alpha;

		GlStateManager.color4f(1f, 1f, 1f, alpha);
		GlStateManager.translated(x, y + entity.getHeight() * 0.3f, z);

		float size = 1.0f + entity.getSize() / 8f;
		GlStateManager.scaled(size, size, size);

		float stretch = MathHelper.lerp(partialTicks, entity.prevStretch, entity.stretch);
		GlStateManager.scaled(1f, stretch, 1f);

		GlStateManager.multMatrix(new Matrix4f(lerp(entity.prevQuat, entity.quat, partialTicks)));

		GlStateManager.rotated(entity.rot1, 1, 0, 0);
		GlStateManager.rotated(entity.rot2, 0, 1, 0);
		GlStateManager.rotated(entity.rot3, 0, 0, 1);

		this.bindTexture(TEXTURE);
		this.tumbleweed.render(0.0625F);

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
		final float d = start.getX() * end.getX() + start.getY() * end.getY() + start.getZ() * end.getZ() + start.getW() * end.getW();
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

		return new Quaternion(
				(scale0 * start.getX()) + (scale1 * end.getX()),
				(scale0 * start.getY()) + (scale1 * end.getY()),
				(scale0 * start.getZ()) + (scale1 * end.getZ()),
				(scale0 * start.getW()) + (scale1 * end.getW())
		);
	}

	public static class Factory implements IRenderFactory<EntityTumbleweed> {

		@Override
		public EntityRenderer<EntityTumbleweed> createRenderFor(EntityRendererManager manager) {
			return new RenderTumbleweed(manager);
		}
	}
}
