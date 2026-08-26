package com.iksxh.create_nuclear_industry.p0probe.numeric;

/**
 * P0 最小服务端适配缝隙，只拥有一次 tick 调用和快照交接；虚拟端口与控制输入
 * 仍是普通数据。它不是方块实体、注册对象或正式 P1 状态源。
 */
public final class P0IntegrationAdapter {
    private int tickCalls;

    /** 执行一次 P0 纯数值服务端 tick，并累计调用次数供测试观察。 */
    public ReactorTickResult serverTick(ReactorSnapshot snapshot, ReactorParameters parameters,
                                        ReactorTickInput input) {
        tickCalls++;
        return ReactorModel.tick(snapshot, parameters, input);
    }

    public int tickCalls() {
        return tickCalls;
    }

    /** 通过 P0 纯 Java 编解码器模拟保存后重载。 */
    public ReactorSnapshot saveAndReload(ReactorSnapshot snapshot) {
        return ReactorSnapshotCodec.decode(ReactorSnapshotCodec.encode(snapshot));
    }
}
