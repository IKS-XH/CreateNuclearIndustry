package com.iksxh.create_nuclear_industry.boiler;

import com.iksxh.create_nuclear_industry.content.BoilerContent;
import java.util.ArrayList;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** 固定5×5×5壳体的只读验证器；不加载区块，返回底面中央热段和所属端口位置。 */
public final class BoilerStructure {
    private static final Map<Level, Map<BlockPos, WeakReference<BoilerControllerBlockEntity>>> OWNERS = new WeakHashMap<>();
    public record Form(BlockPos center, List<BlockPos> sections, List<BlockPos> waterPorts,
                       List<BlockPos> steamPorts, BlockPos valve) {}
    public record Issue(String reason, BlockPos pos) {}
    private BoilerStructure() {}

    /** 只有结构变更时复核相邻候选；弱引用不延长世界或实体寿命。 */
    public static void invalidateNearby(Level level, BlockPos changed) {
        if (level == null || level.isClientSide) return;
        for (int y = -4; y <= 4; y++) for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            BlockPos candidate = changed.offset(x, y, z);
            if (level.hasChunkAt(candidate) && level.getBlockEntity(candidate) instanceof BoilerControllerBlockEntity owner)
                owner.invalidateForm();
        }
        level.invalidateCapabilities(changed);
    }

    /** 两个完整外形的占位包围盒不得相交，防止共享壳体或热段被两个控制器认领。 */
    static boolean unique(Level level, BlockPos controller, Form form) {
        for (int y = -4; y <= 4; y++) for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
            BlockPos candidate = controller.offset(x, y, z);
            if (candidate.equals(controller) || !level.hasChunkAt(candidate)
                    || !(level.getBlockEntity(candidate) instanceof BoilerControllerBlockEntity)) continue;
            Form other = inspect(level, candidate);
            if (other != null && Math.abs(form.center().getX() - other.center().getX()) <= 4
                    && Math.abs(form.center().getZ() - other.center().getZ()) <= 4
                    && Math.abs(form.center().getY() - other.center().getY()) <= 4) return false;
        }
        return true;
    }

    static void cache(Level level, BoilerControllerBlockEntity owner, Form form) {
        Map<BlockPos, WeakReference<BoilerControllerBlockEntity>> entries = OWNERS.computeIfAbsent(level, ignored -> new HashMap<>());
        for (BlockPos port : form.waterPorts()) entries.put(port, new WeakReference<>(owner));
        for (BlockPos port : form.steamPorts()) entries.put(port, new WeakReference<>(owner));
        for (BlockPos section : form.sections()) entries.put(section, new WeakReference<>(owner));
    }

    static void forget(Level level, BoilerControllerBlockEntity owner) {
        Map<BlockPos, WeakReference<BoilerControllerBlockEntity>> entries = OWNERS.get(level);
        if (entries != null) entries.entrySet().removeIf(e -> e.getValue().get() == owner || e.getValue().get() == null);
    }

    /** 正常 tick 仅检查有状态的少数关键格，普通壳体通过放置/拆除通知失效。 */
    static boolean specialPartsStillMatch(Level level, Form form) {
        if (!level.getBlockState(form.valve()).is(BoilerContent.SAFETY_VALVE.get())) return false;
        for (BlockPos section : form.sections())
            if (!level.getBlockState(section).is(BoilerContent.HEAT_SECTION.get())) return false;
        for (BlockPos port : form.waterPorts())
            if (!portMatches(level, form.center(), port, true)) return false;
        for (BlockPos port : form.steamPorts())
            if (!portMatches(level, form.center(), port, false)) return false;
        return true;
    }

    private static boolean portMatches(Level level, BlockPos center, BlockPos port, boolean input) {
        BlockState state = level.getBlockState(port);
        Direction outward = port.getX() < center.getX() ? Direction.WEST
                : port.getX() > center.getX() ? Direction.EAST
                : port.getZ() < center.getZ() ? Direction.NORTH : Direction.SOUTH;
        return state.is(input ? BoilerContent.WATER_PORT.get() : BoilerContent.STEAM_PORT.get())
                && state.getValue(BoilerPartBlock.FACING) == outward;
    }

    private static BoilerControllerBlockEntity cached(Level level, BlockPos pos) {
        Map<BlockPos, WeakReference<BoilerControllerBlockEntity>> entries = OWNERS.get(level);
        WeakReference<BoilerControllerBlockEntity> reference = entries == null ? null : entries.get(pos);
        BoilerControllerBlockEntity owner = reference == null ? null : reference.get();
        return owner != null && owner.current() ? owner : null;
    }

    /** 控制器位于下层侧面正中，facing 指向壳外；内部3×3×3必须为空气。 */
    public static Form inspect(Level level, BlockPos controllerPos) {
        if (level == null || !level.hasChunkAt(controllerPos)) return null;
        BlockState controller = level.getBlockState(controllerPos);
        if (!controller.is(BoilerContent.CONTROLLER.get())) return null;
        Direction outward = controller.getValue(BoilerPartBlock.FACING);
        BlockPos center = controllerPos.relative(outward.getOpposite(), 2).below();
        var sections = new ArrayList<BlockPos>();
        var water = new ArrayList<BlockPos>();
        var steam = new ArrayList<BlockPos>();
        int controls = 0, valves = 0;
        BlockPos valvePos = center.above(4);
        for (int y = 0; y < 5; y++) for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            BlockPos pos = center.offset(x, y, z);
            if (!level.hasChunkAt(pos)) return null;
            BlockState state = level.getBlockState(pos);
            if (y >= 1 && y <= 3 && Math.abs(x) <= 1 && Math.abs(z) <= 1) {
                if (!state.isAir()) return null;
                continue;
            }
            if (y == 4 && x == 0 && z == 0) {
                if (!state.is(BoilerContent.SAFETY_VALVE.get())) return null;
                valves++;
                continue;
            }
            if (state.is(BoilerContent.CASING.get())) continue;
            if (y == 0 && Math.abs(x) <= 1 && Math.abs(z) <= 1
                    && state.is(BoilerContent.HEAT_SECTION.get())) {
                sections.add(pos.immutable());
                continue;
            }
            // 棱边含四角只能为外壳；窗口仅占侧面非棱边三层。
            boolean facePanel = (Math.abs(x) == 2) != (Math.abs(z) == 2);
            boolean faceCenter = facePanel && (x == 0 || z == 0);
            Direction face = x < 0 ? Direction.WEST : x > 0 ? Direction.EAST
                    : z < 0 ? Direction.NORTH : Direction.SOUTH;
            if (facePanel && y >= 1 && y <= 3 && state.is(BoilerContent.WINDOW.get())) continue;
            if (faceCenter && y == 1 && state.is(BoilerContent.CONTROLLER.get())
                    && state.getValue(BoilerPartBlock.FACING) == face) {
                if (!pos.equals(controllerPos)) return null;
                controls++;
                continue;
            }
            if (faceCenter && y == 1 && state.is(BoilerContent.WATER_PORT.get())
                    && state.getValue(BoilerPartBlock.FACING) == face) {
                water.add(pos.immutable());
                continue;
            }
            if (faceCenter && y == 3 && state.is(BoilerContent.STEAM_PORT.get())
                    && state.getValue(BoilerPartBlock.FACING) == face) {
                steam.add(pos.immutable());
                continue;
            }
            return null;
        }
        return controls == 1 && valves == 1 && !sections.isEmpty() && !water.isEmpty() && !steam.isEmpty()
                ? new Form(center.immutable(), List.copyOf(sections), List.copyOf(water),
                List.copyOf(steam), valvePos.immutable()) : null;
    }

    /** 扳手请求才逐格找出首个具体故障，坐标供玩家定位；不改变权威结构状态。 */
    public static Issue issue(Level level, BlockPos controllerPos) {
        if (level == null || !level.hasChunkAt(controllerPos)) return new Issue("chunk", controllerPos);
        BlockState controller = level.getBlockState(controllerPos);
        if (!controller.is(BoilerContent.CONTROLLER.get())) return new Issue("controller", controllerPos);
        BlockPos center = controllerPos.relative(controller.getValue(BoilerPartBlock.FACING).getOpposite(), 2).below();
        int sections = 0, water = 0, steam = 0;
        for (int y = 0; y < 5; y++) for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            BlockPos pos = center.offset(x, y, z);
            if (!level.hasChunkAt(pos)) return new Issue("chunk", pos);
            BlockState state = level.getBlockState(pos);
            if (y >= 1 && y <= 3 && Math.abs(x) <= 1 && Math.abs(z) <= 1) {
                if (!state.isAir()) return new Issue("interior", pos);
                continue;
            }
            if (y == 4 && x == 0 && z == 0) {
                if (!state.is(BoilerContent.SAFETY_VALVE.get())) return new Issue("valve", pos);
                continue;
            }
            if (state.is(BoilerContent.CASING.get())) continue;
            if (y == 0 && Math.abs(x) <= 1 && Math.abs(z) <= 1
                    && state.is(BoilerContent.HEAT_SECTION.get())) { sections++; continue; }
            boolean facePanel = (Math.abs(x) == 2) != (Math.abs(z) == 2);
            boolean faceCenter = facePanel && (x == 0 || z == 0);
            if (facePanel && y >= 1 && y <= 3 && state.is(BoilerContent.WINDOW.get())) continue;
            Direction face = x < 0 ? Direction.WEST : x > 0 ? Direction.EAST
                    : z < 0 ? Direction.NORTH : Direction.SOUTH;
            if (faceCenter && y == 1 && state.is(BoilerContent.CONTROLLER.get())
                    && pos.equals(controllerPos) && state.getValue(BoilerPartBlock.FACING) == face) continue;
            if (faceCenter && y == 1 && state.is(BoilerContent.WATER_PORT.get())
                    && state.getValue(BoilerPartBlock.FACING) == face) { water++; continue; }
            if (faceCenter && y == 3 && state.is(BoilerContent.STEAM_PORT.get())
                    && state.getValue(BoilerPartBlock.FACING) == face) { steam++; continue; }
            return new Issue(y == 0 ? "base_shell" : y == 4 ? "top_shell"
                    : !facePanel ? "edge" : "side", pos);
        }
        if (sections == 0) return new Issue("section", center);
        if (water == 0) return new Issue("water", controllerPos);
        if (steam == 0) return new Issue("steam", controllerPos.above(2));
        return new Issue("overlap", controllerPos);
    }

    /** 端口只认外向控制器的有效结构，且须确实出现在该结构的对应位置。 */
    public static BoilerControllerBlockEntity owner(Level level, BlockPos port, boolean input) {
        if (level == null || !level.hasChunkAt(port)) return null;
        BoilerControllerBlockEntity hit = cached(level, port);
        if (hit != null && hit.currentForm() != null && (input ? hit.currentForm().waterPorts()
                : hit.currentForm().steamPorts()).contains(port)) return hit;
        BoilerControllerBlockEntity found = null;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos center = port.relative(side.getOpposite(), 2).below(input ? 1 : 3);
            for (Direction controlSide : Direction.Plane.HORIZONTAL) {
                BlockPos candidate = center.above().relative(controlSide, 2);
                if (!level.hasChunkAt(candidate) || !(level.getBlockEntity(candidate) instanceof BoilerControllerBlockEntity owner)) continue;
                Form form = owner.currentForm();
                if (form != null && (input ? form.waterPorts() : form.steamPorts()).contains(port)) {
                    if (found != null && found != owner) return null;
                    found = owner;
                }
            }
        }
        return found;
    }

    /** 热段下的换热器仅能被一个完整结构认领；歧义结构安全地拒绝供热。 */
    public static BoilerControllerBlockEntity ownerOfSection(Level level, BlockPos section) {
        if (level == null || !level.hasChunkAt(section)) return null;
        BoilerControllerBlockEntity hit = cached(level, section);
        if (hit != null && hit.currentForm() != null && hit.currentForm().sections().contains(section)) return hit;
        BoilerControllerBlockEntity found = null;
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            BlockPos center = section.offset(x, 0, z);
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockPos candidate = center.above().relative(side, 2);
                if (!level.hasChunkAt(candidate) || !(level.getBlockEntity(candidate) instanceof BoilerControllerBlockEntity owner)) continue;
                Form form = owner.currentForm();
                if (form == null || !form.sections().contains(section)) continue;
                if (found != null && found != owner) return null;
                found = owner;
            }
        }
        return found;
    }
}
