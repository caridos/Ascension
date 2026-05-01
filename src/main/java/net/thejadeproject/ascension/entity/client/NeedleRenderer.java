package net.thejadeproject.ascension.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;

public class NeedleRenderer extends ThrownItemRenderer {

    public NeedleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(net.minecraft.world.entity.Entity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(entityYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(((ThrowableItemProjectile) entity).getXRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f));
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
