package com.iksxh.create_nuclear_industry.structure;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyState;
import com.iksxh.create_nuclear_industry.reactor.ReactorFullShutdownResult;
import com.iksxh.create_nuclear_industry.reactor.ReactorInstrumentTelemetry;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;

/**
 * 反应堆方块破坏事件的只读预检结果。
 *
 * <p>预检计划保存原子提交和回滚所需的服务端快照、端口物品及运行时遥测副本；它不
 * 修改世界，也不代表事件已经放行。计划中的 {@link PortCapture} 返回物品副本，调用方
 * 不能借计划对象改写端口所有权。</p>
 */
public record ReactorDisassemblyPlan(
        Action action,
        ReactorInstrumentPortBlockEntity owner,
        BlockPos brokenPos,
        BlockState brokenState,
        String expectedComponentId,
        BlockPos structureOrigin,
        ReactorSnapshot beforeSnapshot,
        ReactorSnapshot projectedSnapshot,
        ReactorSnapshot nextSnapshot,
        ReactorFullShutdownResult shutdownAssessment,
        List<PortCapture> fuelPorts,
        Map<CoreColumnPosition, FuelAssemblyState> pendingLegacyFuelAssemblies,
        ReactorInstrumentTelemetry beforeTelemetry,
        ReactorInstrumentTelemetry beforeLastSentTelemetry,
        int beforeTelemetryTicksSinceLastSync,
        boolean publishDangerousEvent
) {
    public ReactorDisassemblyPlan {
        if (action == null || owner == null || brokenPos == null || brokenState == null
                || expectedComponentId == null || beforeSnapshot == null
                || projectedSnapshot == null || nextSnapshot == null
                || shutdownAssessment == null || fuelPorts == null
                || pendingLegacyFuelAssemblies == null || beforeTelemetry == null
                || beforeLastSentTelemetry == null || beforeTelemetryTicksSinceLastSync < 0) {
            throw new IllegalArgumentException("拆除预检计划字段不能为空");
        }
        brokenPos = brokenPos.immutable();
        structureOrigin = structureOrigin == null ? null : structureOrigin.immutable();
        fuelPorts = List.copyOf(fuelPorts);
        pendingLegacyFuelAssemblies = Map.copyOf(pendingLegacyFuelAssemblies);
    }

    /** 破坏事件对该有效所有者采取的动作。 */
    public enum Action {
        /** 危险状态只提交既有融毁事件占位，不清空运行时状态。 */
        DANGEROUS,
        /** 完全停机时破坏仪表端口本身，只允许原方块拆除。 */
        INSTRUMENT_MAINTENANCE,
        /** 完全停机时清空燃料和全部反应堆运行时状态。 */
        FULL_SHUTDOWN_RESET,
        /** 有效所有者存在，但安全门或端口预检失败。 */
        REJECTED
    }

    /** 单个燃料列换料端口在事务读阶段的实体和精确物品快照。 */
    public static final class PortCapture {
        private final CoreColumnPosition column;
        private final ReactorPortBlockEntity port;
        private final ItemStack fuelAssembly;

        public PortCapture(
                CoreColumnPosition column,
                ReactorPortBlockEntity port,
                ItemStack fuelAssembly
        ) {
            if (column == null || port == null || fuelAssembly == null) {
                throw new IllegalArgumentException("端口快照字段不能为空");
            }
            this.column = column;
            this.port = port;
            this.fuelAssembly = fuelAssembly.copy();
        }

        public CoreColumnPosition column() {
            return column;
        }

        public ReactorPortBlockEntity port() {
            return port;
        }

        /** 返回含全部数据组件的独立物品副本。 */
        public ItemStack fuelAssembly() {
            return fuelAssembly.copy();
        }
    }
}
