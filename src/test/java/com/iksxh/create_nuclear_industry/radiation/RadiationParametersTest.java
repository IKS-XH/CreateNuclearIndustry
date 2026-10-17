package com.iksxh.create_nuclear_industry.radiation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
/** 逐字段破坏完整配置组，证明有限数、范围及阈值顺序均受构造边界保护。 */
class RadiationParametersTest {
    static RadiationParameters change(String name,Object value) {
        try {
            var fields=RadiationParameters.class.getRecordComponents();Object[] args=new Object[fields.length];Class<?>[] types=new Class<?>[fields.length];
            for(int i=0;i<fields.length;i++){types[i]=fields[i].getType();args[i]=fields[i].getName().equals(name)?value:fields[i].getAccessor().invoke(RadiationParameters.DEFAULT);}
            return RadiationParameters.class.getConstructor(types).newInstance(args);
        }catch(InvocationTargetException e){throw (RuntimeException)e.getCause();}catch(Exception e){throw new AssertionError(e);}
    }
    @Test void rejectsNonFiniteNegativeAndInvalidGroups() {
        for(var field:RadiationParameters.class.getRecordComponents()) if(field.getType()==double.class) {
            assertThrows(IllegalArgumentException.class,()->change(field.getName(),Double.NaN),field.getName());
            assertThrows(IllegalArgumentException.class,()->change(field.getName(),Double.POSITIVE_INFINITY),field.getName());
            assertThrows(IllegalArgumentException.class,()->change(field.getName(),-.1),field.getName());
        }
        for(String ratio:new String[]{"caskTransmission","rackTransmission","reactorTransmission","blockTransmission","head","chest","legs","feet","maxReduction"})
            assertThrows(IllegalArgumentException.class,()->change(ratio,1.01));
        assertThrows(IllegalArgumentException.class,()->change("sampleIntervalTicks",0));
        assertThrows(IllegalArgumentException.class,()->change("detectionRadiusBlocks",65));
        assertThrows(IllegalArgumentException.class,()->change("warningDose",50.0));
        assertThrows(IllegalArgumentException.class,()->change("damageDose",101.0));
        assertDoesNotThrow(()->change("sampleIntervalTicks",Integer.MAX_VALUE));
    }
    @Test void distanceIsInverseSquareAndRadiusBounded() {
        var p=RadiationParameters.DEFAULT;assertEquals(1,p.distanceFactor(0));assertEquals(.25,p.distanceFactor(4));assertEquals(0,p.distanceFactor(1025));
    }
}
