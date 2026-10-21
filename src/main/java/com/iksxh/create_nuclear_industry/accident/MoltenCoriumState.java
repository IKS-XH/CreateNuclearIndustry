package com.iksxh.create_nuclear_industry.accident;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;

/** 世界本体的唯一载荷与加载时钟；source仅审计，不要求任何事故/仪表仍存在。 */
public final class MoltenCoriumState {
    public enum Phase { MOLTEN,HOT,DONE,ADMIN_REMOVED }
    public final UUID id,source;
    public final MoltenCoriumParameters parameters;
    private AccidentThermalState thermal;
    public BlockPos pos;
    public Phase phase=Phase.MOLTEN;
    public int remaining,moveProgress,corrosionProgress,revision;
    public boolean placed;
    public long lastTick=Long.MIN_VALUE;
    private ItemStack payload;
    private final Set<Long> visited=new HashSet<>();
    public MoltenCoriumState(UUID id,UUID source,BlockPos pos,ItemStack payload,MoltenCoriumParameters parameters) {
        this.id=id;this.source=source;this.pos=pos.immutable();this.payload=payload.copy();this.parameters=parameters;
        remaining=parameters.coolingTicks();thermal=new AccidentThermalState(parameters.solid());visited.add(pos.asLong());
    }
    public ItemStack payload(){return payload.copy();}
    public boolean dangerous(){return phase==Phase.MOLTEN&&remaining>0;}
    public boolean active(){return phase==Phase.MOLTEN||phase==Phase.HOT&&thermal.active();}
    public boolean visited(BlockPos target){return visited.contains(target.asLong());}
    public void advance(long tick,boolean loaded,boolean water){
        if(!loaded||phase!=Phase.MOLTEN||lastTick==tick)return;lastTick=tick;
        remaining=Math.max(0,remaining-(water?parameters.solid().waterRate():1));
        // 间隔允许配置到int上界，达到阈值后停止递增，避免加一溢出成为负进度。
        if(moveProgress<parameters.moveTicks())moveProgress++;
        if(corrosionProgress<parameters.corrosionTicks())corrosionProgress++;
    }
    public void moved(BlockPos target){pos=target.immutable();visited.add(pos.asLong());revision++;moveProgress=0;}
    public void corroded(){corrosionProgress=0;}
    /** 只在目标BE已接受同身份载荷后清空托管；终态重入不能再发料。 */
    public void handedOff(){if(phase!=Phase.MOLTEN)return;payload=ItemStack.EMPTY;phase=Phase.HOT;}
    public void adminRemoved(){payload=ItemStack.EMPTY;phase=Phase.ADMIN_REMOVED;remaining=0;}
    public CompoundTag save(HolderLookup.Provider registries) {
        var t=new CompoundTag();t.putUUID("Id",id);if(source!=null)t.putUUID("Source",source);t.putLong("Pos",pos.asLong());t.putString("Phase",phase.name());
        t.put("Parameters",parameters.save());t.putInt("Remaining",remaining);t.putInt("MoveProgress",moveProgress);t.putInt("CorrosionProgress",corrosionProgress);
        t.putInt("Revision",revision);t.putBoolean("Placed",placed);t.putLong("LastTick",lastTick);if(!payload.isEmpty())t.put("Payload",payload.save(registries));
        t.putLongArray("Visited",visited.stream().mapToLong(Long::longValue).toArray());t.put("Thermal",thermal.save());return t;
    }
    public static MoltenCoriumState load(CompoundTag t,HolderLookup.Provider registries) {
        var s=new MoltenCoriumState(t.getUUID("Id"),t.hasUUID("Source")?t.getUUID("Source"):null,BlockPos.of(t.getLong("Pos")),
                ItemStack.parseOptional(registries,t.getCompound("Payload")),MoltenCoriumParameters.load(t.getCompound("Parameters")));
        s.phase=Phase.valueOf(t.getString("Phase"));s.remaining=t.getInt("Remaining");s.moveProgress=t.getInt("MoveProgress");s.corrosionProgress=t.getInt("CorrosionProgress");
        s.revision=t.getInt("Revision");s.placed=t.getBoolean("Placed");s.lastTick=t.getLong("LastTick");for(long pos:t.getLongArray("Visited"))s.visited.add(pos);
        s.thermal=AccidentThermalState.load(t.getCompound("Thermal"));
        if(s.phase!=Phase.MOLTEN)s.payload=ItemStack.EMPTY;return s;
    }
    public AccidentThermalState heat(){return thermal;}
}
