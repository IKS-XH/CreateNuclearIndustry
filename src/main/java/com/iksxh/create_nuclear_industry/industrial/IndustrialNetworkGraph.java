package com.iksxh.create_nuclear_industry.industrial;

import java.util.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import com.iksxh.create_nuclear_industry.config.IndustrialFluidConfig;
import com.iksxh.create_nuclear_industry.content.IndustrialFluidContent;
import com.iksxh.create_nuclear_industry.turbine.TurbineBlockStateRevision;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

/** 服务端已加载物理管网图；只共享静态连接与无分支边，不持库存、热量或交易许可。 */
final class IndustrialNetworkGraph {
    private static final Direction[] DIRECTIONS=Direction.values();
    /** kind为0纯管、1阀、2泵；axis仅约束阀/泵物理轴，tier为两档静态容量。 */
    record Cell(BlockPos pos,int kind,Direction.Axis axis,int tier) {
        boolean permits(Direction direction){return kind==0||direction.getAxis()==axis;}
    }
    record Face(BlockPos pos,Direction side) {}
    private record Visit(BlockPos pos,Direction entry,Link path) {}
    private static final class Link {
        final BlockPos pos;final Link previous;final int size;List<BlockPos> flattened;
        Link(BlockPos pos,Link previous){this.pos=pos;this.previous=previous;size=previous==null?1:previous.size+1;}
        List<BlockPos> flatten(){if(flattened==null){BlockPos[] a=new BlockPos[size];Link p=this;for(int i=size-1;i>=0;i--){a[i]=p.pos;p=p.previous;}flattened=List.of(a);}return flattened;}
    }
    /** 与原FIFO规则相同的内存图搜索；无距离封顶，仍在接受第maxNodes+1节点时拒绝。 */
    static final class Model {
        final Map<BlockPos,Cell> nodes;
        final Set<Face> endpoints;
        final Map<BlockPos,IndustrialFlowBudget.Group> groups=new HashMap<>();
        Model(Map<BlockPos,Cell> nodes,Set<Face> endpoints){this.nodes=Map.copyOf(nodes);this.endpoints=Set.copyOf(endpoints);compress();}
        private List<BlockPos> adjacent(Cell cell) {
            var result=new ArrayList<BlockPos>();
            for(Direction d:DIRECTIONS){var next=nodes.get(cell.pos.relative(d));if(next!=null&&cell.permits(d)&&next.permits(d.getOpposite()))result.add(next.pos);}
            return result;
        }
        private boolean terminal(Cell cell) {
            if(cell.kind!=0||adjacent(cell).size()!=2)return true;
            for(Direction d:DIRECTIONS)if(endpoints.contains(new Face(cell.pos.relative(d),d.getOpposite())))return true;
            return false;
        }
        private void group(List<BlockPos> members) {
            long[] positions=new long[members.size()];byte[] tiers=new byte[members.size()];
            for(int i=0;i<members.size();i++){positions[i]=members.get(i).asLong();tiers[i]=(byte)nodes.get(members.get(i)).tier;}
            var group=new IndustrialFlowBudget.Group(positions,tiers);for(var pos:members)groups.put(pos,group);
        }
        private void compress() {
            var critical=new HashSet<BlockPos>();for(var cell:nodes.values())if(terminal(cell))critical.add(cell.pos);
            for(var cell:nodes.values())if(cell.kind==0&&critical.contains(cell.pos))group(List.of(cell.pos));
            var ordered=new ArrayList<>(nodes.keySet());ordered.sort(Comparator.comparingLong(BlockPos::asLong));
            for(var pos:ordered) {
                if(nodes.get(pos).kind!=0||groups.containsKey(pos))continue;
                var members=new ArrayList<BlockPos>();var queue=new ArrayDeque<BlockPos>();var visited=new HashSet<BlockPos>();queue.add(pos);
                while(!queue.isEmpty()){var p=queue.remove();if(!visited.add(p))continue;members.add(p);for(var next:adjacent(nodes.get(p)))if(!critical.contains(next)&&!groups.containsKey(next))queue.add(next);}
                group(members);
            }
        }
        IndustrialTopology.Side search(BlockPos root,Direction start,boolean output,Map<BlockPos,Direction> pumps,int maxNodes) {
            var queue=new ArrayDeque<Visit>();queue.add(new Visit(root.relative(start),start.getOpposite(),null));
            var visited=new HashSet<BlockPos>();var found=new ArrayList<IndustrialTopology.Endpoint>();var faces=new HashSet<Face>();
            while(!queue.isEmpty()) {
                var v=queue.remove();if(v.pos.equals(root))continue;var cell=nodes.get(v.pos);
                if(cell==null){var face=new Face(v.pos,v.entry);if(endpoints.contains(face)&&faces.add(face))found.add(new IndustrialTopology.Endpoint(v.pos,v.entry,v.path==null?List.of():v.path.flatten()));continue;}
                if(!cell.permits(v.entry))continue;
                if(cell.kind==2){var direction=pumps.get(v.pos);if(direction==null||v.entry!=(output?direction.getOpposite():direction))continue;}
                if(!visited.add(v.pos))continue;
                if(visited.size()>maxNodes)return new IndustrialTopology.Side(List.of(),visited,Set.of(),"too_large");
                var path=new Link(v.pos,v.path);
                for(Direction d:DIRECTIONS){if(d==v.entry||!cell.permits(d))continue;if(cell.kind==2&&d!=(output?pumps.get(v.pos):pumps.get(v.pos).getOpposite()))continue;queue.add(new Visit(v.pos.relative(d),d.getOpposite(),path));}
            }
            found.sort(Comparator.comparingLong((IndustrialTopology.Endpoint e)->e.position().asLong()).thenComparing(e->e.side().ordinal()));
            return new IndustrialTopology.Side(List.copyOf(found),Set.copyOf(visited),Set.of(),found.isEmpty()?"blocked":"ready");
        }
    }

