package com.iksxh.create_nuclear_industry.accident.client;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/** 仅绑定本模组熔融物sprite；沿用既有光照和原生LiquidBlockRenderer，服务端不加载客户端类型。 */
public final class AccidentThermalClientEvents {
    private AccidentThermalClientEvents(){}
    public static void initialize(java.util.function.Consumer<IClientFluidTypeExtensions> consumer){
        consumer.accept(new IClientFluidTypeExtensions(){
            @Override public ResourceLocation getStillTexture(){return ResourceLocation.fromNamespaceAndPath("create_nuclear_industry","fluid/molten_corium_still");}
            @Override public ResourceLocation getFlowingTexture(){return ResourceLocation.fromNamespaceAndPath("create_nuclear_industry","fluid/molten_corium_flow");}
        });
    }
}
