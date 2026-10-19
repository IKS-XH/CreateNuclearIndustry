package com.iksxh.create_nuclear_industry.industrial;

import com.iksxh.create_nuclear_industry.config.IndustrialFluidConfig;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

/** 加载边界内的有界广搜；仅专用节点连通，端点不作为桥梁，环路visited去重。 */
final class IndustrialTopology {
    record Endpoint(BlockPos position, Direction side, List<BlockPos> path) {}
    record Side(List<Endpoint> endpoints, Set<BlockPos> nodes, Set<net.minecraft.world.level.ChunkPos> missingChunks, String failure) {
        /** Load事件可能早于FULL晋升；只探测原缺失边界，恢复时再有界重建该路线。 */
        boolean recovered(ServerLevel level) { return missingChunks.stream().anyMatch(chunk -> level.getChunkSource().getChunkNow(chunk.x, chunk.z) != null); }
    }
    private record Visit(BlockPos pos, Direction entry, PathLink path) {}
    private static final Direction[] DIRECTIONS=Direction.values();
    /** BFS前缀按父链接共享，只有端点需要完整路径时物化一次；顺序与原复制列表一致。 */
    private static final class PathLink {
        private final BlockPos node;
        private final PathLink parent;
        private final int size;
        private List<BlockPos> flattened;
        private PathLink(BlockPos node,PathLink parent){this.node=node;this.parent=parent;size=parent==null?1:parent.size+1;}
        private List<BlockPos> flatten(){
            if(flattened==null){BlockPos[] values=new BlockPos[size];PathLink link=this;for(int i=size-1;i>=0;i--){values[i]=link.node;link=link.parent;}flattened=List.of(values);}
            return flattened;
        }
    }
    private record Face(BlockPos pos, Direction side) {}
    private IndustrialTopology() {}

