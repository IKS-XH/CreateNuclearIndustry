package com.iksxh.create_nuclear_industry.industrial;

import java.util.*;
import com.iksxh.create_nuclear_industry.config.IndustrialFluidConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.fluids.FluidStack;

/** 原序路径的压缩执行视图；动态门保留原调用位置，静态内容见证失效时逐格回退。 */
final class IndustrialRoutePlan {
    private record Step(IndustrialFlowBudget.Group group,int first,int last) {}
    final IndustrialNetworkGraph.Snapshot graph;
    final List<BlockPos> raw;
    private final List<Step> steps;
    private final IndustrialRoutePlan source,target;
    private IndustrialRoutePlan(IndustrialNetworkGraph.Snapshot graph,List<BlockPos> raw,List<Step> steps,IndustrialRoutePlan source,IndustrialRoutePlan target){this.graph=graph;this.raw=raw;this.steps=steps;this.source=source;this.target=target;}
    static IndustrialRoutePlan compile(IndustrialNetworkGraph.Snapshot graph,List<BlockPos> raw) {
        if(graph==null||!graph.live)return null;var steps=new ArrayList<Step>();
        for(int i=0;i<raw.size();) {
            var group=graph.model.groups.get(raw.get(i));int next=i+1;
            if(group!=null){while(next<raw.size()&&graph.model.groups.get(raw.get(next))==group)next++;if(next-i!=group.nodes.length)return null;}
            steps.add(new Step(group,i,next));i=next;
        }
        return new IndustrialRoutePlan(graph,raw,List.copyOf(steps),null,null);
    }
    static IndustrialRoutePlan joined(IndustrialNetworkGraph.Snapshot graph,List<BlockPos> raw,List<BlockPos> source,List<BlockPos> target) {
        var full=compile(graph,raw);var a=compile(graph,source);var b=compile(graph,target);
        if(full==null||a==null||b==null)return null;
        return new IndustrialRoutePlan(graph,raw,full.steps,a,b);
    }
    /** 纯管组仅检查介质和几何；阀/泵及其红石回调仍在原调用位置采集动态额度。 */
    boolean prepare(ServerLevel level,IndustrialFlowBudget.Path limits,IndustrialPumpBlockEntity pump,FluidStack fluid) {
        var registry=graph.owner;
        for(var step:steps) {
            var group=step.group;
            if(group!=null&&graph.staticValid(level,group)) {
                if(!registry.mediumMatches(group,fluid))return false;
                if(graph.staticValid(level,group)){limits.addUnboundedGroup(group);continue;}
            }
            if(group!=null){limits.demote(group);registry.flushMedia();}
            for(int i=step.first;i<step.last;i++){var pos=raw.get(i);registry.ensureRaw(pos);var old=registry.media.get(pos);if(old!=null&&!FluidStack.isSameFluidSameComponents(old,fluid))return false;int cap=IndustrialTopology.prepareLimit(level,pos,limits);if(pump!=null)pump.considerBottleneck(cap);}
        }
        return true;
    }
    /** 原permit的逐格正资格由静态见证替代；失败仅回退当前边，不重放已执行的动态回调。 */
    boolean positive(ServerLevel level) {
        for(var step:steps) {
            var group=step.group;
            if(group!=null&&graph.staticValid(level,group)) {
                if(graph.staticValid(level,group))continue;
            }
            for(int i=step.first;i<step.last;i++)if(IndustrialTopology.limit(level,raw.get(i))<=0)return false;
        }
        return true;
    }
    private void cross(IndustrialFlowBudget.Path limits,int amount) {
        for(var step:steps){if(step.group!=null&&limits.hasGroup(step.group))limits.cross(step.group.key(),amount);else for(int i=step.first;i<step.last;i++)limits.cross(raw.get(i).asLong(),amount);}
    }
    void crossPump(IndustrialFlowBudget.Path limits,BlockPos root,int extracted,int delivered){limits.cross(root.asLong(),Math.max(extracted,delivered));source.cross(limits,extracted);target.cross(limits,delivered);}
    void settle(IndustrialFlowBudget budget,long now,IndustrialFlowBudget.Path limits,FluidStack fluid,boolean crossed) {
        for(int i=0;i<limits.size();i++){long key=limits.key(i);int amount=crossed?limits.takeCrossed(key):1;if(amount<=0)continue;if(crossed)budget.charge(now,limits,key,amount);graph.owner.mark(limits.group(i),key,fluid);}
    }
}
