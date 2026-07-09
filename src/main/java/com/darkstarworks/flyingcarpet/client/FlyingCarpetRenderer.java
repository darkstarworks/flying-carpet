package com.darkstarworks.flyingcarpet.client;

import com.darkstarworks.flyingcarpet.entity.FlyingCarpetEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.cushion.CushionModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.CushionRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.DyeColor;

import java.util.EnumMap;

/**
 * Renders the Flying Carpet by reusing the vanilla Cushion model, render state,
 * and per-color cushion textures — so in-world it looks like a cushion of its
 * color. The enchanted glint is intentionally only on the item form (as designed).
 */
public class FlyingCarpetRenderer extends EntityRenderer<FlyingCarpetEntity, CushionRenderState> {

    private static final EnumMap<DyeColor, Identifier> TEXTURES_BY_COLOR = Util.make(new EnumMap<>(DyeColor.class), textures -> {
        for (DyeColor color : DyeColor.values()) {
            textures.put(color, Identifier.withDefaultNamespace("textures/entity/cushion/" + color.getName() + "_cushion.png"));
        }
    });

    private final CushionModel model;

    public FlyingCarpetRenderer(final EntityRendererProvider.Context context) {
        super(context);
        this.model = new CushionModel(context.bakeLayer(ModelLayers.CUSHION));
    }

    @Override
    public CushionRenderState createRenderState() {
        return new CushionRenderState();
    }

    @Override
    public void extractRenderState(final FlyingCarpetEntity carpet, final CushionRenderState state, final float partialTicks) {
        super.extractRenderState(carpet, state, partialTicks);
        state.direction = Direction.fromYRot(carpet.getYRot());
        state.texture = TEXTURES_BY_COLOR.get(carpet.getColor());
    }

    @Override
    public void submit(final CushionRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.direction.toYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.translate(0.0, -0.25, 0.0);
        submitNodeCollector.submitModel(
            this.model, state, poseStack, this.model.renderType(state.texture), state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor
        );
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}
