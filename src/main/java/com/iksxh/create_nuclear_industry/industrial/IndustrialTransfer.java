package com.iksxh.create_nuclear_industry.industrial;

/** 单泵单介质事务数量核心；先模拟接收再抽源，短收余量只保存在此泵，单位 mB。 */
public final class IndustrialTransfer {
    /** 与外部库存的数量交易；调用者固定介质和已验证路径，模拟必须零写。 */
    public interface Store {
        int remove(int request, boolean execute);
        int add(int request, boolean execute);
    }
    private int cached;
    public int cached() { return cached; }

    /** 当前版本恢复真实缓存，不按变小的新配置截断；只允许非负有限整数。 */
    public void restore(int amount) {
        if (amount < 0) throw new IllegalArgumentException("缓存数量不能为负数");
        cached = amount;
    }

    /** 缓存非空时本次只排缓存，不额外抽源；返回实际交付量而非抽取量。 */
    public int move(Store source, Store target, int available, int capacity, boolean execute) {
        if (available <= 0 || capacity <= 0) return 0;
        int offered = cached > 0 ? Math.min(cached, available)
                : Math.min(Math.min(source.remove(available, false), available), capacity);
        int accepted = Math.min(offered, Math.max(0, target.add(offered, false)));
        if (!execute || accepted <= 0) return accepted;
        if (cached == 0) cached = Math.min(accepted, Math.max(0, source.remove(accepted, true)));
        int delivered = Math.min(cached, Math.max(0, target.add(Math.min(cached, accepted), true)));
        cached -= delivered;
        return delivered;
    }
}
