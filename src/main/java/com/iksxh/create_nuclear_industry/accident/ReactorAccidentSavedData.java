package com.iksxh.create_nuclear_industry.accident;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** 每维度事故持久化；任务与去重收据独立于仪表生命周期，运行游标不持有强世界引用。 */
public final class ReactorAccidentSavedData extends SavedData {
    private final Map<UUID, ReactorAccidentLedger> ledgers=new LinkedHashMap<>();
    private final Map<UUID,MoltenCoriumState> molten=new LinkedHashMap<>();
    private final Set<UUID> moltenActive=new LinkedHashSet<>();
    private final Map<Long,UUID> moltenPositions=new HashMap<>();
    private final Map<Long,Set<UUID>> moltenChunks=new HashMap<>();
    public MoltenCoriumState molten(UUID id){return molten.get(id);}
    public MoltenCoriumState moltenAt(net.minecraft.core.BlockPos pos){return molten.get(moltenPositions.get(pos.asLong()));}
    public List<MoltenCoriumState> moltenWork(){return moltenActive.stream().map(molten::get).toList();}
    public List<MoltenCoriumState> moltenNear(net.minecraft.world.phys.Vec3 target,int radius){
        var result=new ArrayList<MoltenCoriumState>();
        for(int x=((int)Math.floor(target.x)-radius)>>4;x<=((int)Math.floor(target.x)+radius)>>4;x++)
            for(int z=((int)Math.floor(target.z)-radius)>>4;z<=((int)Math.floor(target.z)+radius)>>4;z++)
                for(var id:moltenChunks.getOrDefault(net.minecraft.world.level.ChunkPos.asLong(x,z),Set.of())){var s=molten.get(id);if(s.phase==MoltenCoriumState.Phase.MOLTEN)result.add(s);}
        return result;
    }
    public void addMolten(MoltenCoriumState state){
        if(molten.containsKey(state.id))return;if(moltenAt(state.pos)!=null)throw new IllegalStateException("熔融物位置已占用");
        molten.put(state.id,state);indexMolten(state);setDirty();
    }
    private void indexMolten(MoltenCoriumState state){
        if(!state.active())return;
        var owner=moltenPositions.putIfAbsent(state.pos.asLong(),state.id);
        if(owner!=null&&!owner.equals(state.id))throw new IllegalStateException("熔融物位置已被另一活跃本体预留");
        moltenActive.add(state.id);
        moltenChunks.computeIfAbsent(net.minecraft.world.level.ChunkPos.asLong(state.pos.getX()>>4,state.pos.getZ()>>4),k->new LinkedHashSet<>()).add(state.id);
    }
    /** 仅更改本体当前索引；终止/移动不扫描全部历史或其它区块。 */
    public void reindexMolten(MoltenCoriumState state,net.minecraft.core.BlockPos old){
        // 先核新位置再撤旧索引；包括未投影预留，冲突不能留下半次移动或覆盖另一真实载荷。
        var owner=moltenPositions.get(state.pos.asLong());
        if(state.active()&&owner!=null&&!owner.equals(state.id))throw new IllegalStateException("熔融物重建索引位置冲突");
        moltenPositions.remove(old.asLong(),state.id);long key=net.minecraft.world.level.ChunkPos.asLong(old.getX()>>4,old.getZ()>>4);
        var chunk=moltenChunks.get(key);if(chunk!=null){chunk.remove(state.id);if(chunk.isEmpty())moltenChunks.remove(key);}
        moltenActive.remove(state.id);indexMolten(state);setDirty();
    }
    public AccidentThermalState heat(UUID id){var ledger=byId(id);if(ledger!=null)return ledger.thermal;var state=molten.get(id);return state==null?null:state.heat();}
    private final Set<UUID> initialActive=new LinkedHashSet<>(), thermalActive=new LinkedHashSet<>();
    public List<ReactorAccidentLedger> initialWork(){return initialActive.stream().map(ledgers::get).toList();}
    public List<ReactorAccidentLedger> thermalWork(){return thermalActive.stream().map(ledgers::get).toList();}
    /** 仅由活跃事故边沿更新索引，不在普通tick扫描全部历史事故。 */
    public void refreshWork(ReactorAccidentLedger ledger) {
        if(ledger.complete())initialActive.remove(ledger.id);else initialActive.add(ledger.id);
        if(ledger.thermal.active())thermalActive.add(ledger.id);else thermalActive.remove(ledger.id);
    }
    public void startSolid(UUID accident,net.minecraft.core.BlockPos pos) {
        var heat=heat(accident);if(heat==null)return;heat.startSolid(pos);var ledger=byId(accident);if(ledger!=null)refreshWork(ledger);setDirty();
    }
    public void removeSolid(UUID accident,net.minecraft.core.BlockPos pos) {
        var heat=heat(accident);if(heat==null)return;var s=heat.solid(pos);
        if(s!=null){heat.endSolid(s);var ledger=byId(accident);if(ledger!=null)refreshWork(ledger);setDirty();}
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
    int capThisTick=64;
    public long budgetActions;
    public long automaticBlockWrites;
    /** 原动作、冷热投影与火点共用同一维度预算；重复入口不能刷新同tick额度。 */
    public int availableBudget(long tick,int cap) {
        if(budgetTime!=tick){budgetTime=tick;usedThisTick=0;capThisTick=64;}
        // 本tick已经收紧的冻结上限不能被本体写守卫的64硬上限再次放大。
        capThisTick=Math.min(capThisTick,Math.max(0,Math.min(64,cap)));
        return Math.max(0,capThisTick-usedThisTick);
    }
    public void spendBudget(int used) {
        if(used<0||usedThisTick+used>capThisTick)throw new IllegalArgumentException("超过同维度事故世界动作预算");usedThisTick+=used;budgetActions+=used;
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
        var list=new ListTag(); ledgers.values().forEach(l->list.add(l.save(registries))); tag.put("Accidents",list);
        var bodies=new ListTag();molten.values().forEach(s->bodies.add(s.save(registries)));tag.put("MoltenBodies",bodies);tag.putLong("BudgetActions",budgetActions);tag.putLong("AutomaticBlockWrites",automaticBlockWrites);
        tag.putLong("BudgetTick",budgetTime);tag.putInt("UsedThisTick",usedThisTick);tag.putInt("CapThisTick",capThisTick);return tag;
    }
    public static ReactorAccidentSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        var data=new ReactorAccidentSavedData();
        data.budgetActions=tag.getLong("BudgetActions");data.automaticBlockWrites=tag.getLong("AutomaticBlockWrites");
        if(tag.contains("BudgetTick")){data.budgetTime=tag.getLong("BudgetTick");data.usedThisTick=tag.getInt("UsedThisTick");data.capThisTick=tag.contains("CapThisTick")?tag.getInt("CapThisTick"):64;}
        for(Tag t:tag.getList("Accidents",Tag.TAG_COMPOUND)) {var l=ReactorAccidentLedger.load((CompoundTag)t,registries); data.ledgers.put(l.id,l);data.index(l);data.refreshWork(l);}
        for(Tag t:tag.getList("MoltenBodies",Tag.TAG_COMPOUND)){var state=MoltenCoriumState.load((CompoundTag)t,registries);data.molten.put(state.id,state);data.indexMolten(state);}
        return data;
    }
}
