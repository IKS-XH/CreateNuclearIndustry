package com.iksxh.create_nuclear_industry.reactor;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * 正式换料端口的 Create 机械臂交互点。
 *
 * <p>本类故意不通过 {@code ItemHandler} capability 暴露库存。Create 机械臂的
 * {@link #insert(ArmBlockEntity, ItemStack, boolean)} 与
 * {@link #extract(ArmBlockEntity, int, int, boolean)} 直接映射到换料端口事务，
 * 从而让 {@code simulate=true} 只读、{@code simulate=false} 才提交；普通漏斗和
 * 通用物流仍不能访问端口。</p>
 */
public final class FuelRefuelingArmInteractionPoint extends ArmInteractionPoint {
    /** 由 Create 机械臂交互点注册表发现的换料端口类型。 */
    public static final ArmInteractionPointType TYPE = new ArmInteractionPointType() {
        @Override
        public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
            if (level == null || pos == null || state == null
                    || !state.is(P1Blocks.REACTOR_REFUELING_PORT.get())) {
                return false;
            }
            if (!(level.getBlockEntity(pos) instanceof ReactorPortBlockEntity port)) {
                return false;
            }
            // 客户端只有最近一次服务端同步确认过的列才能进入选点列表；服务端再次检查真实绑定。
            return level.isClientSide ? port.isClientFuelColumnBound() : port.isBound();
        }

        @Override
        public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
            return new FuelRefuelingArmInteractionPoint(this, level, pos, state);
        }

        @Override
        public int getPriority() {
            return 1000;
        }
    };

    private FuelRefuelingArmInteractionPoint(
            ArmInteractionPointType type,
            Level level,
            BlockPos pos,
            BlockState state
    ) {
        super(type, level, pos, state);
    }

    /** 在 Create 注册阶段挂入正式换料端口交互点类型。 */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(FuelRefuelingArmInteractionPoint::onRegister);
    }

    private static void onRegister(RegisterEvent event) {
        event.register(
                CreateRegistries.ARM_INTERACTION_POINT_TYPE,
                ResourceLocation.fromNamespaceAndPath(
                        CreateNuclearIndustry.MOD_ID, "reactor_refueling_port"),
                () -> TYPE
        );
    }

    /**
     * 机械臂向端口放入物品；返回值是 Create 机械臂应继续持有的余量。
     * 错误物品、放热中、占用列或失效绑定均原样退回输入副本。
     */
    @Override
    public ItemStack insert(ArmBlockEntity armBlockEntity, ItemStack stack, boolean simulate) {
        ReactorPortBlockEntity port = refuelingPort();
        if (port == null) {
            return copy(stack);
        }
        FuelRefuelingTransaction.Result result = simulate
                ? port.simulateInsertFuel(stack)
                : port.tryInsertFuel(stack);
        return result.success() ? result.remainingInput() : copy(stack);
    }

    /**
     * 机械臂从端口取出一件物品；端口只有一个逻辑槽，输出可能是保留数据组件的燃料
     * 或耗尽后转换得到的冷却乏燃料。失败时返回空栈且不改变端口。
     */
    @Override
    public ItemStack extract(
            ArmBlockEntity armBlockEntity,
            int slot,
            int amount,
            boolean simulate
    ) {
        if (slot != 0 || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ReactorPortBlockEntity port = refuelingPort();
        if (port == null) {
            return ItemStack.EMPTY;
        }
        FuelRefuelingTransaction.Result result = simulate
                ? port.simulateExtractFuel()
                : port.tryExtractFuel();
        if (!result.success() || result.output().isEmpty()) {
            return ItemStack.EMPTY;
        }
        return result.output().copyWithCount(Math.min(1, amount));
    }

    /** 换料端口只对 Create 机械臂声明一个逻辑槽，不代表暴露普通 ItemHandler。 */
    @Override
    public int getSlotCount(ArmBlockEntity armBlockEntity) {
        return 1;
    }

    /** 读取当前世界中的端口实体；绑定和服务端安全条件由端口事务再次验证。 */
    private ReactorPortBlockEntity refuelingPort() {
        return level != null && level.getBlockEntity(pos) instanceof ReactorPortBlockEntity port
                ? port : null;
    }

    /** 失败时返回输入副本，防止 Create 机械臂因适配层错误丢失物品。 */
    private static ItemStack copy(ItemStack stack) {
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }
}
