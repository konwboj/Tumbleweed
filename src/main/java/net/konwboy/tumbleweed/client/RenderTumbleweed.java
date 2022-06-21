package net.konwboy.tumbleweed.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.konwboy.tumbleweed.Tumbleweed;
import net.konwboy.tumbleweed.common.EntityTumbleweed;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.ForgeRenderTypes;
import net.minecraftforge.common.model.TransformationHelper;

public class RenderTumbleweed extends EntityRenderer<EntityTumbleweed> {

	public static final ModelLayerLocation MAIN_LAYER = new ModelLayerLocation(Tumbleweed.TUMBLEWEED_ENTITY, "main");
	private static final ResourceLocation TEXTURE = new ResourceLocation(Tumbleweed.MOD_ID, "textures/entity/tumbleweed.png");
	private static final RenderType RENDER_TYPE = ForgeRenderTypes.getUnlitTranslucent(TEXTURE, true);

	private final ModelTumbleweed model;

	public RenderTumbleweed(EntityRendererProvider.Context context) {
		super(context);
		this.model = new ModelTumbleweed(context.bakeLayer(MAIN_LAYER));
		this.shadowRadius = 0.4f;
		this.shadowStrength = 0.8f;
	}

	@Override
	public void render(EntityTumbleweed entity, float entityYaw, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn, int packedLightIn) {
		float alpha = (1f - entity.fadeProgress / (float) EntityTumbleweed.FADE_TIME) * 0.7f;
		this.shadowStrength = alpha;
		this.shadowRadius = entity.getBbWidth() * 0.5f;

		matrixStack.pushPose();

		float size = 1.0f + entity.getSize() / 8f;
		float stretch = Mth.lerp(partialTicks, entity.prevStretch, entity.stretch);

		matrixStack.translate(0,entity.getBbHeight() * 0.3f,0);
		matrixStack.scale(size, size, size);
		matrixStack.scale(1, stretch, 1);

		matrixStack.mulPose(TransformationHelper.slerp(entity.prevQuat, entity.quat, partialTicks));

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

}
