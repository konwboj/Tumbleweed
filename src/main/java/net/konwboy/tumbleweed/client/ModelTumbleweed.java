package net.konwboy.tumbleweed.client;

import net.minecraft.client.renderer.entity.model.RendererModel;
import net.minecraft.client.renderer.model.Model;

public class ModelTumbleweed extends Model {

	public ModelTumbleweed(float scale) {
		this.textureHeight = 16;
		this.textureWidth = 16;

		{
			RendererModel box = new RendererModel(this);
			box.addBox(0, -8, -8, 0, 16, 16, scale);
			box.addBox(-8, 0, -8, 16, 0, 16, scale);
			box.addBox(-8, -8, 0, 16, 16, 0, scale);
			this.boxList.add(box);
		}

		{
			RendererModel box = new RendererModel(this);
			box.addBox(0, -8, -8, 0, 16, 16, scale);
			box.addBox(-8, -8, 0, 16, 16, 0, scale);
			box.rotateAngleY = (float) Math.toRadians(45);
			this.boxList.add(box);
		}

		{
			RendererModel box = new RendererModel(this);
			box.addBox(0, -8, -8, 0, 16, 16, scale);
			box.addBox(-8, 0, -8, 16, 0, 16, scale);
			box.rotateAngleZ = (float) Math.toRadians(45);
			this.boxList.add(box);
		}

		{
			RendererModel box = new RendererModel(this);
			box.addBox(-8, 0, -8, 16, 0, 16, scale);
			box.addBox(-8, -8, 0, 16, 16, 0, scale);
			box.rotateAngleX = (float) Math.toRadians(45);
			this.boxList.add(box);
		}
	}

	public void render(float scale) {
		for (RendererModel box : boxList)
			box.render(scale);
	}

	public int getV() {
		return 55;
	}
}