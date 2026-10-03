package com.iksxh.create_nuclear_industry.production;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

    /** Create机械臂只向有效外表面投四料；成品继续经侧面漏斗接外置置物台。 */
public final class ShieldedAssemblyArmInteractionPoint extends ArmInteractionPoint {
    public static final ArmInteractionPointType TYPE = new ArmInteractionPointType() {
        @Override public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
            return level != null && ShieldedAssemblyStructure.master(level, pos, state) != null;
        }
        @Override public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
            return new ShieldedAssemblyArmInteractionPoint(this, level, pos, state);
        }
        @Override public int getPriority() { return 1000; }
    };

    private ShieldedAssemblyArmInteractionPoint(ArmInteractionPointType type, Level level, BlockPos pos,
                                                 BlockState state) {
        super(type, level, pos, state);
    }
    public static void register(IEventBus bus) { bus.addListener(ShieldedAssemblyArmInteractionPoint::onRegister); }
    private static void onRegister(RegisterEvent event) {
        event.register(CreateRegistries.ARM_INTERACTION_POINT_TYPE,
                ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, "shielded_assembly_station"),
                () -> TYPE);
    }
    @Override public void cycleMode() {
        // 选点只能保持投料；即使旧保存数据曾选择取料，下方extract仍强制拒绝。
    }
    @Override protected void deserialize(CompoundTag tag, BlockPos anchor) {
        super.deserialize(tag, anchor);
        mode = Mode.DEPOSIT;
    }
    @Override public ItemStack insert(ArmBlockEntity armBlockEntity, ItemStack stack, boolean simulate) {
        if (!isValid()) return stack.copy();
        ShieldedAssemblyBlockEntity machine = ShieldedAssemblyStructure.master(level, pos, level.getBlockState(pos));
        if (machine == null) return stack.copy();
        int part = ShieldedAssemblyLayout.partAt(machine.getBlockPos(),
                machine.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING), pos);
        net.minecraft.core.Direction side = part >= 4 ? net.minecraft.core.Direction.UP : null;
        if (side == null) for (net.minecraft.core.Direction candidate : net.minecraft.core.Direction.Plane.HORIZONTAL)
            if (ShieldedAssemblyLayout.exterior(part, machine.getBlockState().getValue(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING), candidate)) {
                side = candidate;
                break;
            }
        var port = machine.itemPort(pos, side);
        if (port == null) return stack.copy();
        return net.neoforged.neoforge.items.ItemHandlerHelper.insertItem(port, stack, simulate);
    }
    @Override public ItemStack extract(ArmBlockEntity armBlockEntity, int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }
    @Override public int getSlotCount(ArmBlockEntity armBlockEntity) { return 0; }
}
