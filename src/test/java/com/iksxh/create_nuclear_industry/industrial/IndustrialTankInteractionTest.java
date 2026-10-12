package com.iksxh.create_nuclear_industry.industrial;

import static org.junit.jupiter.api.Assertions.*;
import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity.CreativeSmartFluidTank;
import java.util.List;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.junit.jupiter.api.Test;

/** 外部能力代表合同：玩家身份补提交不得使热液/SC可被普通能力交易，普通库存仍守恒。 */
class IndustrialTankInteractionTest {
    @Test void creativeExternalCapabilityRejectsBothRestrictedFluidsOnEveryAction() {
        for (var fluid : List.of(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), BoilerContent.SUPERCRITICAL_STEAM.get())) {
            var inventory = new CreativeSmartFluidTank(16000, ignored -> { });
            inventory.setContainedFluid(new FluidStack(fluid, 1000));
            var handler = RestrictedFluidHandler.wrap(inventory);
            assertFalse(handler instanceof CreativeSmartFluidTank, "普通能力不得直接暴露原创造库存");
            for (var action : IFluidHandler.FluidAction.values()) {
                assertEquals(0, handler.fill(new FluidStack(fluid, 1000), action));
                assertTrue(handler.drain(new FluidStack(fluid, 1000), action).isEmpty());
                assertTrue(handler.drain(1000, action).isEmpty());
                assertEquals(16000, inventory.getFluidAmount());
            }
            var observed = handler.getFluidInTank(0);
            observed.setAmount(1);
            assertEquals(16000, inventory.getFluidAmount(), "读能力返回值不能变成原库存引用");
        }
    }

    @Test void ordinaryExternalColdAndWaterTransactionsRemainFiniteAndSimulationIsReadOnly() {
        for (var fluid : List.of(ModFluids.COMPOUND_COOLANT_SOURCE.get(), Fluids.WATER)) {
            var inventory = new FluidTank(4000);
            var handler = RestrictedFluidHandler.wrap(inventory);
            assertEquals(1000, handler.fill(new FluidStack(fluid, 1000), IFluidHandler.FluidAction.SIMULATE));
            assertTrue(inventory.isEmpty());
            assertEquals(1000, handler.fill(new FluidStack(fluid, 1000), IFluidHandler.FluidAction.EXECUTE));
            assertEquals(1000, inventory.getFluidAmount());
            assertEquals(1000, handler.drain(1000, IFluidHandler.FluidAction.SIMULATE).getAmount());
            assertEquals(1000, inventory.getFluidAmount());
            assertEquals(1000, handler.drain(1000, IFluidHandler.FluidAction.EXECUTE).getAmount());
            assertTrue(inventory.isEmpty());
        }
    }
}
