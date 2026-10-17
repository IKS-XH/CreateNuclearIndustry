package com.iksxh.create_nuclear_industry.radiation;

import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyItemCodec;
import com.iksxh.create_nuclear_industry.storage.SpentFuelPayload;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/** 仅从真实ItemStack读取载荷；显式栈遍历避免深层容器递归溢出，不创建库存副本账本。 */
public final class RadiationItemSources {
    public static final int MAX_STACKS_PER_DESCRIPTION = 8192;
    private RadiationItemSources() {}
    /** 配置允许任意有限非负强度；在总场强限幅前饱和加乘，避免Inf×0产生NaN。 */
    public static double addFinite(double left,double right) {return Math.min(Double.MAX_VALUE,left+right);}
    private static double multiplyFinite(double left,double right) {return left==0||right==0?0:Math.min(Double.MAX_VALUE,left*right);}
    public static ItemSourceSummary describe(ItemStack stack, RadiationParameters parameters) {
        Cursor cursor = new Cursor(stack,parameters);
        cursor.advance(MAX_STACKS_PER_DESCRIPTION);
        return cursor.summary();
    }
    /** 超预算遍历可跨帧续接；未完成摘要不能解释为无源。每节点只应用一次合法桶比例。 */
    public static final class Cursor {
        private record Entry(ItemStack stack,double multiplier,double count) {}
        private final Deque<Entry> pending = new ArrayDeque<>();
        private final RadiationParameters p;
        private double raw, transmitted;
        private int visited;
        private boolean radioactive;
        public Cursor(ItemStack stack,RadiationParameters p) { this.p=p; pending.push(new Entry(stack,1,1)); }
        public void advance(int budget) {
            while(budget-- > 0 && !pending.isEmpty()) {
                Entry entry=pending.pop(); ItemStack stack=entry.stack(); visited++;
                if(stack==null || stack.isEmpty()) continue;
                double count=multiplyFinite(entry.count(),stack.getCount());
                double source=FuelAssemblyItemCodec.isCooledSpentFuel(stack) ? p.spentFuelDosePerSecond()
                        : FuelAssemblyItemCodec.isFreshFuel(stack)&&FuelUseHistory.isUsed(stack) ? p.usedFuelDosePerSecond() : 0;
                radioactive|=FuelAssemblyItemCodec.isCooledSpentFuel(stack)
                        ||FuelAssemblyItemCodec.isFreshFuel(stack)&&FuelUseHistory.isUsed(stack);
                double strength=multiplyFinite(source,count);
                raw=addFinite(raw,strength);transmitted=addFinite(transmitted,multiplyFinite(strength,entry.multiplier()));
                double multiplier=entry.multiplier()*(SpentFuelPayload.isValid(stack)?p.caskTransmission():1);
                var container=stack.get(DataComponents.CONTAINER);
                if(container!=null) container.nonEmptyItems().forEach(child -> pending.push(new Entry(child,multiplier,count)));
                var bundle=stack.get(DataComponents.BUNDLE_CONTENTS);
                if(bundle!=null) for(ItemStack child:bundle.items()) pending.push(new Entry(child,entry.multiplier(),count));
            }
        }
        public ItemSourceSummary summary() { return new ItemSourceSummary(raw,transmitted,radioactive,pending.isEmpty(),visited); }
    }
}
