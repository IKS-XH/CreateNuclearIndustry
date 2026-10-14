# Create 6.0.10-280 Basin heat and recipe excerpts

Source archive and SHA-256 are listed in `../EXT-B-API-01A/boiler-heater.md`.

## `com/simibubi/create/content/processing/basin/BasinBlockEntity.java` lines 343-347, 618-623, 795-803

```java
public void tick() {
    cachedHeatLevel = null;
    super.tick();
    ...
}
public static HeatLevel getHeatLevelOf(BlockState state) {
    if (state.hasProperty(BlazeBurnerBlock.HEAT_LEVEL))
        return state.getValue(BlazeBurnerBlock.HEAT_LEVEL);
    return AllTags.AllBlockTags.PASSIVE_BOILER_HEATERS.matches(state) && BlockHelper.isNotUnheated(state)
        ? HeatLevel.SMOULDERING
        : HeatLevel.NONE;
}
@NotNull HeatLevel getHeatLevel() {
    if (cachedHeatLevel == null) {
        if (level == null) return HeatLevel.NONE;
        cachedHeatLevel = getHeatLevelOf(level.getBlockState(getBlockPos().below(1)));
    }
    return cachedHeatLevel;
}
```

## `com/simibubi/create/content/processing/recipe/HeatCondition.java` lines 32-45

```java
public boolean testBlazeBurner(BlazeBurnerBlock.HeatLevel level) {
    if (this == SUPERHEATED) return level == HeatLevel.SEETHING;
    if (this == HEATED) return level != HeatLevel.NONE && level != HeatLevel.SMOULDERING;
    return true;
}
```

## `com/simibubi/create/content/processing/basin/BasinRecipe.java` lines 39-58, 65-76

```java
public static boolean match(BasinBlockEntity basin, Recipe<?> recipe) {
    ...
    return apply(basin, recipe, true);
}
private static boolean apply(BasinBlockEntity basin, Recipe<?> recipe, boolean test) {
    ...
    HeatLevel heat = basin.getHeatLevel();
    if (isBasinRecipe && !((BasinRecipe) recipe).getRequiredHeat().testBlazeBurner(heat))
        return false;
    ...
}
```

`BasinOperatingBlockEntity.getMatchingRecipes()` calls `matchBasinRecipe`, which delegates to `BasinRecipe.match` (`BasinOperatingBlockEntity.java:133-155`, `101-108`). Therefore a required-heat recipe is absent from the current matched recipe while its required heat is absent. The current recipe cannot be used as the trigger that supplies that heat.

Create's heat-level reader is not `BoilerHeater.REGISTRY`: the basin reads the block state one cell directly below it and recognizes any state carrying the public `BlazeBurnerBlock.HEAT_LEVEL` property. The recipe itself proceeds only through the powered mixer's normal matching/progress path and requires its ingredient/output conditions (`BasinOperatingBlockEntity.java:69-89`; `BasinRecipe.java:65-76`).
