package com.iksxh.create_nuclear_industry.structure;

import com.iksxh.create_nuclear_industry.reactor.ReactorServerTick;
import net.minecraft.core.BlockPos;
import java.util.ArrayList;

/** 将同一次已提交 Result 和 CoolantInput 投影为显示信封；不扫描世界或重算模拟。 */
public final class ReactorRuntimeDescriptorFactory {
    private ReactorRuntimeDescriptorFactory() { }

    /** 输入是有效 L1、缓存列映射、本次结算及同次容量；输出完整样本，几何/状态缺口抛出异常。 */
    public static ReactorRuntimeDescriptor project(ReactorSurfaceDescriptor geometry,
            ReactorStructureDefinition.ScanResult scan, ReactorServerTick.Result result,
            ReactorServerTick.CoolantInput coolantInput, long revision, long sample, long serverGameTime) {
        if (geometry == null || !geometry.valid() || scan == null || !scan.valid() || result == null || coolantInput == null)
            throw new IllegalArgumentException("runtime projection requires completed settlement and geometry");
        var columns = new ArrayList<ReactorRuntimeDescriptor.Column>();
        for (var entry : scan.columns().entrySet()) {
            var mapping = entry.getValue();
            BlockPos cap = world(geometry.origin(), mapping.capPosition());
            var bodies = mapping.bodyPositions().stream().map(p -> world(geometry.origin(), p)).toList();
            switch (mapping.type()) {
                case EMPTY -> columns.add(new ReactorRuntimeDescriptor.EmptyColumn(cap, bodies));
                case FUEL -> {
                    var fuel = result.snapshot().fuelColumns().get(entry.getKey());
                    var fission = result.fission().columns().get(entry.getKey());
                    // 既有权威模型省略从未装料列；两阶段同时缺席表示真实空列，不重新运行裂变计算。
                    if ((fuel == null) != (fission == null)) throw new IllegalArgumentException("inconsistent settled fuel column");
                    columns.add(new ReactorRuntimeDescriptor.FuelColumn(cap, bodies,
                            fuel != null && fuel.isEffectiveFuel(), fission == null ? 0 : fission.generatedHeatHu()));
                }
                case CONTROL_ROD -> {
                    var rod = result.snapshot().controlRodColumns().get(entry.getKey());
                    if (rod == null) throw new IllegalArgumentException("missing settled control column");
                    columns.add(new ReactorRuntimeDescriptor.ControlRodColumn(cap, bodies, rod.actualDepth(), rod.targetDepth(), rod.jammed()));
                }
            }
        }
        // 列帽必须与同一 L1 几何的预期 ID 一致，不能把两份缓存错配成表面上有效的样本。
        var capIds = new java.util.HashMap<BlockPos, String>();
        geometry.members().forEach(member -> capIds.put(member.pos(), member.expectedBlockId()));
        if (columns.stream().anyMatch(column -> !column.expectedCapBlockId().equals(capIds.get(column.capPos()))))
            throw new IllegalArgumentException("runtime columns do not match L1 geometry");
        return new ReactorRuntimeDescriptor(geometry.dimension(), geometry.ownerPos(), geometry.ownerGeneration(), revision,
                geometry.revision(), sample, serverGameTime, true, geometry.origin(), geometry.maxInclusive(),
                result.snapshot().coldCoolantMb(), result.snapshot().hotCoolantMb(), coolantInput.coolantCapacityMb(),
                result.coolant().settlement().convertedCoolantMb(), columns);
    }
    private static BlockPos world(BlockPos origin, ReactorStructureDefinition.LocalPosition p) { return origin.offset(p.x(), p.y(), p.z()); }
}
