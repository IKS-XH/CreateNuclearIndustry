package com.iksxh.create_nuclear_industry.turbine;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 通过公开解析/租约边界测试；反射允许在缺少运行接口的原始版本记录真实 RED。 */
class TurbineRuntimeDisplayTest {
    static final UUID OWNER=UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID MEMBER=UUID.fromString("00000000-0000-0000-0000-000000000002");
    static Class<?> type(String name) {
        return assertDoesNotThrow(()->Class.forName("com.iksxh.create_nuclear_industry.turbine.display."+name),
                "缺少可信汽轮机运行显示接口："+name);
    }
    static Object call(Object target,String method,Class<?>[] types,Object... args) throws Exception {
        return target.getClass().getMethod(method,types).invoke(target,args);
    }
    static CompoundTag tag(long tick,float rpm) {
        CompoundTag t=new CompoundTag(); t.putString("Dimension","minecraft:overworld");
        t.putIntArray("Owner",new int[]{0,63,3}); t.putUUID("Generation",OWNER); t.putInt("Epoch",2);
        t.putIntArray("Front",new int[]{0,64,0}); t.putString("Facing","north");
        t.putInt("Diameter",3); t.putInt("Rotors",4); t.putInt("Index",0); t.putUUID("Member",MEMBER);
        t.putLong("Tick",tick); t.putFloat("Rpm",rpm); return t;
    }
    static Optional<?> decode(CompoundTag tag) throws Exception {
        return (Optional<?>)type("TurbineRuntimeSample").getMethod("decode",CompoundTag.class).invoke(null,tag);
    }
    @Test void zeroActualSpeedIsReliableStoppedSample() throws Exception {
        var s=decode(tag(10,0)).orElseThrow(); assertEquals(0f,call(s,"rpm",new Class<?>[]{}));
    }
    @Test void reverseSpeedIsPreservedAndRoundTripIsIndependent() throws Exception {
        var s=decode(tag(10,-128)).orElseThrow(); var encoded=(CompoundTag)call(s,"encode",new Class<?>[]{});
        assertEquals(s,decode(encoded).orElseThrow()); encoded.putFloat("Rpm",256);
        assertEquals(-128f,call(s,"rpm",new Class<?>[]{}));
    }
    @Test void malformedWholeSampleIsRejected() throws Exception {
        for(String key:java.util.List.of("Dimension","Owner","Generation","Epoch","Front","Facing","Diameter","Rotors","Index","Member","Tick","Rpm")) {
            var t=tag(10,128); t.remove(key); assertTrue(decode(t).isEmpty(),key);
        }
        for(float rpm:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY}) {
            var t=tag(10,rpm); assertTrue(decode(t).isEmpty());
        }
    }
    @Test void impossibleCoordinatesGeometryAndMemberIndexAreRejected() throws Exception {
        for(String key:java.util.List.of("Epoch","Tick","Index")) {var t=tag(10,128); if(key.equals("Tick"))t.putLong(key,-1);else t.putInt(key,-1);assertTrue(decode(t).isEmpty());}
        var t=tag(10,128); t.putString("Facing","up");assertTrue(decode(t).isEmpty());
        t=tag(10,128);t.putInt("Diameter",4);assertTrue(decode(t).isEmpty());
        t=tag(10,128);t.putInt("Rotors",17);assertTrue(decode(t).isEmpty());
        t=tag(10,128);t.putInt("Index",4);assertTrue(decode(t).isEmpty());
        t=tag(10,128);t.putIntArray("Owner",new int[]{20,63,3});assertTrue(decode(t).isEmpty());
        t=tag(10,128);t.putIntArray("Front",new int[]{0,64});assertTrue(decode(t).isEmpty());
    }
    @Test void signedWorldAxisConvertsToFrontToRearAxis() throws Exception {
        var c=type("TurbineRuntimeSample");var m=c.getMethod("localRpm",net.minecraft.core.Direction.class,float.class);
        assertEquals(128f,m.invoke(null,net.minecraft.core.Direction.NORTH,128f));
        assertEquals(-128f,m.invoke(null,net.minecraft.core.Direction.SOUTH,128f));
        assertEquals(-128f,m.invoke(null,net.minecraft.core.Direction.EAST,128f));
        assertEquals(128f,m.invoke(null,net.minecraft.core.Direction.WEST,128f));
    }
    static Object lease() throws Exception {return type("TurbineRuntimeLease").getConstructor().newInstance();}
    static boolean receive(Object lease,Object s,long now,UUID owner,int epoch,UUID member) throws Exception {
        return (boolean)call(lease,"receive",new Class<?>[]{type("TurbineRuntimeSample"),long.class,UUID.class,int.class,UUID.class},s,now,owner,epoch,member);
    }
    static Optional<?> view(Object lease,long now) throws Exception {return (Optional<?>)call(lease,"current",new Class<?>[]{long.class},now);}
    @Test void duplicateCannotRenewLeaseAndExpiryIsBounded() throws Exception {
        var l=lease();var s=decode(tag(10,128)).orElseThrow();assertTrue(receive(l,s,0,OWNER,2,MEMBER));
        assertFalse(receive(l,s,19,OWNER,2,MEMBER));assertTrue(view(l,19).isPresent());assertTrue(view(l,20).isEmpty());
    }
    @Test void oldEpochAndGenerationCannotReviveAfterReplacement() throws Exception {
        var l=lease();var s=decode(tag(10,128)).orElseThrow();assertTrue(receive(l,s,0,OWNER,2,MEMBER));
        var newer=tag(11,0);newer.putInt("Epoch",3);assertTrue(receive(l,decode(newer).orElseThrow(),1,OWNER,3,MEMBER));
        assertFalse(receive(l,s,2,OWNER,3,MEMBER));assertTrue(view(l,2).isEmpty());
        assertFalse(receive(l,s,3,UUID.randomUUID(),2,MEMBER));
    }
    @Test void wrongMemberOrExplicitRevocationClearsOldAnimation() throws Exception {
        var l=lease();var s=decode(tag(10,128)).orElseThrow();receive(l,s,0,OWNER,2,MEMBER);
        assertFalse(receive(l,s,1,OWNER,2,UUID.randomUUID()));assertTrue(view(l,1).isEmpty());
        var n=decode(tag(11,128)).orElseThrow();assertTrue(receive(l,n,2,OWNER,2,MEMBER));
        call(l,"revoke",new Class<?>[]{});assertTrue(view(l,2).isEmpty());assertFalse(receive(l,n,3,OWNER,2,MEMBER));
    }
    @Test void pauseFreezesOnlyAgeAndWorldSwitchClearsClockSession() throws Exception {
        var c=type("TurbineRuntimeClock").getConstructor().newInstance();Object a=new Object(),b=new Object();
        call(c,"advance",new Class<?>[]{Object.class,boolean.class},a,false);
        long tick=(long)call(c,"tick",new Class<?>[]{Object.class},a);
        for(int i=0;i<100;i++)call(c,"advance",new Class<?>[]{Object.class,boolean.class},a,true);
        assertEquals(tick,call(c,"tick",new Class<?>[]{Object.class},a));
        call(c,"advance",new Class<?>[]{Object.class,boolean.class},b,true);
        assertEquals(-1L,call(c,"tick",new Class<?>[]{Object.class},a));
        call(c,"advance",new Class<?>[]{Object.class,boolean.class},b,false);
        assertEquals(1L,call(c,"tick",new Class<?>[]{Object.class},b));
    }
    @Test void retiredGenerationAndOlderSampleCannotRestoreMotion() throws Exception {
        var l=lease();var first=decode(tag(10,128)).orElseThrow();receive(l,first,0,OWNER,2,MEMBER);
        var next=tag(11,0);UUID replacement=UUID.randomUUID();next.putUUID("Generation",replacement);
        assertTrue(receive(l,decode(next).orElseThrow(),1,replacement,2,MEMBER));
        assertFalse(receive(l,first,2,OWNER,2,MEMBER));assertTrue(view(l,2).isEmpty());
        var later=tag(13,-64);later.putUUID("Generation",replacement);receive(l,decode(later).orElseThrow(),3,replacement,2,MEMBER);
        var older=tag(12,256);older.putUUID("Generation",replacement);
        assertFalse(receive(l,decode(older).orElseThrow(),4,replacement,2,MEMBER));assertTrue(view(l,4).isEmpty());
    }
    @Test void fieldTypesAreNotCoercedAndMutableCoordinatesAreNotRetained() throws Exception {
        for(String key:java.util.List.of("Dimension","Owner","Generation","Epoch","Front","Facing","Diameter","Rotors","Index","Member","Tick","Rpm")) {
            var wrong=tag(10,128);wrong.putBoolean(key,true);assertTrue(decode(wrong).isEmpty(),key);
        }
        var t=tag(10,128);var value=decode(t).orElseThrow();t.getIntArray("Front")[0]=999;
        assertEquals(new net.minecraft.core.BlockPos(0,64,1),call(value,"memberPos",new Class<?>[]{}));
    }
    @Test void pausedLeaseSurvivesButRevocationDuringPauseIsFinalUntilNewSample() throws Exception {
        var clock=type("TurbineRuntimeClock").getConstructor().newInstance();Object world=new Object();
        call(clock,"advance",new Class<?>[]{Object.class,boolean.class},world,false);
        var l=lease();var s=decode(tag(10,128)).orElseThrow();receive(l,s,1,OWNER,2,MEMBER);
        for(int i=0;i<100;i++)call(clock,"advance",new Class<?>[]{Object.class,boolean.class},world,true);
        long now=(long)call(clock,"tick",new Class<?>[]{Object.class},world);assertTrue(view(l,now).isPresent());
        call(l,"revoke",new Class<?>[]{});assertTrue(view(l,now).isEmpty());
        assertFalse(receive(l,s,now,OWNER,2,MEMBER));assertTrue(view(l,now).isEmpty());
        assertTrue(receive(l,decode(tag(11,0)).orElseThrow(),now,OWNER,2,MEMBER));
        assertEquals(0f,call(view(l,now).orElseThrow(),"rpm",new Class<?>[]{}));
    }
    @Test void coalescedMemberReplacementNeedsNewAuthorityEpoch() throws Exception {
        var l=lease();var initial=decode(tag(10,128)).orElseThrow();receive(l,initial,0,OWNER,2,MEMBER);
        UUID replacement=UUID.randomUUID();var next=tag(11,0);next.putUUID("Member",replacement);
        assertFalse(receive(l,decode(next).orElseThrow(),1,OWNER,2,replacement));assertTrue(view(l,1).isEmpty());
        next.putInt("Epoch",3);next.putLong("Tick",12);
        assertTrue(receive(l,decode(next).orElseThrow(),2,OWNER,3,replacement));
        assertFalse(receive(l,initial,3,OWNER,3,MEMBER));assertTrue(view(l,3).isEmpty());
    }
    @Test void sameTickEpochAndMemberChangesRevokePreviouslyRunningSample() throws Exception {
        for(int change=0;change<3;change++) {
            var l=lease();var old=decode(tag(10,128)).orElseThrow();assertTrue(receive(l,old,0,OWNER,2,MEMBER));
            int epoch=change==1?2:3;UUID member=change==0?MEMBER:UUID.randomUUID();
            var changed=tag(10,0);changed.putInt("Epoch",epoch);changed.putUUID("Member",member);
            assertFalse(receive(l,decode(changed).orElseThrow(),1,OWNER,epoch,member));
            assertTrue(view(l,1).isEmpty(),"同tick身份变化仍返回旧128RPM："+change);
            assertFalse(receive(l,old,2,OWNER,2,MEMBER));assertTrue(view(l,2).isEmpty(),"旧重复包复活了已撤销显示");
        }
    }
    @Test void sameTickGeometryOrRpmDifferenceRevokesWholeSample() throws Exception {
        for(int change=0;change<4;change++) {
            var l=lease();var old=decode(tag(10,128)).orElseThrow();receive(l,old,0,OWNER,2,MEMBER);
            var changed=tag(10,128);
            switch(change) {
                case 0 -> changed.putFloat("Rpm",0);
                case 1 -> changed.putInt("Index",1);
                case 2 -> changed.putInt("Rotors",5);
                case 3 -> {changed.putIntArray("Front",new int[]{1,64,0});changed.putIntArray("Owner",new int[]{1,63,3});}
            }
            assertFalse(receive(l,decode(changed).orElseThrow(),1,OWNER,2,MEMBER));
            assertTrue(view(l,1).isEmpty(),"同tick完整包差异未撤销："+change);
            var next=decode(tag(11,0)).orElseThrow();assertTrue(receive(l,next,2,OWNER,2,MEMBER));
            assertEquals(0f,call(view(l,2).orElseThrow(),"rpm",new Class<?>[]{}));
        }
    }
    @Test void equalWholeSampleDoesNotRenewOrReviveRevokedLease() throws Exception {
        var l=lease();var first=decode(tag(10,128)).orElseThrow();var equalCopy=decode(tag(10,128)).orElseThrow();
        assertTrue(receive(l,first,0,OWNER,2,MEMBER));assertFalse(receive(l,equalCopy,19,OWNER,2,MEMBER));
        assertEquals(first,view(l,19).orElseThrow());assertTrue(view(l,20).isEmpty());
        assertFalse(receive(l,equalCopy,20,OWNER,2,MEMBER));assertTrue(view(l,20).isEmpty());
    }
}
