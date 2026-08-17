package com.iksxh.create_nuclear_industry.p0probe.blockentity;

import com.iksxh.create_nuclear_industry.p0probe.content.P0ProbeBlockEntities;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class P0ProbeSliderBlockEntity extends SmartBlockEntity {
    public ScrollValueBehaviour slider;
    private int clientCallbackValue;

    public P0ProbeSliderBlockEntity(BlockPos pos, BlockState state) {
        super(P0ProbeBlockEntities.P0_PROBE_SLIDER.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        slider = new ScrollValueBehaviour(
                Component.literal("P0 slider"),
                this,
                new CenteredSideValueBoxTransform()
        ).between(0, 100)
                .withCallback(value -> setChanged())
                .withClientCallback(value -> clientCallbackValue = value);
        behaviours.add(slider);
    }

    public int clientCallbackValue() {
        return clientCallbackValue;
    }

    public int value() {
        return slider.getValue();
    }
}
