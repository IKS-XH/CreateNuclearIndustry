package com.iksxh.create_nuclear_industry.boiler;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** 每维度锅炉事故计划和活跃索引；普通tick只调度尚未完成者，控制器消失不丢收据。 */
public final class BoilerAccidentSavedData extends SavedData {
    private final Map<UUID,BoilerAccidentLedger> records=new LinkedHashMap<>();
    private final Set<UUID> active=new LinkedHashSet<>();
    private final Map<UUID,UUID> activeGenerations=new HashMap<>();
    public static BoilerAccidentSavedData get(ServerLevel level){return level.getDataStorage().computeIfAbsent(new Factory<>(BoilerAccidentSavedData::new,BoilerAccidentSavedData::load,null),"create_nuclear_industry_boiler_accidents");}
    public List<BoilerAccidentLedger> active(){return active.stream().map(records::get).toList();}
    public BoilerAccidentLedger byId(UUID id){return records.get(id);}
    public BoilerAccidentLedger activeGeneration(UUID generation){return records.get(activeGenerations.get(generation));}
    public void add(BoilerAccidentLedger l){if(records.containsKey(l.id)||activeGeneration(l.generation)!=null)throw new IllegalStateException("锅炉事故重复提交");records.put(l.id,l);active.add(l.id);activeGenerations.put(l.generation,l.id);setDirty();}
    public void refresh(BoilerAccidentLedger l){if(l.complete()){active.remove(l.id);activeGenerations.remove(l.generation,l.id);}setDirty();}
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){var list=new ListTag();records.values().forEach(l->list.add(l.save()));tag.put("Accidents",list);return tag;}
    public static BoilerAccidentSavedData load(CompoundTag tag,HolderLookup.Provider registries){var data=new BoilerAccidentSavedData();for(Tag item:tag.getList("Accidents",Tag.TAG_COMPOUND)){var l=BoilerAccidentLedger.load((CompoundTag)item,registries);data.records.put(l.id,l);if(!l.complete()){data.active.add(l.id);data.activeGenerations.put(l.generation,l.id);}}return data;}
}
