package com.lulan.shincolle.client;

import com.lulan.shincolle.handler.ConfigHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

import java.util.List;

/**
 * クライアント専用: 近くのMobのナビゲーション先・攻撃対象をラインで可視化するデバッグ描画。
 * ConfigHandler.DEBUG_SHOW_AI_GOALS が true の時のみ描画する。
 */
public class AiDebugRenderer {

    private static final int COLOR_NAVIGATION = 0x00FF00; // 緑: 移動先
    private static final int COLOR_ATTACK_TARGET = 0xFF0000; // 赤: 攻撃対象
    private static final double TRACK_RADIUS = 32.0;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!ConfigHandler.DEBUG_SHOW_AI_GOALS) return;

        var player = Minecraft.getInstance().player;
        if (player != null) {
            Vec3 base = player.position();
            drawLine(event.getPoseStack(), base, base.add(0, 5, 0), 0xFF0000); // 動作確認用
        }

        for (Mob mob : getTrackedDebugMobs()) {
            BlockPos targetPos = mob.getNavigation().getTargetPos();
            if (targetPos != null) {
                drawLine(event.getPoseStack(), mob.position(), targetPos.getCenter(), COLOR_NAVIGATION);
            }

            LivingEntity target = mob.getTarget();
            if (target != null) {
                drawLine(event.getPoseStack(), mob.position(), target.position(), COLOR_ATTACK_TARGET);
            }
        }
    }

    private static List<Mob> getTrackedDebugMobs() {
        var player = Minecraft.getInstance().player;
        var level = Minecraft.getInstance().level;
        if (player == null || level == null) return List.of();

        return level.getEntitiesOfClass(Mob.class,
                player.getBoundingBox().inflate(TRACK_RADIUS));
    }

    private static void drawLine(PoseStack poseStack, Vec3 start, Vec3 end, int color) {
        Vec3 camPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();

        consumer.vertex(matrix,
                        (float) (start.x - camPos.x), (float) (start.y - camPos.y), (float) (start.z - camPos.z))
                .color(r, g, b, 1.0f).normal(0, 1, 0).endVertex();
        consumer.vertex(matrix,
                        (float) (end.x - camPos.x), (float) (end.y - camPos.y), (float) (end.z - camPos.z))
                .color(r, g, b, 1.0f).normal(0, 1, 0).endVertex();

        bufferSource.endBatch(RenderType.lines());
    }
}