package com.iksxh.create_nuclear_industry.accident;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** 每维度事故持久化；任务与去重收据独立于仪表生命周期，运行游标不持有强世界引用。 */
public final class ReactorAccidentSavedData extends SavedData {
    private final Map<UUID, ReactorAccidentLedger> ledgers=new LinkedHashMap<>();
    private final Set<UUID> initialActive=new LinkedHashSet<>(), thermalActive=new LinkedHashSet<>();
    public List<ReactorAccidentLedger> initialWork(){return initialActive.stream().map(ledgers::get).toList();}
    public List<ReactorAccidentLedger> thermalWork(){return thermalActive.stream().map(ledgers::get).toList();}
    /** 仅由活跃事故边沿更新索引，不在普通tick扫描全部历史事故。 */
    public void refreshWork(ReactorAccidentLedger ledger) {
        if(ledger.complete())initialActive.remove(ledger.id);else initialActive.add(ledger.id);
        if(ledger.thermal.active())thermalActive.add(ledger.id);else thermalActive.remove(ledger.id);
    }
    public void startSolid(UUID accident,net.minecraft.core.BlockPos pos) {
        var ledger=byId(accident);if(ledger==null)return;ledger.thermal.startSolid(pos);refreshWork(ledger);setDirty();
    }
    public void removeSolid(UUID accident,net.minecraft.core.BlockPos pos) {
        var ledger=byId(accident);if(ledger==null)return;var s=ledger.thermal.solid(pos);
        if(s!=null){ledger.thermal.endSolid(s);refreshWork(ledger);setDirty();}
    }
    /** 索引只引用真实托管，不复制物品；key由事故ID和列底部唯一确定。 */
    public record Custody(UUID accident, ReactorAccidentLedger.Column column) {}
    private final Map<Long,List<Custody>> custodyByChunk=new HashMap<>();
    public List<Custody> custodiesNear(net.minecraft.world.phys.Vec3 target,int radius) {
        var result=new ArrayList<Custody>();
        for(int x=((int)Math.floor(target.x)-radius)>>4;x<=((int)Math.floor(target.x)+radius)>>4;x++)
            for(int z=((int)Math.floor(target.z)-radius)>>4;z<=((int)Math.floor(target.z)+radius)>>4;z++)
                result.addAll(custodyByChunk.getOrDefault(net.minecraft.world.level.ChunkPos.asLong(x,z),List.of()));
        return result;
    }
    /** 完成真实交接即撤销托管索引；已开始采样仍通过column收据验证旧源失效。 */
    public void payloadChanged(ReactorAccidentLedger.Column column) {
        if(column.settled()) {
            long key=net.minecraft.world.level.ChunkPos.asLong(column.bottom.getX()>>4,column.bottom.getZ()>>4);
            var list=custodyByChunk.get(key);if(list!=null){list.removeIf(c->c.column()==column);if(list.isEmpty())custodyByChunk.remove(key);}
        }
        setDirty();
    }
    private void index(ReactorAccidentLedger ledger) {
        for(var c:ledger.columns)if(!c.settled())custodyByChunk.computeIfAbsent(net.minecraft.world.level.ChunkPos.asLong(c.bottom.getX()>>4,c.bottom.getZ()>>4),k->new ArrayList<>()).add(new Custody(ledger.id,c));
    }
    public int rotation;
    long budgetTime=Long.MIN_VALUE;
    int usedThisTick;
    /** 原动作、冷热投影与火点共用同一维度预算；重复入口不能刷新同tick额度。 */
    public int availableBudget(long tick,int cap) {
        if(budgetTime!=tick){budgetTime=tick;usedThisTick=0;}
        return Math.max(0,Math.min(64,cap)-usedThisTick);
    }
    public void spendBudget(int used) {
        if(used<0||usedThisTick+used>64)throw new IllegalArgumentException("超过同维度事故世界动作预算");usedThisTick+=used;
    }
    public static ReactorAccidentSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(ReactorAccidentSavedData::new,
                ReactorAccidentSavedData::load,null),"create_nuclear_industry_accidents");
    }
    public List<ReactorAccidentLedger> records() { return List.copyOf(ledgers.values()); }
    public ReactorAccidentLedger byGeneration(UUID generation) {
        return ledgers.values().stream().filter(l->l.generation.equals(generation)).findFirst().orElse(null);
    }
    public ReactorAccidentLedger byId(UUID id) { return ledgers.get(id); }
    public void add(ReactorAccidentLedger ledger) {
        if (byGeneration(ledger.generation)!=null) return;
        ledgers.put(ledger.id,ledger); index(ledger);refreshWork(ledger); setDirty();
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var list=new ListTag(); ledgers.values().forEach(l->list.add(l.save(registries))); tag.put("Accidents",list); return tag;
    }
    public static ReactorAccidentSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        var data=new ReactorAccidentSavedData();
        for(Tag t:tag.getList("Accidents",Tag.TAG_COMPOUND)) {var l=ReactorAccidentLedger.load((CompoundTag)t,registries); data.ledgers.put(l.id,l);data.index(l);data.refreshWork(l);}
        return data;
    }
}
