package com.iksxh.create_nuclear_industry.turbine.display;

import com.iksxh.create_nuclear_industry.content.TurbineContent;
import com.iksxh.create_nuclear_industry.turbine.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** 仅遍历 onLoad 登记的有限机主弱引用；世界 Post 在 BE/Create 动力 tick 完成后采样，不修改动力。 */
@EventBusSubscriber(modid="create_nuclear_industry")
public final class TurbineRuntimePublisher {
    private static final Map<Level,Map<BlockPos,Entry>> LEVELS=new WeakHashMap<>();
    private TurbineRuntimePublisher() { }
    /** 仅逻辑服务端 onLoad 登记，既不搜索世界也不持有强 BE/世界引用。 */
    public static void register(TurbineControllerBlockEntity owner) {
        Level level=owner.getLevel();if(level==null||level.isClientSide)return;
        var entries=LEVELS.computeIfAbsent(level,ignored->new HashMap<>());
        Entry old=entries.put(owner.getBlockPos(),new Entry(owner));if(old!=null)clear(old);
    }
    /** 成型失效立即撤销已发布成员；后续恢复只能由 Post 的完整新样本发布。 */
    public static void revoke(TurbineControllerBlockEntity owner) {
        if(owner.getLevel()==null||owner.getLevel().isClientSide)return;
        var entries=LEVELS.get(owner.getLevel());if(entries==null)return;
        Entry entry=entries.get(owner.getBlockPos());
        if(entry!=null&&entry.owner.get()==owner){clear(entry);entry.nextTick=0;}
    }
    /** 卸载/移除时清除当前实例登记；旧回调不得清理同坐标新控制器。 */
    public static void unregister(TurbineControllerBlockEntity owner) {
        if(owner.getLevel()==null||owner.getLevel().isClientSide)return;
        var entries=LEVELS.get(owner.getLevel());if(entries==null)return;
        Entry entry=entries.get(owner.getBlockPos());
        if(entry!=null&&entry.owner.get()==owner){clear(entry);entries.remove(owner.getBlockPos());}
        if(entries.isEmpty())LEVELS.remove(owner.getLevel());
    }
    @SubscribeEvent
    public static void onLevelPost(LevelTickEvent.Post event) {
        if(!(event.getLevel() instanceof ServerLevel level))return;
        var entries=LEVELS.get(level);if(entries==null)return;
        var iterator=entries.values().iterator();
        while(iterator.hasNext()) {
            Entry entry=iterator.next();var owner=entry.owner.get();
            if(owner==null||owner.isRemoved()){clear(entry);iterator.remove();continue;}
            long now=level.getGameTime();
            if(now<entry.nextTick)continue;
            entry.nextTick=now+5;
            publish(level,owner,entry,now);
        }
    }
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if(event.getLevel() instanceof Level level&&!level.isClientSide) {
            var entries=LEVELS.remove(level);if(entries!=null)entries.values().forEach(TurbineRuntimePublisher::clear);
        }
    }
    private static void publish(ServerLevel level,TurbineControllerBlockEntity owner,Entry entry,long now) {
        var form=owner.claimedForm();
        if(form==null||!level.hasChunkAt(owner.getBlockPos())||level.getBlockEntity(owner.getBlockPos())!=owner
                || !owner.validShaft(form.front(),TurbineShaftBlock.End.FRONT)
                || !owner.validShaft(form.rear(),TurbineShaftBlock.End.REAR)
                || !(level.getBlockEntity(form.front()) instanceof TurbineOutputShaftBlockEntity front)
                || !(level.getBlockEntity(form.rear()) instanceof TurbineOutputShaftBlockEntity rear)) {clear(entry);return;}
        // getSpeed 包含原生过载/冻结的 0；两端暂不同步时不拼成可靠样本，等待下一完整动力轮。
        float speed=front.getSpeed();
        if(!Float.isFinite(speed)||Float.compare(speed,rear.getSpeed())!=0){clear(entry);return;}
        List<TurbineRotorBlockEntity> members=new ArrayList<>();
        for(int i=0;i<form.rotors();i++) {
            BlockPos pos=TurbineStructure.at(form.front(),form.facing(),0,0,i+1);
            if(!TurbineStructure.ticking(level,pos)||!(level.getBlockEntity(pos) instanceof TurbineRotorBlockEntity rotor)
                    ||rotor.isRemoved()||!form.contains(pos)||!rotor.getBlockState().is(TurbineContent.ROTOR.get())
                    ||!rotor.getBlockState().getValue(TurbinePartBlock.FORMED)
                    ||rotor.getBlockState().getValue(TurbinePartBlock.MACHINE_FACING)!=form.facing()
                    ||rotor.getBlockState().getValue(TurbinePartBlock.DIAMETER).blocks()!=form.diameter()) {clear(entry);return;}
            members.add(rotor);
        }
        // 先构造全部同龄样本，任意成员无效时整组不发布；旧对象不能因当前位置的新转子而继续显示。
        List<TurbineRuntimeSample> samples=new ArrayList<>();
        try {
            for(int i=0;i<members.size();i++)samples.add(new TurbineRuntimeSample(level.dimension().location().toString(),
                    owner.getBlockPos(),owner.runtimeGeneration(),owner.runtimeEpoch(),form.front(),form.facing(),
                    form.diameter(),form.rotors(),i,members.get(i).runtimeMemberGeneration(),now,TurbineRuntimeSample.localRpm(form.facing(),speed)));
        } catch(IllegalArgumentException invalid){clear(entry);return;}
        for(var old:entry.members){var rotor=old.get();if(rotor!=null&&!members.contains(rotor))rotor.publishRuntime(null);}
        entry.members.clear();
        for(int i=0;i<members.size();i++){var rotor=members.get(i);rotor.publishRuntime(samples.get(i));entry.members.add(new WeakReference<>(rotor));}
    }
    private static void clear(Entry entry) {
        for(var reference:entry.members){var rotor=reference.get();if(rotor!=null)rotor.publishRuntime(null);}
        entry.members.clear();
    }
    private static final class Entry {
        private final WeakReference<TurbineControllerBlockEntity> owner;
        private final List<WeakReference<TurbineRotorBlockEntity>> members=new ArrayList<>();
        private long nextTick;
        private Entry(TurbineControllerBlockEntity owner){this.owner=new WeakReference<>(owner);}
    }
}
