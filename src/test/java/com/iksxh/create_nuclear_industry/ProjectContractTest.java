package com.iksxh.create_nuclear_industry;

import com.iksxh.create_nuclear_industry.content.ModBlocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectContractTest {
    @Test
    void exposesStableModAndSampleBlockIds() {
        assertEquals("create_nuclear_industry", CreateNuclearIndustry.MOD_ID);
        assertEquals("experimental_reactor_casing", ModBlocks.EXPERIMENTAL_REACTOR_CASING_ID);
    }
}
