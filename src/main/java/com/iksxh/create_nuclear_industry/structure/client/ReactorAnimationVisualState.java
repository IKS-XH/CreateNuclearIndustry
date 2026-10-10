package com.iksxh.create_nuclear_industry.structure.client;

import com.iksxh.create_nuclear_industry.structure.ReactorRuntimeDescriptor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import java.util.*;

/**
 * 仅客户端的数值投影：HU/t转透明度、mB转合法格体积、实际深度转最多4显示tick的位姿。
 * 不结算世界，不保存BE或Level；身份不含心跳sample，未知状态不能续用旧位姿。
 */
public final class ReactorAnimationVisualState {
    public static final Clock CLOCK = new Clock();
    public static final Rods RODS = new Rods();
    private static final Map<Identity, Phase> PHASES = new HashMap<>();
    private static final Map<Identity, Mesh> MESHES = new HashMap<>();
    private record Mesh(Set<Cell> cells,Map<Cell,Cell> lighting,double fill,List<Face> faces,double last) {}
    private ReactorAnimationVisualState() {}

    /** 客户端会话加权威几何身份，owner为不可变方块打包坐标。 */
    public record Identity(long session, String dimension, long owner, UUID generation, long geometry) {}
    public record Cell(int x, int y, int z) {}
    /** 以世界方块为单位的完整棒体边界，head固定3/16而不随travel缩放。 */
    public record RodPose(double bottom,double top,int travel) {}
    public enum Side { DOWN, UP, NORTH, SOUTH, WEST, EAST }
    /** 显示并集外表面；low/high为相对格底高度，lightingCell为同层合法空气采光格，均用方块单位。 */
    public record Face(Cell cell, Side side, double low, double high, Cell lightingCell) {
        public Face(Cell cell,Side side,double low,double high) { this(cell,side,low,high,cell); }
    }
    private record RodKey(Identity owner, long cap) {}
    private static final class Rod {
        double from, to, start, last;
        Rod(double value, double time) { from = to = value; start = last = time; }
        double at(double time) { return from + (to - from) * clamp((time - start) / 4); }
    }
    private static final class Phase { double value, last; Phase(double time) { last = time; } }

    public static Identity identity(ReactorRuntimeDescriptor owner) {
        return new Identity(ReactorAnimationClientEvents.session(), owner.dimension(), owner.ownerPos().asLong(),
                owner.ownerGeneration(), owner.geometryRevision());
    }

