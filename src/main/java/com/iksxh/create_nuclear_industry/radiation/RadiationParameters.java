package com.iksxh.create_nuclear_industry.radiation;

/** 不可变服务端参数；剂量使用游戏单位D，场强与吸收强度使用D/s。构造时整组验证。 */
public record RadiationParameters(boolean enabled, int sampleIntervalTicks, int detectionRadiusBlocks,
        double usedFuelDosePerSecond, double spentFuelDosePerSecond, double accidentWasteDosePerSecond,
        double maxFieldDosePerSecond, double caskTransmission, double rackTransmission,
        double reactorTransmission, double blockTransmission, double safeAbsorptionPerSecond,
        double maxDose, double warningDose, double symptomDose, double damageDose,
        int recoveryDelayTicks, double recoveryPerSecond, int damageIntervalTicks, double damageHealthPoints,
        double head, double chest, double legs, double feet, double maxReduction) {
    public static final RadiationParameters DEFAULT = new RadiationParameters(true,20,32,1,2,4,10,
            .05,0,0,.05,.01,100,25,50,80,100,1,40,1,.25,.30,.25,.15,.95);
    public RadiationParameters {
        if (sampleIntervalTicks < 1 || detectionRadiusBlocks < 1 || detectionRadiusBlocks > 64
                || recoveryDelayTicks < 0 || damageIntervalTicks < 1)
            throw new IllegalArgumentException("invalid radiation tick/radius range");
        for (double value : new double[]{usedFuelDosePerSecond,spentFuelDosePerSecond,accidentWasteDosePerSecond,
                maxFieldDosePerSecond,safeAbsorptionPerSecond,maxDose,warningDose,symptomDose,damageDose,
                recoveryPerSecond,damageHealthPoints}) nonnegative(value);
        if (maxFieldDosePerSecond <= 0 || !(warningDose < symptomDose && symptomDose < damageDose && damageDose <= maxDose))
            throw new IllegalArgumentException("invalid radiation dose thresholds");
        for (double value : new double[]{caskTransmission,rackTransmission,reactorTransmission,blockTransmission,
                head,chest,legs,feet,maxReduction}) {
            nonnegative(value);
            if (value > 1) throw new IllegalArgumentException("radiation ratio must be in [0,1]");
        }
    }
    private static void nonnegative(double value) {
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("radiation value must be finite and nonnegative");
    }
    /** 距离为格；同位置与一格内按参考源强计算，半径外不计。 */
    public double distanceFactor(double squaredDistance) {
        return squaredDistance > (double)detectionRadiusBlocks*detectionRadiusBlocks ? 0 : 1 / Math.max(1,squaredDistance);
    }
}