    private record SectionKey(int x,int z,int index) {}
    /** 实际容器身份及写前后版本；弱区块见证不延长世界或区块寿命。 */
    private record Watch(SectionKey key,WeakReference<LevelChunk> chunk,LevelChunkSection section,Object palette,long revision) {
        boolean matches(ServerLevel level) {
            var current=level.getChunkSource().getChunkNow(key.x,key.z);
            if(current==null||current!=chunk.get())return false;var all=current.getSections();
            if(key.index<0||key.index>=all.length||all[key.index]!=section||section.getStates()!=palette||!(palette instanceof TurbineBlockStateRevision state))return false;
            return !state.turbineWriting()&&state.turbineRevision()==revision&&!state.turbineWriting();
        }
    }
    private record Provider(Object block,Object list) {}
    private static final Field PROVIDERS=providerField();
    private static Field providerField(){try{Field f=net.neoforged.neoforge.capabilities.BlockCapability.class.getDeclaredField("providers");return f.trySetAccessible()?f:null;}catch(ReflectiveOperationException|RuntimeException missing){return null;}}
    private static Map<?,?> providers(){if(PROVIDERS==null)return null;try{Object v=PROVIDERS.get(Capabilities.FluidHandler.BLOCK);return v instanceof Map<?,?> m?m:null;}catch(ReflectiveOperationException|RuntimeException missing){return null;}}
    private static boolean noProvider(Object value){return value==null||value instanceof List<?> list&&list.isEmpty();}
    private record Request(BlockPos root,Direction direction,boolean output) {}
    record Route(IndustrialTopology.Side side,Snapshot graph) {}
    static Map<BlockPos,Integer> order(Set<BlockPos> nodes){var result=new HashMap<BlockPos,Integer>();int index=0;for(var pos:nodes)result.put(pos,index++);return Map.copyOf(result);}

