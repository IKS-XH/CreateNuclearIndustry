package com.iksxh.create_nuclear_industry.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** 独立服务端贮存容量配置；容量只限制新插入，不截断已有完整桶记录。 */
public final class SpentFuelStorageConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue RACK_SLOTS;
    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("spentFuelStorage");
        RACK_SLOTS = builder.comment("每架可插入的单件桶位，范围1～16；减容后超额槽仍可取出。")
                .defineInRange("rackSlots", 16, 1, 16);
        builder.pop();
        SPEC = builder.build();
    }
    private SpentFuelStorageConfig() {}
    /** 随NeoForge SERVER配置同步，真实库存仅由服务端操作。 */
    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, SPEC, "create_nuclear_industry-storage.toml");
    }
    public static int rackSlots() { return RACK_SLOTS.get(); }
}
