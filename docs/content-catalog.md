# 当前内容注册清单

更新：2026-10-18。按本分支[注册源码](../src/main/java/com/iksxh/create_nuclear_industry/content/)核对：常规块52、item126，其中同名BlockItem49由块表承接，普通物品仅列77；流体8、fluid_type5。这里只去重展示，不改变注册；条件诊断内容另列。

命名空间create_nuclear_industry，ID小写下划线。方块物品通常共用ID；实际模型/状态/贴图在src/main/resources/assets/create_nuclear_industry，具体来源以源码/资源为准，不要求每个ID都存在同名全套文件。流体/桶策略不能由名字推断；热液/SC专用运输、Create罐保留，SC不作为普通桶/世界流体使用。

再处理/新堆型等未来清单移入[归档策划](archive/2026-10-15/snapshots/docs/content-catalog.md)，不与已注册内容混列。辐射传感器是零件，手持测量使用radiation_meter；本分支新增事故残骸/废料，不代表main或人工门已完成。进度看[STATUS](STATUS.md)，配方看[recipes](recipes.md)。

ID命名空间均为`create_nuclear_industry`，表内省略前缀。注册存在不等于可生存获取。

## 块（52）

| ID | 源字段 |
| --- | --- |
| `boiler_heat_exchange_section` | `BoilerContent.HEAT_SECTION` |
| `boiler_safety_valve` | `BoilerContent.SAFETY_VALVE` |
| `ceramic_fluid_pipe` | `IndustrialFluidContent.CERAMIC_PIPE` |
| `ceramic_fluid_valve` | `IndustrialFluidContent.CERAMIC_VALVE` |
| `compound_coolant` | `ModFluids.COMPOUND_COOLANT_BLOCK` |
| `control_rod_drive` | `P1Blocks.CONTROL_ROD_DRIVE` |
| `deepslate_lead_ore` | `OreContent.LEAD (helper)` |
| `deepslate_tin_ore` | `OreContent.TIN (helper)` |
| `deepslate_uranium_ore` | `OreContent.URANIUM (helper)` |
| `depleted_uranium_block` | `DepletedUraniumContent.BLOCK` |
| `dry_storage_rack` | `SpentFuelStorageContent.DRY_STORAGE_RACK` |
| `enrichment_centrifuge` | `FuelProcessingContent.ENRICHMENT_CENTRIFUGE` |
| `experimental_reactor_casing` | `ModBlocks.EXPERIMENTAL_REACTOR_CASING` |
| `fuel_sintering_furnace` | `FuelProcessingContent.FUEL_SINTERING_FURNACE` |
| `high_pressure_boiler_casing` | `BoilerContent.CASING` |
| `high_pressure_boiler_cold_coolant_port` | `BoilerContent.COLD_PORT` |
| `high_pressure_boiler_controller` | `BoilerContent.CONTROLLER` |
| `high_pressure_boiler_hot_coolant_port` | `BoilerContent.HOT_PORT` |
| `high_pressure_boiler_steam_port` | `BoilerContent.STEAM_PORT` |
| `high_pressure_boiler_water_port` | `BoilerContent.WATER_PORT` |
| `high_pressure_boiler_window` | `BoilerContent.WINDOW` |
| `industrial_fluid_pump` | `IndustrialFluidContent.PUMP` |
| `lead_ore` | `OreContent.LEAD (helper)` |
| `nuclear_heat_exchanger` | `HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER` |
| `raw_lead_block` | `OreContent.LEAD (helper)` |
| `raw_tin_block` | `OreContent.TIN (helper)` |
| `raw_uranium_block` | `OreContent.URANIUM (helper)` |
| `reactor_automation_port` | `ReactorAutomationContent.PORT` |
| `reactor_casing` | `P1Blocks.REACTOR_CASING` |
| `reactor_cold_port` | `P1Blocks.REACTOR_COLD_PORT` |
| `reactor_corium` | `AccidentContent.REACTOR_CORIUM` |
| `reactor_fuel_rod` | `P1Blocks.REACTOR_FUEL_ROD` |
| `reactor_hot_port` | `P1Blocks.REACTOR_HOT_PORT` |
| `reactor_instrument_port` | `P1Blocks.REACTOR_INSTRUMENT_PORT` |
| `reactor_refueling_port` | `P1Blocks.REACTOR_REFUELING_PORT` |
| `reactor_window` | `P1Blocks.REACTOR_WINDOW` |
| `reinforced_fluid_pipe` | `IndustrialFluidContent.REINFORCED_PIPE` |
| `reinforced_fluid_valve` | `IndustrialFluidContent.REINFORCED_VALVE` |
| `shielded_assembly_part` | `FuelProcessingContent.SHIELDED_ASSEMBLY_PART` |
| `shielded_assembly_station` | `FuelProcessingContent.SHIELDED_ASSEMBLY_STATION` |
| `shielding_concrete` | `ReactorCraftingContent.SHIELDING_CONCRETE` |
| `tin_ore` | `OreContent.TIN (helper)` |
| `turbine_casing` | `TurbineContent.CASING` |
| `turbine_controller` | `TurbineContent.CONTROLLER` |
| `turbine_exhaust` | `TurbineContent.EXHAUST` |
| `turbine_inlet` | `TurbineContent.INLET` |
| `turbine_output_shaft` | `TurbineContent.OUTPUT_SHAFT` |
| `turbine_rotor` | `TurbineContent.ROTOR` |
| `turbine_window` | `TurbineContent.WINDOW` |
| `uranium_ore` | `OreContent.URANIUM (helper)` |
| `uranium_slurry` | `FuelProcessingContent.URANIUM_SLURRY_BLOCK` |
| `uranium_tailings_brick` | `FuelProcessingContent.URANIUM_TAILINGS_BRICK` |

