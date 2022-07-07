package net.konwboy.tumbleweed.client;

import net.konwboy.tumbleweed.common.EntityTumbleweed;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class ModelTumbleweed extends HierarchicalModel<EntityTumbleweed> {

	private final ModelPart root;

	public ModelTumbleweed(ModelPart root) {
		this.root = root;
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition parts = mesh.getRoot();

		var expand = new CubeDeformation(0.1f); // Prevents z-fighting
		float pi = (float)Math.PI;

		parts.addOrReplaceChild("planes1",
				CubeListBuilder.create()
						.addBox(0, -8, -8, 0, 16, 16, expand)
						.addBox(-8, 0, -8, 16, 0, 16, expand)
						.addBox(-8, -8, 0, 16, 16, 0, expand),
				PartPose.ZERO
		);

		parts.addOrReplaceChild("planes2",
				CubeListBuilder.create()
						.addBox(0, -8, -8, 0, 16, 16, expand)
						.addBox(-8, -8, 0, 16, 16, 0, expand),
				PartPose.rotation(0, pi / 4f, 0)
		);

		parts.addOrReplaceChild("planes3",
				CubeListBuilder.create()
						.addBox(0, -8, -8, 0, 16, 16, expand)
						.addBox(-8, 0, -8, 16, 0, 16, expand),
				PartPose.rotation(0, 0, pi / 4f)
		);

		parts.addOrReplaceChild("planes4",
				CubeListBuilder.create()
						.addBox(-8, 0, -8, 16, 0, 16, expand)
						.addBox(-8, -8, 0, 16, 16, 0, expand),
				PartPose.rotation(pi / 4f, 0, 0)
		);

		return LayerDefinition.create(mesh, 16, 16);
	}

	@Override
	public void setupAnim(EntityTumbleweed p_102618_, float p_102619_, float p_102620_, float p_102621_, float p_102622_, float p_102623_) {
	}

	@Override
	public ModelPart root() {
		return root;
	}

}