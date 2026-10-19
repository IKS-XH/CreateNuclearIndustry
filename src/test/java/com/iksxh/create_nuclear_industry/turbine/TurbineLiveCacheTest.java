package com.iksxh.create_nuclear_industry.turbine;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 版本见证只在稳定读取期间生效；不依赖FML世界，实际palette钩子另由真实包验证。 */
class TurbineLiveCacheTest {
    private static final class Source {
        long version;
        boolean writing;
        Runnable onRead = () -> {};
        Object proxy() throws Exception {
            Class<?> type;
            try { type = Class.forName("com.iksxh.create_nuclear_industry.turbine.TurbineBlockStateRevision"); }
            catch (ClassNotFoundException missing) { fail("尚无结构检查的世界写入版本见证"); return null; }
            return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p,m,args) -> switch (m.getName()) {
                case "turbineRevision" -> { onRead.run(); yield version; }
                case "turbineWriting" -> writing;
                case "toString" -> "版本见证";
                case "hashCode" -> System.identityHashCode(p);
                case "equals" -> p == args[0];
                default -> throw new AssertionError(m);
            });
        }
    }
    private Object capture(Object... sources) throws Exception {
        Class<?> type = Class.forName("com.iksxh.create_nuclear_industry.turbine.TurbineStructure$RevisionSnapshot");
        Method method = type.getDeclaredMethod("capture", List.class); method.setAccessible(true);
        return method.invoke(null, List.of(sources));
    }
    private boolean matches(Object snapshot) throws Exception {
        Method method = snapshot.getClass().getDeclaredMethod("matches"); method.setAccessible(true);
        return (boolean)method.invoke(snapshot);
    }
    @Test void stableVersionsPermitReuse() throws Exception {
        Source a = new Source(), b = new Source(); b.version = 7;
        Object snapshot = capture(a.proxy(), b.proxy());
        assertNotNull(snapshot); assertTrue(matches(snapshot)); assertTrue(matches(snapshot));
    }
    @Test void firstOrLastMutationRejectsOldSnapshot() throws Exception {
        Source a = new Source(), b = new Source(); Object ap = a.proxy(), bp = b.proxy();
        Object first = capture(ap,bp); a.version++; assertFalse(matches(first));
        Object second = capture(ap,bp); b.version++; assertFalse(matches(second));
    }
    @Test void inFlightWriteCannotPublishSnapshot() throws Exception {
        Source a = new Source(); a.writing = true;
        assertNull(capture(a.proxy()));
    }
    @Test void inFlightWriteImmediatelyRejectsOldSnapshot() throws Exception {
        Source a = new Source(); Object snapshot = capture(a.proxy());
        a.writing = true; assertFalse(matches(snapshot));
        a.writing = false; a.version++; assertFalse(matches(snapshot));
    }
    @Test void mutationOfEarlierSourceDuringCapturePreventsPublication() throws Exception {
        Source a = new Source(), b = new Source(); b.onRead = () -> a.version++;
        assertNull(capture(a.proxy(),b.proxy()));
    }
    @Test void recoveryRequiresNewStableSnapshot() throws Exception {
        Source a = new Source(); Object ap = a.proxy(), old = capture(ap);
        a.version++; a.writing = true; assertFalse(matches(old)); assertNull(capture(ap));
        a.version++; a.writing = false;
        Object restored = capture(ap); assertNotNull(restored); assertTrue(matches(restored)); assertFalse(matches(old));
    }
}
