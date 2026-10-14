package com.iksxh.create_nuclear_industry.automation;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity.FuelType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/** 付热窗口不能一笔两用，假KINDLED不参与点燃资格。 */
class ReactorAdministratorHeatSupplyTest {
    @Test void completedSupplyIsVisibleBeforeNextBurnerTickWithoutGrantingAnotherConsumption() {
        var account=new ReactorAutomationHeatSupply.Account();account.pay(1,1);
        assertFalse(account.completedSource(1));assertTrue(account.consume(1));assertTrue(account.completedSource(2));
        assertFalse(account.consume(2));assertFalse(account.completedSource(3));
        account.clear();assertFalse(account.completedSource(2));
    }
    @Test void eachPaymentOnlyCoversOneSettlementAcrossTickOrdering() {
        var a=new ReactorAutomationHeatSupply.Account();
        a.pay(10,1); assertTrue(a.consume(10)); assertFalse(a.consume(10)); assertFalse(a.consume(11));
        assertTrue(a.recent(11)); assertFalse(a.recent(12));
        a.pay(12,1); assertTrue(a.consume(13)); assertFalse(a.consume(13));
        a.pay(14,1); assertFalse(a.consume(16));
        a.pay(20,1); a.clear(); assertFalse(a.consume(20)); assertFalse(a.recent(20));
        a.pay(20,1); assertFalse(a.recent(20),"同tick失联再入不能重新出票");
        a.pay(21,1); assertTrue(a.consume(21));a.clear();a.pay(22,1);assertTrue(a.consume(22));
        a.pay(23,1);assertTrue(a.consume(23));a.pay(24,1);assertFalse(a.consume(23));assertTrue(a.consume(24));
    }
    @Test void noFreeRestartFromItsOwnHeatLevelAndSpecialFuelRemainsNative() {
        assertFalse(ReactorAutomationHeatSupply.eligible(FuelType.NONE,0,false,false));
        assertTrue(ReactorAutomationHeatSupply.eligible(FuelType.NORMAL,1,false,false));
        assertTrue(ReactorAutomationHeatSupply.eligible(FuelType.NONE,0,false,true));
        assertFalse(ReactorAutomationHeatSupply.eligible(FuelType.SPECIAL,1,false,true));
        assertFalse(ReactorAutomationHeatSupply.eligible(FuelType.NORMAL,10,true,true));
    }
}
