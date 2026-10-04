package com.iksxh.create_nuclear_industry.boiler;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * 服务端把已成型汽口作为 Create 原生管路的压力源，不保存流体或改写外部泵。
 * 每个已接管汽口提供512压力，对应Create实测的256mB/t；单口内部的分支按拓扑均分压力。
 */
final class BoilerSteamPressure {
    private record Node(BlockPos pos, Direction entry, int distance, float pressure) {}
    private final long owner;
    private Map<PipeConnection, float[]> applied = new IdentityHashMap<>();

    BoilerSteamPressure(BlockPos controller) { owner = controller.asLong(); }

    /** 每 tick 按现有拓扑设置自有贡献；Create wipe 后记录清零，下个 tick 自动补上。 */
    void refresh(Level level, BoilerStructure.Form form, boolean hasSteam) {
        if (level == null || level.isClientSide || form == null || !hasSteam) {
            release();
            return;
        }
        List<BlockPos> connected = new ArrayList<>();
        for (BlockPos port : form.steamPorts()) {
            Direction outward = level.getBlockState(port).getValue(BoilerPartBlock.FACING);
            BlockPos first = port.relative(outward);
            if (!level.hasChunkAt(first)) continue;
            FluidTransportBehaviour pipe = FluidPropagator.getPipe(level, first);
            if (pipe != null && !(level.getBlockEntity(first) instanceof PumpBlockEntity)
                    && pipe.getConnection(outward.getOpposite()) != null) connected.add(port);
        }
        Map<PipeConnection, float[]> next = new IdentityHashMap<>();
        for (BlockPos port : connected) {
            Direction outward = level.getBlockState(port).getValue(BoilerPartBlock.FACING);
            spread(level, port.relative(outward), outward.getOpposite(), BoilerState.FLOW_LIMIT * 2f, next);
        }
        for (PipeConnection old : applied.keySet()) if (!next.containsKey(old)) set(old, 0, 0);
        for (Map.Entry<PipeConnection, float[]> entry : next.entrySet()) {
            float[] values = entry.getValue();
            set(entry.getKey(), values[0], values[1]);
        }
        applied = next;
    }

    private void spread(Level level, BlockPos start, Direction entry, float initial,
                        Map<PipeConnection, float[]> desired) {
        int range = Math.max(1, FluidPropagator.getPumpRange());
        var pending = new ArrayDeque<Node>();
        Set<BlockPos> visited = new HashSet<>();
        pending.add(new Node(start, entry, 1, initial));
        while (!pending.isEmpty()) {
            Node node = pending.removeFirst();
            if (!level.hasChunkAt(node.pos()) || !visited.add(node.pos())) continue;
            FluidTransportBehaviour pipe = FluidPropagator.getPipe(level, node.pos());
            if (pipe == null || level.getBlockEntity(node.pos()) instanceof PumpBlockEntity
                    || pipe.getConnection(node.entry()) == null) continue;
            add(desired, pipe.getConnection(node.entry()), true, node.pressure());
            List<Direction> outlets = new ArrayList<>();
            for (Direction side : FluidPropagator.getPipeConnections(level.getBlockState(node.pos()), pipe)) {
                if (side == node.entry() || pipe.getConnection(side) == null) continue;
                BlockPos neighbor = node.pos().relative(side);
                if (!level.hasChunkAt(neighbor)) continue;
                FluidTransportBehaviour next = FluidPropagator.getPipe(level, neighbor);
                if (next != null && !(level.getBlockEntity(neighbor) instanceof PumpBlockEntity)
                        && next.getConnection(side.getOpposite()) == null) continue;
                if (next != null && visited.contains(neighbor)) continue;
                outlets.add(side);
            }
            float branch = outlets.isEmpty() ? 0 : node.pressure() / outlets.size();
            for (Direction side : outlets) {
                add(desired, pipe.getConnection(side), false, branch);
                BlockPos neighbor = node.pos().relative(side);
                FluidTransportBehaviour next = FluidPropagator.getPipe(level, neighbor);
                if (next != null && !(level.getBlockEntity(neighbor) instanceof PumpBlockEntity)
                        && node.distance() < range)
                    pending.addLast(new Node(neighbor, side.getOpposite(), node.distance() + 1, branch));
            }
        }
    }

    private static void add(Map<PipeConnection, float[]> desired, PipeConnection connection,
                            boolean inbound, float pressure) {
        desired.computeIfAbsent(connection, ignored -> new float[2])[inbound ? 0 : 1] += pressure;
    }

    private void set(PipeConnection connection, float inbound, float outward) {
        BoilerPressureConnection pressure = (BoilerPressureConnection) connection;
        pressure.createNuclearIndustry$setBoilerPressure(owner, true, inbound);
        pressure.createNuclearIndustry$setBoilerPressure(owner, false, outward);
    }

    /** 结构拆除、控制器卸载及主动输出关闭时只撤销本控制器贡献。 */
    void release() {
        for (PipeConnection connection : applied.keySet()) set(connection, 0, 0);
        applied.clear();
    }
}
