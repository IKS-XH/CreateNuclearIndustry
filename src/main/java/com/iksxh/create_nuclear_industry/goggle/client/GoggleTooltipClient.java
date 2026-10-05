package com.iksxh.create_nuclear_industry.goggle.client;

import com.simibubi.create.foundation.utility.CreateLang;
import java.util.ArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** 仅在物理客户端调用Create的字体测量，生成护目镜图标避让格式。 */
@OnlyIn(Dist.CLIENT)
public final class GoggleTooltipClient {
    private GoggleTooltipClient() {}

    /** 使用Create原生forGoggles格式按当前字体宽度为一行标题保留16像素。 */
    public static Component indentFirstLine(Component component) {
        var line = new ArrayList<MutableComponent>(1);
        CreateLang.builder().add(component).forGoggles(line);
        return line.get(0);
    }
}
