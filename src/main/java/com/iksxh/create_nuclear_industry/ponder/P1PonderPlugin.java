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
 * <p>这里列出的每个组件都已由模组注册，并指向介绍或使用该组件的同一条反应堆教学故事线。
 * 未来机器应在其注册任务完成后建立独立条目，不能提前写入未注册内容。</p>
 *
 * <p>Ponder 运行在客户端临时世界中，只提供教学演示，不拥有服务端反应堆状态，也不替代
 * GameTest 或正式结构扫描。</p>
 */
public final class P1PonderPlugin implements PonderPlugin {
    /** 正式 P1 实验反应堆教学故事线的注册路径。 */
    public static final String REACTOR_SCENE_ID = P1ContentIds.EXPERIMENTAL_REACTOR_ID;

    /**
     * P1 条目列表故意排除 P0 探针、保留的样例外壳、纯材料和不可直接放置的控制棒组件。
     * 后者只有在后续故事线讲解装配用途时，才通过控制棒驱动器条目间接展示。
     */
    public static final List<String> COVERED_COMPONENT_IDS = List.of(
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

    /** 将已注册组件绑定到唯一 P1 反应堆故事线，不创建或修改服务端内容。 */
    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(COVERED_COMPONENT_IDS.stream()
                        .map(P1PonderPlugin::componentId)
                .toList())
                .addStoryBoard(
                        ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, REACTOR_SCENE_ID),
                        P1PonderScenes::experimentalReactorBasics
                );
    }

    /** 将 P1 内容路径转换为本模组命名空间下的 Ponder 组件 ID。 */
    private static ResourceLocation componentId(String path) {
        return ResourceLocation.fromNamespaceAndPath(CreateNuclearIndustry.MOD_ID, path);
    }
}
