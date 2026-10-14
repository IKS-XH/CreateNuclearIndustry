package com.iksxh.create_nuclear_industry.content;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.automation.ReactorAutomationPortBlock;
import com.iksxh.create_nuclear_industry.automation.ReactorAutomationPortBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import com.iksxh.create_nuclear_industry.automation.gui.ReactorAdministratorMenu;

/** 独立注册可选反应堆接口，不引入管理台或第二仪表。 */
public final class ReactorAutomationContent {
    public static final String ID="reactor_automation_port";
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE,CreateNuclearIndustry.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(BuiltInRegistries.MENU,CreateNuclearIndustry.MOD_ID);
    public static final DeferredHolder<MenuType<?>,MenuType<ReactorAdministratorMenu>> ADMINISTRATOR_MENU=MENUS.register("reactor_administrator",()->IMenuTypeExtension.create(ReactorAdministratorMenu::new));
    public static final DeferredBlock<ReactorAutomationPortBlock> PORT=BLOCKS.register(ID,
            ()->new ReactorAutomationPortBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5,10).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> PORT_ITEM=ITEMS.register(ID,()->new BlockItem(PORT.get(),new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<ReactorAutomationPortBlockEntity>> PORT_ENTITY=ENTITIES.register(ID,
            ()->BlockEntityType.Builder.of(ReactorAutomationPortBlockEntity::new,PORT.get()).build(null));
    private ReactorAutomationContent() {}
    public static void register(IEventBus bus) { BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); MENUS.register(bus); }
}