    /** getChunkNow只读已加载表；避免Level便捷读取在轮询中反复续UNKNOWN票据。 */
    static BlockState stateAt(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server) {
            var chunk = server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            return chunk == null ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : chunk.getBlockState(pos);
        }
        return level.getBlockState(pos);
    }
    static net.minecraft.world.level.block.entity.BlockEntity entityAt(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server) {
            var chunk = server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            return chunk == null ? null : chunk.getBlockEntity(pos);
        }
        return level.getBlockEntity(pos);
    }
    static net.neoforged.neoforge.fluids.capability.IFluidHandler capabilityAt(Level level, BlockPos pos, Direction side) {
        return bindingAt(level, pos, side).handler();
    }
    /** 实体身份与罐控制器共同构成交易绑定，不能沿成员缓存访问卸载或已替换的控制器。 */
    record Binding(net.neoforged.neoforge.fluids.capability.IFluidHandler handler,
                   java.util.function.BooleanSupplier valid, boolean reusable,
                   java.util.function.BooleanSupplier reuseGuard) {
        Binding(net.neoforged.neoforge.fluids.capability.IFluidHandler handler,
                java.util.function.BooleanSupplier valid) { this(handler, valid, false); }
        Binding(net.neoforged.neoforge.fluids.capability.IFluidHandler handler,
                java.util.function.BooleanSupplier valid, boolean reusable) {
            this(handler, valid, reusable, reusable ? valid : () -> false);
        }
    }

    /**
     * 仅存活于一次同步调度的正绑定；未知能力和空结果每次重查，不能跨tick或跨泵续用。
     * reusable只由自有汽轮机端口签发，守卫包含成型世代；它不代替端口的实时运行校验。
     */
    static final class DispatchBindings {
        private final java.util.function.BiFunction<BlockPos, Direction, Binding> lookup;
        private final java.util.Map<Face, Binding> entries = new java.util.HashMap<>();

        DispatchBindings(Level level) { this((pos, side) -> bindingAt(level, pos, side)); }
        DispatchBindings(java.util.function.BiFunction<BlockPos, Direction, Binding> lookup) {
            this.lookup = lookup;
        }
        Binding get(BlockPos pos, Direction side) {
            Face key = new Face(pos.immutable(), side);
            Binding previous = entries.get(key);
            if (previous != null && previous.valid().getAsBoolean() && previous.reuseGuard().getAsBoolean()) return previous;
            entries.remove(key);
            Binding current = lookup.apply(pos, side);
            if (current.handler() != null && current.reusable() && current.valid().getAsBoolean()
                    && current.reuseGuard().getAsBoolean())
                entries.put(key, current);
            return current;
        }
    }

    /** 只跳过自有入口必为空的抽取尝试；原端点列表、混输预检和游标不变。 */
    static boolean rejectsExtraction(Level level, Endpoint endpoint) {
        return entityAt(level, endpoint.position()) instanceof com.iksxh.create_nuclear_industry.turbine.TurbinePortBlockEntity
                && stateAt(level, endpoint.position()).is(com.iksxh.create_nuclear_industry.content.TurbineContent.INLET.get());
    }

    /** 自有排汽口从不接收填充；未知设备继续原能力路径，不按库存或瞬时资格作负缓存。 */
    static boolean rejectsInsertion(Level level, Endpoint endpoint) {
        return entityAt(level, endpoint.position()) instanceof com.iksxh.create_nuclear_industry.turbine.TurbinePortBlockEntity
                && stateAt(level, endpoint.position()).is(com.iksxh.create_nuclear_industry.content.TurbineContent.EXHAUST.get());
    }
    static Binding bindingAt(Level level, BlockPos pos, Direction side) {
        Binding absent = new Binding(null, () -> false);
        if (!level.hasChunkAt(pos)) return absent;
        var state = stateAt(level, pos);
        var member = entityAt(level, pos);
        // NF会在hasBlockEntity且传入null时补读Level；缺失实体必须在调用能力API前拒绝。
        if (state.hasBlockEntity() && (member == null || member.isRemoved())) return absent;
        if (member instanceof com.simibubi.create.content.fluids.tank.FluidTankBlockEntity tank) {
            BlockPos controllerPos = tank.getController();
            if (controllerPos == null || !level.hasChunkAt(controllerPos)) return absent;
            var controller = entityAt(level, controllerPos);
            if (!(controller instanceof com.simibubi.create.content.fluids.tank.FluidTankBlockEntity owner)
                    || owner.isRemoved() || !owner.isController() || owner.getType() != tank.getType()) return absent;
            // 查询当前控制器能力避开成员的旧fluidCapability，仍使用Create同份权威库存及标准包装。
            var handler = level.getCapability(Capabilities.FluidHandler.BLOCK, controllerPos, stateAt(level, controllerPos), owner, side);
            return new Binding(handler, () -> level.hasChunkAt(pos) && level.hasChunkAt(controllerPos)
                    && entityAt(level, pos) == tank && !tank.isRemoved() && controllerPos.equals(tank.getController())
                    && entityAt(level, controllerPos) == owner && !owner.isRemoved() && owner.isController());
        }
        var turbine = member instanceof com.iksxh.create_nuclear_industry.turbine.TurbinePortBlockEntity
                ? com.iksxh.create_nuclear_industry.turbine.TurbineStructure.ownerForPart(level, pos) : null;
        var form = turbine == null ? null : turbine.currentForm();
        int portEpoch = turbine == null ? 0 : turbine.portBindingEpoch();
        var handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, state, member, side);
        if (handler != null && turbine != null && form != null && turbine.issuedPortBinding(handler, pos, side)
                && turbine.currentForm() == form && turbine.portBindingEpoch() == portEpoch
                && (state.is(com.iksxh.create_nuclear_industry.content.TurbineContent.INLET.get())
                || state.is(com.iksxh.create_nuclear_industry.content.TurbineContent.EXHAUST.get()))) {
            // 任一身份改变即重新获取能力；红石、票据和内腔仍由每次Port操作实时检查。
            return new Binding(handler, () -> level.hasChunkAt(pos) && entityAt(level, pos) == member
                    && !member.isRemoved(), true, () -> level.hasChunkAt(pos) && stateAt(level, pos) == state
                    && entityAt(level, pos) == member && !member.isRemoved() && !turbine.isRemoved()
                    && turbine.currentForm() == form && turbine.portBindingEpoch() == portEpoch
                    && com.iksxh.create_nuclear_industry.turbine.TurbineStructure.ownerForPart(level, pos) == turbine);
        }
        return new Binding(handler, () -> level.hasChunkAt(pos) && entityAt(level, pos) == member
                && (member == null || !member.isRemoved()));
    }

    /** 机器绑定可能稍后才生效；已知端口须先登记几何端点，每次交易再查实时能力。 */
    static boolean knownEndpoint(Level level, BlockPos pos) {
        var entity = entityAt(level, pos);
        return entity instanceof com.simibubi.create.content.fluids.tank.FluidTankBlockEntity
                || entity instanceof com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity
                || entity instanceof com.iksxh.create_nuclear_industry.boiler.BoilerPortBlockEntity
                || entity instanceof com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlockEntity
                || entity instanceof com.iksxh.create_nuclear_industry.turbine.TurbinePortBlockEntity;
    }

    static boolean connectable(Level level, BlockPos pos, Direction entry) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState state = stateAt(level, pos);
        if (state.getBlock() instanceof IndustrialValveBlock) return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS) == entry.getAxis();
        if (state.getBlock() instanceof IndustrialPipeBlock) return true;
        if (state.getBlock() instanceof IndustrialPumpBlock) return state.getValue(IndustrialPumpBlock.FACING).getAxis() == entry.getAxis();
        return knownEndpoint(level, pos) || capabilityAt(level, pos, entry) != null;
    }
    static Side search(ServerLevel level, IndustrialPumpBlockEntity root, boolean output) {
        Direction start = output ? root.outputDirection() : root.outputDirection().getOpposite();
        return searchFrom(level, root.getBlockPos(), start, output);
    }
    /** 锅炉汽口是主动源而不是节点；必须先接一段专用管/阀，不能据此开放SC邻罐直推。 */
    static Side searchBoiler(ServerLevel level, BlockPos port, Direction outward) {
        if (!(stateAt(level, port.relative(outward)).getBlock() instanceof IndustrialPipeBlock))
            return new Side(List.of(), Set.of(), Set.of(), "blocked");
        return searchFrom(level, port, outward, true);
    }
    private static Side searchFrom(ServerLevel level, BlockPos root, Direction start, boolean output) {
        List<Endpoint> endpoints = new ArrayList<>(); Set<BlockPos> visited = new HashSet<>(); Set<BlockPos> nodes = new HashSet<>();
        Set<Face> endpointFaces = new HashSet<>();
        Set<net.minecraft.world.level.ChunkPos> missingChunks = new HashSet<>();
        Queue<Visit> queue = new ArrayDeque<>();
        queue.add(new Visit(root.relative(start), start.getOpposite(), null));
        String failure = "blocked";
        while (!queue.isEmpty()) {
            Visit visit = queue.remove(); BlockPos pos = visit.pos();
            if (pos.equals(root)) continue;
            if (!level.hasChunkAt(pos)) { missingChunks.add(new net.minecraft.world.level.ChunkPos(pos)); failure = "unloaded"; continue; }
            BlockState state = stateAt(level, pos);
            if (!(state.getBlock() instanceof IndustrialPipeBlock) && !(state.getBlock() instanceof IndustrialPumpBlock)) {
                if (endpointFaces.add(new Face(pos.immutable(), visit.entry())) && (knownEndpoint(level, pos) || capabilityAt(level, pos, visit.entry()) != null))
                    endpoints.add(new Endpoint(pos.immutable(), visit.entry(), visit.path()==null?List.of():visit.path().flatten()));
                continue;
            }
            // 有方向节点先验证入口，再去重；非法侧向触达不能吞掉稍后的合法轴向绕行。
            if (state.getBlock() instanceof IndustrialValveBlock && visit.entry().getAxis() != state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS)) continue;
            if (state.getBlock() instanceof IndustrialPumpBlock) {
                if (!(entityAt(level, pos) instanceof IndustrialPumpBlockEntity member) || member.flowLimit() == 0) continue;
                Direction legalEntry = output ? member.outputDirection().getOpposite() : member.outputDirection();
                if (visit.entry() != legalEntry) continue;
            }
            if (!visited.add(pos)) continue;
            nodes.add(pos.immutable());
            if (nodes.size() > IndustrialFluidConfig.MAX_NODES.get()) return new Side(List.of(), nodes, Set.copyOf(missingChunks), "too_large");
            PathLink path=new PathLink(pos.immutable(),visit.path());
            for (Direction direction : DIRECTIONS) {
                if (direction == visit.entry()) continue;
                if (state.getBlock() instanceof IndustrialValveBlock && (direction.getAxis() != state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS)
                        || visit.entry().getAxis() != direction.getAxis())) continue;
                if (state.getBlock() instanceof IndustrialPumpBlock) {
                    if (!(entityAt(level, pos) instanceof IndustrialPumpBlockEntity pump) || pump.flowLimit() == 0) continue;
                    Direction expectedEntry = output ? pump.outputDirection().getOpposite() : pump.outputDirection();
                    Direction expectedExit = expectedEntry.getOpposite();
                    if (visit.entry() != expectedEntry || direction != expectedExit) continue;
                }
                queue.add(new Visit(pos.relative(direction), direction.getOpposite(), path));
            }
        }
        endpoints.sort(Comparator.comparingLong((Endpoint endpoint) -> endpoint.position().asLong()).thenComparing(endpoint -> endpoint.side().ordinal()));
        return new Side(List.copyOf(endpoints), Set.copyOf(nodes), Set.copyOf(missingChunks), endpoints.isEmpty() ? failure : "ready");
    }
    /** 原/回退路径同样区分无额度普通管；阀和泵仍在原位置读取动态限额。 */
    static int prepareLimit(ServerLevel level,BlockPos pos,IndustrialFlowBudget.Path path) {
        var block=stateAt(level,pos).getBlock();
        int capacity=limit(level,pos);
        // 未知子类若重写为有限上限，保守回退仍须尊重它，不能只凭继承关系免除额度。
        if(block instanceof IndustrialPipeBlock&&!(block instanceof IndustrialValveBlock)&&capacity==Integer.MAX_VALUE){path.addUnbounded(pos.asLong());return capacity;}
        path.add(pos.asLong(),capacity);return capacity;
    }
    static int limit(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return 0;
        var state = stateAt(level, pos);
        if (state.getBlock() instanceof IndustrialValveBlock)
            return entityAt(level, pos) instanceof IndustrialValveBlockEntity valve ? valve.effectiveLimit() : 0;
        if (state.getBlock() instanceof IndustrialPipeBlock pipe) return pipe.flowLimit();
        if (state.getBlock() instanceof IndustrialPumpBlock)
            return entityAt(level, pos) instanceof IndustrialPumpBlockEntity pump ? pump.flowLimit() : 0;
        return 0;
    }
}
