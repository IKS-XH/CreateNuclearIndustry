package com.iksxh.create_nuclear_industry.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

import java.util.Objects;

/**
 * 服务端反应堆融毁状态完成事件；事件不可取消，也不携带可变世界操作。
 *
 * <p>事件只作为模组内部扩展点，快照是完成时的只读权威状态。后续事故系统可根据
 * {@link Reason} 读取服务端维度和结构坐标，但 P1 本身不为事件注册世界副作用监听器。</p>
 */
public final class ReactorMeltdownEvent extends Event {
    /** 当前 P1 允许发布的完成原因；具体事故行为由后续任务决定。 */
    public enum Reason {
        COUNTDOWN_COMPLETE,
        DANGEROUS_DISASSEMBLY
    }

    private final Reason reason;
    private final ServerLevel level;
    private final BlockPos structureOrigin;
    private final BlockPos instrumentPort;
    private final ReactorSnapshot snapshot;

    /** 创建已经完成服务端状态提交的不可取消事件。 */
    public ReactorMeltdownEvent(
            Reason reason,
            ServerLevel level,
            BlockPos structureOrigin,
            BlockPos instrumentPort,
            ReactorSnapshot snapshot
    ) {
        this.reason = Objects.requireNonNull(reason, "meltdown event reason is required");
        this.level = Objects.requireNonNull(level, "meltdown event level is required");
        this.structureOrigin = Objects.requireNonNull(
                structureOrigin, "meltdown event structure origin is required").immutable();
        this.instrumentPort = Objects.requireNonNull(
                instrumentPort, "meltdown event instrument port is required").immutable();
        this.snapshot = Objects.requireNonNull(snapshot, "meltdown event snapshot is required");
    }

    /** 返回触发本次完成状态的原因。 */
    public Reason reason() {
        return reason;
    }

    /** 返回发布事件的服务端维度对象；调用者不得把它替换为客户端 Level。 */
    public ServerLevel level() {
        return level;
    }

    /** 返回稳定的维度键，供不需要持有世界对象的后续消费者使用。 */
    public ResourceKey<Level> dimension() {
        return level.dimension();
    }

    /** 返回结构原点的不可变世界坐标。 */
    public BlockPos structureOrigin() {
        return structureOrigin;
    }

    /** 返回发布事件的仪表端口不可变世界坐标。 */
    public BlockPos instrumentPort() {
        return instrumentPort;
    }

    /** 返回完成时的不可变权威快照；事件不提供替换快照的写入口。 */
    public ReactorSnapshot snapshot() {
        return snapshot;
    }
}
