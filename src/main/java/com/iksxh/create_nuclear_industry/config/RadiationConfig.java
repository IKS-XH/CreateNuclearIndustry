package com.iksxh.create_nuclear_industry.config;

import com.iksxh.create_nuclear_industry.radiation.RadiationParameters;
import com.mojang.logging.LogUtils;
import java.util.*;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.bus.api.IEventBus;

/** 独立SERVER配置；加载/重载以完整不可变组原子替换，非法组合回退默认并节流告警。 */
public final class RadiationConfig {
    private static final Map<String, ModConfigSpec.ConfigValue<?>> VALUES = new LinkedHashMap<>();
    public static final ModConfigSpec SPEC;
    private static volatile RadiationParameters current = RadiationParameters.DEFAULT;
    private static long lastWarning;
    static {
        var b = new ModConfigSpec.Builder();
        add(b,"general","enabled",true); add(b,"general","sampleIntervalTicks",20); add(b,"general","detectionRadiusBlocks",32);
        String[] names = {"usedFuelDosePerSecond","spentFuelDosePerSecond","accidentWasteDosePerSecond","maxFieldDosePerSecond"};
        double[] values = {1,2,4,10}; for(int i=0;i<names.length;i++) add(b,"sources",names[i],values[i]);
        names = new String[]{"caskTransmission","rackTransmission","reactorTransmission","blockTransmission"};
        values = new double[]{.05,0,0,.05}; for(int i=0;i<names.length;i++) add(b,"shielding",names[i],values[i]);
        names = new String[]{"safeAbsorptionPerSecond","maxDose","warningDose","symptomDose","damageDose","recoveryPerSecond","damageHealthPoints"};
        values = new double[]{.01,100,25,50,80,1,1}; for(int i=0;i<names.length;i++) add(b,"dose",names[i],values[i]);
        add(b,"dose","recoveryDelayTicks",100); add(b,"dose","damageIntervalTicks",40);
        names = new String[]{"head","chest","legs","feet","maxReduction"}; values = new double[]{.25,.30,.25,.15,.95};
        for(int i=0;i<names.length;i++) add(b,"protection",names[i],values[i]); SPEC=b.build();
    }
    private RadiationConfig() {}
    private static <T> void add(ModConfigSpec.Builder b,String group,String key,T value) {
        VALUES.put(key,b.define(List.of(group,key),value));
    }
    private static double d(String key) { return ((Number)VALUES.get(key).get()).doubleValue(); }
    private static int i(String key) { return ((Number)VALUES.get(key).get()).intValue(); }
    public static RadiationParameters parameters() { return current; }
    public static void register(ModContainer container, IEventBus bus) {
        container.registerConfig(ModConfig.Type.SERVER,SPEC,"create_nuclear_industry-radiation-server.toml");
        bus.addListener(RadiationConfig::reload);
    }
    private static void reload(ModConfigEvent event) {
        if(event.getConfig().getSpec()!=SPEC || event instanceof ModConfigEvent.Unloading) return;
        try {
            current=new RadiationParameters((Boolean)VALUES.get("enabled").get(),i("sampleIntervalTicks"),i("detectionRadiusBlocks"),
                    d("usedFuelDosePerSecond"),d("spentFuelDosePerSecond"),d("accidentWasteDosePerSecond"),d("maxFieldDosePerSecond"),
                    d("caskTransmission"),d("rackTransmission"),d("reactorTransmission"),d("blockTransmission"),d("safeAbsorptionPerSecond"),
                    d("maxDose"),d("warningDose"),d("symptomDose"),d("damageDose"),i("recoveryDelayTicks"),d("recoveryPerSecond"),
                    i("damageIntervalTicks"),d("damageHealthPoints"),d("head"),d("chest"),d("legs"),d("feet"),d("maxReduction"));
        } catch(RuntimeException invalid) {
            current=RadiationParameters.DEFAULT;
            long now=System.nanoTime();
            if(lastWarning==0 || now-lastWarning>60_000_000_000L) {
                LogUtils.getLogger().warn("辐射配置组非法，整组回退默认值：{}",invalid.toString()); lastWarning=now;
            }
        }
    }
}
