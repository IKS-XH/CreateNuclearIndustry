package com.iksxh.create_nuclear_industry.p0probe.events;

import com.iksxh.create_nuclear_industry.p0probe.blockentity.P0ProbeBlockEntity;
import com.iksxh.create_nuclear_industry.p0probe.content.P0ProbeContent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** P0 探针专用事件监听器，验证锁定破坏和修复物品交互边界。 */
public final class P0ProbeEvents {
    private P0ProbeEvents() {
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!event.getState().is(P0ProbeContent.P0_PROBE_BLOCK.get()))
            return;
        BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
        if (blockEntity instanceof P0ProbeBlockEntity probe && probe.breakLocked())
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide())
            return;
        if (!event.getItemStack().is(P0ProbeContent.P0_PROBE_REPAIR_ITEM.get()))
            return;
        BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
        if (!(blockEntity instanceof P0ProbeBlockEntity probe))
            return;
        if (!probe.repairByP0Item())
            return;
        ItemStack held = event.getItemStack();
        held.shrink(1);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
