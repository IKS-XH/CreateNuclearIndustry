package com.iksxh.create_nuclear_industry.boiler;

import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** 控制器前面两个 Create 原生控件；各用独立类型及 NBT 槽，真实值由服务端账本持有。 */
final class BoilerControls {
    private BoilerControls() {}
    enum Mode implements INamedIconOptions {
        STEAM, SUPERCRITICAL;
        @Override public AllIcons getIcon() { return this == STEAM ? AllIcons.I_PASSIVE : AllIcons.I_ACTIVE; }
        @Override public String getTranslationKey() { return "gui.create_nuclear_industry.boiler.mode." + (this == STEAM ? "normal" : "supercritical"); }
    }
    private static ValueBoxTransform slot(double y) {
        return new ValueBoxTransform.Sided() {
            @Override protected Vec3 getSouthLocation() { return new Vec3(.5, y, 1.01); }
            @Override protected boolean isSideActive(BlockState state, Direction direction) {
                return state.hasProperty(BoilerPartBlock.FACING) && direction == state.getValue(BoilerPartBlock.FACING);
            }
            @Override public float getScale() { return .32f; }
        };
    }
    static final class ModeBehaviour extends ScrollOptionBehaviour<Mode> {
        private static final BehaviourType<ModeBehaviour> TYPE = new BehaviourType<>();
        ModeBehaviour(BoilerControllerBlockEntity owner) {
            super(Mode.class, Component.translatable("gui.create_nuclear_industry.boiler.mode"), owner, slot(.72));
            value = 1; withCallback(v -> owner.selectMode(v == 1));
        }
        @Override public BehaviourType<?> getType() { return TYPE; }
        @Override public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
            CompoundTag part = new CompoundTag(); super.write(part, registries, clientPacket); tag.put("ModeControl", part);
        }
        @Override public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
            super.read(tag.getCompound("ModeControl"), registries, clientPacket); value = Math.clamp(value, 0, 1);
        }
    }
    static final class PressureBehaviour extends ScrollValueBehaviour {
        private static final BehaviourType<PressureBehaviour> TYPE = new BehaviourType<>();
        PressureBehaviour(BoilerControllerBlockEntity owner) {
            super(Component.translatable("gui.create_nuclear_industry.boiler.minimum"), owner, slot(.28));
            between(0, 200); value = 60; withFormatter(v -> String.format(java.util.Locale.ROOT, "%.2f", v / 100D));
            withCallback(owner::selectMinimum);
        }
        @Override public BehaviourType<?> getType() { return TYPE; }
        @Override public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
            CompoundTag part = new CompoundTag(); super.write(part, registries, clientPacket); tag.put("PressureControl", part);
        }
        @Override public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
            super.read(tag.getCompound("PressureControl"), registries, clientPacket); value = Math.clamp(value, 0, 200);
        }
    }
}
