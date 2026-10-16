# 当前实际配方索引

更新：2026-10-16。当前常规116个配方、开发探针2个，共18种type。下表列实际ID/type和唯一JSON入口，输入、输出、amount/count、概率和工时由源数据定义，不复制另一套数量表；游戏/数据包覆盖后以实际配方管理器和JEI为准。

## 1. 生产关系与工序

三矿/原料加工→工业材料和零件→铀精矿/料浆→离心富集→芯块烧结→燃料组件装配→反应堆。耗尽组件→屏蔽装配台封装→干式贮存架；复杂再处理未实现。换热器对盆/烧结炉的供热是设备规则，不是新增重复加工配方。

离心副产贫化铀粉→熔炉/高炉炼锭→储存块，或与钻石装备及下界合金升级模板进行原生锻造升级。装备不提供直接工作台制造配方，核废料处理规则不随本批改变。

专用centrifuging/sintering/shielded_assembly工序的字段与实际机器协议一致，工时不要照抄普通Create processing_time。工作台/动力合成/搅拌/压块/部署器等走各自原生类型。只允许现有配方链，不根据旧草案生成空内容或凭空增加材料数量。

## 2. 配方通用规则

设备和零件工作台配方采用有序，防止自动转为无序搅拌；粗矿块/金属锭拆解保持无序。默认不用序列装配，确有量产和多步必要性才另行确认；已验配方保持。Ponder只讲用法，不介绍具体配方。

### 2.3 NeoForge 通用标签目录

通用材料使用实际c命名空间标签（如c:ingots/steel、c:plates/steel），核专用产物用本模组标签。标签文件位于[数据目录](../src/main/resources/data/)，键使用实际ingredients/key引用；不把物品名、整颗宝石或染料当粉末标签替代，不借空标签补未来物料。

原成本/未来工序完整草案见[整理前配方表](archive/2026-10-15/snapshots/docs/recipes.md)，有效后置议题由[路线图](implementation-roadmap.md)承接，不追认旧备选数值。

## 3. 实际配方入口

`create:sequenced_assembly`的`sequence`是该外层配方内部执行的步骤，`transitional_item`承载序列加工进度，`loops`定义该序列循环；外层`results`才是最终产出。嵌套步骤不另计为独立顶层配方，也不从步骤数量推算材料消耗。

## 常规配方（116）

