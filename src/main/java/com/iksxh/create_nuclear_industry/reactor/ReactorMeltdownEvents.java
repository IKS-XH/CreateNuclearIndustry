package com.iksxh.create_nuclear_industry.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Objects;

/**
 * 反应堆融毁事件的唯一发布入口。
 *
 * <p>发布前强制检查逻辑服务端边界；客户端调用只返回 {@code false}，不会构造或投递
 * 事件。调用者负责在自己的权威状态事务中完成持久化去重，不能用本类另建状态机。</p>
 */
public final class ReactorMeltdownEvents {
    private ReactorMeltdownEvents() {
    }

    /**
     * 将一次已经提交的融毁完成状态发布到 NeoForge 总线。
     *
     * @return 在逻辑服务端实际投递事件时为 {@code true}，客户端或非服务端为 {@code false}
     */
    public static boolean publish(
            ReactorMeltdownEvent.Reason reason,
            Level level,
            BlockPos structureOrigin,
            BlockPos instrumentPort,
            ReactorSnapshot snapshot
    ) {
        Objects.requireNonNull(reason, "meltdown event reason is required");
        Objects.requireNonNull(level, "meltdown event level is required");
        Objects.requireNonNull(structureOrigin, "meltdown event structure origin is required");
        Objects.requireNonNull(instrumentPort, "meltdown event instrument port is required");
        Objects.requireNonNull(snapshot, "meltdown event snapshot is required");
        if (!(level instanceof ServerLevel serverLevel) || level.isClientSide) {
            return false;
        }
        NeoForge.EVENT_BUS.post(new ReactorMeltdownEvent(
                reason, serverLevel, structureOrigin, instrumentPort, snapshot));
        return true;
    }
}
