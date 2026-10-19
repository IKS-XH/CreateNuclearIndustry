package com.iksxh.create_nuclear_industry.turbine;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

/** 验证实际坐标索引的身份和撤销语义；真实世界运行资格由独立服覆盖。 */
final class TurbineMemberIndexTest {
    private static final class Index {
        final Object value;
        final Method put, get, remove, conflicted;
        Index() throws Exception {
            Class<?> type = Class.forName(TurbineStructure.class.getName() + "$MemberIndex");
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            value = constructor.newInstance();
            put = method(type, "put", Object.class, List.class);
            get = method(type, "get", BlockPos.class);
            remove = method(type, "remove", Object.class);
            conflicted = method(type, "conflicted", BlockPos.class);
        }
        void put(Object owner, List<BlockPos> members) throws Exception { put.invoke(value, owner, members); }
        Object get(BlockPos pos) throws Exception { return get.invoke(value, pos); }
        void remove(Object owner) throws Exception { remove.invoke(value, owner); }
        boolean conflicted(BlockPos pos) throws Exception { return (boolean) conflicted.invoke(value, pos); }
    }
    private static Method method(Class<?> type, String name, Class<?>... parameters) throws Exception {
        Method m = type.getDeclaredMethod(name, parameters); m.setAccessible(true); return m;
    }

    @Test void sparseActualMembersDoNotClaimBoundingBoxHoles() throws Exception {
        Index index = new Index(); Object owner = new Object();
        BlockPos a = new BlockPos(-17, -60, -16), b = new BlockPos(31, 79, 48);
        index.put(owner, List.of(a, b));
        assertSame(owner, index.get(a)); assertSame(owner, index.get(b));
        assertNull(index.get(BlockPos.ZERO)); assertNull(index.get(a.east()));
    }

    @Test void replacingFormRevokesAllOldCoordinates() throws Exception {
        Index index = new Index(); Object owner = new Object();
        index.put(owner, List.of(BlockPos.ZERO, BlockPos.ZERO.east()));
        index.put(owner, List.of(BlockPos.ZERO.north()));
        assertNull(index.get(BlockPos.ZERO)); assertNull(index.get(BlockPos.ZERO.east()));
        assertSame(owner, index.get(BlockPos.ZERO.north()));
        index.remove(owner); assertNull(index.get(BlockPos.ZERO.north()));
    }

    @Test void removalUsesIdentityAndRetainsOtherOwnerAfterConflict() throws Exception {
        record Token(int id) {}
        Index index = new Index(); Object first = new Token(1), second = new Token(1);
        index.put(first, List.of(BlockPos.ZERO)); index.put(second, List.of(BlockPos.ZERO));
        assertTrue(index.conflicted(BlockPos.ZERO));
        assertNull(index.get(BlockPos.ZERO), "冲突需要走原查询回退，不能静默指定某一方");
        index.remove(first);
        assertFalse(index.conflicted(BlockPos.ZERO)); assertSame(second, index.get(BlockPos.ZERO));
        index.remove(first); assertSame(second, index.get(BlockPos.ZERO), "旧身份重复释放不能撤销新绑定");
        index.remove(second); assertNull(index.get(BlockPos.ZERO));
    }

    @Test void mutableCallerCoordinatesAreCapturedAsValues() throws Exception {
        Index index = new Index(); Object owner = new Object();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(-16, 64, 15);
        index.put(owner, List.of(mutable)); mutable.set(100, 80, 100);
        assertSame(owner, index.get(new BlockPos(-16, 64, 15)));
        assertNull(index.get(mutable)); index.remove(owner);
        assertNull(index.get(new BlockPos(-16, 64, 15)));
    }

    @Test void duplicateMembersAndEmptyReplacementRemainReleasable() throws Exception {
        Index index = new Index(); Object owner = new Object();
        index.put(owner, List.of(BlockPos.ZERO, BlockPos.ZERO));
        assertSame(owner, index.get(BlockPos.ZERO)); assertFalse(index.conflicted(BlockPos.ZERO));
        index.put(owner, List.of()); assertNull(index.get(BlockPos.ZERO));
        index.remove(owner); assertNull(index.get(BlockPos.ZERO));
    }

    @Test void steadyLookupsNeverRevisitTheMemberList() throws Exception {
        Index index = new Index(); Object owner = new Object();
        class Members extends AbstractList<BlockPos> {
            boolean sealed;
            @Override public int size() { return 4096; }
            @Override public BlockPos get(int i) {
                if (sealed) throw new AssertionError("稳定查询重新读取了原成员列表");
                return new BlockPos(i - 2048, -60, 17);
            }
        }
        Members members = new Members(); index.put(owner, members); members.sealed = true;
        for (int n = 0; n < 4; n++) for (int x = -2048; x < 2048; x++)
            assertSame(owner, index.get(new BlockPos(x, -60, 17)));
        index.remove(owner); assertNull(index.get(new BlockPos(0, -60, 17)));
    }
}
