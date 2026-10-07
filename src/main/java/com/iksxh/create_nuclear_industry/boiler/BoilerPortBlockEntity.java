package com.iksxh.create_nuclear_industry.boiler;

import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.goggle.GoggleTooltip;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/** 四类口共享轻量Create实体；仅汽口保存独立选择，mB/HU库存始终在控制器。 */
public final class BoilerPortBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private SteamBehaviour steamControl;
    private String outputView = "unformed";
    public BoilerPortBlockEntity(BlockPos pos, BlockState state) {
        super(BoilerContent.PORT_BE.get(), pos, state);
    }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        if (getBlockState().is(BoilerContent.STEAM_PORT.get())) {
            steamControl = new SteamBehaviour(this); behaviours.add(steamControl);
        }
    }
    public BoilerSteamKind selectedSteamKind() { return steamControl == null ? BoilerSteamKind.SUPERCRITICAL : steamControl.get(); }
    private void selectionChanged(int ignored) {
        if (level == null || level.isClientSide) return;
        var owner = BoilerStructure.owner(level, worldPosition);
        if (owner != null) owner.steamSelectionChanged(worldPosition);
    }
    /** 原生行为须在两侧tick初始化；只有服务端更新遥测，水/冷热液口没有新增控件。 */
    @Override public void tick() {
        super.tick();
        if (steamControl == null || level == null || level.isClientSide) return;
        var owner = BoilerStructure.owner(level, worldPosition);
        String next = owner == null ? "unformed" : owner.steamPortStatus(worldPosition);
        if (!next.equals(outputView)) { outputView = next; sendData(); }
    }
    @Override protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (clientPacket) tag.putString("SteamOutputStatus", outputView);
    }
    @Override protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        BoilerSteamKind before = selectedSteamKind();
        super.read(tag, registries, clientPacket);
        if (clientPacket) outputView = tag.getString("SteamOutputStatus");
        else if (before != selectedSteamKind()) selectionChanged(0);
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        if (steamControl == null) return false;
        String key = "gui.create_nuclear_industry.boiler.steam_port.";
        tooltip.add(GoggleTooltip.indentFirstLine(Component.translatable("block.create_nuclear_industry.high_pressure_boiler_steam_port")));
        tooltip.add(Component.translatable(key + "selected", Component.translatable(selectedSteamKind().getTranslationKey())));
        tooltip.add(Component.translatable(key + "state." + outputView)); return true;
    }
    /** Create原生包仅接受两项有效值；行为本身负责同步及当前版本保存，不复制炉内库存。 */
    private static final class SteamBehaviour extends ScrollOptionBehaviour<BoilerSteamKind> {
        SteamBehaviour(BoilerPortBlockEntity owner) {
            // 水平原生泵轮廓遮挡y=2/16～14/16；控件中心放在上缘空隙，接管后仍能从朝外面命中。
            super(BoilerSteamKind.class, Component.translatable("gui.create_nuclear_industry.boiler.steam_port.select"), owner, BoilerControls.slot(.94));
            value = BoilerSteamKind.SUPERCRITICAL.ordinal(); withCallback(owner::selectionChanged);
        }
        @Override public void setValueSettings(Player player, ValueSettings settings, boolean ctrlDown) {
            if (settings.row() != 0 || settings.value() < 0 || settings.value() > 1) return;
            super.setValueSettings(player, settings, ctrlDown);
        }
        @Override public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
            if (!tag.contains("ScrollValue")) { value = BoilerSteamKind.SUPERCRITICAL.ordinal(); return; }
            super.read(tag, registries, clientPacket); value = Math.clamp(value, 0, 1);
        }
    }
}
