package com.iksxh.create_nuclear_industry.automation.client;

import com.iksxh.create_nuclear_industry.automation.*;
import com.iksxh.create_nuclear_industry.automation.gui.*;
import com.iksxh.create_nuclear_industry.content.ReactorAutomationContent;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** 客户端仅更新当前菜单投影；失效或其他container回包不能复活旧面板。 */
@EventBusSubscriber(modid="create_nuclear_industry",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class ReactorAutomationClientEvents {
    private ReactorAutomationClientEvents() { }
    @SubscribeEvent public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ReactorAutomationContent.ADMINISTRATOR_MENU.get(),ReactorAdministratorScreen::new);
    }
    public static void apply(ReactorAutomationNetwork.Reply reply) {
        var mc=Minecraft.getInstance();
        if(mc.player==null || !(mc.player.containerMenu instanceof ReactorAdministratorMenu menu) || menu.containerId!=reply.containerId() || !menu.sessionId().equals(reply.sessionId())) return;
        var projection=new ReactorAdministratorProjection(reply.view());
        menu.receive(projection);
        if(mc.screen instanceof ReactorAdministratorScreen screen) screen.receive(reply);
    }
    /** 已撤下世界控件入口；保留旧开发夹具签名但不注册或发送任何写包。 */
    public static void prepare(ReactorAutomationBehaviours.Control control,ValueSettingsBoard board) { }
    public static void send(ReactorAutomationBehaviours.Control control,int value) { }
    /** 仅Ctrl+主手扳手的有效目标拦截原右键；普通扳手、潜行拆除、喂料及副手不受影响。 */
    @EventBusSubscriber(modid="create_nuclear_industry",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.GAME)
    public static final class WorldBinding {
        private static long sequence;
        private WorldBinding() { }
        @SubscribeEvent public static void interact(net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered event) {
            var mc=Minecraft.getInstance();
            if(!event.isUseItem()||event.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND||mc.player==null||mc.level==null
                    ||!net.minecraft.client.gui.screens.Screen.hasControlDown()||mc.player.isShiftKeyDown()
                    ||!com.simibubi.create.AllItems.WRENCH.isIn(mc.player.getMainHandItem())
                    ||!(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit))return;
            var target=mc.level.getBlockEntity(hit.getBlockPos());ReactorStressGaugeService.Intent intent;
            if(target instanceof com.simibubi.create.content.kinetics.gauge.StressGaugeBlockEntity) {
                intent=new ReactorStressGaugeService.Intent(true,hit.getBlockPos(),net.minecraft.core.BlockPos.ZERO,new java.util.UUID(0,0),0,++sequence);
            } else if(target instanceof ReactorAutomationBlazeBridge.Access access&&access.cniAutomationView().hasUUID("Generation")) {
                var view=access.cniAutomationView();intent=new ReactorStressGaugeService.Intent(false,hit.getBlockPos(),net.minecraft.core.BlockPos.of(view.getLong("Port")),view.getUUID("Generation"),view.getLong("Epoch"),++sequence);
            } else return;
            event.setCanceled(true);event.setSwingHand(false);net.neoforged.neoforge.network.PacketDistributor.sendToServer(new ReactorAutomationNetwork.Bind(intent));
        }
    }
}
