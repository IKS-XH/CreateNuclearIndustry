package com.iksxh.create_nuclear_industry.accident;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;

/** 服务端热计时与火点租约；不保存任何物品，固体真实载荷始终仅由Corium BE拥有。 */
public final class AccidentThermalState {
    public final AccidentThermalParameters parameters;
    /** 单件固体的热引用；终态保留防重入收据，冷投影完成后退出活跃集合。 */
    public static final class Solid {
        public final BlockPos pos;
        public int remaining;
        public boolean terminal;
        long lastTick=Long.MIN_VALUE;
        Solid(BlockPos pos,int remaining){this.pos=pos.immutable();this.remaining=remaining;}
    }
    /** 一个位置只尝试一次；UUID同时绑定世界火点，清理不能删除后来的同类方块。 */
    public static final class Fire {
        public final UUID id;
        public final BlockPos pos;
        public int remaining;
        public boolean attempted, placed, terminal, cleaned;
        long lastTick=Long.MIN_VALUE;
        Fire(UUID id,BlockPos pos,int remaining){this.id=id;this.pos=pos.immutable();this.remaining=remaining;}
        public boolean dangerous(){return placed&&!terminal&&remaining>0;}
    }
    private final Map<Long,Solid> solids=new LinkedHashMap<>();
    private final Map<UUID,Fire> fires=new LinkedHashMap<>();
    private final Set<Long> activeSolids=new LinkedHashSet<>();
    private final Set<UUID> activeFires=new LinkedHashSet<>();
    public AccidentThermalState(AccidentThermalParameters parameters){this.parameters=parameters;}
    public Solid startSolid(BlockPos pos) {
        var s=solids.get(pos.asLong());
        if(s==null){s=new Solid(pos,parameters.solidTicks());solids.put(pos.asLong(),s);activeSolids.add(pos.asLong());}
        return s;
    }
    public Fire planFire(BlockPos pos) {
        if(fires.size()>=parameters.fireLimit()||fires.values().stream().anyMatch(f->f.pos.equals(pos)))return null;
        var f=new Fire(UUID.randomUUID(),pos,parameters.fireTicks());fires.put(f.id,f);activeFires.add(f.id);return f;
    }
    public Solid solid(BlockPos pos){return solids.get(pos.asLong());}
    public Fire fire(UUID id){return fires.get(id);}
    public List<Solid> activeSolids(){return activeSolids.stream().map(solids::get).toList();}
    public List<Fire> activeFires(){return activeFires.stream().map(fires::get).toList();}
    public List<Fire> fires(){return List.copyOf(fires.values());}
    public boolean active(){return !activeSolids.isEmpty()||!activeFires.isEmpty();}
    /** 每个世界tick最多推进一次；不可用时不扣时，不根据两个tick之间的差额追补。 */
    public void advanceSolid(Solid s,long tick,boolean loaded,boolean water) {
        if(!loaded||s.terminal||s.lastTick==tick)return;s.lastTick=tick;
        s.remaining=Math.max(0,s.remaining-(water?parameters.waterRate():1));
    }
    public void advanceFire(Fire f,long tick,boolean loaded) {
        if(!loaded||!f.dangerous()||f.lastTick==tick)return;f.lastTick=tick;
        if(--f.remaining<=0)endFire(f);
    }
    public void endSolid(Solid s){s.terminal=true;s.remaining=0;activeSolids.remove(s.pos.asLong());}
    public void endFire(Fire f){f.terminal=true;f.remaining=0;}
    public void cleaned(Fire f){f.cleaned=true;activeFires.remove(f.id);}
    public CompoundTag save() {
        var t=new CompoundTag();t.put("Parameters",parameters.save());var ss=new ListTag();var fs=new ListTag();
        for(var s:solids.values()){var v=new CompoundTag();v.putLong("Pos",s.pos.asLong());v.putInt("Remaining",s.remaining);v.putBoolean("Terminal",s.terminal);ss.add(v);}
        for(var f:fires.values()){var v=new CompoundTag();v.putUUID("Id",f.id);v.putLong("Pos",f.pos.asLong());v.putInt("Remaining",f.remaining);
            v.putBoolean("Attempted",f.attempted);v.putBoolean("Placed",f.placed);v.putBoolean("Terminal",f.terminal);v.putBoolean("Cleaned",f.cleaned);fs.add(v);}
        t.put("Solids",ss);t.put("Fires",fs);return t;
    }
    public static AccidentThermalState load(CompoundTag t) {
        var state=new AccidentThermalState(AccidentThermalParameters.load(t.getCompound("Parameters")));
        for(var raw:t.getList("Solids",Tag.TAG_COMPOUND)){var v=(CompoundTag)raw;var s=new Solid(BlockPos.of(v.getLong("Pos")),v.getInt("Remaining"));
            s.terminal=v.getBoolean("Terminal");state.solids.put(s.pos.asLong(),s);if(!s.terminal)state.activeSolids.add(s.pos.asLong());}
        for(var raw:t.getList("Fires",Tag.TAG_COMPOUND)){var v=(CompoundTag)raw;var f=new Fire(v.getUUID("Id"),BlockPos.of(v.getLong("Pos")),v.getInt("Remaining"));
            f.attempted=v.getBoolean("Attempted");f.placed=v.getBoolean("Placed");f.terminal=v.getBoolean("Terminal");f.cleaned=v.getBoolean("Cleaned");
            state.fires.put(f.id,f);if(!f.cleaned)state.activeFires.add(f.id);}
        return state;
    }
}
