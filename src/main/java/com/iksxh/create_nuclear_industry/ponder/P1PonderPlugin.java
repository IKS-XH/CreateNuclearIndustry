package com.iksxh.create_nuclear_industry.ponder;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.content.P1ContentIds;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * P1 Ponder 条目索引。
 *
 * <p>这里列出的每个组件都已由模组注册，并指向介绍或使用该组件的四条反应堆教学故事线之一。
 * 未来机器应在其注册任务完成后建立独立条目，不能提前写入未注册内容。</p>
 *
 * <p>Ponder 运行在客户端临时世界中，只提供教学演示，不拥有服务端反应堆状态，也不替代
 * GameTest 或正式结构扫描。</p>
 */
public final class P1PonderPlugin implements PonderPlugin {
    /** 正式 P1 实验反应堆教学故事线的注册路径。 */
    public static final String REACTOR_SCENE_ID = P1ContentIds.EXPERIMENTAL_REACTOR_ID;
    /** 四条独立反应堆故事线按搭建、棒列关系、运行停机、装料换料顺序注册。 */
    public static final List<String> REACTOR_SCENE_IDS = List.of(
            REACTOR_SCENE_ID,
            "experimental_reactor_rods",
            "experimental_reactor_operation",
            "experimental_reactor_refueling"
    );

    /** 锅炉三条独立故事线按搭建、运行和出汽调压顺序注册。 */
    public static final List<String> BOILER_SCENE_IDS = List.of(
            "high_pressure_boiler_build",
            "high_pressure_boiler_operation",
            "high_pressure_boiler_steam"
    );

    /** 三幕共用的九种锅炉部件；每个入口均已有正式方块注册。 */
    public static final List<String> BOILER_ENTRY_IDS = List.of(
            "high_pressure_boiler_casing",
            "high_pressure_boiler_window",
            "high_pressure_boiler_water_port",
            "high_pressure_boiler_steam_port",
            "high_pressure_boiler_hot_coolant_port",
            "high_pressure_boiler_cold_coolant_port",
            "boiler_safety_valve",
            "boiler_heat_exchange_section",
            "high_pressure_boiler_controller"
    );

    /**
     * P1 条目列表故意排除 P0 探针、保留的样例外壳、纯材料和不可直接放置的控制棒组件。
     */
    public static final List<String> DIRECT_ENTRY_IDS = List.of(
            P1ContentIds.REACTOR_CASING_ID,
            P1ContentIds.REACTOR_WINDOW_ID,
            P1ContentIds.REACTOR_INSTRUMENT_PORT_ID,
            P1ContentIds.REACTOR_COLD_PORT_ID,
            P1ContentIds.REACTOR_HOT_PORT_ID,
            P1ContentIds.REACTOR_REFUELING_PORT_ID,
            P1ContentIds.REACTOR_FUEL_ROD_ID,
            P1ContentIds.CONTROL_ROD_DRIVE_ID,
            P1ContentIds.FRESH_FUEL_ASSEMBLY_ID,
            P1ContentIds.COOLED_SPENT_FUEL_ASSEMBLY_ID,
            P1ContentIds.COMPOUND_COOLANT_BUCKET_ID
    );

    /** 返回本插件所属模组 ID，供 Ponder 将条目限制在本模组命名空间内。 */
    @Override
    public String getModId() {
        return CreateNuclearIndustry.MOD_ID;
    }

    /** 保留离心机和反应堆原有绑定，并将锅炉部件挂接到本批三条独立故事线。 */
    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        var reactorEntries = helper.forComponents(DIRECT_ENTRY_IDS.stream()
                .map(P1PonderPlugin::componentId)
                .toList());
        reactorEntries.addStoryBoard(componentId(REACTOR_SCENE_IDS.get(0)),
                P1PonderScenes::experimentalReactorBasics);
        reactorEntries.addStoryBoard(componentId(REACTOR_SCENE_IDS.get(1)),
                P1PonderScenes::experimentalReactorRods);
        reactorEntries.addStoryBoard(componentId(REACTOR_SCENE_IDS.get(2)),
                P1PonderScenes::experimentalReactorOperation);
        reactorEntries.addStoryBoard(componentId(REACTOR_SCENE_IDS.get(3)),
                P1PonderScenes::experimentalReactorRefueling);
        helper.forComponents(componentId("enrichment_centrifuge"))
                .addStoryBoard(
                        ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, "enrichment_centrifuge"),
                        CentrifugePonderScenes::enrichmentCentrifugeBasics
                );
        var boilerEntries = helper.forComponents(BOILER_ENTRY_IDS.stream()
                .map(P1PonderPlugin::componentId)
                .toList());
        boilerEntries.addStoryBoard(componentId(BOILER_SCENE_IDS.get(0)),
                BoilerPonderScenes::highPressureBoilerBuild);
        boilerEntries.addStoryBoard(componentId(BOILER_SCENE_IDS.get(1)),
                BoilerPonderScenes::highPressureBoilerOperation);
        boilerEntries.addStoryBoard(componentId(BOILER_SCENE_IDS.get(2)),
                BoilerPonderScenes::highPressureBoilerSteam);
    }

    /** 将 P1 内容路径转换为本模组命名空间下的 Ponder 组件 ID。 */
    private static ResourceLocation componentId(String path) {
        return ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, path);
    }
}