## 普通物品（去掉同名BlockItem后77）

| ID | 源字段 |
| --- | --- |
| `accident_fuel_waste` | `AccidentContent.ACCIDENT_FUEL_WASTE` |
| `charcoal_dust` | `BasicMaterialContent.CHARCOAL_DUST` |
| `coal_dust` | `BasicMaterialContent.COAL_DUST` |
| `compound_coolant_bucket` | `ModItems.COMPOUND_COOLANT_BUCKET` |
| `control_rod` | `ModItems.CONTROL_ROD` |
| `cooled_spent_fuel_assembly` | `ModItems.COOLED_SPENT_FUEL_ASSEMBLY` |
| `depleted_uranium_axe` | `DepletedUraniumContent.AXE` |
| `depleted_uranium_boots` | `DepletedUraniumContent.BOOTS` |
| `depleted_uranium_chestplate` | `DepletedUraniumContent.CHESTPLATE` |
| `depleted_uranium_dust` | `FuelProcessingContent.DEPLETED_URANIUM_DUST` |
| `depleted_uranium_helmet` | `DepletedUraniumContent.HELMET` |
| `depleted_uranium_hoe` | `DepletedUraniumContent.HOE` |
| `depleted_uranium_ingot` | `DepletedUraniumContent.INGOT` |
| `depleted_uranium_leggings` | `DepletedUraniumContent.LEGGINGS` |
| `depleted_uranium_pickaxe` | `DepletedUraniumContent.PICKAXE` |
| `depleted_uranium_shovel` | `DepletedUraniumContent.SHOVEL` |
| `depleted_uranium_sword` | `DepletedUraniumContent.SWORD` |
| `fresh_fuel_assembly` | `ModItems.FRESH_FUEL_ASSEMBLY` |
| `fuel_cladding_tube` | `BasicMaterialContent.FUEL_CLADDING_TUBE` |
| `glass_dust` | `SpentFuelStorageContent.GLASS_DUST` |
| `green_fuel_pellet` | `FuelProcessingContent.GREEN_FUEL_PELLET` |
| `heavy_bearing` | `BasicMaterialContent.HEAVY_BEARING` |
| `incomplete_control_rod` | `ReactorCraftingContent.INCOMPLETE_CONTROL_ROD` |
| `incomplete_control_rod_drive` | `ReactorCraftingContent.INCOMPLETE_CONTROL_ROD_DRIVE` |
| `incomplete_heavy_bearing` | `BasicMaterialContent.INCOMPLETE_HEAVY_BEARING` |
| `incomplete_industrial_sensor` | `BasicMaterialContent.INCOMPLETE_INDUSTRIAL_SENSOR` |
| `incomplete_radiation_sensor` | `BasicMaterialContent.INCOMPLETE_RADIATION_SENSOR` |
| `incomplete_reactor_instrument_port` | `ReactorCraftingContent.INCOMPLETE_REACTOR_INSTRUMENT_PORT` |
| `incomplete_reactor_refueling_port` | `ReactorCraftingContent.INCOMPLETE_REACTOR_REFUELING_PORT` |
| `incomplete_reinforced_steel_plate` | `HeatMaterialsContent.INCOMPLETE_REINFORCED_STEEL_PLATE` |
| `incomplete_shielded_glass` | `ReactorCraftingContent.INCOMPLETE_SHIELDED_GLASS` |
| `incomplete_steel_grate` | `BasicMaterialContent.INCOMPLETE_STEEL_GRATE` |
| `industrial_ceramic` | `ReactorCraftingContent.INDUSTRIAL_CERAMIC` |
| `industrial_sensor` | `BasicMaterialContent.INDUSTRIAL_SENSOR` |
| `iron_dust` | `BasicMaterialContent.IRON_DUST` |
| `lapis_dust` | `ModItems.LAPIS_DUST` |
| `lead_ingot` | `BasicMaterialContent.LEAD_INGOT` |
| `lead_nugget` | `BasicMaterialContent.LEAD_NUGGET` |
| `lead_plate` | `BasicMaterialContent.LEAD_PLATE` |
| `lead_shielding_cask` | `SpentFuelStorageContent.LEAD_SHIELDING_CASK` |
| `low_enriched_uranium_dust` | `FuelProcessingContent.LOW_ENRICHED_URANIUM_DUST` |
| `mod_logo` | `ModItems.MOD_LOGO` |
| `neutron_absorbing_ceramic` | `ReactorCraftingContent.NEUTRON_ABSORBING_CERAMIC` |
| `nuclear_heat_exchange_bundle` | `HeatMaterialsContent.NUCLEAR_HEAT_EXCHANGE_BUNDLE` |
| `pressure_fitting` | `ReactorCraftingContent.PRESSURE_FITTING` |
| `quartz_dust` | `BasicMaterialContent.QUARTZ_DUST` |
| `radiation_boots` | `RadiationContent.BOOTS` |
| `radiation_chestplate` | `RadiationContent.CHESTPLATE` |
| `radiation_helmet` | `RadiationContent.HELMET` |
| `radiation_leggings` | `RadiationContent.LEGGINGS` |
| `radiation_meter` | `RadiationContent.METER` |
| `radiation_sensor` | `BasicMaterialContent.RADIATION_SENSOR` |
| `raw_lead` | `OreContent.LEAD (helper)` |
| `raw_tin` | `OreContent.TIN (helper)` |
| `raw_uranium` | `OreContent.URANIUM (helper)` |
| `refractory_brick` | `BasicMaterialContent.REFRACTORY_BRICK` |
| `reinforced_steel_plate` | `HeatMaterialsContent.REINFORCED_STEEL_PLATE` |
| `seal_ring` | `ReactorCraftingContent.SEAL_RING` |
| `sealed_spent_fuel_cask` | `SpentFuelStorageContent.SEALED_SPENT_FUEL_CASK` |
| `shielded_glass` | `ReactorCraftingContent.SHIELDED_GLASS` |
| `sintered_fuel_pellet` | `FuelProcessingContent.SINTERED_FUEL_PELLET` |
| `solder_ingot` | `BasicMaterialContent.SOLDER_INGOT` |
| `steel_dust` | `BasicMaterialContent.STEEL_DUST` |
| `steel_grate` | `BasicMaterialContent.STEEL_GRATE` |
| `steel_ingot` | `BasicMaterialContent.STEEL_INGOT` |
| `steel_mesh` | `BasicMaterialContent.STEEL_MESH` |
| `steel_pipe_blank` | `HeatMaterialsContent.STEEL_PIPE_BLANK` |
| `steel_plate` | `ModItems.STEEL_PLATE` |
| `steel_rod` | `ReactorCraftingContent.STEEL_ROD` |
| `tin_ingot` | `BasicMaterialContent.TIN_INGOT` |
| `tin_nugget` | `BasicMaterialContent.TIN_NUGGET` |
| `tin_plate` | `BasicMaterialContent.TIN_PLATE` |
| `tin_wire` | `BasicMaterialContent.TIN_WIRE` |
| `uranium_concentrate` | `FuelProcessingContent.URANIUM_CONCENTRATE` |
| `uranium_slurry_bucket` | `FuelProcessingContent.URANIUM_SLURRY_BUCKET` |
| `uranium_tailings` | `FuelProcessingContent.URANIUM_TAILINGS` |
| `vitrification_medium` | `SpentFuelStorageContent.VITRIFICATION_MEDIUM` |

