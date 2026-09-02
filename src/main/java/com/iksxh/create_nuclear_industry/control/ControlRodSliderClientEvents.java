package com.iksxh.create_nuclear_industry.control;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** 客户端控制棒滑块会话生命周期监听器。 */
@EventBusSubscriber(
        modid = CreateNuclearIndustry.MOD_ID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.GAME
)
public final class ControlRodSliderClientEvents {
    private ControlRodSliderClientEvents() {
    }

    /** Create 数值面板异常关闭或正常关闭后，清除客户端临时拖动会话。 */
    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof ValueSettingsScreen) {
            ControlRodSliderClientAdapter.clearSession();
        }
    }

    /** 玩家退出、切换服务器或重建单人世界时清除客户端临时拖动会话。 */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ControlRodSliderClientAdapter.clearSession();
    }
}
