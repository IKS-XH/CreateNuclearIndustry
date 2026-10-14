package com.iksxh.create_nuclear_industry.reactor;

import java.util.Map;
import java.util.TreeMap;

/** 无世界副作用的维护热结算，输入输出均为本tick真实HU；不足整笔时不扣账。 */
public final class ReactorMaintenanceHeat {
    private ReactorMaintenanceHeat() { }
    /** 本tick支出HU与两个不可变移热图；燃料图只能接入原Thermal，杆图只扣真实杆缓存。 */
    public record Settlement(double paidHeatHu, double generatedExpenseHu, double cachedExpenseHu,
                             Map<CoreColumnPosition, Double> removedByColumn,Map<CoreColumnPosition,Double> removedFromControlRods) {
        public Settlement {
            if (!Double.isFinite(paidHeatHu) || paidHeatHu < 0 || !Double.isFinite(generatedExpenseHu)
                    || generatedExpenseHu < 0 || !Double.isFinite(cachedExpenseHu) || cachedExpenseHu < 0)
                throw new IllegalArgumentException("维护支出必须为有限非负HU");
            removedByColumn = Map.copyOf(removedByColumn);
            removedFromControlRods = Map.copyOf(removedFromControlRods);
            for(var map:java.util.List.of(removedByColumn,removedFromControlRods))map.values().forEach(value->{
                if(!Double.isFinite(value)||value<0)throw new IllegalArgumentException("逐列维护移热必须为有限非负HU");
            });
        }
    }
    public static Settlement settle(ReactorSnapshot snapshot, ReactorFissionResult fission, double requestedHeatHu) {
        if (snapshot == null || fission == null || !Double.isFinite(requestedHeatHu) || requestedHeatHu < 0)
            throw new IllegalArgumentException("维护请求必须为有限非负HU");
        double cached = snapshot.fuelColumns().values().stream().mapToDouble(FuelColumnState::cachedHeatHu).sum()
                +snapshot.controlRodColumns().values().stream().mapToDouble(ControlRodColumnState::cachedHeatHu).sum();
        double available = cached + fission.generatedHeatHu();
        if (!Double.isFinite(available)) throw new IllegalArgumentException("维护可用热量溢出");
        if (requestedHeatHu == 0 || available < requestedHeatHu) return new Settlement(0,0,0,Map.of(),Map.of());
        double generatedExpense = Math.min(requestedHeatHu, fission.generatedHeatHu());
        double cachedExpense = requestedHeatHu - generatedExpense;
        var generatedWeights = new TreeMap<CoreColumnPosition,Double>();
        var cachedWeights = new TreeMap<CoreColumnPosition,Double>();
        snapshot.fuelColumns().forEach((position,state) -> {
            var column = fission.columns().get(position);
            generatedWeights.put(position, column == null ? 0 : column.generatedHeatHu());
            cachedWeights.put(position, state.cachedHeatHu());
        });
        snapshot.controlRodColumns().forEach((position,state)->cachedWeights.put(position,state.cachedHeatHu()));
        var removed = new TreeMap<CoreColumnPosition,Double>();
        var rodRemoved = new TreeMap<CoreColumnPosition,Double>();
        allocate(generatedWeights, generatedExpense).forEach((position, amount) -> removed.merge(position,amount,Double::sum));
        // 两类缓存共享比例预算，但原燃料热工只接收燃料键；杆缓存由编排器直接扣除并保留机械字段。
        allocate(cachedWeights, cachedExpense).forEach((position, amount) -> {
            if(snapshot.fuelColumns().containsKey(position))removed.merge(position,amount,Double::sum);
            else rodRemoved.put(position,amount);
        });
        return new Settlement(requestedHeatHu,generatedExpense,cachedExpense,removed,rodRemoved);
    }
    /** 确定性逐列按真实余额分配，最后一个正余额列承接浮点尾差；不创建额外热预算。 */
    static Map<CoreColumnPosition,Double> allocate(Map<CoreColumnPosition,Double> weights,double amount) {
        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (amount == 0 || total == 0) return Map.of();
        var result = new TreeMap<CoreColumnPosition,Double>();
        double assigned = 0; CoreColumnPosition last = null;
        for (var entry : new TreeMap<>(weights).entrySet()) {
            if (entry.getValue() <= 0) continue;
            double part = amount * (entry.getValue()/total);
            result.put(entry.getKey(),part); assigned += part; last = entry.getKey();
        }
        if (last != null) result.put(last,Math.max(0,result.get(last)+amount-assigned));
        return result;
    }
}
