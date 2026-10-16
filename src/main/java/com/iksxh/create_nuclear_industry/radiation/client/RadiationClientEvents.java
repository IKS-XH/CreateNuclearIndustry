package com.iksxh.create_nuclear_industry.radiation.client;

import com.iksxh.create_nuclear_industry.radiation.*;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyItemCodec;
import com.iksxh.create_nuclear_industry.storage.SpentFuelPayload;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** 客户端仅显示同世界、未过期的服务端读数；右上角留开Create左侧护目镜区域。 */
@EventBusSubscriber(modid="create_nuclear_industry",value=Dist.CLIENT)
public final class RadiationClientEvents {
    private static RadiationExposurePayload sample;
    private static Object world;
    private static long lastSoundTick=Long.MIN_VALUE;
    private RadiationClientEvents() {}
    public static void receive(RadiationExposurePayload next) {
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null||!mc.level.dimension().location().equals(next.dimension()))return;
        if(next.gameTime()>mc.level.getGameTime()+40||mc.level.getGameTime()-next.gameTime()>60)return;
        if(world!=mc.level){sample=null;world=mc.level;}
        if(!next.supersedes(sample))return;
        if(!Double.isFinite(next.field())||!Double.isFinite(next.dose())||!Double.isFinite(next.absorbed()))return;
        sample=next;
        long soundInterval=next.field()>=1?20:next.field()>=.1?40:100;
        if(next.complete()&&next.field()>.01&&RadiationEquipment.holdingMeter(mc.player)
                &&(lastSoundTick==Long.MIN_VALUE||next.gameTime()-lastSoundTick>=soundInterval)) {
            mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(),.12F,(float)Math.clamp(.7+next.field()*.1,.7,1.7));
            lastSoundTick=next.gameTime();
        }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {sample=null;world=null;lastSoundTick=Long.MIN_VALUE;}
    @SubscribeEvent public static void render(RenderGuiEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(mc.level!=world){sample=null;world=mc.level;lastSoundTick=Long.MIN_VALUE;}
        if(mc.level==null||mc.player==null||sample==null||!RadiationEquipment.holdingMeter(mc.player)
                ||mc.options.hideGui||mc.level.getGameTime()-sample.gameTime()>60)return;
        Component[] lines=sample.complete()?new Component[]{
                Component.translatable("radiation.create_nuclear_industry.field",format(sample.field())),
                Component.translatable("radiation.create_nuclear_industry.dose",format(sample.dose())),
                Component.translatable("radiation.create_nuclear_industry.absorbed",format(sample.absorbed()))}
                :new Component[]{Component.translatable("radiation.create_nuclear_industry.sampling")};
        for(int row=0;row<lines.length;row++) {
            int x=Math.max(4,event.getGuiGraphics().guiWidth()-mc.font.width(lines[row])-6);
            event.getGuiGraphics().drawString(mc.font,lines[row],x,66+row*10,0xFFE080,true);
        }
    }
    private static String format(double value) {return String.format(Locale.ROOT,"%.2f",value);}
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        var item=event.getItemStack();
        if(FuelAssemblyItemCodec.isFreshFuel(item)&&FuelUseHistory.isUsed(item))event.getToolTip().add(Component.translatable("radiation.create_nuclear_industry.used"));
        if(FuelAssemblyItemCodec.isCooledSpentFuel(item))event.getToolTip().add(Component.translatable("radiation.create_nuclear_industry.spent"));
        if(SpentFuelPayload.isValid(item))event.getToolTip().add(Component.translatable("radiation.create_nuclear_industry.cask"));
    }
}
