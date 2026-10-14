package com.iksxh.create_nuclear_industry.automation;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity.FuelType;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.reactor.ReactorServerTick;

/** 付热资格仅覆盖当tick或相邻tick，每笔只用于一次真实燃烧室结算且不持久化。 */
public final class ReactorAutomationHeatSupply {
    private ReactorAutomationHeatSupply() { }
    public static final class Account {
        private long paidAt=-1, consumedAt=-1, requestedAt=-1, committedAt=-1, lastPaymentAt=-1, lastConsumeAt=-1;
        private boolean consumed=true;
        private ReactorAutomationBinding identity;
        private BlazeBurnerBlockEntity burner;
        private ReactorAutomationPortBlockEntity port;
        private double lastPaid, lastGeneratedExpense, lastCachedExpense;
        /** 无世界副作用的付热凭据；同tick重复结算不能覆盖已消费凭据。 */
        public void pay(long time,double heat) {
            if (time<0 || !Double.isFinite(heat) || heat<=0) throw new IllegalArgumentException("付热凭据输入非法");
            if (time<=lastPaymentAt) return;
            paidAt=time; lastPaymentAt=time; consumed=false;
        }
        public boolean consume(long time) {
            if (consumed || lastConsumeAt==time || !recent(time)) return false;
            consumed=true; consumedAt=time; lastConsumeAt=time; return true;
        }
        public boolean recent(long time) { return paidAt>=0 && time>=paidAt && time-paidAt<=1; }
        /** 最近完整供能只用于菜单来源文字，绝不作为本tick冻时或热级授权。 */
        public boolean completedSource(long time) { return consumedAt>=0&&time>=consumedAt&&time-consumedAt<=1&&recent(time); }
        public void clear() {
            // 消费显示随成员身份撤销；请求、付款、真实燃烧结算的去重时刻属于owner本tick，换代也不能重收费用。
            paidAt=-1; consumedAt=-1; consumed=true; identity=null; burner=null; port=null;
            lastPaid=0; lastGeneratedExpense=0; lastCachedExpense=0;
        }
        public double paidHeatHu() { return lastPaid; }
        public double generatedExpenseHu() { return lastGeneratedExpense; }
        public double cachedExpenseHu() { return lastCachedExpense; }
    }
    /** 原生燃料或已支付短期核热才允许请求；单独KINDLED方块状态不是免费点火证据。 */
    public static boolean eligible(FuelType fuel,int remaining,boolean creative,boolean paidRecently) {
        return !creative && !(fuel==FuelType.SPECIAL && remaining>0)
                && (fuel==FuelType.NORMAL && remaining>0 || paidRecently);
    }
    private static boolean sameIdentity(ReactorAutomationBinding a,ReactorAutomationBinding b) {
        return a!=null && b!=null && a.dimension().equals(b.dimension()) && a.ownerPos().equals(b.ownerPos())
                && a.ownerGeneration().equals(b.ownerGeneration()) && a.portPos().equals(b.portPos()) && a.burnerPos().equals(b.burnerPos());
    }
    /** 不强载地核对实际实例；设置epoch不改供热几何身份，结构代际与成员换代立即失效。 */
    private static Account resolve(ReactorInstrumentPortBlockEntity owner) {
        var account=owner.automationHeatAccount();
        var binding=owner.automationBinding();
        if (!owner.structureValid() || !ReactorAutomationService.hasCurrentInstances(owner,binding)) { account.clear(); return null; }
        var burner=(BlazeBurnerBlockEntity)owner.automationBurner();
        var port=owner.automationPort();
        if (!sameIdentity(account.identity,binding) || account.burner!=burner || account.port!=port) {
            account.clear(); account.identity=binding; account.burner=burner; account.port=port;
        }
        return account;
    }
    /** 本tick请求单位HU；创建请求不出票，原核事务提交成功之后才能提交付款。 */
    public static double requestedHeat(ReactorInstrumentPortBlockEntity owner,long time) {
        var account=resolve(owner);
        // 管理调节有自己的beginAutomationTick；热费还必须覆盖本tick先零请求提交、再改变燃料的边界。
        if(account==null || account.requestedAt==time || account.committedAt==time) return 0;
        var burner=account.burner;
        // 正常续接首笔仅在当前结构/预算及真实低沿已经验证后请求，仍须正式核事务足额支付；KINDLED不授资格。
        if (!eligible(burner.getActiveFuel(),burner.getRemainingBurnTime(),burner.isCreative(),account.recent(time))
                &&!ReactorAutomationResume.requiresNewNuclearPayment(owner)) return 0;
        double cost;
        try {cost=com.iksxh.create_nuclear_industry.config.ReactorAutomationConfig.parameters().burnerMaintenanceHuPerTick();}
        catch(IllegalArgumentException invalidConfig) {account.clear();return 0;}
        account.requestedAt=time; return cost;
    }
    /** 燃料端口、热账本和正式结果已提交后调用；不足整笔不延长上一凭据。 */
    public static void commitPayment(ReactorInstrumentPortBlockEntity owner,ReactorServerTick.Result result,long time) {
        var account=resolve(owner); if(account==null) return;
        if(account.committedAt==time) return;
        account.committedAt=time;
        var expense=result.maintenance();
        account.lastPaid=expense.paidHeatHu(); account.lastGeneratedExpense=expense.generatedExpenseHu(); account.lastCachedExpense=expense.cachedExpenseHu();
        if(expense.paidHeatHu()>0) account.pay(time,expense.paidHeatHu());
        else if(account.requestedAt==time) { account.paidAt=-1; account.consumed=true; }
        if(!dutyValid(account.burner) && owner.automationState().claimed()) ReactorAutomationService.revoke(owner,"duty_lost");
        ReactorAutomationBlazeBridge.notifyPermission(owner,account.burner.getBlockPos());
    }
    public static void invalidate(ReactorInstrumentPortBlockEntity owner) { owner.automationHeatAccount().clear(); }
    private static ReactorInstrumentPortBlockEntity owner(BlazeBurnerBlockEntity burner) {
        if(burner.getLevel()==null || burner.getLevel().isClientSide) return null;
        return ReactorAutomationBlazeBridge.owner(burner.getLevel(),ReactorAutomationBlazeBridge.findBinding(burner.getLevel(),burner.getBlockPos()).orElse(null));
    }
    /** 每笔只消费一次。由真实燃烧室tick头调用，不以渲染热级或墙钟续租。 */
    public static boolean consumeForBurner(BlazeBurnerBlockEntity burner) {
        var owner=owner(burner); if(owner==null) return false;
        var account=resolve(owner);
        return account!=null && !burner.isCreative() && !(burner.getActiveFuel()==FuelType.SPECIAL && burner.getRemainingBurnTime()>0)
                && account.consume(burner.getLevel().getGameTime());
    }
    /** 只把已消费本tick付款显示为普通燃烧，不伪造燃料时长或超级加热。 */
    public static boolean displayingNuclear(BlazeBurnerBlockEntity burner) {
        var owner=owner(burner); if(owner==null) return false;
        var account=resolve(owner);
        return account!=null && account.consumedAt==burner.getLevel().getGameTime() && account.recent(burner.getLevel().getGameTime())
                && !burner.isCreative() && !(burner.getActiveFuel()==FuelType.SPECIAL && burner.getRemainingBurnTime()>0);
    }
    public static boolean dutyValid(BlazeBurnerBlockEntity burner) {
        if(burner.isCreative()) return ReactorAutomationBlazeBridge.dutyLevelValid(burner.getHeatLevelFromBlock());
        if(burner.getActiveFuel()!=FuelType.NONE && burner.getRemainingBurnTime()>0) return true;
        var owner=owner(burner); if(owner==null) return false;
        var account=resolve(owner); return account!=null && account.recent(burner.getLevel().getGameTime());
    }
    public static String source(BlazeBurnerBlockEntity burner) {
        if(burner.isCreative()) return "creative";
        if(burner.getActiveFuel()==FuelType.SPECIAL && burner.getRemainingBurnTime()>0) return "special";
        var owner=owner(burner);var account=owner==null?null:resolve(owner);
        if(account!=null&&account.completedSource(burner.getLevel().getGameTime())) return "nuclear";
        return burner.getActiveFuel()!=FuelType.NONE && burner.getRemainingBurnTime()>0?"external":"none";
    }
}
