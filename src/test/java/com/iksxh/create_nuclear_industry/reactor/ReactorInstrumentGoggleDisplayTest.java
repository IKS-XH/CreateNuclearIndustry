package com.iksxh.create_nuclear_industry.reactor;

import com.iksxh.create_nuclear_industry.structure.ReactorInstrumentStructureSummary;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import com.iksxh.create_nuclear_industry.content.ModItems;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证护目镜动态文本的字段顺序、格式化精度、行列编号和空数据边界。 */
class ReactorInstrumentGoggleDisplayTest {
    @Test
    void dynamicDisplayKeepsOnlyWholeReactorRows() {
        ReactorInstrumentStructureSummary summary = validSummary();
        ReactorInstrumentTelemetry telemetry = new ReactorInstrumentTelemetry(
                true,
                "",
                128L,
                64L,
                List.of(
                        new ReactorInstrumentTelemetry.FuelColumnTelemetry(
                                new CoreColumnPosition(2, 0), 0.875D, 1.25D),
                        new ReactorInstrumentTelemetry.FuelColumnTelemetry(
                                new CoreColumnPosition(0, 1), 0.5D, 2.0D)
                ),
                List.of(new ReactorInstrumentTelemetry.ControlRodColumnTelemetry(
                        new CoreColumnPosition(1, 0), 0.25D)),
                3.25D,
                0.0D
        );

        List<Component> tooltip = new ArrayList<>();
        ReactorInstrumentGoggleDisplay.appendDynamicTooltip(tooltip, summary, telemetry);

        assertEquals(5, tooltip.size(), "instrument dynamic section must contain only whole-reactor rows");
        assertKey(tooltip.get(0), "dynamic_summary");
        assertArguments(tooltip.get(1), "128", "1500");
        assertArguments(tooltip.get(2), "64", "750");
        assertArguments(tooltip.get(3), "3.3");
        assertArguments(tooltip.get(4), "0.0");
        assertTrue(tooltip.stream().noneMatch(component ->
                        component.getString().contains("fuel_column")
                                || component.getString().contains("control_rod_column")),
                "instrument dynamic section must not contain per-column rows");
    }

    @Test
    void columnDisplaysKeepOneBasedPositionAndStableFieldFormatting() {
        List<Component> fuelTooltip = new ArrayList<>();
        ReactorInstrumentGoggleDisplay.appendFuelColumnTooltip(
                fuelTooltip,
                new ReactorInstrumentTelemetry.FuelColumnTelemetry(
                        new CoreColumnPosition(2, 0), 0.875D, 1.25D));
        assertEquals(1, fuelTooltip.size());
        assertArguments(fuelTooltip.get(0), "1", "3", "87.5%", "1.3");

        List<Component> controlRodTooltip = new ArrayList<>();
        ReactorInstrumentGoggleDisplay.appendControlRodColumnTooltip(
                controlRodTooltip,
                new ReactorInstrumentTelemetry.ControlRodColumnTelemetry(
                        new CoreColumnPosition(1, 0), 0.25D));
        assertEquals(1, controlRodTooltip.size());
        assertArguments(controlRodTooltip.get(0), "1", "2", "25.0%");
    }

    @Test
    void emptyRuntimeDataShowsExplicitZeroesAndNeverFallsBackToStaticValues() {
        List<Component> tooltip = new ArrayList<>();
        ReactorInstrumentGoggleDisplay.appendDynamicTooltip(
                tooltip,
                validSummary(),
                new ReactorInstrumentTelemetry(true, "", 0L, 0L,
                        List.of(), List.of(), 0.0D, 0.0D));

        assertEquals(5, tooltip.size());
        assertArguments(tooltip.get(1), "0", "1500");
        assertArguments(tooltip.get(2), "0", "750");
        assertArguments(tooltip.get(3), "0.0");
        assertArguments(tooltip.get(4), "0.0");
    }

    @Test
    void unavailableRuntimeDataDisplaysWaitingInsteadOfStaleOrZeroTelemetry() {
        List<Component> tooltip = new ArrayList<>();
        ReactorInstrumentGoggleDisplay.appendDynamicTooltip(
                tooltip,
                validSummary(),
                ReactorInstrumentTelemetry.unavailable("not ticked"));

        assertEquals(2, tooltip.size());
        assertTrue(tooltip.get(1).getString().contains("runtime_data_waiting")
                        || tooltip.get(1).getString().contains("等待首次成功运行数据")
                        || tooltip.get(1).getString().contains("Waiting for the first successful runtime tick"));
    }

    @Test
    void formatterAlwaysUsesRootLocaleAndOneDecimalPlace() {
        assertEquals("0.0%", ReactorInstrumentGoggleDisplay.formatPercent(0.0D));
        assertEquals("87.5%", ReactorInstrumentGoggleDisplay.formatPercent(0.875D));
        assertEquals("3.3", ReactorInstrumentGoggleDisplay.formatRate(3.25D));
        assertEquals("0.0", ReactorInstrumentGoggleDisplay.formatRate(-0.0D));
    }

    @Test
    void fuelAssemblyDisplayUsesDeterministicRemainingDurability() {
        ItemStack fuel = new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get());
        fuel.setDamageValue(54_321);
        List<Component> tooltip = new ArrayList<>();

        ReactorInstrumentGoggleDisplay.appendFuelAssemblyTooltip(tooltip, fuel);

        assertEquals(1, tooltip.size());
        assertKey(tooltip.get(0), "fuel_assembly_durability");
        assertArguments(tooltip.get(0), "161679", "216000", "74.9%");
    }

    private static ReactorInstrumentStructureSummary validSummary() {
        return ReactorInstrumentStructureSummary.from(
                ReactorStructureDefinition.scan(ReactorStructureDefinition.canonicalTemplate()),
                1_500L,
                750L);
    }

    private static void assertKey(Component component, String expectedKey) {
        assertTrue(component.getContents() instanceof TranslatableContents);
        assertEquals("goggle.create_nuclear_industry.reactor." + expectedKey,
                ((TranslatableContents) component.getContents()).getKey());
    }

    private static void assertArguments(Component component, String... expected) {
        assertTrue(component.getContents() instanceof TranslatableContents);
        Object[] actual = ((TranslatableContents) component.getContents()).getArgs();
        assertEquals(List.of(expected), List.of(actual));
    }
}
