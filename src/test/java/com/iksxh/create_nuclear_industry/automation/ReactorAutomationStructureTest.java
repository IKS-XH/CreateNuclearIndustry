package com.iksxh.create_nuclear_industry.automation;

import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;

/** 验证可选接口的数量和真实侧壁规则，不依赖方块注册。 */
class ReactorAutomationStructureTest {
    private static final String ID = "create_nuclear_industry:reactor_automation_port";
    @Test void acceptsZeroOrOneAndRejectsTwo() {
        var blocks = new HashMap<>(ReactorStructureDefinition.canonicalTemplate());
        assertTrue(ReactorStructureDefinition.scan(blocks).valid());
        blocks.put(new ReactorStructureDefinition.LocalPosition(0, 2, 1), ID);
        assertTrue(ReactorStructureDefinition.scan(blocks).valid());
        blocks.put(new ReactorStructureDefinition.LocalPosition(4, 2, 1), ID);
        assertFalse(ReactorStructureDefinition.scan(blocks).valid());
    }
    @Test void rejectsFrameworkTopAndBottom() {
        for (var pos : new ReactorStructureDefinition.LocalPosition[] {
                new ReactorStructureDefinition.LocalPosition(0, 0, 0),
                new ReactorStructureDefinition.LocalPosition(0, 4, 2),
                new ReactorStructureDefinition.LocalPosition(0, 0, 2)}) {
            var blocks = new HashMap<>(ReactorStructureDefinition.canonicalTemplate());
            blocks.put(pos, ID);
            assertFalse(ReactorStructureDefinition.scan(blocks).valid());
        }
    }
    @Test void playerDiagnosticIncludesTheRejectedAutomationPosition() {
        var blocks=new HashMap<>(ReactorStructureDefinition.canonicalTemplate());
        blocks.put(new ReactorStructureDefinition.LocalPosition(0,0,0),ID);
        var result=ReactorStructureDefinition.scan(blocks);
        var message=com.iksxh.create_nuclear_industry.structure.ReactorStructureDiagnostics.message(result);
        var translated=(net.minecraft.network.chat.contents.TranslatableContents)message.getContents();
        assertEquals(1,translated.getArgs().length);
        assertTrue(translated.getArgs()[0].toString().contains("0"));
    }
}
