package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

/** S1真实服务端定向回归：列查询分配预算、绑定生命周期和完整燃料事务；不提供生产测试入口。 */
@GameTestHolder("cni_reactor_port_lookup")
@PrefixGameTestTemplate(false)
public final class ReactorPortLookupGameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos OWNER = new BlockPos(2, 2, 0);
    private static final BlockPos CAP = new BlockPos(1, 4, 1);
    private static final BlockPos OTHER_CAP = new BlockPos(3, 4, 3);
    private static final CoreColumnPosition COLUMN = new CoreColumnPosition(0, 0);
    private static final CoreColumnPosition OTHER_COLUMN = new CoreColumnPosition(2, 2);

    private ReactorPortLookupGameTests() { }

    /** 重复只读查询不得重建整组端口集合；预算允许每次256字节，远宽于直接索引本身。 */
    @GameTest(template = TEMPLATE, templateNamespace = "create_nuclear_industry", timeoutTicks = 160)
    public static void repeatedLookupStaysWithinAllocationBudget(GameTestHelper helper) {
        ready(helper, () -> {
            var owner = owner(helper);
            var expected = port(helper, CAP);
            MethodHandle lookup = lookupHandle();
            var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
            check(helper, bean.isThreadAllocatedMemorySupported(), "当前Java21未提供线程分配计数");
            boolean enabled = bean.isThreadAllocatedMemoryEnabled();
            if (!enabled) bean.setThreadAllocatedMemoryEnabled(true);
            try {
                for (int i = 0; i < 1024; i++) query(lookup, owner, COLUMN);
                long thread = Thread.currentThread().threadId();
                long before = bean.getThreadAllocatedBytes(thread);
                ReactorPortBlockEntity actual = null;
                for (int i = 0; i < 512; i++) actual = query(lookup, owner, COLUMN);
                long allocated = bean.getThreadAllocatedBytes(thread) - before;
                System.out.println("S1 512次列查询分配字节=" + allocated);
                check(helper, actual == expected, "重复查询未返回该列当前真实端口");
                check(helper, allocated <= 131_072L,
                        "512次列查询分配" + allocated + "字节，仍在重复重建端口集合（预算131072）");
                helper.succeed();
            } finally {
                if (!enabled) bean.setThreadAllocatedMemoryEnabled(false);
            }
        });
    }

    /** 同坐标新端口已经重扫绑定后，旧实例迟到的反向解绑不能删除新索引。 */
    @GameTest(template = TEMPLATE, templateNamespace = "create_nuclear_industry", timeoutTicks = 100)
    public static void lateDetachKeepsReplacementAtSamePosition(GameTestHelper helper) {
        ready(helper, () -> {
            var owner = owner(helper);
            var old = port(helper, CAP);
            helper.setBlock(CAP, Blocks.AIR.defaultBlockState());
            helper.setBlock(CAP, P1Blocks.REACTOR_REFUELING_PORT.get().defaultBlockState());
            rescan(helper);
            var replacement = port(helper, CAP);
            check(helper, replacement != old, "同坐标夹具未创建新实体");
            replacement.setFuelAssembly(fuel(24_000));
            invoke(owner, "detachPort", new Class<?>[]{ReactorPortBlockEntity.class}, old);
            check(helper, query(lookupHandle(), owner, COLUMN) == replacement, "旧解绑撤掉了同坐标新端口");
            owner.tickReactor();
            check(helper, replacement.fuelAssembly().getDamageValue() == 24_000,
                    "旧实体燃料覆盖新端口或全插棒发生额外燃耗");
            helper.succeed();
        });
    }

    /** 主动解绑后立即拒绝结算，重扫恢复；外壳失效清空绑定，再成型可重新查询。 */
    @GameTest(template = TEMPLATE, templateNamespace = "create_nuclear_industry", timeoutTicks = 100)
    public static void detachAndStructureRebuildKeepTransactionsSafe(GameTestHelper helper) {
        ready(helper, () -> {
            var owner = owner(helper);
            var port = port(helper, CAP);
            var before = owner.snapshot();
            var stored = port.fuelAssembly();
            port.clearBinding(owner);
            check(helper, query(lookupHandle(), owner, COLUMN) == null, "解绑后仍可查询旧端口");
            check(helper, !owner.tickReactor() && owner.snapshot().equals(before)
                    && ItemStack.matches(stored, port.fuelAssembly()), "缺失绑定仍结算或产生半提交");
            rescan(helper);
            check(helper, query(lookupHandle(), owner, COLUMN) == port, "重扫没有重建列查询");
            helper.setBlock(BlockPos.ZERO, Blocks.AIR.defaultBlockState());
            rescan(helper);
            check(helper, query(lookupHandle(), owner, COLUMN) == null, "结构失效仍保留可用端口");
            helper.setBlock(BlockPos.ZERO, P1Blocks.REACTOR_CASING.get().defaultBlockState());
            rescan(helper);
            check(helper, query(lookupHandle(), owner, COLUMN) == port, "重新成型无法找回燃料端口");
            helper.succeed();
        });
    }

    /** 缓存命中也须验证实际列，不能借错误列绑定读取另一个燃料端口。 */
    @GameTest(template = TEMPLATE, templateNamespace = "create_nuclear_industry", timeoutTicks = 100)
    public static void changedColumnBindingRejectsCachedPort(GameTestHelper helper) {
        ready(helper, () -> {
            var owner = owner(helper);
            var port = port(helper, CAP);
            check(helper, port.bindTo(owner, ReactorPortBlockEntity.BindingType.REFUELING, OTHER_COLUMN),
                    "无法建立同owner错误列的前置状态");
            var before = owner.snapshot();
            check(helper, query(lookupHandle(), owner, COLUMN) == null, "索引命中跳过了实际列校验");
            check(helper, !owner.tickReactor() && owner.snapshot().equals(before), "错列绑定没有取消完整结算");
            rescan(helper);
            check(helper, query(lookupHandle(), owner, COLUMN) == port, "纠正绑定后查询不能恢复");
            helper.succeed();
        });
    }

    /** 预读后任一端口物品变化必须拒绝整批准备，不能改另一列或权威快照。 */
    @GameTest(template = TEMPLATE, templateNamespace = "create_nuclear_industry", timeoutTicks = 100)
    public static void changedFuelAfterCaptureRejectsWholeCommit(GameTestHelper helper) {
        ready(helper, () -> {
            var owner = owner(helper);
            var before = owner.snapshot();
            var captured = invoke(owner, "captureFuelPortItems", new Class<?>[0]);
            check(helper, captured != null, "正式物品预读失败");
            var otherBefore = port(helper, OTHER_CAP).fuelAssembly();
            port(helper, CAP).setFuelAssembly(fuel(31_000));
            var prepared = invoke(owner, "prepareFuelPortCommit", new Class<?>[]{Map.class,
                    com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot.class}, captured, before);
            check(helper, prepared == null && owner.snapshot().equals(before)
                    && port(helper, CAP).fuelAssembly().getDamageValue() == 31_000
                    && ItemStack.matches(otherBefore, port(helper, OTHER_CAP).fuelAssembly()),
                    "预读后变化没有拒绝整批提交或污染另一列");
            helper.succeed();
        });
    }

    /** 当前版本保存后替换owner实例，索引只能由新实例重扫重建，不能沿用旧实体。 */
    @GameTest(template = TEMPLATE, templateNamespace = "create_nuclear_industry", timeoutTicks = 100)
    public static void currentSaveReloadRebuildsOwnerBindings(GameTestHelper helper) {
        ready(helper, () -> {
            var old = owner(helper);
            var saved = old.saveForServerTest(helper.getLevel().registryAccess());
            var stored = port(helper, CAP).fuelAssembly();
            helper.setBlock(OWNER, Blocks.AIR.defaultBlockState());
            helper.setBlock(OWNER, P1Blocks.REACTOR_INSTRUMENT_PORT.get().defaultBlockState());
            var replacement = owner(helper);
            replacement.loadForServerTest(saved, helper.getLevel().registryAccess());
            // 新BE由下一服务端加载边界登记；立即重扫会只看到已撤销的旧owner。
            helper.runAfterDelay(2, () -> {
                rescan(helper);
                check(helper, replacement != old && query(lookupHandle(), old, COLUMN) == null,
                        "旧owner在替换后仍返回端口");
                check(helper, query(lookupHandle(), replacement, COLUMN) == port(helper, CAP),
                        "当前版本续接未重建端口绑定");
                check(helper, ItemStack.matches(stored, port(helper, CAP).fuelAssembly()),
                        "当前版本续接改变了端口燃料");
                helper.succeed();
            });
        });
    }

    /** 复用原机械臂装卸事务，避免另造预检/提交替身。 */
    @GameTest(template = TEMPLATE, templateNamespace = "create_nuclear_industry", timeoutTicks = 120)
    public static void existingArmAtomicLoadAndUnload(GameTestHelper helper) {
        P1Refuel03GameTests.armPointCompletesAtomicLoadAndUnload(helper);
    }

    /** 复用原错误物品与运行列拒绝用例。 */
    @GameTest(template = TEMPLATE, templateNamespace = "create_nuclear_industry", timeoutTicks = 140)
    public static void existingArmRejectsWrongAndActiveFuel(GameTestHelper helper) {
        P1Refuel03GameTests.armPointRejectsWrongItemAndActiveColumn(helper);
    }

    /** 复用原结构失效和预检后变更回滚用例。 */
    @GameTest(template = TEMPLATE, templateNamespace = "create_nuclear_industry", timeoutTicks = 180)
    public static void existingArmInvalidationRollback(GameTestHelper helper) {
        P1Refuel03GameTests.armPointRejectsInvalidTargetsAndRollsBack(helper);
    }

    /** 复用现有完整显示样本生命周期；不更改其实现或断言。 */
    @GameTest(template = "empty", templateNamespace = "reactor_runtime_display_probe", timeoutTicks = 100)
    public static void existingRuntimeLifecycle(GameTestHelper helper) {
        ReactorRuntimeDisplayGameTests.completedRuntimeSampleTracksAuthorityAndLifecycle(helper);
    }

    /** 两个燃料列均被全插棒覆盖；延迟后显式重扫，不靠任意睡眠推定绑定成功。 */
    private static void ready(GameTestHelper helper, Runnable assertion) {
        Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> layout = new HashMap<>();
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) {
            var column = new CoreColumnPosition(x, z);
            layout.put(column, column.equals(COLUMN) || column.equals(OTHER_COLUMN)
                    ? ReactorStructureDefinition.ColumnType.FUEL : ReactorStructureDefinition.ColumnType.CONTROL_ROD);
        }
        ReactorStructureDefinition.templateFor(layout).forEach((pos, id) -> helper.setBlock(
                new BlockPos(pos.x(), pos.y(), pos.z()), BuiltInRegistries.BLOCK.get(
                        ResourceLocation.parse(id)).defaultBlockState()));
        helper.runAfterDelay(5, () -> {
            rescan(helper);
            check(helper, owner(helper).structureValid(), "定向夹具未成型");
            port(helper, CAP).setFuelAssembly(fuel(12_000));
            port(helper, OTHER_CAP).setFuelAssembly(fuel(18_000));
            owner(helper).tickReactor();
            assertion.run();
        });
    }

    private static void rescan(GameTestHelper helper) {
        ReactorStructureLifecycle.rescanAroundNow(helper.getLevel(), helper.absolutePos(OWNER));
    }
    private static ReactorInstrumentPortBlockEntity owner(GameTestHelper helper) {
        return (ReactorInstrumentPortBlockEntity) helper.getBlockEntity(OWNER);
    }
    private static ReactorPortBlockEntity port(GameTestHelper helper, BlockPos pos) {
        return (ReactorPortBlockEntity) helper.getBlockEntity(pos);
    }
    private static ItemStack fuel(int damage) {
        var stack = new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get());
        stack.setDamageValue(damage);
        return stack;
    }
    /** MethodHandle避免反射varargs本身污染分配预算；只调用真实私有生产路径。 */
    private static MethodHandle lookupHandle() {
        try {
            var method = ReactorInstrumentPortBlockEntity.class.getDeclaredMethod("findRefuelingPort", CoreColumnPosition.class);
            method.setAccessible(true);
            return MethodHandles.lookup().unreflect(method);
        } catch (ReflectiveOperationException exception) { throw new IllegalStateException(exception); }
    }
    private static ReactorPortBlockEntity query(MethodHandle lookup, ReactorInstrumentPortBlockEntity owner, CoreColumnPosition column) {
        try { return (ReactorPortBlockEntity) lookup.invokeExact(owner, column); }
        catch (Throwable exception) { throw new IllegalStateException(exception); }
    }
    /** 只在测试中观察事务准备和反向生命周期，不增加生产访问器。 */
    private static Object invoke(ReactorInstrumentPortBlockEntity owner, String name, Class<?>[] types, Object... args) {
        try {
            var method = ReactorInstrumentPortBlockEntity.class.getDeclaredMethod(name, types);
            method.setAccessible(true);
            return method.invoke(owner, args);
        } catch (ReflectiveOperationException exception) { throw new IllegalStateException(exception); }
    }
    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
