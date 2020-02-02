package net.konwboy.tumbleweed.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

public class ModelTumbleweed extends ModelBase {

	public ModelTumbleweed(float scale) {
		this.textureHeight = 16;
		this.textureWidth = 16;

		{
			ModelRenderer box = new ModelRenderer(this);
			box.addBox(0, -8, -8, 0, 16, 16, scale);
			box.addBox(-8, 0, -8, 16, 0, 16, scale);
			box.addBox(-8, -8, 0, 16, 16, 0, scale);
			this.boxList.add(box);
		}

		{
			ModelRenderer box = new ModelRenderer(this);
			box.addBox(0, -8, -8, 0, 16, 16, scale);
			box.addBox(-8, -8, 0, 16, 16, 0, scale);
			box.rotateAngleY = (float) Math.toRadians(45);
			this.boxList.add(box);
		}

		{
			ModelRenderer box = new ModelRenderer(this);
			box.addBox(0, -8, -8, 0, 16, 16, scale);
			box.addBox(-8, 0, -8, 16, 0, 16, scale);
			box.rotateAngleZ = (float) Math.toRadians(45);
			this.boxList.add(box);
		}

		{
			ModelRenderer box = new ModelRenderer(this);
			box.addBox(-8, 0, -8, 16, 0, 16, scale);
			box.addBox(-8, -8, 0, 16, 16, 0, scale);
			box.rotateAngleX = (float) Math.toRadians(45);
			this.boxList.add(box);
		}
	}

	@Override
	public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
		for (ModelRenderer box : boxList)
			box.render(scale);
	}

	public int getV() {
		return 55;
	}
}