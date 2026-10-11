package com.iksxh.create_nuclear_industry.boiler.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import java.util.*;

/**
 * 客户端汽雾的纯显示投影；mB只决定浓度和配色，不改变气域高度或服务器库存。
 * 几何只来源于可靠L1闭边界和绝对隔层，不扫描结构，不保留Level/BE引用。
 */
public final class BoilerSteamVisualState {
    static final Clock CLOCK=new Clock();
    static final Cache CACHE=new Cache();
    static final double EPSILON=1D/1024;
    private BoilerSteamVisualState() {}

    /** 当前世界会话与服务器几何身份共同隔离材质槽和网格，心跳库存不属于几何键。 */
    record Identity(long session,String dimension,long owner,UUID generation,long revision) {}
    /** 坐标为owner局部方块单位，UV锚在世界整数格；采光格始终在真实汽区air内。 */
    record Point(double x,double y,double z,double u,double v,BlockPos lightingCell) {}
    /** 同一材质网格中的一张雾片；层权重仅调材质透明度，不是第二份库存。 */
    record Quad(Direction normal,List<Point> points,double weight) {
        Quad { points=List.copyOf(points); }
    }
    record Mesh(AABB bounds,List<Quad> quads) {
        Mesh { quads=List.copyOf(quads); }
    }
    static Identity identity(long session,BoilerSurfaceSnapshot.Member member) {
        return new Identity(session,member.dimension(),member.ownerPos().asLong(),member.ownerGeneration(),member.revision());
    }
    static double clamp(double value) {return Double.isFinite(value)?Math.clamp(value,0,1):0;}
    /** Long合计避免两种int库存相加溢出；非法或零容量没有可显示的浓度。 */
    static double concentration(int normalMb,int supercriticalMb,int capacityMb) {
        if(normalMb<0 || supercriticalMb<0 || capacityMb<=0)return 0;
        return clamp(((long)normalMb+supercriticalMb)/(double)capacityMb);
    }
    static double supercriticalRatio(int normalMb,int supercriticalMb) {
        if(normalMb<0 || supercriticalMb<0)return 0;
        long total=(long)normalMb+supercriticalMb;
        return total==0?0:supercriticalMb/(double)total;
    }
    /** 密度单调映射到最高.68的汽雾透明度；零/非法严格为零，仅提高可见度，不改液位或采光。 */
    static double alpha(double concentration) {double value=clamp(concentration);return value==0?0:.18+.50*Math.sqrt(value);}

    /** 从当前快照的控制器成员取唯一归属；Ponder/撤销/局部异常不能用旧网格续画。 */
    static BoilerSurfaceSnapshot.Member owner(BoilerSurfaceSnapshot snapshot,BlockPos pos) {
        var member=snapshot.members().get(pos);
        return member!=null && member.ownerPos().equals(pos) && member.pos().equals(pos)
                && member.expectedBlockId().equals("create_nuclear_industry:high_pressure_boiler_controller")
                && !member.outwardFaces().isEmpty()?member:null;
    }
    static Set<Identity> validIdentities(BoilerSurfaceSnapshot snapshot,long session,Set<Identity> candidates) {
        Set<Identity> valid=new HashSet<>();
        for(var key:candidates) {
            var member=owner(snapshot,BlockPos.of(key.owner()));
            if(member!=null && identity(session,member).equals(key))valid.add(key);
        }
        return Set.copyOf(valid);
    }

