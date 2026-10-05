package com.iksxh.create_nuclear_industry.goggle;

import com.iksxh.create_nuclear_industry.goggle.client.GoggleTooltipClient;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/** 护目镜标题的共用排版入口；服务端调用时原样返回组件，不触碰客户端字体。 */
public final class GoggleTooltip {
    private GoggleTooltip() {}

    /** 在客户端为设备首行预留Create图标空间；服务端测试与专服保持原始组件。 */
    public static Component indentFirstLine(Component component) {
        return FMLEnvironment.dist == Dist.CLIENT
                ? GoggleTooltipClient.indentFirstLine(component) : component;
    }
}
