package com.iksxh.create_nuclear_industry;

import com.iksxh.create_nuclear_industry.content.ModBlocks;
import com.iksxh.create_nuclear_industry.content.ModCreativeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(CreateNuclearIndustry.MOD_ID)
public final class CreateNuclearIndustry {
    public static final String MOD_ID = "create_nuclear_industry";

    public CreateNuclearIndustry(IEventBus modEventBus) {
        ModBlocks.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
    }
}
