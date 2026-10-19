package com.iksxh.create_nuclear_industry.industrial;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.junit.jupiter.api.Test;

/** 单次调度绑定复用契约；失效、空能力和未知能力必须重新查找，互不跨调度借用。 */
class IndustrialDispatchBindingsTest {
    private static IFluidHandler handler() {
        return (IFluidHandler) Proxy.newProxyInstance(IFluidHandler.class.getClassLoader(),
                new Class<?>[]{IFluidHandler.class}, (proxy, method, args) -> null);
    }
    private static IndustrialTopology.Binding binding(IFluidHandler handler, BooleanSupplier valid,
                                                       boolean reusable) {
        try {
            var constructor = IndustrialTopology.Binding.class.getDeclaredConstructor(
                    IFluidHandler.class, BooleanSupplier.class, boolean.class);
            constructor.setAccessible(true);
            return constructor.newInstance(handler, valid, reusable);
        } catch (ReflectiveOperationException error) {
            return fail("缺少带复用边界的调度绑定", error);
        }
    }
    private static Object cache(BiFunction<BlockPos, Direction, IndustrialTopology.Binding> lookup) {
        try {
            var type = Class.forName(IndustrialTopology.class.getName() + "$DispatchBindings");
            var constructor = type.getDeclaredConstructor(BiFunction.class);
            constructor.setAccessible(true);
            return constructor.newInstance(lookup);
        } catch (ReflectiveOperationException error) {
            return fail("缺少单次调度绑定缓存", error);
        }
    }
    private static IndustrialTopology.Binding get(Object cache, BlockPos pos, Direction side) {
        try {
            var method = cache.getClass().getDeclaredMethod("get", BlockPos.class, Direction.class);
            method.setAccessible(true);
            return (IndustrialTopology.Binding) method.invoke(cache, pos, side);
        } catch (ReflectiveOperationException error) {
            return fail("绑定查询失败", error);
        }
    }
    @Test void positiveBindingIsReusedWhileItsGuardHolds() {
        var calls = new AtomicInteger();var value = binding(handler(), () -> true, true);
        Object cache = cache((p, s) -> { calls.incrementAndGet();return value; });
        assertSame(value, get(cache, BlockPos.ZERO, Direction.NORTH));
        assertSame(value, get(cache, BlockPos.ZERO, Direction.NORTH));
        assertEquals(1, calls.get());
    }
    @Test void invalidatedBindingIsReplacedBeforeTheNextUse() {
        var oldValid = new AtomicBoolean(true);var calls = new AtomicInteger();
        var first = binding(handler(), oldValid::get, true);var next = binding(handler(), () -> true, true);
        Object cache = cache((p, s) -> calls.getAndIncrement() == 0 ? first : next);
        assertSame(first, get(cache, BlockPos.ZERO, Direction.NORTH));oldValid.set(false);
        assertSame(next, get(cache, BlockPos.ZERO, Direction.NORTH));
        assertSame(next, get(cache, BlockPos.ZERO, Direction.NORTH));assertEquals(2, calls.get());
    }
    @Test void absenceIsNotCachedSoSameCallCanWake() {
        var calls = new AtomicInteger();var absent = binding(null, () -> false, false);
        var positive = binding(handler(), () -> true, true);
        Object cache = cache((p, s) -> calls.getAndIncrement() == 0 ? absent : positive);
        assertNull(get(cache, BlockPos.ZERO, Direction.UP).handler());
        assertSame(positive, get(cache, BlockPos.ZERO, Direction.UP));assertEquals(2, calls.get());
    }
    @Test void thirdPartyBindingIsAlwaysResolvedAgain() {
        var calls = new AtomicInteger();var value = binding(handler(), () -> true, false);
        Object cache = cache((p, s) -> { calls.incrementAndGet();return value; });
        get(cache, BlockPos.ZERO, Direction.UP);get(cache, BlockPos.ZERO, Direction.UP);
        assertEquals(2, calls.get());
    }
    @Test void aFreshAlreadyInvalidBindingCannotBecomeAReusableEntry() {
        var calls = new AtomicInteger();Object cache = cache((p, s) -> {
            calls.incrementAndGet();return binding(handler(), () -> false, true);
        });
        get(cache, BlockPos.ZERO, Direction.UP);get(cache, BlockPos.ZERO, Direction.UP);
        assertEquals(2, calls.get());
    }
    @Test void positionAndSideAreBothPartOfIdentity() {
        var calls = new AtomicInteger();Object cache = cache((p, s) -> {
            calls.incrementAndGet();return binding(handler(), () -> true, true);
        });
        get(cache, BlockPos.ZERO, Direction.UP);get(cache, BlockPos.ZERO, Direction.DOWN);
        get(cache, new BlockPos(1, 0, 0), Direction.UP);assertEquals(3, calls.get());
    }
    @Test void mutablePositionCannotRewriteAnExistingKey() {
        var calls = new AtomicInteger();Object cache = cache((p, s) -> {
            calls.incrementAndGet();return binding(handler(), () -> true, true);
        });
        var pos = new BlockPos.MutableBlockPos();var first = get(cache, pos, Direction.UP);
        pos.set(1, 0, 0);assertNotSame(first, get(cache, pos, Direction.UP));
        assertSame(first, get(cache, BlockPos.ZERO, Direction.UP));assertEquals(2, calls.get());
    }
    @Test void twoDispatchesDoNotShareEntries() {
        var calls = new AtomicInteger();BiFunction<BlockPos, Direction, IndustrialTopology.Binding> lookup = (p, s) -> {
            calls.incrementAndGet();return binding(handler(), () -> true, true);
        };
        Object one = cache(lookup), two = cache(lookup);
        assertNotSame(get(one, BlockPos.ZERO, Direction.UP), get(two, BlockPos.ZERO, Direction.UP));
        assertEquals(2, calls.get());
    }
    @Test void reuseExpiryDoesNotChangeTheOriginalTransactionGuard() throws Exception {
        var reusable=new AtomicBoolean(true);var calls=new AtomicInteger();
        var constructor=assertDoesNotThrow(()->IndustrialTopology.Binding.class.getDeclaredConstructor(
                IFluidHandler.class,BooleanSupplier.class,boolean.class,BooleanSupplier.class));
        constructor.setAccessible(true);
        var first=constructor.newInstance(handler(),(BooleanSupplier)()->true,true,(BooleanSupplier)reusable::get);
        var next=binding(handler(),()->true,true);
        Object cache=cache((p,s)->calls.getAndIncrement()==0?first:next);
        assertSame(first,get(cache,BlockPos.ZERO,Direction.NORTH));reusable.set(false);
        assertTrue(first.valid().getAsBoolean(),"复用过期不能悄悄改变已签发交易绑定的原守卫");
        assertSame(next,get(cache,BlockPos.ZERO,Direction.NORTH));assertEquals(2,calls.get());
    }
}
