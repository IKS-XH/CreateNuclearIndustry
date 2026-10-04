package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.content.TurbineContent;
import com.iksxh.create_nuclear_industry.config.TurbineConfig;
import com.iksxh.create_nuclear_industry.turbine.TurbineControllerBlockEntity;
import com.iksxh.create_nuclear_industry.turbine.TurbineOutputShaftBlockEntity;
import com.iksxh.create_nuclear_industry.turbine.TurbinePartBlock;
import com.iksxh.create_nuclear_industry.turbine.TurbineStructure;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 正式三档机组的结构、真实流体口、Create 轴网和拆放保量场景。 */
@GameTestHolder("create_nuclear_industry_turbine")
@PrefixGameTestTemplate(false)
public final class ExtensionTurbineGameTests {
    private static final BlockPos FRONT = new BlockPos(5, 2, 1);
    private ExtensionTurbineGameTests() {}

    /** 三档使用同一截面与当前配置长度，多物理口共享库存但各自限流。 */
    @GameTest(template = "turbine_empty", timeoutTicks = 115)
    public static void threeTiersFormAndAllocateOnlyActualProcessedSteam(GameTestHelper helper) {
        BlockPos[] fronts = {FRONT, FRONT.east(6), FRONT.east(12)};
        var settings = TurbineConfig.settings();
        int[] rotors = {settings.shortTier().rotorCount(), settings.mediumTier().rotorCount(),
                settings.longTier().rotorCount()};
        List<BlockPos> feeds = new ArrayList<>();
        for (int index = 0; index < 3; index++) {
            build(helper, fronts[index], rotors[index], index == 2);
            feeds.add(part(fronts[index], -1, 1, 1));
        }
        helper.onEachTick(() -> {
            for (BlockPos feed : feeds) {
                IFluidHandler inlet = handler(helper, feed, Direction.WEST);
                if (inlet != null) inlet.fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 256),
                        IFluidHandler.FluidAction.EXECUTE);
            }
        });
        helper.runAfterDelay(8, () -> {
            String[] recipeIds = {"turbine_casing", "turbine_rotor", "turbine_controller",
                    "turbine_output_shaft", "turbine_inlet", "turbine_exhaust"};
            Item[] outputs = {TurbineContent.CASING_ITEM.get(), TurbineContent.ROTOR_ITEM.get(),
                    TurbineContent.CONTROLLER_ITEM.get(), TurbineContent.OUTPUT_SHAFT_ITEM.get(),
                    TurbineContent.INLET_ITEM.get(), TurbineContent.EXHAUST_ITEM.get()};
            for (int recipe = 0; recipe < recipeIds.length; recipe++) {
                var loaded = helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
                        "create_nuclear_industry", recipeIds[recipe])).orElseThrow();
                ItemStack result = loaded.value().getResultItem(helper.getLevel().registryAccess());
                require(helper, result.is(outputs[recipe]) && result.getCount() == (recipe == 0 ? 8 : 1)
                                && (recipe == 2 ? loaded.value() instanceof MechanicalCraftingRecipe
                                : loaded.value() instanceof CraftingRecipe),
                        "六件真实配方身份/数量/工序不符：" + recipeIds[recipe]);
            }
            for (int index = 0; index < 3; index++) {
                var owner = owner(helper, fronts[index]);
                require(helper, owner.currentForm() != null && owner.currentForm().rotors() == rotors[index],
                        "配置档位未按转子数形成：" + rotors[index]);
                require(helper, TurbineStructure.inspect(helper.getLevel(), helper.absolutePos(fronts[index]),
                        owner.ledger().settings()) != null, "结构扫描不能复核当前档位");
            }
            IFluidHandler second = handler(helper, part(fronts[2], 0, 2, 2), Direction.UP);
            require(helper, second != null && second.fill(new FluidStack(TurbineContent.STEAM.get(), 100),
                    IFluidHandler.FluidAction.EXECUTE) == 0, "普通蒸汽被进汽口接收");
            require(helper, second.fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 256),
                    IFluidHandler.FluidAction.EXECUTE) == 256, "第二物理进口没有独立限流");
            require(helper, owner(helper, fronts[2]).ledger().input() > 0, "多口未汇入同一个 owner 库存");
        });
        helper.runAfterDelay(75, () -> {
            for (int index = 0; index < 3; index++) {
                var owner = owner(helper, fronts[index]);
                int flow = owner.ledger().ratedFlowMbPerTick();
                double expected = flow * owner.ledger().settings().suPerMbPerTick();
                require(helper, owner.ledger().processed() == flow && owner.ledger().totalSu() == expected,
                        "实际稳态流量/SU 不符：转子=" + rotors[index] + " 实际=" + owner.ledger().totalSu());
                BlockPos rear = part(fronts[index], 0, 1, rotors[index] + 1);
                var output = (TurbineOutputShaftBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(rear));
                require(helper, output != null && owner.hasNetwork() && output.hasNetwork(), "双端轴没有进入 Create 网络");
                float front = owner.getOrCreateNetwork().calculateCapacity();
                float rearSu = output.getOrCreateNetwork().calculateCapacity();
                require(helper, Math.abs(front - expected * settings.frontShare()) < 2
                                && Math.abs(rearSu - expected * (1 - settings.frontShare())) < 2,
                        "双轴未按配置份额各登记一次或单端发生重分配");
            }
            helper.succeed();
        });
    }

    /** 红石撤销动力保留库存；破坏控制器仅掉一份携物数据，重放不能复制蒸汽。 */
    @GameTest(template = "turbine_empty", timeoutTicks = 100)
    public static void redstoneStopAndPortableControllerKeepSingleLedger(GameTestHelper helper) {
        build(helper, FRONT, TurbineConfig.settings().shortTier().rotorCount(), false);
        helper.runAfterDelay(7, () -> {
            var owner = owner(helper, FRONT);
            require(helper, owner.currentForm() != null, "拆装场景未成型");
            var inlet = handler(helper, part(FRONT, -1, 1, 1), Direction.WEST);
            require(helper, inlet != null && inlet.fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 256),
                    IFluidHandler.FluidAction.EXECUTE) == 256, "真实进汽口未收汽");
        });
        final int[] stored = {0, 0};
        final IFluidHandler[] oldHandle = {null};
        helper.runAfterDelay(12, () -> {
            var owner = owner(helper, FRONT);
            stored[0] = owner.ledger().input(); stored[1] = owner.ledger().exhaust();
            require(helper, stored[0] + stored[1] == 256 && owner.ledger().totalSu() > 0,
                    "加工前后蒸汽不守恒或轴没有实际 SU");
            oldHandle[0] = handler(helper, part(FRONT, -1, 1, 1), Direction.WEST);
            helper.setBlock(FRONT.north(), Blocks.REDSTONE_BLOCK);
        });
        helper.runAfterDelay(18, () -> {
            var owner = owner(helper, FRONT);
            require(helper, owner.ledger().totalSu() == 0
                            && (!owner.hasNetwork() || owner.getOrCreateNetwork().calculateCapacity() == 0),
                    "红石停机未撤销前轴 SU");
            require(helper, owner.ledger().input() + owner.ledger().exhaust() == 256,
                    "红石停机改变了库存");
            ItemStack carried = Block.getDrops(helper.getBlockState(FRONT), helper.getLevel(),
                    helper.absolutePos(FRONT), owner).stream()
                    .filter(stack -> stack.is(TurbineContent.CONTROLLER_ITEM.get())).findFirst().orElse(ItemStack.EMPTY);
            require(helper, !carried.isEmpty() && carried.get(DataComponents.CUSTOM_DATA) != null,
                    "控制器掉落没有携带唯一库存 NBT");
            helper.setBlock(FRONT, Blocks.AIR);
            require(helper, oldHandle[0] != null && oldHandle[0].fill(
                    new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 1), IFluidHandler.FluidAction.EXECUTE) == 0,
                    "拆除后的旧端口句柄仍能写库存");
            helper.setBlock(FRONT, TurbineContent.CONTROLLER.get().defaultBlockState()
                    .setValue(TurbinePartBlock.MACHINE_FACING, Direction.NORTH));
            helper.getBlockState(FRONT).getBlock().setPlacedBy(helper.getLevel(), helper.absolutePos(FRONT),
                    helper.getBlockState(FRONT), null, carried);
        });
        helper.runAfterDelay(26, () -> {
            var owner = owner(helper, FRONT);
            require(helper, owner.currentForm() != null && owner.ledger().input() + owner.ledger().exhaust() == 256,
                    "携物重放未重新成型或复制/删除了库存");
            require(helper, owner.ledger().totalSu() == 0, "重放后错误保留旧平滑动力历史");
            helper.succeed();
        });
    }

    /** 排汽先经真实 Create 无泵管道再入储罐，流体身份和总量不丢失。 */
    @GameTest(template = "turbine_empty", timeoutTicks = 90)
    public static void ordinarySteamMovesThroughNativeCreatePipe(GameTestHelper helper) {
        build(helper, FRONT, TurbineConfig.settings().mediumTier().rotorCount(), false);
        BlockPos exhaust = part(FRONT, 1, 1, 1);
        BlockPos[] pipes = {exhaust.east(), exhaust.east(2), exhaust.east(3)};
        BlockPos tankPos = exhaust.east(4);
        helper.setBlock(tankPos, AllBlocks.FLUID_TANK.get());
        for (BlockPos pipe : pipes) helper.setBlock(pipe, AllBlocks.FLUID_PIPE.get());
        sealOpenEnds(helper, pipes);
        helper.runAfterDelay(6, () -> {
            CompoundTag stored = new CompoundTag();
            stored.putInt("Exhaust", 1000);
            owner(helper, FRONT).ledger().load(stored);
            require(helper, owner(helper, FRONT).currentForm() != null, "管网场景未成型");
        });
        helper.runAfterDelay(55, () -> {
            IFluidHandler tank = handler(helper, tankPos, Direction.WEST);
            int moved = tank == null ? 0 : tank.getFluidInTank(0).getAmount();
            var pipe = FluidPropagator.getPipe(helper.getLevel(), helper.absolutePos(pipes[0]));
            var connection = pipe == null ? null : pipe.getConnection(Direction.WEST);
            var output = handler(helper, exhaust, Direction.EAST);
            String diagnostic = " 排汽=" + owner(helper, FRONT).ledger().exhaust()
                    + " 端口=" + (output == null ? "无" : output.getFluidInTank(0))
                    + " 首管西连接=" + (connection == null ? "无" : connection.getPressure())
                    + " 储罐=" + (tank == null ? "无能力" : tank.getFluidInTank(0));
            require(helper, moved > 0 && TurbineContent.isOrdinarySteam(tank.getFluidInTank(0)),
                    "无泵 Create 管道未收到普通蒸汽" + diagnostic);
            require(helper, moved + owner(helper, FRONT).ledger().exhaust() == 1000,
                    "真实排汽管路复制或删除普通蒸汽");
            helper.succeed();
        });
    }

    static void build(GameTestHelper helper, BlockPos front, int rotors, boolean extraPorts) {
        int length = rotors + 2;
        for (int z = 0; z < length; z++) for (int y = 0; y <= 2; y++) for (int x = -1; x <= 1; x++) {
            BlockPos pos = part(front, x, y, z);
            BlockState state;
            if (x == 0 && y == 1) state = z == 0 ? TurbineContent.CONTROLLER.get().defaultBlockState()
                    : z == length - 1 ? TurbineContent.OUTPUT_SHAFT.get().defaultBlockState()
                    : TurbineContent.ROTOR.get().defaultBlockState();
            else if (z == 1 && x == -1 && y == 1 || extraPorts && z == 2 && x == 0 && y == 2)
                state = TurbineContent.INLET.get().defaultBlockState().setValue(TurbinePartBlock.OUTWARD,
                        y == 2 ? Direction.UP : Direction.WEST);
            else if (z == 1 && x == 1 && y == 1 || extraPorts && z == 3 && x == -1 && y == 1)
                state = TurbineContent.EXHAUST.get().defaultBlockState().setValue(TurbinePartBlock.OUTWARD,
                        x < 0 ? Direction.WEST : Direction.EAST);
            else state = TurbineContent.CASING.get().defaultBlockState();
            helper.setBlock(pos, state);
        }
    }
    static BlockPos part(BlockPos front, int x, int y, int z) { return front.offset(x, y - 1, z); }
    static TurbineControllerBlockEntity owner(GameTestHelper helper, BlockPos front) {
        return (TurbineControllerBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(front));
    }
    static IFluidHandler handler(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), side);
    }
    private static void sealOpenEnds(GameTestHelper helper, BlockPos[] pipes) {
        for (BlockPos pipe : pipes) FluidPropagator.propagateChangedPipe(helper.getLevel(),
                helper.absolutePos(pipe), helper.getBlockState(pipe));
        for (BlockPos pipe : pipes) {
            FluidTransportBehaviour behaviour = com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour.get(
                    helper.getLevel(), helper.absolutePos(pipe), FluidTransportBehaviour.TYPE);
            require(helper, behaviour != null, "Create 管道缺少 FluidTransportBehaviour");
            for (Direction side : Direction.values())
                if (behaviour.getConnection(side) != null && helper.getBlockState(pipe.relative(side)).isAir()
                        && FluidPropagator.isOpenEnd(helper.getLevel(), helper.absolutePos(pipe), side))
                    helper.setBlock(pipe.relative(side), Blocks.IRON_BLOCK);
        }
        for (BlockPos pipe : pipes) FluidPropagator.propagateChangedPipe(helper.getLevel(),
                helper.absolutePos(pipe), helper.getBlockState(pipe));
    }
    static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
