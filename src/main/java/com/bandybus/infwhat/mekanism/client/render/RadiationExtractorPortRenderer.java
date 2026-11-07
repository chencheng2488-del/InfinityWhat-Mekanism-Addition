package com.bandybus.infwhat.mekanism.client.render;

import com.bandybus.infwhat.mekanism.InfinityWhatMekanism;
import com.bandybus.infwhat.mekanism.block.entity.RadiationExtractorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mekanism.api.RelativeSide;
import mekanism.api.text.EnumColor;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Paints the radiation extractor's back port with the colour of its side configuration.
 *
 * <p>The block model carries a static Mekanism LED ring; this renderer draws an identically
 * shaped white ring just in front of it and tints it from {@link DataType#getColor()}. The port
 * sits on the model's south face, which is {@link RelativeSide#BACK} once the block is rotated
 * by its facing, so that is the side whose configuration is read.
 */
public class RadiationExtractorPortRenderer implements BlockEntityRenderer<RadiationExtractorBlockEntity> {

    private static final ResourceLocation LED_TEXTURE = new ResourceLocation(
            InfinityWhatMekanism.MOD_ID, "textures/block/port_led_white.png");

    /** Colour source order: the machine's own gas config wins over items, items over energy. */
    private static final TransmissionType[] COLOR_PRIORITY = {
            TransmissionType.GAS, TransmissionType.ITEM, TransmissionType.ENERGY};

    //Port face on the south side of the model: x 4..12, y 4..12 of 16.
    private static final float LOCAL_X0 = 4.0F / 16.0F;
    private static final float LOCAL_X1 = 12.0F / 16.0F;
    private static final float LOCAL_Y0 = 4.0F / 16.0F;
    private static final float LOCAL_Y1 = 12.0F / 16.0F;
    /** A hair past the static LED plane so the overlay always wins the depth test. */
    private static final float LOCAL_Z = 16.06F / 16.0F;

    //The uncut ring is the top right 8x8 quadrant of the 16x16 LED sheet.
    private static final float TEX_U0 = 8.0F / 16.0F;
    private static final float TEX_U1 = 1.0F;
    private static final float TEX_V0 = 0.0F;
    private static final float TEX_V1 = 8.0F / 16.0F;

    /** @return the colour the port glows with for the machine's current side configuration. */
    private static EnumColor portColor(RadiationExtractorBlockEntity tile) {
        TileComponentConfig config = tile.getConfig();
        if (config != null) {
            for (TransmissionType type : COLOR_PRIORITY) {
                ConfigInfo info = config.getConfig(type);
                if (info != null) {
                    DataType dataType = info.getDataType(RelativeSide.BACK);
                    if (dataType != DataType.NONE) {
                        return dataType.getColor();
                    }
                }
            }
        }
        return DataType.NONE.getColor();
    }

    @Override
    public void render(RadiationExtractorBlockEntity tile, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int packedLight, int packedOverlay) {
        int[] rgb = portColor(tile).getRgbCode();
        Direction facing = tile.getDirection();
        //The model's local +X and +Z axes once the block state has been rotated to this facing.
        Direction right = facing.getClockWise();
        Direction out = facing.getOpposite();

        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(LED_TEXTURE));
        Matrix4f matrix = pose.last().pose();
        float nx = out.getStepX();
        float nz = out.getStepZ();

        vertex(consumer, matrix, right, out, LOCAL_X1, LOCAL_Y1, TEX_U0, TEX_V0, rgb, nx, nz);
        vertex(consumer, matrix, right, out, LOCAL_X0, LOCAL_Y1, TEX_U1, TEX_V0, rgb, nx, nz);
        vertex(consumer, matrix, right, out, LOCAL_X0, LOCAL_Y0, TEX_U1, TEX_V1, rgb, nx, nz);
        vertex(consumer, matrix, right, out, LOCAL_X1, LOCAL_Y0, TEX_U0, TEX_V1, rgb, nx, nz);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Direction right, Direction out,
            float localX, float localY, float u, float v, int[] rgb, float nx, float nz) {
        //Block model rotations pivot on the block centre, not on the corner at the block position.
        float dx = localX - 0.5F;
        float dz = LOCAL_Z - 0.5F;
        consumer.vertex(matrix,
                        0.5F + right.getStepX() * dx + out.getStepX() * dz,
                        localY,
                        0.5F + right.getStepZ() * dx + out.getStepZ() * dz)
                .color(rgb[0], rgb[1], rgb[2], 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(nx, 0.0F, nz)
                .endVertex();
    }
}
