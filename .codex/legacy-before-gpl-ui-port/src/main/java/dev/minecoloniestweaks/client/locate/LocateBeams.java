package dev.minecoloniestweaks.client.locate;

import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.api.colony.IColonyManager;
import dev.minecoloniestweaks.client.ClientPreferences;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class LocateBeams {
    private static final List<Target> TARGETS = new ArrayList<>();
    public static final int CITIZEN_COLOR = 0xFF55FFFF;
    public static final int HUT_COLOR = 0xFF55FF55;
    public static final int GRAVE_COLOR = 0xFFFF55FF;

    private LocateBeams() {}

    private record Target(String dimension, String name, BlockPos position, int colony, int citizen, int color, long expires) {
        BlockPos currentPosition() {
            var client = Minecraft.getInstance();
            if (citizen < 0 || client.level == null) return position;
            var view = IColonyManager.getInstance().getColonyView(colony, client.level.dimension());
            var data = view == null ? null : view.getCitizen(citizen);
            if (data == null) return position;
            var entity = client.level.getEntity(data.getEntityId());
            return entity == null ? data.getPosition() : entity.blockPosition();
        }
    }

    public static void citizen(ICitizenDataView citizen) {
        add(citizen.getName(), citizen.getPosition(), citizen.getColonyId(), citizen.getId(), CITIZEN_COLOR);
    }

    public static void block(String name, BlockPos position, boolean grave) {
        add(name, position, -1, -1, grave ? GRAVE_COLOR : HUT_COLOR);
    }

    private static void add(String name, BlockPos position, int colony, int citizen, int color) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        String dimension = level.dimension().location().toString();
        TARGETS.removeIf(target -> target.dimension().equals(dimension) && target.colony() == colony
                && target.citizen() == citizen && (citizen >= 0 || target.position().equals(position)));
        if (TARGETS.size() >= 8) TARGETS.removeFirst();
        TARGETS.add(new Target(dimension, name, position.immutable(), colony, citizen, color,
                level.getGameTime() + ClientPreferences.INSTANCE.beamSeconds * 20L));
    }

    public static void clear() { TARGETS.clear(); }

    public static void renderWorld(RenderLevelStageEvent event) {
        var client = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || client.level == null
                || !ClientPreferences.INSTANCE.beamsVisible) return;
        TARGETS.removeIf(target -> target.expires() < client.level.getGameTime());
        var camera = event.getCamera().getPosition();
        var pose = event.getPoseStack();
        var buffers = client.renderBuffers().bufferSource();
        for (var target : TARGETS) {
            if (!target.dimension().equals(client.level.dimension().location().toString())) continue;
            var pos = target.currentPosition();
            pose.pushPose();
            pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            BeaconRenderer.renderBeaconBeam(pose, buffers, BeaconRenderer.BEAM_LOCATION,
                    event.getPartialTick().getGameTimeDeltaPartialTick(false), 1, client.level.getGameTime(), 0,
                    Math.max(64, client.level.getMaxBuildHeight() - pos.getY()), target.color(), 0.15F, 0.2F);
            pose.popPose();
        }
    }

    public static void renderLabels(GuiGraphics graphics) {
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.options.hideGui || !ClientPreferences.INSTANCE.beamsVisible) return;
        int y = 8;
        for (var target : TARGETS) {
            if (target.expires() < client.level.getGameTime()
                    || !target.dimension().equals(client.level.dimension().location().toString())) continue;
            Vec3 delta = Vec3.atCenterOf(target.currentPosition()).subtract(client.player.position());
            double bearing = Math.toDegrees(Math.atan2(-delta.x, delta.z));
            double relative = Mth.wrapDegrees(bearing - client.player.getYRot());
            String direction = Math.abs(relative) < 22.5 ? "↑" : Math.abs(relative) > 157.5 ? "↓" : relative > 0 ? "→" : "←";
            String text = direction + " " + target.name() + " · " + Math.round(delta.length()) + " m";
            graphics.drawCenteredString(client.font, text, graphics.guiWidth() / 2, y, target.color());
            y += 12;
        }
    }
}
