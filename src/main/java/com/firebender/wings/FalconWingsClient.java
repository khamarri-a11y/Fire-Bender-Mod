package com.firebender.wings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public final class FalconWingsClient implements ClientModInitializer {
    private static final float BLUE_R = 0.16f, BLUE_G = 0.75f, BLUE_B = 1.0f;

    private static final int ENERGY_MAX = 1000;
    private static int lastEnergy = -1;
    private static long lastSpendTick = -1000;
    private static boolean wasAirborne = false;
    private static int transformationTicks = 0;
    private static boolean previousOverdrive = false;
    private static int getScore(PlayerEntity player, String objectiveName, int fallback) {
        if (player.getWorld() == null) return fallback;
        ScoreboardObjective obj = player.getWorld().getScoreboard().getObjective(objectiveName);
        if (obj == null) return fallback;
        ScoreboardPlayerScore score = player.getWorld().getScoreboard().getPlayerScore(player.getEntityName(), obj);
        return score == null ? fallback : score.getScore();
    }

    private static void drawHud(DrawContext ctx, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        int energy = getScore(mc.player, "fb_energy", -1);
        if (energy < 0) return; // never fabricate energy readings
        if (lastEnergy >= 0 && energy < lastEnergy) lastSpendTick = mc.player.age;
        lastEnergy = energy;
        int width = 118, height = 12;
        int x = ctx.getScaledWindowWidth() / 2 - width / 2;
        int y = ctx.getScaledWindowHeight() - 65;
        int fill = MathHelper.clamp(energy, 0, ENERGY_MAX) * (width - 4) / ENERGY_MAX;
        ctx.fill(x, y, x + width, y + height, 0xC9000A1A);
        float fraction = energy / (float)ENERGY_MAX;
        int barColor = fraction < 0.2f ? 0xFFFF764B : fraction < 0.5f ? 0xFF55AFFF : 0xFF22BFFF;
        ctx.fill(x + 2, y + 2, x + 2 + fill, y + height - 2, barColor);
        if (fraction < 0.2f && mc.player.age % 20 < 10)
            ctx.drawBorder(x-1, y-1, width+2, height+2, 0xFFFF754D);
        if (mc.player.age - lastSpendTick < 8)
            ctx.drawBorder(x-2, y-2, width+4, height+4, 0xFFB4F4FF);
        ctx.drawBorder(x, y, width, height, 0xFF6CE9FF);
        String text = "FIRE ENERGY " + energy + " / 1000";
        ctx.drawCenteredTextWithShadow(mc.textRenderer, text, ctx.getScaledWindowWidth() / 2, y - 11, 0xFF9CEEFF);
        if (mc.player.age - lastSpendTick < 15)
            ctx.drawCenteredTextWithShadow(mc.textRenderer, "ENERGY USED", ctx.getScaledWindowWidth() / 2, y + 15, 0xFF82DBFF);
    }

    @Override public void onInitializeClient() {
        HudRenderCallback.EVENT.register(FalconWingsClient::drawHud);
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world == null || mc.player == null) return;
            MatrixStack matrices = context.matrixStack();
            if (matrices == null) return;
            float tick = context.tickDelta();
            var camera = context.camera().getPos();
            for (PlayerEntity player : mc.world.getPlayers()) {
                // Client-only approximation: draw wings during actual flight or elytra gliding.
                // A dedicated sync channel would be needed to follow the Origins B-toggle exactly.
                boolean flight = player.isFallFlying() || player.getAbilities().flying;
                boolean overdriveActive = player.getCommandTags().contains("fb_overdrive");
                boolean flameCast = player.getCommandTags().contains("fb_flame_cast");
                boolean breathCast = player.getCommandTags().contains("fb_breath_cast");
                if ((flameCast || breathCast) && player.age % 2 == 0) {
                    Vec3d start = player.getEyePos();
                    Vec3d direction = player.getRotationVec(tick);
                    int count = breathCast ? 32 : 18;
                    for (int i=0; i<count; i++) {
                        double distance = 0.5 + i * (breathCast ? 0.34 : 0.23);
                        double spread = (breathCast ? 0.045 : 0.11) * distance;
                        mc.world.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                            start.x + direction.x * distance + (player.getRandom().nextDouble()-0.5)*spread,
                            start.y + direction.y * distance + (player.getRandom().nextDouble()-0.5)*spread,
                            start.z + direction.z * distance + (player.getRandom().nextDouble()-0.5)*spread,
                            direction.x * 0.025, direction.y * 0.025, direction.z * 0.025);
                    }
                }
                if (player == mc.player) {
                    boolean grounded = player.isOnGround();
                    if (wasAirborne && grounded && player.age % 2 == 0) {
                        Vec3d land = player.getPos();
                        for (int k=0;k<24;k++) {
                            double a = k*Math.PI/12.0;
                            mc.world.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                                land.x + Math.cos(a)*1.2, land.y+0.07,
                                land.z + Math.sin(a)*1.2, 0, 0.018, 0);
                        }
                    }
                    wasAirborne = !grounded;
                    if (overdriveActive && !previousOverdrive) transformationTicks = 35;
                    previousOverdrive = overdriveActive;
                    if (transformationTicks > 0) {
                        transformationTicks--;
                        if (player.age % 2 == 0) {
                            double a = (35-transformationTicks)*0.6;
                            Vec3d pos = player.getPos();
                            for (int k=0;k<5;k++) {
                                double angle=a+k*2*Math.PI/5;
                                mc.world.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                                    pos.x+Math.cos(angle)*0.85,
                                    pos.y+0.15+(35-transformationTicks)*0.05,
                                    pos.z+Math.sin(angle)*0.85,0,0.04,0);
                            }
                        }
                    }
                }
                if (!flight) continue;
                double x = MathHelper.lerp(tick, player.prevX, player.getX()) - camera.x;
                double y = MathHelper.lerp(tick, player.prevY, player.getY()) - camera.y;
                double z = MathHelper.lerp(tick, player.prevZ, player.getZ()) - camera.z;
                float yaw = MathHelper.lerpAngleDegrees(tick, player.prevBodyYaw, player.bodyYaw);
                boolean dive = player.isFallFlying() && player.getPitch() > 35;
                boolean fold = dive || player.isSprinting();
                // Fast downstroke / slower recovery, like a falcon.
                float cycle = ((player.age + tick) % 16f) / 16f;
                float flap = cycle < 0.32f
                    ? 1f - 2f * cycle / 0.32f
                    : -1f + 2f * (float)Math.pow((cycle - 0.32f) / 0.68f, 1.7);
                boolean overdrive = player.getCommandTags().contains("fb_overdrive");
                matrices.push();
                matrices.translate(x, y + 1.25, z);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
                // Vanilla player forward is -Z, wings project backward (+Z).
                matrices.translate(0, 0, 0.14);
                var tess = Tessellator.getInstance();
                var buffer = tess.getBuffer();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableCull();
                RenderSystem.setShader(GameRenderer::getPositionColorProgram);
                buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                Matrix4f m = matrices.peek().getPositionMatrix();
                for (int side : new int[]{-1, 1}) {
                    float spread = fold ? 0.65f : 2.2f;
                    if (!fold && player.getVelocity().horizontalLength() < 0.12) spread *= 1.12f;
                    if (player.isOnGround()) spread *= 0.7f;
                    float lift = fold ? -0.2f : flap * 0.50f;
                    if (overdrive) spread *= 1.15f;
                    // Layered tapered feather-shaped blue flame panels.
                    for (int feather=0; feather<9; feather++) {
                        float f=feather/9f;
                        float inner=0.18f + f*spread*0.73f;
                        float outer=0.34f + f*spread;
                        float yy=lift*(0.25f+f) - f*0.12f;
                        float back=0.12f + f*(fold ? 0.65f : 0.32f);
                        quad(buffer,m,side*inner,yy,back,side*outer,yy-0.20f,back+0.17f,
                            side*(outer+0.13f),yy-0.39f,back+0.35f,
                            side*(inner+0.07f),yy-0.22f,back+0.17f,overdrive ? 0.96f : 0.76f);
                    }
                }
                BufferRenderer.drawWithGlobalProgram(buffer.end());
                RenderSystem.enableCull();
                RenderSystem.disableBlend();
                matrices.pop();
                // Cosmetic-only particles; no damage, resource or keybind changes.
                if (mc.world != null && player.age % 2 == 0) {
                    int localEnergy = player == mc.player ? getScore(player,"fb_energy",1000) : 1000;
                    boolean lowEnergy = localEnergy < 200;
                    Vec3d pos = player.getPos();
                    if (overdrive) {
                        mc.world.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                            pos.x + (player.getRandom().nextDouble() - 0.5) * 1.0,
                            pos.y + player.getRandom().nextDouble() * 1.8,
                            pos.z + (player.getRandom().nextDouble() - 0.5) * 1.0,
                            0, 0.025, 0);
                    }
                    if (player.isFallFlying() && (!lowEnergy || player.age % 4 == 0)) {
                        mc.world.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                            pos.x, pos.y + 1.0, pos.z, 0, 0.01, 0);
                    }
                }
            }
        });
    }
    private static void quad(BufferBuilder b, Matrix4f m,
        float x1,float y1,float z1,float x2,float y2,float z2,
        float x3,float y3,float z3,float x4,float y4,float z4,float alpha) {
        b.vertex(m,x1,y1,z1).color(BLUE_R,BLUE_G,BLUE_B,alpha).next();
        b.vertex(m,x2,y2,z2).color(BLUE_R,BLUE_G,BLUE_B,alpha).next();
        b.vertex(m,x3,y3,z3).color(0.04f,0.35f,1f,0.12f).next();
        b.vertex(m,x4,y4,z4).color(BLUE_R,BLUE_G,BLUE_B,alpha).next();
    }
}
