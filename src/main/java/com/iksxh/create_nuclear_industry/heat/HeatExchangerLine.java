package com.iksxh.create_nuclear_industry.heat;

import com.iksxh.create_nuclear_industry.config.HeatExchangerConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * 服务端水平直列的共享几何；每机仍拥有自己的库存和 HU，几何不持久化。
 * 任一成员无法确认可 tick、边界区块未知或长度超过服务端配置上限时，整列拒绝交易与供热。
 */
final class HeatExchangerLine implements HeatExchangerState.Exchange {
    private final List<NuclearHeatExchangerBlockEntity> members;
    private final Direction facing;
    private final Object identity;
    private final Object[] memberIdentities;
    private final BlockPos before, after;

    private HeatExchangerLine(List<NuclearHeatExchangerBlockEntity> members, Direction facing) {
        this.members = List.copyOf(members);
        this.facing = facing;
        this.identity = members.getFirst().inventoryIdentity();
        this.memberIdentities = new Object[members.size()];
        for (int i = 0; i < members.size(); i++) memberIdentities[i] = members.get(i).inventoryIdentity();
        this.before = members.getFirst().getBlockPos().relative(facing.getOpposite());
        this.after = members.getLast().getBlockPos().relative(facing);
    }

    /** 扫描前后各端，先查区块再查方块实体，不触发区块加载。 */
    static HeatExchangerLine find(NuclearHeatExchangerBlockEntity origin) {
        return find(origin, HeatExchangerConfig.settings(), HeatExchangerConfig.condensationSettings());
    }

    /** 同次查询共享不可变配置快照；缓存只保存几何，不保存模式、罐量、空位或 tick 许可。 */
    static HeatExchangerLine find(NuclearHeatExchangerBlockEntity origin, HeatExchangerState.Settings settings,
            CondensationState.Settings condensation) {
        if (!origin.current() || !origin.canTick() || origin.getLevel().isClientSide || origin.inBoiler()) return null;
        Level level = origin.getLevel();
        Direction facing = origin.getBlockState().getValue(NuclearHeatExchangerBlock.FACING);
        int maxLength = settings.maxLineLength();
        HeatExchangerLine cached = origin.cachedLine();
        if (cached != null && cached.valid(level, facing, maxLength)) {
            cached.applySettings(settings, condensation);
            return cached.validModeLength() ? cached : null;
        }
        origin.cacheLine(null);
        List<NuclearHeatExchangerBlockEntity> members = new ArrayList<>();
        BlockPos cursor = origin.getBlockPos();
        for (int distance = 0; distance <= maxLength; distance++) {
            BlockPos prior = cursor.relative(facing.getOpposite());
            if (!level.hasChunkAt(prior)) return null;
            if (!matches(level, prior, facing)) break;
            cursor = prior;
            if (distance == maxLength) return null;
        }
        for (int distance = 0; distance <= maxLength; distance++) {
            if (!level.hasChunkAt(cursor)) return null;
            if (!matches(level, cursor, facing)) break;
            if (!(level.getBlockEntity(cursor) instanceof NuclearHeatExchangerBlockEntity member)
                    || !member.current() || !member.canTick()) return null;
            members.add(member);
            member.ledger().setSettings(settings);
            member.ledger().setCondensationSettings(condensation);
            if (members.size() > maxLength) return null;
            cursor = cursor.relative(facing);
        }
        if (!level.hasChunkAt(cursor) || members.isEmpty()) return null;
        HeatExchangerLine line = new HeatExchangerLine(members, facing);
        if (!line.validModeLength()) return null;
        for (var member : members) member.cacheLine(line);
        return line;
    }

    /** 两端未知或任一成员身份、方向、归属、原版 tick 门变化即失效；不使用时间窗放宽安全校验。 */
    private boolean valid(Level level, Direction direction, int maxLength) {
        if (facing != direction || members.size() > maxLength) return false;
        for (int i = 0; i < members.size(); i++) {
            var member = members.get(i);
            if (member.getLevel() != level || member.inventoryIdentity() != memberIdentities[i]
                    || !member.current() || !member.canTick() || member.inBoiler()) return false;
            var state = level.getBlockState(member.getBlockPos());
            if (!(state.getBlock() instanceof NuclearHeatExchangerBlock)
                    || state.getValue(NuclearHeatExchangerBlock.FACING) != facing) return false;
        }
        return level.hasChunkAt(before) && level.hasChunkAt(after)
                && !matches(level, before, facing) && !matches(level, after, facing);
    }

