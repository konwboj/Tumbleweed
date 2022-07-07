package net.konwboy.tumbleweed.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.konwboy.tumbleweed.Constants;
import net.konwboy.tumbleweed.common.EntityTumbleweed;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class RenderTumbleweed extends EntityRenderer<EntityTumbleweed> {

	public static final ModelLayerLocation MAIN_LAYER = new ModelLayerLocation(Constants.TUMBLEWEED_ENTITY, "main");
	private static final ResourceLocation TEXTURE = new ResourceLocation("tumbleweed", "textures/entity/tumbleweed.png");
	private static final RenderType RENDER_TYPE = RenderType.entityTranslucent(TEXTURE);

	private final ModelTumbleweed model;

	public RenderTumbleweed(EntityRendererProvider.Context context) {
		super(context);
		this.model = new ModelTumbleweed(context.bakeLayer(MAIN_LAYER));
		this.shadowRadius = 0.4f;
		this.shadowStrength = 0.8f;
	}

	@Override
	public void render(EntityTumbleweed entity, float entityYaw, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn, int packedLightIn) {
		float alpha = 1f - entity.fadeProgress / (float) EntityTumbleweed.FADE_TIME;
		this.shadowStrength = alpha * 0.7f;
		this.shadowRadius = entity.getBbWidth() * 0.5f;

		matrixStack.pushPose();

		float size = 1.0f + entity.getSize() / 8f;
		float stretch = Mth.lerp(partialTicks, entity.prevStretch, entity.stretch);

		matrixStack.translate(0,entity.getBbHeight() * 0.3f,0);
		matrixStack.scale(size, size, size);
		matrixStack.scale(1, stretch, 1);

		matrixStack.mulPose(slerp(entity.prevQuat, entity.quat, partialTicks));

		matrixStack.mulPose(Vector3f.XP.rotationDegrees(entity.rot1));
		matrixStack.mulPose(Vector3f.YP.rotationDegrees(entity.rot2));
		matrixStack.mulPose(Vector3f.ZP.rotationDegrees(entity.rot3));

		VertexConsumer buf = bufferIn.getBuffer(RENDER_TYPE);

		this.model.renderToBuffer(
			matrixStack,
			buf,
			packedLightIn,
			OverlayTexture.NO_OVERLAY,
			1,
			1,
			1,
			alpha
		);

		matrixStack.popPose();

		super.render(entity, entityYaw, partialTicks, matrixStack, bufferIn, packedLightIn);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityTumbleweed entity) {
		return TEXTURE;
	}

	private static final double THRESHOLD = 0.9995;
	public static Quaternion slerp(Quaternion v0, Quaternion v1, float t)
	{
		// From https://en.wikipedia.org/w/index.php?title=Slerp&oldid=928959428
		// License: CC BY-SA 3.0 https://creativecommons.org/licenses/by-sa/3.0/

		// Compute the cosine of the angle between the two vectors.
		// If the dot product is negative, slerp won't take
		// the shorter path. Note that v1 and -v1 are equivalent when
		// the negation is applied to all four components. Fix by
		// reversing one quaternion.
		float dot = v0.i() * v1.i() + v0.j() * v1.j() + v0.k() * v1.k() + v0.r() * v1.r();
		if (dot < 0.0f) {
			v1 = new Quaternion(-v1.i(), -v1.j(), -v1.k(), -v1.r());
			dot = -dot;
		}

		// If the inputs are too close for comfort, linearly interpolate
		// and normalize the result.
		if (dot > THRESHOLD) {
			float x = Mth.lerp(t, v0.i(), v1.i());
			float y = Mth.lerp(t, v0.j(), v1.j());
			float z = Mth.lerp(t, v0.k(), v1.k());
			float w = Mth.lerp(t, v0.r(), v1.r());
			return new Quaternion(x,y,z,w);
		}

		// Since dot is in range [0, DOT_THRESHOLD], acos is safe
		float angle01 = (float)Math.acos(dot);
		float angle0t = angle01*t;
		float sin0t = Mth.sin(angle0t);
		float sin01 = Mth.sin(angle01);
		float sin1t = Mth.sin(angle01 - angle0t);

		float s1 = sin0t / sin01;
		float s0 = sin1t / sin01;

		return new Quaternion(
				s0 * v0.i() + s1 * v1.i(),
				s0 * v0.j() + s1 * v1.j(),
				s0 * v0.k() + s1 * v1.k(),
				s0 * v0.r() + s1 * v1.r()
		);
	}

}