| 配方ID | type | 实际JSON |
| --- | --- | --- |
| `create_nuclear_industry:compacting/uranium_tailings_brick` | `create:compacting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/compacting/uranium_tailings_brick.json) |
| `create_nuclear_industry:crushing/charcoal_dust` | `create:crushing` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crushing/charcoal_dust.json) |
| `create_nuclear_industry:crushing/coal_dust` | `create:crushing` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crushing/coal_dust.json) |
| `create_nuclear_industry:crushing/iron_dust` | `create:crushing` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crushing/iron_dust.json) |
| `create_nuclear_industry:crushing/lapis_dust` | `create:crushing` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crushing/lapis_dust.json) |
| `create_nuclear_industry:deploying/pressure_fitting` | `create:deploying` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/deploying/pressure_fitting.json) |
| `create_nuclear_industry:mechanical_crafting/control_rod_drive` | `create:mechanical_crafting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/mechanical_crafting/control_rod_drive.json) |
| `create_nuclear_industry:mechanical_crafting/enrichment_centrifuge` | `create:mechanical_crafting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/mechanical_crafting/enrichment_centrifuge.json) |
| `create_nuclear_industry:mechanical_crafting/high_pressure_boiler_controller` | `create:mechanical_crafting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/mechanical_crafting/high_pressure_boiler_controller.json) |
| `create_nuclear_industry:mechanical_crafting/reactor_instrument_port` | `create:mechanical_crafting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/mechanical_crafting/reactor_instrument_port.json) |
| `create_nuclear_industry:mechanical_crafting/reactor_refueling_port` | `create:mechanical_crafting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/mechanical_crafting/reactor_refueling_port.json) |
| `create_nuclear_industry:mechanical_crafting/shielded_assembly_station` | `create:mechanical_crafting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/mechanical_crafting/shielded_assembly_station.json) |
| `create_nuclear_industry:turbine_controller` | `create:mechanical_crafting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/turbine_controller.json) |
| `create_nuclear_industry:milling/glass_dust` | `create:milling` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/milling/glass_dust.json) |
| `create_nuclear_industry:milling/quartz_dust` | `create:milling` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/milling/quartz_dust.json) |
| `create_nuclear_industry:mixing/compound_coolant` | `create:mixing` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/mixing/compound_coolant.json) |
| `create_nuclear_industry:mixing/industrial_ceramic` | `create:mixing` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/mixing/industrial_ceramic.json) |
| `create_nuclear_industry:mixing/neutron_absorbing_ceramic` | `create:mixing` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/mixing/neutron_absorbing_ceramic.json) |
| `create_nuclear_industry:mixing/refractory_brick` | `create:mixing` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/mixing/refractory_brick.json) |
| `create_nuclear_industry:mixing/shielded_glass` | `create:mixing` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/mixing/shielded_glass.json) |
| `create_nuclear_industry:mixing/shielding_concrete` | `create:mixing` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/mixing/shielding_concrete.json) |
| `create_nuclear_industry:mixing/solder_ingot` | `create:mixing` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/mixing/solder_ingot.json) |
| `create_nuclear_industry:mixing/steel_dust_from_charcoal` | `create:mixing` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/mixing/steel_dust_from_charcoal.json) |
| `create_nuclear_industry:mixing/steel_dust_from_coal` | `create:mixing` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/mixing/steel_dust_from_coal.json) |
| `create_nuclear_industry:mixing/uranium_slurry` | `create:mixing` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/mixing/uranium_slurry.json) |
| `create_nuclear_industry:mixing/vitrification_medium` | `create:mixing` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/mixing/vitrification_medium.json) |
| `create_nuclear_industry:pressing/green_fuel_pellet` | `create:pressing` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/pressing/green_fuel_pellet.json) |
| `create_nuclear_industry:pressing/lead_plate` | `create:pressing` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/pressing/lead_plate.json) |
| `create_nuclear_industry:pressing/seal_ring` | `create:pressing` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/pressing/seal_ring.json) |
| `create_nuclear_industry:pressing/steel_plate` | `create:pressing` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/pressing/steel_plate.json) |
| `create_nuclear_industry:pressing/tin_plate` | `create:pressing` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/pressing/tin_plate.json) |
| `create_nuclear_industry:heat_exchanger/reinforced_steel_plate` | `create:sequenced_assembly` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/heat_exchanger/reinforced_steel_plate.json) |
| `create_nuclear_industry:sequenced_assembly/heavy_bearing` | `create:sequenced_assembly` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/sequenced_assembly/heavy_bearing.json) |
| `create_nuclear_industry:sequenced_assembly/industrial_sensor` | `create:sequenced_assembly` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/sequenced_assembly/industrial_sensor.json) |
| `create_nuclear_industry:sequenced_assembly/radiation_sensor` | `create:sequenced_assembly` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/sequenced_assembly/radiation_sensor.json) |
| `create_nuclear_industry:sequenced_assembly/steel_grate` | `create:sequenced_assembly` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/sequenced_assembly/steel_grate.json) |
| `create_nuclear_industry:splashing/crushed_raw_lead` | `create:splashing` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/splashing/crushed_raw_lead.json) |
| `create_nuclear_industry:splashing/crushed_raw_tin` | `create:splashing` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/splashing/crushed_raw_tin.json) |
| `create_nuclear_industry:splashing/crushed_raw_uranium` | `create:splashing` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/splashing/crushed_raw_uranium.json) |
| `create_nuclear_industry:centrifuging/uranium_slurry` | `create_nuclear_industry:centrifuging` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/centrifuging/uranium_slurry.json) |
| `create_nuclear_industry:shielded_assembly/fresh_fuel_assembly` | `create_nuclear_industry:shielded_assembly` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/shielded_assembly/fresh_fuel_assembly.json) |
| `create_nuclear_industry:shielded_assembly/sealed_spent_fuel_cask` | `create_nuclear_industry:shielded_assembly` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/shielded_assembly/sealed_spent_fuel_cask.json) |
| `create_nuclear_industry:sintering/sintered_fuel_pellet` | `create_nuclear_industry:sintering` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/sintering/sintered_fuel_pellet.json) |
| `create_nuclear_industry:blasting/lead_ingot_from_crushed_raw_lead` | `minecraft:blasting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/blasting/lead_ingot_from_crushed_raw_lead.json) |
| `create_nuclear_industry:blasting/lead_ingot_from_lead_ore` | `minecraft:blasting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/blasting/lead_ingot_from_lead_ore.json) |
| `create_nuclear_industry:blasting/lead_ingot_from_raw_lead` | `minecraft:blasting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/blasting/lead_ingot_from_raw_lead.json) |
| `create_nuclear_industry:blasting/steel_ingot_from_dust` | `minecraft:blasting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/blasting/steel_ingot_from_dust.json) |
| `create_nuclear_industry:blasting/tin_ingot_from_crushed_raw_tin` | `minecraft:blasting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/blasting/tin_ingot_from_crushed_raw_tin.json) |
| `create_nuclear_industry:blasting/tin_ingot_from_raw_tin` | `minecraft:blasting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/blasting/tin_ingot_from_raw_tin.json) |
| `create_nuclear_industry:blasting/tin_ingot_from_tin_ore` | `minecraft:blasting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/blasting/tin_ingot_from_tin_ore.json) |
| `create_nuclear_industry:ceramic_fluid_pipe` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/ceramic_fluid_pipe.json) |
| `create_nuclear_industry:ceramic_fluid_valve` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/ceramic_fluid_valve.json) |
| `create_nuclear_industry:crafting/boiler_heat_exchange_section` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crafting/boiler_heat_exchange_section.json) |
| `create_nuclear_industry:crafting/boiler_safety_valve` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crafting/boiler_safety_valve.json) |
| `create_nuclear_industry:crafting/dry_storage_rack` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crafting/dry_storage_rack.json) |
| `create_nuclear_industry:crafting/fuel_sintering_furnace` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crafting/fuel_sintering_furnace.json) |
| `create_nuclear_industry:crafting/high_pressure_boiler_casing` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crafting/high_pressure_boiler_casing.json) |
| `create_nuclear_industry:crafting/high_pressure_boiler_cold_coolant_port` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crafting/high_pressure_boiler_cold_coolant_port.json) |
| `create_nuclear_industry:crafting/high_pressure_boiler_hot_coolant_port` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crafting/high_pressure_boiler_hot_coolant_port.json) |
| `create_nuclear_industry:crafting/high_pressure_boiler_steam_port` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crafting/high_pressure_boiler_steam_port.json) |
| `create_nuclear_industry:crafting/high_pressure_boiler_water_port` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crafting/high_pressure_boiler_water_port.json) |
| `create_nuclear_industry:crafting/high_pressure_boiler_window` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crafting/high_pressure_boiler_window.json) |
| `create_nuclear_industry:crafting/lead_shielding_cask` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crafting/lead_shielding_cask.json) |
| `create_nuclear_industry:crafting/materials/lead_ingot_from_nuggets` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crafting/materials/lead_ingot_from_nuggets.json) |
| `create_nuclear_industry:crafting/materials/tin_ingot_from_nuggets` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crafting/materials/tin_ingot_from_nuggets.json) |
| `create_nuclear_industry:crafting/reactor/control_rod` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crafting/reactor/control_rod.json) |
| `create_nuclear_industry:crafting/reactor/reactor_automation_port` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crafting/reactor/reactor_automation_port.json) |
| `create_nuclear_industry:crafting/reactor/reactor_casing` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crafting/reactor/reactor_casing.json) |
| `create_nuclear_industry:crafting/reactor/reactor_cold_port` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crafting/reactor/reactor_cold_port.json) |
| `create_nuclear_industry:crafting/reactor/reactor_fuel_rod` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crafting/reactor/reactor_fuel_rod.json) |
| `create_nuclear_industry:crafting/reactor/reactor_hot_port` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crafting/reactor/reactor_hot_port.json) |
| `create_nuclear_industry:crafting/reactor/reactor_window` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crafting/reactor/reactor_window.json) |
| `create_nuclear_industry:crafting/turbine_window` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crafting/turbine_window.json) |
| `create_nuclear_industry:heat_exchanger/nuclear_heat_exchange_bundle` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/heat_exchanger/nuclear_heat_exchange_bundle.json) |
| `create_nuclear_industry:heat_exchanger/nuclear_heat_exchanger` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/heat_exchanger/nuclear_heat_exchanger.json) |
| `create_nuclear_industry:industrial_fluid_pump` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/industrial_fluid_pump.json) |
| `create_nuclear_industry:raw_lead_block` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/raw_lead_block.json) |
| `create_nuclear_industry:raw_tin_block` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/raw_tin_block.json) |
| `create_nuclear_industry:raw_uranium_block` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/raw_uranium_block.json) |
| `create_nuclear_industry:reinforced_fluid_pipe` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/reinforced_fluid_pipe.json) |
| `create_nuclear_industry:reinforced_fluid_valve` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/reinforced_fluid_valve.json) |
| `create_nuclear_industry:turbine_casing` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/turbine_casing.json) |
| `create_nuclear_industry:turbine_exhaust` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/turbine_exhaust.json) |
| `create_nuclear_industry:turbine_inlet` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/turbine_inlet.json) |
| `create_nuclear_industry:turbine_output_shaft` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/turbine_output_shaft.json) |
| `create_nuclear_industry:turbine_rotor` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/turbine_rotor.json) |
| `create_nuclear_industry:crafting/materials/lead_nugget_from_ingot` | `minecraft:crafting_shapeless` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crafting/materials/lead_nugget_from_ingot.json) |
| `create_nuclear_industry:crafting/materials/tin_nugget_from_ingot` | `minecraft:crafting_shapeless` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crafting/materials/tin_nugget_from_ingot.json) |
| `create_nuclear_industry:raw_lead_from_block` | `minecraft:crafting_shapeless` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/raw_lead_from_block.json) |
| `create_nuclear_industry:raw_tin_from_block` | `minecraft:crafting_shapeless` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/raw_tin_from_block.json) |
| `create_nuclear_industry:raw_uranium_from_block` | `minecraft:crafting_shapeless` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/raw_uranium_from_block.json) |
| `create_nuclear_industry:smelting/lead_ingot_from_crushed_raw_lead` | `minecraft:smelting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/smelting/lead_ingot_from_crushed_raw_lead.json) |
| `create_nuclear_industry:smelting/lead_ingot_from_lead_ore` | `minecraft:smelting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/smelting/lead_ingot_from_lead_ore.json) |
| `create_nuclear_industry:smelting/lead_ingot_from_raw_lead` | `minecraft:smelting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/smelting/lead_ingot_from_raw_lead.json) |
| `create_nuclear_industry:smelting/steel_ingot_from_dust` | `minecraft:smelting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/smelting/steel_ingot_from_dust.json) |
| `create_nuclear_industry:smelting/tin_ingot_from_crushed_raw_tin` | `minecraft:smelting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/smelting/tin_ingot_from_crushed_raw_tin.json) |
| `create_nuclear_industry:smelting/tin_ingot_from_raw_tin` | `minecraft:smelting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/smelting/tin_ingot_from_raw_tin.json) |
| `create_nuclear_industry:smelting/tin_ingot_from_tin_ore` | `minecraft:smelting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/smelting/tin_ingot_from_tin_ore.json) |
| `create_nuclear_industry:heat_exchanger/steel_pipe_blank` | `minecraft:stonecutting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/heat_exchanger/steel_pipe_blank.json) |
| `create_nuclear_industry:stonecutting/fuel_cladding_tube` | `minecraft:stonecutting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/stonecutting/fuel_cladding_tube.json) |
| `create_nuclear_industry:stonecutting/steel_mesh` | `minecraft:stonecutting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/stonecutting/steel_mesh.json) |
| `create_nuclear_industry:stonecutting/steel_rod` | `minecraft:stonecutting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/stonecutting/steel_rod.json) |
| `create_nuclear_industry:stonecutting/tin_wire` | `minecraft:stonecutting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/stonecutting/tin_wire.json) |
| `create_nuclear_industry:smelting/depleted_uranium_ingot_from_dust` | `minecraft:smelting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/smelting/depleted_uranium_ingot_from_dust.json) |
| `create_nuclear_industry:blasting/depleted_uranium_ingot_from_dust` | `minecraft:blasting` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/blasting/depleted_uranium_ingot_from_dust.json) |
| `create_nuclear_industry:crafting/depleted_uranium_block` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crafting/depleted_uranium_block.json) |
| `create_nuclear_industry:crafting/depleted_uranium_ingots_from_block` | `minecraft:crafting_shapeless` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/crafting/depleted_uranium_ingots_from_block.json) |
| `create_nuclear_industry:smithing/depleted_uranium_helmet` | `minecraft:smithing_transform` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/smithing/depleted_uranium_helmet.json) |
| `create_nuclear_industry:smithing/depleted_uranium_chestplate` | `minecraft:smithing_transform` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/smithing/depleted_uranium_chestplate.json) |
| `create_nuclear_industry:smithing/depleted_uranium_leggings` | `minecraft:smithing_transform` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/smithing/depleted_uranium_leggings.json) |
| `create_nuclear_industry:smithing/depleted_uranium_boots` | `minecraft:smithing_transform` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/smithing/depleted_uranium_boots.json) |
| `create_nuclear_industry:smithing/depleted_uranium_sword` | `minecraft:smithing_transform` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/smithing/depleted_uranium_sword.json) |
| `create_nuclear_industry:smithing/depleted_uranium_pickaxe` | `minecraft:smithing_transform` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/smithing/depleted_uranium_pickaxe.json) |
| `create_nuclear_industry:smithing/depleted_uranium_axe` | `minecraft:smithing_transform` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/smithing/depleted_uranium_axe.json) |
| `create_nuclear_industry:smithing/depleted_uranium_shovel` | `minecraft:smithing_transform` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/smithing/depleted_uranium_shovel.json) |
| `create_nuclear_industry:smithing/depleted_uranium_hoe` | `minecraft:smithing_transform` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/smithing/depleted_uranium_hoe.json) |

## 开发探针取用配方（2）

| 配方ID | type | 实际JSON |
| --- | --- | --- |
| `create_nuclear_industry:p0_probe_goggle_block` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/p0_probe_goggle_block.json) |
| `create_nuclear_industry:p0_probe_slider_block` | `minecraft:crafting_shaped` | [JSON](../src/main/resources/data/create_nuclear_industry/recipe/p0_probe_slider_block.json) |
