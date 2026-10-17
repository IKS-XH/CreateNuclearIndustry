package com.iksxh.create_nuclear_industry.radiation;
import com.google.gson.*;
import com.iksxh.create_nuclear_industry.content.RadiationContent;
import java.nio.file.*;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/** 验证实际皮革材质/耐久/修复及五配方准确成本，而非只验JSON存在。 */
class RadiationEquipmentDataTest {
    @Test void leatherPropertiesAndRecipesMatchApprovedCost() throws Exception {
        Item[] ours={RadiationContent.HELMET.get(),RadiationContent.CHESTPLATE.get(),RadiationContent.LEGGINGS.get(),RadiationContent.BOOTS.get()};
        Item[] vanilla={Items.LEATHER_HELMET,Items.LEATHER_CHESTPLATE,Items.LEATHER_LEGGINGS,Items.LEATHER_BOOTS};
        for(int i=0;i<ours.length;i++) {
            assertEquals(new ItemStack(vanilla[i]).getMaxDamage(),new ItemStack(ours[i]).getMaxDamage());
            assertEquals(((ArmorItem)vanilla[i]).getDefense(),((ArmorItem)ours[i]).getDefense());
            assertTrue(ours[i].isValidRepairItem(new ItemStack(ours[i]),new ItemStack(Items.LEATHER)));
        }
        int leather=0,lead=0,goggles=0;
        for(String part:new String[]{"helmet","chestplate","leggings","boots"}) {
            var recipe=JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/create_nuclear_industry/recipe/crafting/radiation_"+part+".json"))).getAsJsonObject();
            assertEquals("minecraft:crafting_shaped",recipe.get("type").getAsString());
            for(var row:recipe.getAsJsonArray("pattern"))for(char c:row.getAsString().toCharArray()){
                if(c=='L')leather++;if(c=='P')lead++;
                if(c=='G'){
                    assertEquals("helmet",part);
                    assertEquals("create:goggles",recipe.getAsJsonObject("key").getAsJsonObject("G").get("item").getAsString());
                    goggles++;
                }
            }
        }
        assertEquals(24,leather);assertEquals(4,lead);assertEquals(1,goggles);
    }
}
