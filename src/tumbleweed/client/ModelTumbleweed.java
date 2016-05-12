package tumbleweed.client;

import com.google.common.collect.Lists;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

import java.util.List;

public class ModelTumbleweed extends ModelBase
{
	public List<ModelRenderer> boxes;

	public ModelTumbleweed()
	{
		this.textureHeight = 16;
		this.textureWidth = 16;

		this.boxes = Lists.newArrayList();

		{
			ModelRenderer box = new ModelRenderer(this);
			box.addBox(0, -8, -8, 0, 16, 16);
			box.addBox(-8, 0, -8, 16, 0, 16);
			box.addBox(-8, -8, 0, 16, 16, 0);
			this.boxes.add(box);
		}

		{
			ModelRenderer box = new ModelRenderer(this);
			box.addBox(0, -8, -8, 0, 16, 16);
			box.addBox(-8, -8, 0, 16, 16, 0);
			box.rotateAngleY = (float) Math.toRadians(45);
			this.boxes.add(box);
		}

		{
			ModelRenderer box = new ModelRenderer(this);
			box.addBox(0, -8, -8, 0, 16, 16);
			box.addBox(-8, 0, -8, 16, 0, 16);
			box.rotateAngleZ = (float) Math.toRadians(45);
			this.boxes.add(box);
		}

		{
			ModelRenderer box = new ModelRenderer(this);
			box.addBox(-8, 0, -8, 16, 0, 16);
			box.addBox(-8, -8, 0, 16, 16, 0);
			box.rotateAngleX = (float) Math.toRadians(45);
			this.boxes.add(box);
		}
	}

	@Override
	public void render(Entity entity, float data1, float data2, float data3, float data4, float data5, float scale)
	{
		for (ModelRenderer box : boxes)
			box.render(scale);
	}

	public int getV()
	{
		return 45;
	}
}