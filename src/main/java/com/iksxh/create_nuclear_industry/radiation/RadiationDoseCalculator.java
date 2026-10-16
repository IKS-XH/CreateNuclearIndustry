package com.iksxh.create_nuclear_industry.radiation;
/** 不访问世界的在线结算器；只消费完整样本，elapsedTicks不包含离线时间。 */
public final class RadiationDoseCalculator {
    private RadiationDoseCalculator() {}
    public static RadiationDoseState advance(RadiationDoseState state,double absorbedDosePerSecond,int elapsedTicks,RadiationParameters p) {
        if(!Double.isFinite(absorbedDosePerSecond)||absorbedDosePerSecond<0||elapsedTicks<0)throw new IllegalArgumentException("invalid radiation step");
        double dose=Math.min(state.dose(),p.maxDose());int safe=0;
        if(p.enabled()&&absorbedDosePerSecond>p.safeAbsorptionPerSecond())dose+=absorbedDosePerSecond*(elapsedTicks/20.0);
        else {
            safe=(int)Math.min(Integer.MAX_VALUE,(long)state.safeTicks()+elapsedTicks);
            long recoveryTicks=Math.max(0,(long)safe-p.recoveryDelayTicks())-Math.max(0,(long)state.safeTicks()-p.recoveryDelayTicks());
            dose-=recoveryTicks/20.0*p.recoveryPerSecond();
        }
        dose=Math.clamp(dose,0,p.maxDose());
        int damage=p.enabled()&&dose>=p.damageDose()?(int)Math.min(Integer.MAX_VALUE,(long)state.damageTicks()+elapsedTicks):0;
        return new RadiationDoseState(dose,safe,damage);
    }
}
