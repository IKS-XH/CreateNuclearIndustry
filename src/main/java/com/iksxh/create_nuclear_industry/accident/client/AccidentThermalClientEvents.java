package com.iksxh.create_nuclear_industry.accident.client;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/** 只注册原版熔岩纹理，服务端不加载客户端渲染类型；液面仍走原生LiquidBlockRenderer。 */
public final class AccidentThermalClientEvents {
    private AccidentThermalClientEvents(){}
    public static void initialize(java.util.function.Consumer<IClientFluidTypeExtensions> consumer){
        consumer.accept(new IClientFluidTypeExtensions(){
            @Override public ResourceLocation getStillTexture(){return ResourceLocation.withDefaultNamespace("block/lava_still");}
            @Override public ResourceLocation getFlowingTexture(){return ResourceLocation.withDefaultNamespace("block/lava_flow");}
        });
    }
}
