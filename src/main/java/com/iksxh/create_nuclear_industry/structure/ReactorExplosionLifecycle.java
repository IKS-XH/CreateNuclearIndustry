package com.iksxh.create_nuclear_industry.structure;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** 服务端外爆最终删块前接管；仅查询已加载所有者与最终命中的缓存成员，不扫描世界体积。 */
public final class ReactorExplosionLifecycle {
    private ReactorExplosionLifecycle() {}

    /**
     * 在首个成员取loot前为所有受损机组完成只读预检，再统一提交原事故事务。
     * KEEP/TRIGGER_BLOCK不拆结构；冷态计划不执行玩家拆除清空，也不引入新危险阈值。
     * 返回false时调用者清空本轮破坏及发包列表，防止先掉料或客户端误删块，并继续原生收尾。
     */
    public static boolean beforeBlockDestruction(ServerLevel level, Explosion.BlockInteraction interaction,
            List<BlockPos> finalBlocks) {
        if (interaction != Explosion.BlockInteraction.DESTROY
                && interaction != Explosion.BlockInteraction.DESTROY_WITH_DECAY) return true;
        Map<ReactorInstrumentPortBlockEntity, ReactorDisassemblyPlan> plans = new LinkedHashMap<>();
        var inspected = new LinkedHashSet<ReactorInstrumentPortBlockEntity>();
        for (BlockPos pos : finalBlocks) {
            if (!level.hasChunkAt(pos)
                    || level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) continue;
            for (var owner : ReactorStructureLifecycle.loadedOwnersNear(level, pos)) {
                if (inspected.contains(owner) || !owner.structureValid()) continue;
                var plan = owner.prepareDisassemblyPlan(pos, level.getBlockState(pos));
                if (plan == null) continue;
                // 只有确认缓存成员后才登记已检查；每个所有者的196列端口最多捕获一次，冷态也不重复复制。
                inspected.add(owner);
                if (!plan.shutdownAssessment().inputValid()) {
                    com.mojang.logging.LogUtils.getLogger().error("反应堆外爆预检失败，保留本轮方块与物料：仪表={} 命中={}",
                            owner.getBlockPos(), pos);
                    return false;
                }
                if (plan.action() == ReactorDisassemblyPlan.Action.DANGEROUS) plans.put(owner, plan);
            }
        }
        return ReactorStructureLifecycle.commitExternalExplosionPlans(List.copyOf(plans.values()));
    }
}
