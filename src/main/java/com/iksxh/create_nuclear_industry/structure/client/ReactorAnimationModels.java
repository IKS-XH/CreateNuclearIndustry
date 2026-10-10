package com.iksxh.create_nuclear_industry.structure.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

/** 仅在物理客户端首次模型准备之前同步登记三个动态分件；无重复监听器或几何loader。 */
public final class ReactorAnimationModels {
    public static PartialModel GLOW, SHAFT, HEAD;
    private ReactorAnimationModels() {}
    public static void initializeOnce() {
        if (GLOW != null) return;
        GLOW = model("fuel_rod_glow"); SHAFT = model("control_rod_shaft"); HEAD = model("control_rod_head");
    }
    private static PartialModel model(String name) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath("create_nuclear_industry", "block/reactor_animation/"+name));
    }
}
