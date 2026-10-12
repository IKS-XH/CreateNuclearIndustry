package com.iksxh.create_nuclear_industry.turbine.display;

import com.iksxh.create_nuclear_industry.turbine.TurbineRotorBlockEntity;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/** 仅物理客户端装载：已知转子的弱引用集合，真实暂停冻结年龄，世界/区块结束仍立即撤销。 */
@EventBusSubscriber(modid="create_nuclear_industry",value=Dist.CLIENT)
public final class TurbineRuntimeClientEvents {
    private static final TurbineRuntimeClock CLOCK=new TurbineRuntimeClock();
    private static final Map<TurbineRotorBlockEntity,Boolean> ROTORS=new WeakHashMap<>();
    private static WeakReference<ClientLevel> world=new WeakReference<>(null);
    private static WeakReference<ClientLevel> ended=new WeakReference<>(null);
    private TurbineRuntimeClientEvents(){ }
    private static void synchronize() {
        ClientLevel next=Minecraft.getInstance().level;
        if(next!=null&&next==ended.get())next=null;
        if(next==world.get())return;
        ROTORS.keySet().forEach(TurbineRotorBlockEntity::revokeRuntimeClient);ROTORS.clear();
        world=new WeakReference<>(next);CLOCK.advance(next,true);
    }
    @SubscribeEvent public static void onTick(ClientTickEvent.Pre event) {
        synchronize();var current=world.get();if(current==null)return;
        CLOCK.advance(current,Minecraft.getInstance().isPaused());
        for(var rotor:List.copyOf(ROTORS.keySet())) {
            if(rotor.isRemoved()||rotor.getLevel()!=current){rotor.revokeRuntimeClient();ROTORS.remove(rotor);}
            else rotor.tickRuntimeClient(CLOCK.tick(current));
        }
    }
    @SubscribeEvent public static void onUpdate(TurbineRuntimeSyncEvent event) {
        if(!Minecraft.getInstance().isSameThread())return;
        synchronize();var current=world.get();var rotor=event.rotor();
        if(current==null||rotor.getLevel()!=current||rotor.isRemoved())return;
        ROTORS.put(rotor,Boolean.TRUE);rotor.acceptRuntimeClient(CLOCK.tick(current));
    }
    @SubscribeEvent public static void onChunkUnload(ChunkEvent.Unload event) {
        if(event.getLevel()!=world.get())return;
        for(var rotor:ROTORS.keySet())rotor.unloadRuntimeChunk(event.getChunk().getPos());
    }
    @SubscribeEvent public static void onWorldUnload(LevelEvent.Unload event) {
        if(event.getLevel()!=world.get())return;
        ended=new WeakReference<>(world.get());
        ROTORS.keySet().forEach(TurbineRotorBlockEntity::revokeRuntimeClient);ROTORS.clear();
        world=new WeakReference<>(null);CLOCK.advance(null,true);
    }
}
