package net.konwboy.tumbleweed.client;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.model.ModelRenderer;

import java.util.List;

public class ModelTumbleweed {

	private List<ModelRenderer> boxList = Lists.newArrayList();

	public ModelTumbleweed(float expand) {
		{
			ModelRenderer box = new ModelRenderer(16,16,0,0);
			box.addBox(0, -8, -8, 0, 16, 16, expand);
			box.addBox(-8, 0, -8, 16, 0, 16, expand);
			box.addBox(-8, -8, 0, 16, 16, 0, expand);
			this.boxList.add(box);
		}

		{
			ModelRenderer box = new ModelRenderer(16,16,0,0);
			box.addBox(0, -8, -8, 0, 16, 16, expand);
			box.addBox(-8, -8, 0, 16, 16, 0, expand);
			box.rotateAngleY = (float) Math.toRadians(45);
			this.boxList.add(box);
		}

		{
			ModelRenderer box = new ModelRenderer(16,16,0,0);
			box.addBox(0, -8, -8, 0, 16, 16, expand);
			box.addBox(-8, 0, -8, 16, 0, 16, expand);
			box.rotateAngleZ = (float) Math.toRadians(45);
			this.boxList.add(box);
		}

		{
			ModelRenderer box = new ModelRenderer(16,16,0,0);
			box.addBox(-8, 0, -8, 16, 0, 16, expand);
			box.addBox(-8, -8, 0, 16, 16, 0, expand);
			box.rotateAngleX = (float) Math.toRadians(45);
			this.boxList.add(box);
		}
	}

	public void render(MatrixStack matrixStackIn, IVertexBuilder bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
		for (ModelRenderer box : boxList)
			box.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
	}

}