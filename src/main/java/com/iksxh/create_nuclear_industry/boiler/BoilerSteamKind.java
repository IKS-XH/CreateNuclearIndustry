package com.iksxh.create_nuclear_industry.boiler;

import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.content.TurbineContent;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;
import net.minecraft.world.level.material.Fluid;

/** 汽口的两项纯过滤选择；不持有mB/HU，也不转换控制器内的实际汽种。 */
public enum BoilerSteamKind implements INamedIconOptions {
    NORMAL, SUPERCRITICAL;

    public Fluid fluid() { return this == NORMAL ? TurbineContent.STEAM.get() : BoilerContent.SUPERCRITICAL_STEAM.get(); }
    @Override public AllIcons getIcon() { return this == NORMAL ? AllIcons.I_PRIORITY_LOW : AllIcons.I_PRIORITY_HIGH; }
    @Override public String getTranslationKey() { return "gui.create_nuclear_industry.boiler.steam_kind." + (this == NORMAL ? "normal" : "supercritical"); }
}