    private void applySettings(HeatExchangerState.Settings settings, CondensationState.Settings condensation) {
        for (var member : members) {
            member.ledger().setSettings(settings);
            member.ledger().setCondensationSettings(condensation);
        }
    }

    private boolean validModeLength() {
        if (members.size() <= 16) return true;
        for (var member : members) if (member.ledger().activeMode() == HeatExchangerMode.CONDENSATION) return false;
        return true;
    }

    private static boolean matches(Level level, BlockPos pos, Direction facing) {
        var state = level.getBlockState(pos);
        return state.getBlock() instanceof NuclearHeatExchangerBlock
                && state.getValue(NuclearHeatExchangerBlock.FACING) == facing
                && (!(level.getBlockEntity(pos) instanceof NuclearHeatExchangerBlockEntity machine) || !machine.inBoiler());
    }

    boolean contains(NuclearHeatExchangerBlockEntity machine) { return members.contains(machine); }
    int count() { return members.size(); }
    /** 非空成员必须同工质，空机保留的历史模式不造成假冲突。 */
    HeatExchangerMode mode() {
        HeatExchangerMode selected = HeatExchangerMode.EMPTY;
        for (var member : members) {
            var active = member.ledger().activeMode();
            if (active == HeatExchangerMode.EMPTY) continue;
            if (selected != HeatExchangerMode.EMPTY && selected != active) return null;
            selected = active;
        }
        return selected;
    }
    boolean conflict() { return mode() == null; }
    HeatExchangerMode displayMode() {
        var active = mode();
        return active == null || active == HeatExchangerMode.EMPTY ? members.getFirst().ledger().mode() : active;
    }
    boolean accepts(HeatExchangerMode candidate) {
        var selected = mode();
        if (selected == null || selected != HeatExchangerMode.EMPTY && selected != candidate
                || candidate == HeatExchangerMode.CONDENSATION && members.size() > 16) return false;
        for (var member : members) if (!member.ledger().acceptsMode(candidate)
                || !(candidate == HeatExchangerMode.CONDENSATION ? member.ledger().condensationSettings().valid()
                : member.ledger().settings().valid())) return false;
        return true;
    }
    void adoptMode(HeatExchangerMode candidate) { for (var member : members) member.ledger().selectMode(candidate); }
    int capacity(HeatExchangerMode candidate, boolean input) {
        int sum = 0;
        for (var member : members) sum += member.ledger().capacityFor(candidate, input);
        return sum;
    }
    int hotCapacity() { return capacity(displayMode(), true); }
    int coldCapacity() { return capacity(displayMode(), false); }
    int inputSpace(HeatExchangerMode candidate) {
        int sum = 0;
        for (var member : members) sum += Math.max(0, member.ledger().capacityFor(candidate, true) - member.ledger().hot());
        return sum;
    }
    boolean inlet(NuclearHeatExchangerBlockEntity machine, Direction side) {
        return side == facing.getOpposite() && members.getFirst() == machine;
    }
    boolean outlet(NuclearHeatExchangerBlockEntity machine, Direction side) {
        return side == facing && members.getLast() == machine;
    }
    Object identity() { return identity; }

    /** 同一拓扑的旧句柄仍要逐成员核对身份，避免拆放后作用于新实体。 */
    boolean sameMembers(HeatExchangerLine other) {
        if (other == this) return true;
        if (other == null || facing != other.facing || identity != other.identity || members.size() != other.members.size()) return false;
        for (int i = 0; i < members.size(); i++) if (members.get(i) != other.members.get(i)
                || memberIdentities[i] != other.memberIdentities[i]) return false;
        return true;
    }

    int totalHot() {
        int sum = 0;
        for (var member : members) sum += member.ledger().hot();
        return sum;
    }
    int totalCold() {
        int sum = 0;
        for (var member : members) sum += member.ledger().cold();
        return sum;
    }
    @Override public int hot() { return totalHot(); }
    @Override public int coldSpace() {
        // 各机缩容后的超额存量不抵销另一台真实空位；只能加入容量内仍有空位的成员。
        var mode = displayMode();
        int sum = 0;
        for (var member : members) sum += Math.max(0, member.ledger().capacityFor(mode, false) - member.ledger().cold());
        return sum;
    }

