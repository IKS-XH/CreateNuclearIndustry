package com.iksxh.create_nuclear_industry.turbine;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

/** 验证结构查询的静态几何裁剪；世界实时资格另由独立真实服探针验证。 */
final class TurbineStructureLookupTest {
    private static Method method(Class<?> type, String name, Class<?>... args) throws Exception {
        Method method = type.getDeclaredMethod(name, args);
        method.setAccessible(true);
        return method;
    }

    @Test void cavityCoordinatesAndOrderMatchOriginalAtForEveryAllowedShape() throws Exception {
        Method section = method(TurbineStructure.class, "airSection", int.class);
        Method set = method(TurbineStructure.class, "setAirPosition", BlockPos.MutableBlockPos.class,
                BlockPos.class, Direction.class, Direction.class, int.class, int.class, int.class);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int diameter : new int[]{3, 5, 7}) {
            int radius = (diameter - 1) / 2;
            int[] offsets = (int[]) section.invoke(null, diameter);
            for (Direction facing : Direction.Plane.HORIZONTAL) for (int rotors = 3; rotors <= 16; rotors++)
                for (BlockPos front : List.of(new BlockPos(-17, -60, -16), new BlockPos(15, 70, 31))) {
                    List<BlockPos> reference = new ArrayList<>(), actual = new ArrayList<>();
                    for (int z = 1; z <= rotors; z++)
                        for (int y = -radius; y <= radius; y++) for (int x = -radius; x <= radius; x++)
                            if (TurbineGeometry.airSlot(diameter, x, y))
                                reference.add(TurbineStructure.at(front, facing, x, y, z));
                    for (int z = 1; z <= rotors; z++) for (int i = 0; i < offsets.length; i += 2) {
                        set.invoke(null, cursor, front, facing.getClockWise(), facing.getOpposite(),
                                offsets[i], offsets[i + 1], z);
                        actual.add(cursor.immutable());
                    }
                    assertEquals(reference, actual, facing + "/" + diameter + "/" + rotors + "/" + front);
                }
        }
    }

    @Test void boundsUseActualMembersAndOnlyRejectDisjointCandidates() throws Exception {
        Class<?> bounds = Class.forName(TurbineStructure.class.getName() + "$PartBounds");
        Method of = method(bounds, "of", List.class);
        Method intersects = method(bounds, "intersects", bounds);
        Method contains = method(bounds, "contains", BlockPos.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) for (int diameter : new int[]{3, 5, 7})
            for (int rotors = 3; rotors <= 16; rotors++) {
                List<BlockPos> members = new ArrayList<>();
                int r = (diameter - 1) / 2;
                for (int z = 0; z < rotors + 2; z++) for (int y = -r; y <= r; y++)
                    for (int x = -r; x <= r; x++) if (TurbineGeometry.footprint(diameter, x, y))
                        members.add(TurbineStructure.at(new BlockPos(-17, -60, 15), facing, x, y, z));
                Object box = of.invoke(null, members);
                for (BlockPos member : members) assertEquals(true, contains.invoke(box, member));
                Object distant = of.invoke(null, List.of(new BlockPos(900, 60, -900)));
                assertEquals(false, intersects.invoke(box, distant));
            }
        List<BlockPos> sparse = List.of(new BlockPos(-100, -30, -5), new BlockPos(100, 30, 5));
        Object outer = of.invoke(null, sparse), inside = of.invoke(null, List.of(BlockPos.ZERO));
        assertEquals(true, intersects.invoke(outer, inside));
        TurbineStructure.Form form = new TurbineStructure.Form(Direction.NORTH, 3, 3,
                BlockPos.ZERO, BlockPos.ZERO.south(4), BlockPos.ZERO, sparse, List.of(), List.of(), List.of());
        assertFalse(form.contains(BlockPos.ZERO), "相交包围盒不能改变公开的实际成员语义");
        assertTrue(form.contains(sparse.getFirst()), "手工成员可在理论外形之外");
        assertEquals(false, intersects.invoke(of.invoke(null, List.of()), outer));
        Object touching = of.invoke(null, List.of(sparse.getFirst()));
        assertEquals(true, intersects.invoke(outer, touching), "边界接触不能被粗筛排除");
    }
}