## 流体（8）

| ID | 源字段 |
| --- | --- |
| `compound_coolant` | `ModFluids.COMPOUND_COOLANT_SOURCE` |
| `compound_coolant_flowing` | `ModFluids.COMPOUND_COOLANT_FLOWING` |
| `hot_compound_coolant` | `ModFluids.HOT_COMPOUND_COOLANT_SOURCE` |
| `hot_compound_coolant_flowing` | `ModFluids.HOT_COMPOUND_COOLANT_FLOWING` |
| `steam` | `TurbineContent.STEAM` |
| `supercritical_steam` | `BoilerContent.SUPERCRITICAL_STEAM` |
| `uranium_slurry` | `FuelProcessingContent.URANIUM_SLURRY` |
| `uranium_slurry_flowing` | `FuelProcessingContent.URANIUM_SLURRY_FLOWING` |

## 流体类型（5）

| ID | 源字段 |
| --- | --- |
| `compound_coolant` | `ModFluids.COMPOUND_COOLANT_TYPE` |
| `hot_compound_coolant` | `ModFluids.HOT_COMPOUND_COOLANT_TYPE` |
| `steam` | `TurbineContent.STEAM_TYPE` |
| `supercritical_steam` | `BoilerContent.SUPERCRITICAL_TYPE` |
| `uranium_slurry` | `FuelProcessingContent.URANIUM_SLURRY_TYPE` |

