package com.iksxh.create_nuclear_industry.boiler;

import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** 控制器唯一的 Create 原生压力控件；网络ID只有一个接收者，真实百分数由服务端账本持有。 */
final class BoilerControls {
    private BoilerControls() {}
    private static ValueBoxTransform slot(double y) {
        return new ValueBoxTransform.Sided() {
            @Override protected Vec3 getSouthLocation() { return new Vec3(.5, y, 1.01); }
            @Override protected boolean isSideActive(BlockState state, Direction direction) {
                return state.hasProperty(BoilerPartBlock.FACING) && direction == state.getValue(BoilerPartBlock.FACING);
            }
            @Override public float getScale() { return .32f; }
        };
    }
    static final class PressureBehaviour extends ScrollValueBehaviour {
        private static final BehaviourType<PressureBehaviour> TYPE = new BehaviourType<>();
        PressureBehaviour(BoilerControllerBlockEntity owner) {
            super(Component.translatable("gui.create_nuclear_industry.boiler.minimum"), owner, slot(.5));
            between(0, 100); value = 60; withFormatter(v -> v + "%");
            withCallback(owner::selectMinimum);
        }
        /** 数值板十格是刻度间距；实际值仍逐整数提交，板上与滚动框统一显示百分数。 */
        @Override public ValueSettingsBoard createBoard(Player player, BlockHitResult hit) {
            return new ValueSettingsBoard(label, 100, 10, List.of(Component.translatable("gui.create_nuclear_industry.boiler.minimum")),
                    new ValueSettingsFormatter(setting -> Component.literal(setting.value() + "%")));
        }
        @Override public BehaviourType<?> getType() { return TYPE; }
        @Override public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
            CompoundTag part = new CompoundTag(); super.write(part, registries, clientPacket); tag.put("PressureControl", part);
        }
        @Override public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
            super.read(tag.getCompound("PressureControl"), registries, clientPacket); value = Math.clamp(value, 0, 100);
        }
    }
}
