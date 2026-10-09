package com.iksxh.create_nuclear_industry.structure.client;

import com.iksxh.create_nuclear_industry.structure.ReactorRuntimeDescriptor;
import net.minecraft.core.BlockPos;
import java.util.Map;
import java.util.Optional;

/** 一次绘制使用的不可变运行索引；不持有 Level、BE、NBT 或可变集合。 */
public final class ReactorRuntimeSnapshot {
    private static final ReactorRuntimeSnapshot EMPTY = new ReactorRuntimeSnapshot(Map.of(), Map.of());
    private final Map<BlockPos, ReactorRuntimeDescriptor> owners;
    private final Map<BlockPos, ReactorRuntimeDescriptor.ControlRodColumn> rods;
    private final Map<BlockPos, ReactorRuntimeDescriptor> controlOwners;
    ReactorRuntimeSnapshot(Map<BlockPos, ReactorRuntimeDescriptor> owners,
            Map<BlockPos, ReactorRuntimeDescriptor.ControlRodColumn> rods) {
        this.owners = Map.copyOf(owners); this.rods = Map.copyOf(rods);
        var indexed = new java.util.HashMap<BlockPos, ReactorRuntimeDescriptor>();
        for (var descriptor : this.owners.values()) for (var column : descriptor.columns())
            if (column instanceof ReactorRuntimeDescriptor.ControlRodColumn rod && rod.equals(this.rods.get(rod.capPos())))
                indexed.put(rod.capPos(), descriptor);
        this.controlOwners = Map.copyOf(indexed);
    }
    public static ReactorRuntimeSnapshot empty() { return EMPTY; }
    /** 只返回可靠且租约有效的完整 owner；消费者核对当次本地预期 ID 后再绘制。 */
    public Optional<ReactorRuntimeDescriptor> findOwner(BlockPos ownerPos) { return Optional.ofNullable(owners.get(ownerPos)); }
    /** cap 世界坐标对应实际控制棒列，深度和行程来自同一次结算。 */
    public Optional<ReactorRuntimeDescriptor.ControlRodColumn> findControlRod(BlockPos capPos) { return Optional.ofNullable(rods.get(capPos)); }
    /** 同份快照内 cap 的完整 owner 身份，供控制棒绘制按 generation/geometryRevision 隔离插值。 */
    public Optional<ReactorRuntimeDescriptor> findControlOwner(BlockPos capPos) { return Optional.ofNullable(controlOwners.get(capPos)); }
}