无同名item的块：`compound_coolant`、`shielded_assembly_part`、`uranium_slurry`。

## 开发探针（单列）

| 类型 | ID |
| --- | --- |
| block | `create_nuclear_industry:p0_probe_arm_target` |
| block | `create_nuclear_industry:p0_probe_block` |
| block | `create_nuclear_industry:p0_probe_cold_port` |
| block | `create_nuclear_industry:p0_probe_goggle_block` |
| block | `create_nuclear_industry:p0_probe_hot_port` |
| block | `create_nuclear_industry:p0_probe_slider_block` |
| fluid | `create_nuclear_industry:p0_probe_cold` |
| fluid | `create_nuclear_industry:p0_probe_cold_flowing` |
| fluid | `create_nuclear_industry:p0_probe_hot` |
| fluid | `create_nuclear_industry:p0_probe_hot_flowing` |
| fluid_type | `create_nuclear_industry:p0_probe_cold` |
| fluid_type | `create_nuclear_industry:p0_probe_hot` |
| item | `create_nuclear_industry:p0_probe_arm_target` |
| item | `create_nuclear_industry:p0_probe_block` |
| item | `create_nuclear_industry:p0_probe_cold_port` |
| item | `create_nuclear_industry:p0_probe_goggle_block` |
| item | `create_nuclear_industry:p0_probe_hot_port` |
| item | `create_nuclear_industry:p0_probe_repair_item` |
| item | `create_nuclear_industry:p0_probe_slider_block` |

P0共6block/7item/4fluid/2fluid_type，不计入上方常规清单。`create_nuclear_industry_turbine_probe:shaft/owner`另为仅按GameTest命名空间条件注册的2个测试方块，不注册普通item。

已实读`src/main/java/com/iksxh/create_nuclear_industry/config/SpentFuelStorageConfig.java`，类和文件实际存在，`docs/server-config.md`当前对应source链接正确；不需要替换成其它类。
