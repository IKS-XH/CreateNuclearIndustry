# Create 6.0.10-280 extension entrypoint locator

Source archive and SHA-256 are listed in `../EXT-B-API-01A/boiler-heater.md`.

## Public registries

- `com/simibubi/create/api/boiler/BoilerHeater.java:35-50` — public `BoilerHeater.REGISTRY`, `findHeat`, and callback contract. A block's callback can be registered directly; no mixin hook is needed to expose a block to native tank heat scoring.
- `com/simibubi/create/api/registry/SimpleRegistry.java:28-53,61-83` — direct `register(K,V)` and dynamic provider API; registry is thread-safe and direct entries take priority over providers.
- `com/simibubi/create/content/processing/burner/BlazeBurnerBlock.java:72-84,310-325` — public `HEAT_LEVEL` property and `HeatLevel` enum (`NONE`, `SMOULDERING`, `FADING`, `KINDLED`, `SEETHING`). This is the property read by basin heat logic.

## Future stress source (locator only)

- `com/simibubi/create/api/stress/BlockStressValues.java:10-31` — `IMPACTS` and `CAPACITIES` registries of `DoubleSupplier`; public `getImpact(Block)` / `getCapacity(Block)`. This only locates Create's native stress-value registry; no multi-axis or turbine behavior was investigated.