    /** 每个调度世界独占图索引；全局通知仍由原revision撤销许可，这里不签发许可。 */
    static final class Registry {
        final IndustrialFlowBudget budget;
        final Map<BlockPos,FluidStack> media;
        private final Map<IndustrialFlowBudget.Group,FluidStack> groupMedia=new IdentityHashMap<>();
        private final Map<IndustrialFlowBudget.Group,Medium> checkedMedia=new IdentityHashMap<>();
        final Map<BlockPos,Snapshot> membership=new HashMap<>();
        final Set<Snapshot> snapshots=Collections.newSetFromMap(new IdentityHashMap<>());
        long serial,builds,reuses,fallbacks,graphSearches;
        private record Medium(FluidStack fluid,boolean mixed) {}
        Registry(IndustrialFlowBudget budget){this(budget,new HashMap<>());}
        Registry(IndustrialFlowBudget budget,Map<BlockPos,FluidStack> media){this.budget=budget;this.media=media;}
        void beginTick(){groupMedia.clear();checkedMedia.clear();}
        void flushMedia(){for(var entry:groupMedia.entrySet())for(long node:entry.getKey().nodes)media.put(BlockPos.of(node),entry.getValue());groupMedia.clear();checkedMedia.clear();}
        boolean mediumMatches(IndustrialFlowBudget.Group group,FluidStack fluid) {
            var whole=groupMedia.get(group);if(whole!=null)return FluidStack.isSameFluidSameComponents(whole,fluid);
            var checked=checkedMedia.get(group);
            if(checked==null){FluidStack selected=FluidStack.EMPTY;boolean mixed=false;for(long node:group.nodes){var old=media.get(BlockPos.of(node));if(old!=null){if(!selected.isEmpty()&&!FluidStack.isSameFluidSameComponents(selected,old))mixed=true;selected=old;}}checked=new Medium(selected,mixed);checkedMedia.put(group,checked);}
            return !checked.mixed&&(checked.fluid.isEmpty()||FluidStack.isSameFluidSameComponents(checked.fluid,fluid));
        }
        void markRaw(BlockPos pos,FluidStack fluid){var graph=membership.get(pos);if(graph!=null&&groupMedia.containsKey(graph.model.groups.get(pos)))flushMedia();media.put(pos,fluid.copyWithAmount(1));checkedMedia.clear();}
        void ensureRaw(BlockPos pos){var graph=membership.get(pos);if(graph!=null&&groupMedia.containsKey(graph.model.groups.get(pos)))flushMedia();}
        void mark(IndustrialFlowBudget.Group group,long key,FluidStack fluid){if(group==null){markRaw(BlockPos.of(key),fluid);return;}if(!group.live){flushMedia();for(long node:group.nodes)markRaw(BlockPos.of(node),fluid);return;}if(!groupMedia.containsKey(group))groupMedia.put(group,fluid.copyWithAmount(1));}
        void notified(){serial++;}
        void clear(){for(var graph:new ArrayList<>(snapshots))retire(graph);serial++;}
        void unload(net.minecraft.world.level.ChunkPos chunk){for(var graph:new ArrayList<>(snapshots))if(graph.chunks.contains(chunk.toLong()))retire(graph);serial++;}
        void retire(Snapshot graph){if(!graph.live)return;budget.flushGroups();flushMedia();graph.live=false;for(var group:new HashSet<>(graph.model.groups.values()))group.live=false;for(var pos:graph.model.nodes.keySet())membership.remove(pos,graph);snapshots.remove(graph);}
        Route query(ServerLevel level,BlockPos root,Direction direction,boolean output,boolean boiler) {
            BlockPos seed=boiler?root.relative(direction):root;
            if(boiler&&!(IndustrialTopology.stateAt(level,seed).getBlock() instanceof IndustrialPipeBlock))return null;
            Snapshot graph=membership.get(seed);
            if(graph!=null&&!graph.matches(level)){retire(graph);graph=null;}
            if(graph==null){graph=new Builder(level,this).build(seed);if(graph==null){fallbacks++;return null;}for(var old:new HashSet<>(membership.values())){boolean overlaps=false;for(var pos:graph.model.nodes.keySet())if(old.model.nodes.containsKey(pos)){overlaps=true;break;}if(overlaps)retire(old);}snapshots.add(graph);for(var pos:graph.model.nodes.keySet())membership.put(pos,graph);builds++;}
            else reuses++;
            if(!graph.refreshPumps(level)){retire(graph);fallbacks++;return null;}
            var key=new Request(root.immutable(),direction,output);var side=graph.routes.get(key);
            if(side==null){side=graph.model.search(root,direction,output,graph.pumps,IndustrialFluidConfig.MAX_NODES.get());graph.routes.put(key,side);graphSearches++;}
            return new Route(side,graph);
        }
    }
    /** 一份不可变物理图及可撤销的静态见证；路径仅按实际访问的根/方向生成，不预建端点平方表。 */
    static final class Snapshot {
        final Registry owner;final Model model;final Map<SectionKey,Watch> watches;
        final it.unimi.dsi.fastutil.longs.LongOpenHashSet reads,chunks;
        final Map<BlockPos,WeakReference<Object>> entities;final List<Provider> providerStamps;
        final Map<Request,IndustrialTopology.Side> routes=new HashMap<>();
        final Map<List<BlockPos>,IndustrialRoutePlan> plans=new IdentityHashMap<>();
        final Map<IndustrialTopology.Side,Map<BlockPos,Integer>> pumpOrders=new IdentityHashMap<>();
        final List<BlockPos> pumpNodes;
        final Map<IndustrialFlowBudget.Group,List<Watch>> groupWatches=new IdentityHashMap<>();
        Map<BlockPos,Direction> pumps=Map.of();long checkedEntities=Long.MIN_VALUE;boolean live=true;
        Snapshot(Builder b){owner=b.owner;model=new Model(b.nodes,b.endpoints);watches=Map.copyOf(b.watches);reads=b.reads;chunks=b.chunks;entities=Map.copyOf(b.entities);providerStamps=List.copyOf(b.providers.values());pumpNodes=model.nodes.values().stream().filter(cell->cell.kind==2).map(Cell::pos).toList();
            for(var group:new HashSet<>(model.groups.values())){var ws=new LinkedHashSet<Watch>();for(long p:group.nodes){var pos=BlockPos.of(p);ws.add(watches.get(new SectionKey(pos.getX()>>4,pos.getZ()>>4,b.level.getSectionIndex(pos.getY()))));}groupWatches.put(group,List.copyOf(ws));}
        }
        boolean staticValid(ServerLevel level,IndustrialFlowBudget.Group group){if(!live||!group.live)return false;for(var watch:groupWatches.get(group))if(!watch.matches(level))return false;return true;}
        IndustrialRoutePlan plan(List<BlockPos> path){return live?plans.computeIfAbsent(path,ignored->IndustrialRoutePlan.compile(this,path)):null;}
        /** 从实际区块实体表枚举关键泵，仍核当前位置并保留原节点迭代顺序；不逐管读取世界。 */
        List<BlockPos> pumpCandidates(ServerLevel level,Set<BlockPos> scope,Map<BlockPos,Integer> order) {
            var result=new ArrayList<BlockPos>();
            for(long chunkKey:chunks){var chunk=level.getChunkSource().getChunkNow(net.minecraft.world.level.ChunkPos.getX(chunkKey),net.minecraft.world.level.ChunkPos.getZ(chunkKey));if(chunk==null)continue;for(var entry:chunk.getBlockEntities().entrySet())if(entry.getValue() instanceof IndustrialPumpBlockEntity&&scope.contains(entry.getKey()))result.add(entry.getKey());}
            result.sort(Comparator.comparingInt(order::get));return result;
        }
        List<BlockPos> steamPumps(ServerLevel level,IndustrialTopology.Side side){return pumpCandidates(level,side.nodes(),pumpOrders.computeIfAbsent(side,ignored->order(side.nodes())));}
        private boolean registrationsMatch(){var registry=providers();if(registry==null)return false;for(var stamp:providerStamps){Object current=registry.get(stamp.block);if(current!=stamp.list||!noProvider(current))return false;}return true;}
        private boolean entitiesMatch(ServerLevel level) {
            int seen=0;
            for(long chunkKey:chunks){var chunk=level.getChunkSource().getChunkNow(net.minecraft.world.level.ChunkPos.getX(chunkKey),net.minecraft.world.level.ChunkPos.getZ(chunkKey));if(chunk==null)return false;
                for(var entry:chunk.getBlockEntities().entrySet())if(reads.contains(entry.getKey().asLong())){var old=entities.get(entry.getKey());if(old==null||old.get()!=entry.getValue())return false;seen++;}}
            return seen==entities.size();
        }
        boolean matches(ServerLevel level) {
            if(!live||!registrationsMatch())return false;for(var watch:watches.values())if(!watch.matches(level))return false;
            if(checkedEntities!=owner.serial){if(!entitiesMatch(level))return false;checkedEntities=owner.serial;}
            return registrationsMatch();
        }
        boolean refreshPumps(ServerLevel level) {
            Map<BlockPos,Direction> next=null;
            for(var pos:pumpNodes){var entity=IndustrialTopology.entityAt(level,pos);Direction direction=entity instanceof IndustrialPumpBlockEntity pump&&pump.flowLimit()>0?pump.outputDirection():null;
                // 直接改BE状态可能不推进palette；实体方向与物理图轴不一致时不能按旧轴裁剪连通图。
                if(direction!=null&&direction.getAxis()!=model.nodes.get(pos).axis)return false;
                if(!Objects.equals(pumps.get(pos),direction)&&next==null)next=new HashMap<>(pumps);if(next!=null){if(direction==null)next.remove(pos);else next.put(pos,direction);}}
            if(next!=null){pumps=Map.copyOf(next);routes.clear();plans.clear();pumpOrders.clear();}
            return true;
        }
    }
    private static final class Builder {
        final ServerLevel level;final Registry owner;
        final Map<BlockPos,Cell> nodes=new LinkedHashMap<>();final Set<Face> endpoints=new HashSet<>();
        final Map<SectionKey,Watch> watches=new LinkedHashMap<>();final Map<BlockPos,WeakReference<Object>> entities=new HashMap<>();
        final it.unimi.dsi.fastutil.longs.LongOpenHashSet reads=new it.unimi.dsi.fastutil.longs.LongOpenHashSet(),chunks=new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        final Map<Object,Provider> providers=new IdentityHashMap<>();boolean safe=true;
        Builder(ServerLevel level,Registry owner){this.level=level;this.owner=owner;}
        BlockState observe(BlockPos pos) {
            var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);if(chunk==null){safe=false;return null;}
            var key=new SectionKey(pos.getX()>>4,pos.getZ()>>4,level.getSectionIndex(pos.getY()));var all=chunk.getSections();
            if(key.index<0||key.index>=all.length){safe=false;return null;}var section=all[key.index];Object palette=section.getStates();
            if(!(palette instanceof TurbineBlockStateRevision stamp)||stamp.turbineWriting()){safe=false;return null;}
            var prior=watches.putIfAbsent(key,new Watch(key,new WeakReference<>(chunk),section,palette,stamp.turbineRevision()));
            if(prior!=null&&!prior.matches(level)){safe=false;return null;}
            boolean fresh=reads.add(pos.asLong());chunks.add(new net.minecraft.world.level.ChunkPos(pos).toLong());
            var entity=chunk.getBlockEntity(pos);var oldEntity=entities.get(pos);
            if(!fresh&&(oldEntity==null?entity!=null:oldEntity.get()!=entity))safe=false;
            if(fresh&&entity!=null)entities.put(pos.immutable(),new WeakReference<>(entity));
            return chunk.getBlockState(pos);
        }
        Cell cell(BlockPos pos,BlockState state) {
            if(state.is(IndustrialFluidContent.REINFORCED_PIPE.get()))return new Cell(pos.immutable(),0,null,0);
            if(state.is(IndustrialFluidContent.CERAMIC_PIPE.get()))return new Cell(pos.immutable(),0,null,1);
            if(state.is(IndustrialFluidContent.REINFORCED_VALVE.get())||state.is(IndustrialFluidContent.CERAMIC_VALVE.get()))return new Cell(pos.immutable(),1,state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS),0);
            if(state.is(IndustrialFluidContent.PUMP.get()))return new Cell(pos.immutable(),2,state.getValue(IndustrialPumpBlock.FACING).getAxis(),0);
            return null;
        }
        Snapshot build(BlockPos seed) {
            var queue=new ArrayDeque<BlockPos>();var visited=new HashSet<BlockPos>();queue.add(seed.immutable());
            while(!queue.isEmpty()&&safe) {
                var pos=queue.remove();if(!visited.add(pos))continue;var state=observe(pos);if(state==null)break;var cell=cell(pos,state);if(cell==null){safe=false;break;}
                nodes.put(pos,cell);if(nodes.size()>IndustrialFluidConfig.MAX_NODES.get()){safe=false;break;}
                for(Direction d:DIRECTIONS) {
                    if(!cell.permits(d))continue;var next=pos.relative(d);var otherState=observe(next);if(otherState==null)break;var other=cell(next,otherState);
                    if(other!=null){if(other.permits(d.getOpposite()))queue.add(next);continue;}
                    // 未知提供者连一次试探都留给原BFS，避免构图提前触发不可达设备的能力回调。
                    if(otherState.getBlock() instanceof IndustrialPipeBlock||otherState.getBlock() instanceof IndustrialPumpBlock){safe=false;break;}
                    if(IndustrialTopology.knownEndpoint(level,next)){endpoints.add(new Face(next.immutable(),d.getOpposite()));continue;}
                    var registry=providers();if(registry==null){safe=false;break;}Object list=registry.get(otherState.getBlock());if(!noProvider(list)){safe=false;break;}
                    var old=providers.putIfAbsent(otherState.getBlock(),new Provider(otherState.getBlock(),list));if(old!=null&&old.list!=list){safe=false;break;}
                }
            }
            if(!safe)return null;var snapshot=new Snapshot(this);return snapshot.matches(level)?snapshot:null;
        }
    }
    private IndustrialNetworkGraph() {}
}
