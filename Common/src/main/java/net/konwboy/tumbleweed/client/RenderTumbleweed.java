package net.konwboy.tumbleweed.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.konwboy.tumbleweed.Constants;
import net.konwboy.tumbleweed.common.EntityTumbleweed;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.EntityHitResult;
import org.joml.Quaternionf;

public class RenderTumbleweed extends EntityRenderer<EntityTumbleweed> {

	public static final ModelLayerLocation MAIN_LAYER = new ModelLayerLocation(Constants.TUMBLEWEED_ENTITY, "main");
	private static final ResourceLocation TEXTURE = new ResourceLocation("tumbleweed", "textures/entity/tumbleweed.png");
	private static final RenderType RENDER_TYPE = RenderType.entityTranslucent(TEXTURE);

	private final ModelTumbleweed model;
	private final Quaternionf tempQuat = new Quaternionf();

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

		matrixStack.mulPose(entity.prevQuat.slerp(entity.quat, partialTicks, tempQuat));

		matrixStack.mulPose(Axis.XP.rotationDegrees(entity.rotOffsetX));
		matrixStack.mulPose(Axis.YP.rotationDegrees(entity.rotOffsetY));
		matrixStack.mulPose(Axis.ZP.rotationDegrees(entity.rotOffsetZ));

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
	protected boolean shouldShowName(EntityTumbleweed entity) {
		return entity.hasCustomName() &&
				(entity.shouldShowName() || Minecraft.getInstance().hitResult instanceof EntityHitResult hit && hit.getEntity() == entity);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityTumbleweed entity) {
		return TEXTURE;
	}

}
