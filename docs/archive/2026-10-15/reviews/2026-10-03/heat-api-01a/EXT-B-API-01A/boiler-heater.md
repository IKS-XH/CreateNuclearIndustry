# Create 6.0.10-280 BoilerHeater source excerpts

Source archive: `C:/Users/IKSXH/.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280/23e1219501c0debfa0bb56c30ef8e0193341aae5/create-1.21.1-6.0.10-280-sources.jar`
SHA-256: `376DE15CA5ACF720106A075CA4EB2EF53E63E0E5D9EC93523A1ABBDD0F9F0CB4`

## `com/simibubi/create/api/boiler/BoilerHeater.java` lines 20-50

```java
@FunctionalInterface
public interface BoilerHeater {
    int PASSIVE_HEAT = 0;
    int NO_HEAT = -1;
    BoilerHeater PASSIVE = BoilerHeaters::passive;
    BoilerHeater BLAZE_BURNER = BoilerHeaters::blazeBurner;
    SimpleRegistry<Block, BoilerHeater> REGISTRY = SimpleRegistry.create();
    static float findHeat(Level level, BlockPos pos, BlockState state) {
        BoilerHeater heater = REGISTRY.get(state);
        return heater != null ? heater.getHeat(level, pos, state) : NO_HEAT;
    }
    float getHeat(Level level, BlockPos pos, BlockState state);
}
```

## `com/simibubi/create/content/fluids/tank/BoilerHeaters.java` lines 16-37

```java
public static void registerDefaults() {
    BoilerHeater.REGISTRY.register(AllBlocks.BLAZE_BURNER.get(), BoilerHeater.BLAZE_BURNER);
    BoilerHeater.REGISTRY.registerProvider(SimpleRegistry.Provider.forBlockTag(...));
}
public static int blazeBurner(Level level, BlockPos pos, BlockState state) {
    HeatLevel value = state.getValue(BlazeBurnerBlock.HEAT_LEVEL);
    if (value == HeatLevel.NONE) return BoilerHeater.NO_HEAT;
    if (value == HeatLevel.SEETHING) return 2;
    if (value.isAtLeast(HeatLevel.FADING)) return 1;
    return BoilerHeater.PASSIVE_HEAT;
}
```

## `com/simibubi/create/content/fluids/tank/BoilerData.java` lines 89-112, 157-191, 382-408

```java
public void tick(FluidTankBlockEntity controller) {
    if (!isActive()) return;
    ...
    if (needsHeatLevelUpdate && updateTemperature(controller)) controller.notifyUpdate();
    ...
}
public int getMaxHeatLevelForBoilerSize(int boilerSize) {
    return (int) Math.min(18, boilerSize / 4);
}
private static final int waterSupplyPerLevel = 10;
public int getMaxHeatLevelForWaterSupply() {
    return (int) Math.min(18, Mth.ceil(waterSupply) / waterSupplyPerLevel);
}
private int getActualHeat(int boilerSize) {
    int forBoilerSize = getMaxHeatLevelForBoilerSize(boilerSize);
    int forWaterSupply = getMaxHeatLevelForWaterSupply();
    int actualHeat = Math.min(activeHeat, Math.min(forWaterSupply, forBoilerSize));
    return actualHeat;
}
public boolean updateTemperature(FluidTankBlockEntity controller) {
    ...
    for (int xOffset = 0; xOffset < controller.width; xOffset++)
        for (int zOffset = 0; zOffset < controller.width; zOffset++) {
            BlockPos pos = controllerPos.offset(xOffset, -1, zOffset);
            float heat = BoilerHeater.findHeat(level, pos, level.getBlockState(pos));
            if (heat == 0) passiveHeat = true;
            else if (heat > 0) activeHeat += heat;
        }
    ...
}
```

`isActive()` is `attachedEngines > 0 || attachedWhistles > 0` (`BoilerData.java:410-412`). Thus the native aggregation queries floor blocks under a formed tank controller, accumulates provider levels, and clamps usable level to 18, tank-size, and measured-water limits. The heat callback has no HU consumption/transaction parameter.

## `FluidTankBlock.java` lines 146-151; `FluidTankBlockEntity.java` lines 271-278

```java
public BlockState updateShape(...) {
    if (pDirection == Direction.DOWN && pNeighborState.getBlock() != this)
        withBlockEntityDo(pLevel, pCurrentPos, FluidTankBlockEntity::updateBoilerTemperature);
    return pState;
}
public void updateBoilerTemperature() {
    FluidTankBlockEntity be = getControllerBE();
    if (be == null) return;
    if (!be.boiler.isActive()) return;
    be.boiler.needsHeatLevelUpdate = true;
}
```

This shows explicit refresh on a floor-neighbor update, only when attached load exists. The flag is consumed by the controller's normal tick; this is not an unload callback.
