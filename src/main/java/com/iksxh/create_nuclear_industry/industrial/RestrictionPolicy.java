package com.iksxh.create_nuclear_industry.industrial;

import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.content.BoilerContent;
import net.neoforged.neoforge.fluids.FluidStack;

/** 受限身份同时包含热液 source/flowing 与 SC；不按温度或流体名称猜测身份。 */
public final class RestrictionPolicy {
    private RestrictionPolicy() {}
    public static boolean restricted(FluidStack stack) {
        return stack != null && !stack.isEmpty() && (ModFluids.isHotCompoundCoolant(stack) || stack.is(BoilerContent.SUPERCRITICAL_STEAM.get()));
    }
}
