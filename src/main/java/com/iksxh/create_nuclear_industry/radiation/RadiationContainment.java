package com.iksxh.create_nuclear_industry.radiation;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

/** 每维度持久化已验证壳体边界；只缓存几何，不存燃料，不以运行/停机状态判定包容性。 */
public final class RadiationContainment extends SavedData {
    /** 服务端只读屏蔽可见性；正式查询只接受已加载区块，测试可注入更保守的可见范围。 */
    @FunctionalInterface public interface ChunkVisibility {
        boolean loaded(ServerLevel level,int x,int z);
    }
    public static final ChunkVisibility LOADED_CHUNKS=(level,x,z)->level.getChunkSource().getChunkNow(x,z)!=null;
    private final Map<BlockPos,Frame> frames=new LinkedHashMap<>();
    private final Map<Long,Set<Frame>> framesByChunk=new HashMap<>();
    private final Map<BlockPos,BlockPos> portLocations=new HashMap<>();
    private long shellReads;
    public static final int SHELL_CELL_BUDGET=8192;
    private long budgetTime=Long.MIN_VALUE;
    private int shellCellsRemaining;
    private boolean workComplete=true;
    private static final Set<String> CAPS=Set.of("reactor_casing","reactor_refueling_port","control_rod_drive");
    private static final Set<String> WALLS=Set.of("reactor_casing","reactor_window","reactor_instrument_port",
            "reactor_cold_port","reactor_hot_port","reactor_automation_port");
    private static final class Frame {
        final BlockPos min,max; final Map<BlockPos,BlockPos> ports;
        boolean dirty=true,complete;
        Frame(BlockPos min,BlockPos max,Map<BlockPos,BlockPos> ports) {this.min=min;this.max=max;this.ports=Map.copyOf(ports);}
        boolean inside(Vec3 v) {return v.x>min.getX()+1 && v.x<max.getX() && v.y>min.getY()+1
                && v.y<max.getY() && v.z>min.getZ()+1 && v.z<max.getZ();}
        boolean bounds(BlockPos pos) {return pos.getX()>=min.getX()&&pos.getX()<=max.getX()
                &&pos.getY()>=min.getY()&&pos.getY()<=max.getY()&&pos.getZ()>=min.getZ()&&pos.getZ()<=max.getZ();}
    }
    public static RadiationContainment get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(RadiationContainment::new,
                RadiationContainment::load,null),"create_nuclear_industry_radiation_containment");
    }
    /** 只在有效扫描后登记边界；以后仪表/内部机构损坏也保留同一几何依据。 */
    public static void remember(ReactorInstrumentPortBlockEntity owner) {
        if(!(owner.getLevel() instanceof ServerLevel level) || !owner.structureValid() || owner.structureOrigin()==null) return;
        var scan=owner.structureScan(); var min=owner.structureOrigin(); var d=scan.dimensions();
        var max=min.offset(d.width()-1,d.height()-1,d.depth()-1);
        Map<BlockPos,BlockPos> ports=new LinkedHashMap<>();
        scan.columns().forEach((column,mapping)-> {
            if(mapping.type()==ReactorStructureDefinition.ColumnType.FUEL) {
                var cap=mapping.capPosition();
                ports.put(min.offset(cap.x(),cap.y(),cap.z()),min.offset(column.x()+1,1,column.z()+1));
            }
        });
        var data=get(level); Frame old=data.frames.get(min);
        if(old!=null && old.max.equals(max) && old.ports.equals(ports)) return;
        if(old!=null) {
            data.framesByChunk.values().forEach(set->set.remove(old));
            old.ports.keySet().forEach(data.portLocations::remove);
        }
        data.add(new Frame(min.immutable(),max,ports)); data.setDirty();
    }
    public static Vec3 sourcePosition(ServerLevel level,BlockPos port) {
        return Vec3.atCenterOf(get(level).portLocations.getOrDefault(port,port));
    }
    /** 相关真实方块变化仅使缓存失效；此处不扫描世界。 */
    public static void changed(ServerLevel level,BlockPos pos) {
        for(Frame frame:get(level).near(pos)) if(frame.bounds(pos)
                &&(pos.getX()==frame.min.getX()||pos.getX()==frame.max.getX()||pos.getY()==frame.min.getY()
                ||pos.getY()==frame.max.getY()||pos.getZ()==frame.min.getZ()||pos.getZ()==frame.max.getZ())) frame.dirty=true;
    }
    public static void chunkChanged(ServerLevel level,int x,int z) {
        for(Frame frame:get(level).framesByChunk.getOrDefault(net.minecraft.world.level.ChunkPos.asLong(x,z),Set.of())) frame.dirty=true;
    }
    public double transmission(ServerLevel level,Vec3 source,Vec3 target,RadiationParameters p) {
        return transmission(level,source,target,p,LOADED_CHUNKS);
    }
    /** 同一屏蔽算法的只读查询边界；不加载区块，不改变服务器的实际区块状态。 */
    public double transmission(ServerLevel level,Vec3 source,Vec3 target,RadiationParameters p,ChunkVisibility visibility) {
        beginSampling(level.getGameTime());
        for(Frame frame:near(BlockPos.containing(source))) if(frame.inside(source)&&!frame.inside(target)&&shellComplete(level,frame,visibility)) return p.reactorTransmission();
        return 1;
    }
    /** 无源采样也必须开启新tick，不能继承上一tick的预算未完成状态而永久阻止恢复。 */
    public void beginSampling(long gameTime) {
        if(budgetTime!=gameTime){budgetTime=gameTime;shellCellsRemaining=SHELL_CELL_BUDGET;workComplete=true;}
    }
    /** 返回已知包容壳依赖，供跨帧射线只失效受影响的组，不读取世界。 */
    public Set<BlockPos> sourceFrames(Vec3 source) {
        Set<BlockPos> result=new LinkedHashSet<>();for(Frame frame:near(BlockPos.containing(source)))if(frame.inside(source))result.add(frame.min);return result;
    }
    /** 变化位置所处的已知壳边界；内部普通更新不触发整壳射线重算。 */
    public Set<BlockPos> boundaryFrames(BlockPos pos) {
        Set<BlockPos> result=new LinkedHashSet<>();for(Frame frame:near(pos))if(frame.bounds(pos)
                &&(pos.getX()==frame.min.getX()||pos.getX()==frame.max.getX()||pos.getY()==frame.min.getY()
                ||pos.getY()==frame.max.getY()||pos.getZ()==frame.min.getZ()||pos.getZ()==frame.max.getZ()))result.add(frame.min);return result;
    }
    /** 区块可见性涉及的已知壳范围；只查几何索引，不加载区块或扫描外壳。 */
    public Set<BlockPos> chunkFrames(int x,int z) {
        Set<BlockPos> result=new LinkedHashSet<>();
        for(Frame frame:framesByChunk.getOrDefault(net.minecraft.world.level.ChunkPos.asLong(x,z),Set.of()))result.add(frame.min);
        return result;
    }
    private Set<Frame> near(BlockPos pos) {return framesByChunk.getOrDefault(net.minecraft.world.level.ChunkPos.asLong(pos.getX()>>4,pos.getZ()>>4),Set.of());}
    private void add(Frame frame) {
        frames.put(frame.min,frame);portLocations.putAll(frame.ports);
        for(int x=frame.min.getX()>>4;x<=frame.max.getX()>>4;x++)for(int z=frame.min.getZ()>>4;z<=frame.max.getZ()>>4;z++)
            framesByChunk.computeIfAbsent(net.minecraft.world.level.ChunkPos.asLong(x,z),key->new LinkedHashSet<>()).add(frame);
    }
    private boolean shellComplete(ServerLevel level,Frame frame,ChunkVisibility visibility) {
        if(!frame.dirty) return frame.complete;
        for(int x=frame.min.getX()>>4;x<=frame.max.getX()>>4;x++)
            for(int z=frame.min.getZ()>>4;z<=frame.max.getZ()>>4;z++)
                if(!LOADED_CHUNKS.loaded(level,x,z)||!visibility.loaded(level,x,z)) return false;
        frame.complete=true; frame.dirty=false;
        for(BlockPos pos:BlockPos.betweenClosed(frame.min,frame.max)) {
            if(pos.getX()!=frame.min.getX()&&pos.getX()!=frame.max.getX()&&pos.getY()!=frame.min.getY()
                    &&pos.getY()!=frame.max.getY()&&pos.getZ()!=frame.min.getZ()&&pos.getZ()!=frame.max.getZ()) continue;
            if(shellCellsRemaining--<=0){frame.dirty=true;frame.complete=false;workComplete=false;return false;}
            shellReads++;
            var id=BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
            if(!id.getNamespace().equals("create_nuclear_industry")||!allowedShellBlock(frame,pos,id.getPath())) {frame.complete=false;break;}
        }
        return frame.complete;
    }
    /** 与扫描器的底/棱边、顶帽、侧槽位置名单一致；不要求燃料/内部机构或运行状态有效。 */
    private static boolean allowedShellBlock(Frame frame,BlockPos pos,String path) {
        boolean xFace=pos.getX()==frame.min.getX()||pos.getX()==frame.max.getX();
        boolean zFace=pos.getZ()==frame.min.getZ()||pos.getZ()==frame.max.getZ();
        if(!xFace&&!zFace&&pos.getY()==frame.max.getY())return CAPS.contains(path);
        if((xFace^zFace)&&pos.getY()>frame.min.getY()&&pos.getY()<frame.max.getY())return WALLS.contains(path);
        return path.equals("reactor_casing");
    }
    public long shellReads() {return shellReads;}
    public boolean workComplete() {return workComplete;}
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries) {
        ListTag list=new ListTag();
        for(Frame frame:frames.values()) {
            CompoundTag f=new CompoundTag();f.putLong("Min",frame.min.asLong());f.putLong("Max",frame.max.asLong());
            ListTag ports=new ListTag();frame.ports.forEach((port,inner)->{CompoundTag t=new CompoundTag();t.putLong("Port",port.asLong());t.putLong("Inner",inner.asLong());ports.add(t);});
            f.put("Ports",ports);list.add(f);
        }
        tag.put("Frames",list); return tag;
    }
    private static RadiationContainment load(CompoundTag tag,HolderLookup.Provider registries) {
        var data=new RadiationContainment();
        for(Tag entry:tag.getList("Frames",Tag.TAG_COMPOUND)) {
            CompoundTag f=(CompoundTag)entry; BlockPos min=BlockPos.of(f.getLong("Min")),max=BlockPos.of(f.getLong("Max"));
            if(max.getX()-min.getX()<4||max.getX()-min.getX()>15||max.getY()-min.getY()<4||max.getY()-min.getY()>15||max.getZ()-min.getZ()<4||max.getZ()-min.getZ()>15) continue;
            Map<BlockPos,BlockPos> ports=new LinkedHashMap<>();
            for(Tag e:f.getList("Ports",Tag.TAG_COMPOUND)) {var t=(CompoundTag)e;ports.put(BlockPos.of(t.getLong("Port")),BlockPos.of(t.getLong("Inner")));}
            data.add(new Frame(min,max,ports));
        }
        return data;
    }
}