    /** 六外面加四横/四竖雾片，共14quad；复杂度不随体积格数增长。 */
    static Mesh buildMesh(BoilerSurfaceSnapshot.Member m) {
        var min=m.min();var max=m.maxInclusive();var base=m.ownerPos();
        int x0=min.getX()+1,x1=max.getX(),y0=m.partitionY()+1,y1=max.getY(),z0=min.getZ()+1,z1=max.getZ();
        AABB bounds=new AABB(x0,y0,z0,x1,y1,z1);
        if(x1<=x0 || y1<=y0 || z1<=z0 || m.partitionY()<=min.getY() || m.partitionY()>=max.getY())return new Mesh(bounds,List.of());
        double loX=(long)x0-base.getX()+EPSILON,hiX=(long)x1-base.getX()-EPSILON;
        double loY=(long)y0-base.getY()+EPSILON,hiY=(long)y1-base.getY()-EPSILON;
        double loZ=(long)z0-base.getZ()+EPSILON,hiZ=(long)z1-base.getZ()-EPSILON;
        List<Quad> quads=new ArrayList<>();
        quads.add(plane(Direction.WEST,loX,loY,hiY,loZ,hiZ,.7,base,x0,x1,y0,y1,z0,z1));
        quads.add(plane(Direction.EAST,hiX,loY,hiY,loZ,hiZ,.7,base,x0,x1,y0,y1,z0,z1));
        quads.add(plane(Direction.NORTH,loZ,loX,hiX,loY,hiY,.7,base,x0,x1,y0,y1,z0,z1));
        quads.add(plane(Direction.SOUTH,hiZ,loX,hiX,loY,hiY,.7,base,x0,x1,y0,y1,z0,z1));
        quads.add(plane(Direction.DOWN,loY,loX,hiX,loZ,hiZ,.7,base,x0,x1,y0,y1,z0,z1));
        quads.add(plane(Direction.UP,hiY,loX,hiX,loZ,hiZ,.7,base,x0,x1,y0,y1,z0,z1));
        for(int i=1;i<=4;i++)quads.add(plane(Direction.UP,loY+(hiY-loY)*i/5,loX,hiX,loZ,hiZ,.35,base,x0,x1,y0,y1,z0,z1));
        for(int i=1;i<=2;i++) {
            quads.add(plane(Direction.EAST,loX+(hiX-loX)*i/3,loY,hiY,loZ,hiZ,.35,base,x0,x1,y0,y1,z0,z1));
            quads.add(plane(Direction.SOUTH,loZ+(hiZ-loZ)*i/3,loX,hiX,loY,hiY,.35,base,x0,x1,y0,y1,z0,z1));
        }
        return new Mesh(bounds,quads);
    }
    /** 六面绕序与原生QUADS一致；采光点在气域闭内格钳制，法线与姿态由最终BER共同变换。 */
    private static Quad plane(Direction side,double fixed,double a0,double a1,double b0,double b1,double weight,
                              BlockPos base,int x0,int x1,int y0,int y1,int z0,int z1) {
        double[][] points=switch(side) {
            case WEST -> new double[][]{{fixed,a0,b0},{fixed,a0,b1},{fixed,a1,b1},{fixed,a1,b0}};
            case EAST -> new double[][]{{fixed,a0,b1},{fixed,a0,b0},{fixed,a1,b0},{fixed,a1,b1}};
            case NORTH -> new double[][]{{a1,b0,fixed},{a0,b0,fixed},{a0,b1,fixed},{a1,b1,fixed}};
            case SOUTH -> new double[][]{{a0,b0,fixed},{a1,b0,fixed},{a1,b1,fixed},{a0,b1,fixed}};
            case DOWN -> new double[][]{{a0,fixed,b1},{a1,fixed,b1},{a1,fixed,b0},{a0,fixed,b0}};
            case UP -> new double[][]{{a0,fixed,b0},{a0,fixed,b1},{a1,fixed,b1},{a1,fixed,b0}};
        };
        List<Point> result=new ArrayList<>(4);
        for(double[] p:points) {
            int lx=(int)Math.floor(p[0]),ly=(int)Math.floor(p[1]),lz=(int)Math.floor(p[2]);
            var light=new BlockPos(Math.clamp((long)base.getX()+lx,x0,x1-1),Math.clamp((long)base.getY()+ly,y0,y1-1),Math.clamp((long)base.getZ()+lz,z0,z1-1));
            // owner是整数坐标，周期为一格的世界UV与局部格UV完全同相；绝对大坐标不转换float。
            double u=side.getAxis()==Direction.Axis.X?p[2]:p[0];
            double v=side.getAxis()==Direction.Axis.Y?p[2]:p[1];
            result.add(new Point(p[0],p[1],p[2],u,v,light));
        }
        return new Quad(side,result,weight);
    }

    /** 单次owner修订对应稳定网格；撤销与世界切换只清已有键，不扫描成员世界。 */
    static final class Cache {
        private record Entry(BlockPos min,BlockPos max,int partition,Mesh mesh) {}
        private final Map<Identity,Entry> meshes=new HashMap<>();
        Mesh mesh(Identity key,BoilerSurfaceSnapshot.Member m) {
            var old=meshes.get(key);
            if(old!=null && old.min().equals(m.min()) && old.max().equals(m.maxInclusive()) && old.partition()==m.partitionY())return old.mesh();
            var result=buildMesh(m);
            if(meshes.containsKey(key) || meshes.size()<BoilerSteamMaterials.SLOTS)meshes.put(key,new Entry(m.min(),m.maxInclusive(),m.partitionY(),result));
            return result;
        }
        Set<Identity> keys(){return Set.copyOf(meshes.keySet());}
        /** 卸载可能发生在远离owner的外壳区块，必须按该炉完整闭边界而非仅owner区块撤销。 */
        boolean touchesChunk(Identity key,int x,int z) {
            var entry=meshes.get(key);
            return entry!=null && x>=(entry.min().getX()>>4) && x<=(entry.max().getX()>>4)
                    && z>=(entry.min().getZ()>>4) && z<=(entry.max().getZ()>>4);
        }
        void retain(Set<Identity> valid){meshes.keySet().retainAll(valid);}
        int size(){return meshes.size();}
        void clear(){meshes.clear();}
    }
    /** 动画tick与partial同时冻结；暂停期间渲染仍可读取可靠库存但不推进纹理。 */
    static final class Clock {
        private long ticks;
        private double last;
        void tick(boolean paused){if(!paused)ticks++;}
        double time(float partial,boolean paused){if(paused)return last;last=ticks+clamp(partial);return last;}
        long tick(){return ticks;}
        void clear(){ticks=0;last=0;}
    }
    static void clear(){CACHE.clear();CLOCK.clear();}
}