    /** 执行期重新读取各台空位；模拟只返回可接收量，不预约真实库存。 */
    int fillHot(int amount, boolean simulate) {
        return fillInput(amount, HeatExchangerMode.NUCLEAR, simulate);
    }
    int fillInput(int amount, HeatExchangerMode candidate, boolean simulate) {
        if (!accepts(candidate)) return 0;
        int space = inputSpace(candidate);
        int accepted = Math.min(Math.max(0, amount), space);
        if (simulate) return accepted;
        if (accepted == 0) return 0;
        // 配置长度最多64台；位图覆盖实际库存及空成员的模式变化，不分配逐交易容器。
        long changed = 0;
        for (int i = 0; i < members.size(); i++) {
            var ledger = members.get(i).ledger();
            var previous = ledger.mode();
            ledger.selectMode(candidate);
            if (ledger.mode() != previous) changed |= 1L << i;
        }
        int remaining = accepted;
        for (int i = 0; i < members.size(); i++) {
            int filled = members.get(i).ledger().fillInput(remaining, candidate, false);
            if (filled > 0) changed |= 1L << i;
            remaining -= filled;
            if (remaining == 0) break;
        }
        if (accepted != remaining) notifyInventoryChanged(changed);
        return accepted - remaining;
    }

    int drainCold(int amount, boolean simulate) {
        if (conflict()) return 0;
        int taken = Math.min(Math.max(0, amount), totalCold());
        if (simulate) return taken;
        int remaining = taken;
        long changed = 0;
        for (int i = 0; i < members.size(); i++) {
            int drained = members.get(i).ledger().drainCold(remaining, false);
            if (drained > 0) changed |= 1L << i;
            remaining -= drained;
            if (remaining == 0) break;
        }
        if (taken != remaining) notifyInventoryChanged(changed);
        return taken - remaining;
    }

    /**
     * 先用全列实际热量和冷空位定额，再依序从成员热罐扣除、冷罐加入。
     * 服务端单线程提交期间不调用外部回调；两段账本完成后才通知库存变化。
     */
    @Override public int convert(int amount) {
        if (conflict() || displayMode() != HeatExchangerMode.NUCLEAR) return 0;
        int moved = Math.min(Math.max(0, amount), Math.min(totalHot(), coldSpace()));
        return transfer(moved, moved);
    }

    /** 整列汽→水预检后在服务端无回调提交；输入量和产出量分别记账。 */
    @Override public int condense(int input, int output) {
        if (conflict() || displayMode() != HeatExchangerMode.CONDENSATION
                || input < 0 || output < 0 || input > totalHot() || output > coldSpace()) return 0;
        return transfer(input, output);
    }

    private int transfer(int moved, int output) {
        int remaining = moved;
        long changed = 0;
        for (int i = 0; i < members.size(); i++) {
            int drawn = members.get(i).ledger().takeHotForConversion(remaining);
            if (drawn > 0) changed |= 1L << i;
            remaining -= drawn;
            if (remaining == 0) break;
        }
        if (remaining != 0) throw new IllegalStateException("换热直列热液预检与提交不一致");
        remaining = output;
        for (int i = 0; i < members.size(); i++) {
            int added = members.get(i).ledger().putColdFromConversion(remaining);
            if (added > 0) changed |= 1L << i;
            remaining -= added;
            if (remaining == 0) break;
        }
        if (remaining != 0) throw new IllegalStateException("换热直列冷液预检与提交不一致");
        if (moved > 0) notifyInventoryChanged(changed);
        return moved;
    }

    /** 所有成员仍立即刷新整列显示；只有实际持久状态变化才触发原版/辐射库存通知。 */
    private void notifyInventoryChanged(long changed) {
        int hot = totalHot();
        int cold = totalCold();
        for (int i = 0; i < members.size(); i++) {
            var member = members.get(i);
            member.updateLineView(members.size(), hot, cold);
            if ((changed & 1L << i) != 0) member.markInventoryChanged();
        }
    }
}
