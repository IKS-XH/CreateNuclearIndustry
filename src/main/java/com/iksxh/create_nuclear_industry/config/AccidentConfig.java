package com.iksxh.create_nuclear_industry.config;

import com.iksxh.create_nuclear_industry.accident.AccidentParameters;
import com.iksxh.create_nuclear_industry.accident.AccidentThermalParameters;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/** 独立SERVER事故配置；原子校验整组，旧任务保留创建时规则，非法值整组回退。 */
public final class AccidentConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.ConfigValue<Boolean> DESTRUCTION;
    private static final ModConfigSpec.ConfigValue<Integer> EXTENSION, EXTERNAL, BUDGET;
    private static final ModConfigSpec.ConfigValue<Double> SHOCK,SOLID_DAMAGE;
    private static final ModConfigSpec.ConfigValue<Integer> SOLID_TICKS,WATER_RATE,FIRE_LIMIT,FIRE_TICKS,SOLID_IGNITE;
    private static volatile AccidentParameters current = AccidentParameters.DEFAULT;
    private static volatile AccidentThermalParameters thermal=AccidentThermalParameters.DEFAULT;
    private static long warningTime;
    static {
        var b = new ModConfigSpec.Builder();
        DESTRUCTION = b.define("shellAndExternalDestruction", true);
        EXTENSION = b.define("extensionBlocks", 2); EXTERNAL = b.define("externalBlockLimit", 32);
        SHOCK = b.define("shockHealthPoints", 8D); BUDGET = b.define("worldActionsPerTick", 64);
        SOLID_TICKS=b.define("solidCoolingLoadedTicks",2400);WATER_RATE=b.define("waterCoolingRate",4);
        FIRE_LIMIT=b.define("firePointLimit",8);FIRE_TICKS=b.define("fireLoadedTicks",600);
        SOLID_DAMAGE=b.define("solidContactDamageHp",2D);SOLID_IGNITE=b.define("solidContactIgniteSeconds",3);
        SPEC = b.build();
    }
    private AccidentConfig() {}
    public static AccidentParameters parameters() { return current; }
    public static AccidentThermalParameters thermalParameters(){return thermal;}
    public static void register(ModContainer container, IEventBus bus) {
        container.registerConfig(ModConfig.Type.SERVER, SPEC, "create_nuclear_industry-accident-server.toml");
        bus.addListener(AccidentConfig::reload);
    }
    private static void reload(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC || event instanceof ModConfigEvent.Unloading) return;
        try {
            var next=new AccidentParameters(DESTRUCTION.get(), EXTENSION.get(), EXTERNAL.get(), SHOCK.get(), BUDGET.get());
            var nextThermal=new AccidentThermalParameters(SOLID_TICKS.get(),WATER_RATE.get(),FIRE_LIMIT.get(),FIRE_TICKS.get(),SOLID_DAMAGE.get(),SOLID_IGNITE.get());
            current=next;thermal=nextThermal;
        }
        catch (RuntimeException invalid) {
            current = AccidentParameters.DEFAULT;thermal=AccidentThermalParameters.DEFAULT; long now = System.nanoTime();
            if (warningTime == 0 || now - warningTime > 60_000_000_000L) {
                com.mojang.logging.LogUtils.getLogger().warn("事故配置组非法，回退默认值：{}", invalid.toString()); warningTime = now;
            }
        }
    }
}