    /** 只查询已经加载的列成员；不加载区块，不用扫描补齐信封。任一矛盾撤下该列。 */
    public static boolean matches(Level level, ReactorRuntimeDescriptor.Column column) {
        if (!matches(level, column.capPos(), column.expectedCapBlockId())) return false;
        for (BlockPos pos : column.bodyPositions()) if (!matches(level, pos, column.expectedBodyBlockId())) return false;
        return true;
    }
    public static boolean matches(Level level, BlockPos pos, String id) {
        var chunk = level.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4,
                net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false);
        return chunk != null && BuiltInRegistries.BLOCK.getKey(chunk.getBlockState(pos).getBlock()).toString().equals(id);
    }

    /** 耗尽tick即使仍有功率也熄辉；6 HU/t仅为展示比例尺。 */
    public static double glow(boolean usable, double heat) {
        return usable && Double.isFinite(heat) && heat > 0 ? .12 + .53 * heat / (heat + 6) : 0;
    }
    public static double fill(long cold, long hot, long capacity) {
        return capacity > 0 ? clamp(((double)cold + (double)hot) / capacity) : 0;
    }
    public static double hotRatio(long cold, long hot) {
        double total = (double)cold + (double)hot;
        return total > 0 ? clamp(hot / total) : 0;
    }
    static double clamp(double value) { return Double.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0; }

    /** 等效体积按真实Y层面积自下向上分配，非矩形/分离集合也不填洞。 */
    public static Map<Cell, Double> allocate(Set<Cell> cells, double fill) {
        var layers = new TreeMap<Integer, List<Cell>>();
        for (Cell cell : cells) layers.computeIfAbsent(cell.y(), key -> new ArrayList<>()).add(cell);
        double remaining = cells.size() * clamp(fill);
        Map<Cell, Double> result = new HashMap<>();
        for (List<Cell> layer : layers.values()) {
            double height = Math.min(1, remaining / layer.size());
            if (height <= 0) break;
            for (Cell cell : layer) result.put(cell, height);
            remaining -= height * layer.size();
        }
        return Map.copyOf(result);
    }

    /** 并集只产生向外面；相邻高度不同时仅保留高格暴露的上半条带。 */
    public static List<Face> faces(Map<Cell, Double> cells) {
        List<Face> result = new ArrayList<>();
        for (var entry : cells.entrySet()) {
            Cell c = entry.getKey(); double h = entry.getValue();
            if (h <= 0) continue;
            if (cells.getOrDefault(new Cell(c.x(), c.y()-1, c.z()), 0.) < 1) result.add(new Face(c, Side.DOWN, 0, 0));
            if (h < 1 || cells.getOrDefault(new Cell(c.x(), c.y()+1, c.z()), 0.) <= 0) result.add(new Face(c, Side.UP, h, h));
            addSide(result, cells, c, Side.NORTH, c.x(), c.z()-1, h);
            addSide(result, cells, c, Side.SOUTH, c.x(), c.z()+1, h);
            addSide(result, cells, c, Side.WEST, c.x()-1, c.z(), h);
            addSide(result, cells, c, Side.EAST, c.x()+1, c.z(), h);
        }
        return List.copyOf(result);
    }
    private static void addSide(List<Face> result, Map<Cell, Double> cells, Cell c, Side side, int x, int z, double h) {
        double neighbour = cells.getOrDefault(new Cell(x, c.y(), z), 0.);
        if (neighbour < h) result.add(new Face(c, side, neighbour, h));
    }

    /** 实际列投影入口由BER直接调用；target不参与位置或缓动。 */
    public static RodPose rodPose(Identity owner,ReactorRuntimeDescriptor.ControlRodColumn column,double time) {
        int travel=column.bodyPositions().size();
        double actual=RODS.depth(owner,column.capPos().asLong(),column.actualDepth(),column.jammed(),time);
        double bottom=column.capPos().getY()-travel*actual;
        return new RodPose(bottom,bottom+travel,travel);
    }
    public static final class Rods {
        private final Map<RodKey, Rod> entries = new HashMap<>();
        /** 输入是权威actual而非target；首次、卡死、失见超过4tick均立即定位。 */
        public double depth(Identity owner, long cap, double actual, boolean jammed, double time) {
            var key = new RodKey(owner, cap); double value = clamp(actual);
            Rod rod = entries.get(key);
            if (rod == null || time - rod.last > 4 || jammed) { rod = new Rod(value, time); entries.put(key, rod); }
            else if (rod.to != value) { rod.from = rod.at(time); rod.to = value; rod.start = time; }
            rod.last = time; return rod.at(time);
        }
        public void invalidate(Identity owner) { entries.keySet().removeIf(key -> key.owner().equals(owner)); }
        public void invalidateCap(long cap) { entries.keySet().removeIf(key -> key.cap() == cap); }
        public void clear() { entries.clear(); }
        public void expire(double time) { entries.values().removeIf(rod -> time - rod.last > 20); }
    }
    public static final class Clock {
        private long ticks; private double last;
        public void tick(boolean paused) { if (!paused) ticks++; }
        /** 暂停时连partial都不读，位姿和材质共用冻结显示时间。 */
        public double time(float partial, boolean paused) { if (!paused) last = ticks + clamp(partial); return last; }
        public long tick() { return ticks; }
        public void clear() { ticks = 0; last = 0; }
    }
    /** 换热量只改变纹样活跃度；停热仍有环境纹样，不宣称实际泵速。 */
    public static double phase(Identity owner, double time, double converted) {
        Phase phase = PHASES.get(owner);
        if(phase==null) { phase=new Phase(time); if(PHASES.size()<256)PHASES.put(owner,phase); }
        double speed = .08 + .42 * converted / (converted + 8);
        phase.value = (phase.value + Math.max(0, time - phase.last) * speed) % 8;
        phase.last = time; return phase.value;
    }
    /**
     * 权威geometry固定时缓存合法容量单元与显示/采光映射；燃料格只补同层显示包络，不进入容量分母。
     * 采光坐标在建几何时确定，光值由每帧提交重读；不保存descriptor、Level或实体引用。
     */
    public static List<Face> mesh(Identity owner,ReactorRuntimeDescriptor descriptor,double fill,double time) {
        Mesh previous=MESHES.get(owner);
        Set<Cell> cells;
        Map<Cell,Cell> lighting;
        if(previous!=null) { cells=previous.cells();lighting=previous.lighting(); }
        else {
            var mutable=new HashSet<Cell>();
            for(BlockPos pos:descriptor.coolantSpace())mutable.add(new Cell(pos.getX(),pos.getY(),pos.getZ()));
            cells=Set.copyOf(mutable);
            var mapping=new HashMap<Cell,Cell>();
            var layers=new HashMap<Integer,List<Cell>>();
            for(Cell cell:cells) { mapping.put(cell,cell);layers.computeIfAbsent(cell.y(),key->new ArrayList<>()).add(cell); }
            var order=Comparator.comparingInt(Cell::x).thenComparingInt(Cell::y).thenComparingInt(Cell::z);
            layers.values().forEach(layer->layer.sort(order));
            for(var column:descriptor.columns())if(column instanceof ReactorRuntimeDescriptor.FuelColumn fuel) {
                for(BlockPos pos:fuel.bodyPositions()) {
                    Cell visual=new Cell(pos.getX(),pos.getY(),pos.getZ()),nearest=null;
                    long best=Long.MAX_VALUE;
                    // 只遍历有界同层合法集合；等距保留排序首项，保证源集合迭代顺序不影响光坐标。
                    for(Cell candidate:layers.getOrDefault(visual.y(),List.of())) {
                        long dx=(long)candidate.x()-visual.x(),dz=(long)candidate.z()-visual.z(),distance=dx*dx+dz*dz;
                        if(distance<best) { best=distance;nearest=candidate; }
                    }
                    if(nearest!=null)mapping.put(visual,nearest);
                }
            }
            lighting=Map.copyOf(mapping);
        }
        return mesh(owner,cells,lighting,fill,time);
    }
    /** 平稳库存沿用不可变并集网格；身份/填充比例变化才重建，不保存信封或外部集合。 */
    public static List<Face> mesh(Identity owner,Set<Cell> cells,double fill,double time) {
        var lighting=new HashMap<Cell,Cell>();for(Cell cell:cells)lighting.put(cell,cell);
        return mesh(owner,cells,lighting,fill,time);
    }
    private static List<Face> mesh(Identity owner,Set<Cell> cells,Map<Cell,Cell> lighting,double fill,double time) {
        Mesh previous=MESHES.get(owner);
        List<Face> result;
        if(previous!=null && previous.fill()==fill && previous.cells().equals(cells) && previous.lighting().equals(lighting))result=previous.faces();
        else {
            var allocated=allocate(cells,fill);var heights=new HashMap<Integer,Double>();
            allocated.forEach((cell,height)->heights.put(cell.y(),height));
            var display=new HashMap<>(allocated);
            lighting.forEach((cell,light)->{ Double height=heights.get(cell.y());if(height!=null)display.put(cell,height); });
            result=faces(display).stream().map(face->new Face(face.cell(),face.side(),face.low(),face.high(),lighting.get(face.cell()))).toList();
        }
        if(previous!=null || MESHES.size()<256)MESHES.put(owner,new Mesh(Set.copyOf(cells),Map.copyOf(lighting),fill,result,time));
        return result;
    }
    public static void invalidate(Identity owner) { RODS.invalidate(owner); PHASES.remove(owner); MESHES.remove(owner); ReactorAnimationMaterials.invalidate(owner); }
    public static void invalidatePosition(long owner) {
        var keys=new HashSet<Identity>(PHASES.keySet());keys.addAll(MESHES.keySet());
        for(Identity key:keys)if(key.owner()==owner)invalidate(key);
    }
    /** 同次可靠快照在安全tick撤下未知身份，恢复不能沿用未知期间的缓动或纹样。 */
    public static void boundary(ReactorRuntimeSnapshot snapshot) {
        RODS.entries.keySet().removeIf(key->snapshot.findControlRod(BlockPos.of(key.cap())).isEmpty()
                || snapshot.findControlOwner(BlockPos.of(key.cap())).map(owner->!identity(owner).equals(key.owner())).orElse(true));
        PHASES.keySet().removeIf(key->!reliable(snapshot,key)); MESHES.keySet().removeIf(key->!reliable(snapshot,key));
    }
    private static boolean reliable(ReactorRuntimeSnapshot snapshot,Identity key) {
        return snapshot.findOwner(BlockPos.of(key.owner())).map(owner->identity(owner).equals(key)).orElse(false);
    }
    public static void expire() { double time = CLOCK.tick(); RODS.expire(time); PHASES.values().removeIf(phase -> time - phase.last > 20); MESHES.values().removeIf(mesh->time-mesh.last()>20); }
    public static void clear() { RODS.clear(); PHASES.clear(); MESHES.clear(); CLOCK.clear(); }
}
