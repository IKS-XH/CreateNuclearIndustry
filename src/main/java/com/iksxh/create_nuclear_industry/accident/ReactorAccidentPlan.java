package com.iksxh.create_nuclear_industry.accident;

import java.util.*;
import com.iksxh.create_nuclear_industry.blockentity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

/** 仅创建时读取可用世界，冻结确定性目标；内部受16³界限约束，壳和外部合计不超过32。 */
public final class ReactorAccidentPlan {
    private ReactorAccidentPlan() {}
    public static List<ReactorAccidentLedger.Action> create(ServerLevel level,BlockPos min,BlockPos max,
            AccidentParameters p,Set<BlockPos> ports,BlockPos owner) {
        var inside=new ArrayList<ReactorAccidentLedger.Action>();
        var outside=new ArrayList<BlockPos>();
        for(BlockPos mutable:BlockPos.betweenClosed(min.offset(-p.extension(),-p.extension(),-p.extension()),max.offset(p.extension(),p.extension(),p.extension()))) {
            var pos=mutable.immutable();boolean interior=pos.getX()>min.getX()&&pos.getX()<max.getX()
                    &&pos.getY()>min.getY()&&pos.getY()<max.getY()&&pos.getZ()>min.getZ()&&pos.getZ()<max.getZ();
            if(!interior) { if(p.destruction())outside.add(pos);continue; }
            var a=capture(level,pos,false,ports,owner);if(a!=null)inside.add(a);
        }
        // 壳优先，再按距结构中心/坐标固定排序；选择时跳过空气、不可破坏块和非本堆库存。
        double cx=(min.getX()+max.getX())*.5,cy=(min.getY()+max.getY())*.5,cz=(min.getZ()+max.getZ())*.5;
        outside.sort(Comparator.<BlockPos>comparingDouble(v->v.distToCenterSqr(cx+.5,cy+.5,cz+.5)).thenComparingLong(BlockPos::asLong));
        int count=0;
        for(BlockPos pos:outside) {
            if(count>=p.externalBlocks())break;
            var a=capture(level,pos,true,ports,owner);if(a!=null){inside.add(a);count++;}
        }
        // 换料口同属该列机构；已经计入外部目标的口无需重复。
        var targets=new HashSet<BlockPos>();inside.forEach(a->targets.add(a.pos));
        // 顶部机构属于壳，上限耗尽时保留空口，不能借内部处理突破外部破坏限额。
        return inside;
    }
    private static ReactorAccidentLedger.Action capture(ServerLevel level,BlockPos pos,boolean external,Set<BlockPos> ports,BlockPos owner) {
        if(!level.hasChunkAt(pos)||level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null)return null;
        var state=level.getBlockState(pos);
        if(state.isAir()||!state.getFluidState().isEmpty()||state.getDestroySpeed(level,pos)<0)return null;
        var be=level.getBlockEntity(pos);UUID generation=null;
        if(be instanceof ReactorPortBlockEntity port&&ports.contains(pos)) generation=port.accidentGeneration();
        else if(be instanceof ReactorInstrumentPortBlockEntity instrument&&pos.equals(owner))generation=instrument.accidentGeneration();
        else if(be!=null)return null;
        return new ReactorAccidentLedger.Action(pos,Block.getId(state),external,generation);
    }
}
