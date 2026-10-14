package com.iksxh.create_nuclear_industry.production;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** 以真实256工时同步量检验客户端插帧；输入都是同步快照，不访问Minecraft单例。 */
final class ShieldedAssemblyAnimationClockTest {
    private static ShieldedAssemblyAnimationClock.Sample sample(String operation, int progress, float rpm,
                                                                 boolean working, boolean overload,
                                                                 boolean ready, boolean output) {
        return new ShieldedAssemblyAnimationClock.Sample(operation, progress, 25600, rpm,
                true, true, working, overload, ready, output);
    }

    private static ShieldedAssemblyAnimationClock.Sample run(int progress, float rpm) {
        return sample("manufacture", progress, rpm, true, false, true, false);
    }

    @Test void lowSpeedMovesBetweenEightTickSamplesAndStopsAtOnePacketQuantum() {
        var clock = new ShieldedAssemblyAnimationClock();
        assertEquals(512, clock.update(run(512, 32), 10, false).progress());
        assertEquals(640, clock.update(run(512, 32), 14, false).progress());
        assertEquals(752, clock.update(run(512, 32), 17.5, false).progress());
        assertEquals(768, clock.update(run(512, 32), 18, false).progress());
        assertEquals(768, clock.update(run(512, 32), 26, false).progress());
        assertEquals(800, clock.update(run(768, 32), 27, false).progress());
    }

    @Test void arrivalDoesNotCountTheAlreadySynchronizedIntervalTwice() {
        var fast = new ShieldedAssemblyAnimationClock();
        fast.update(run(0, 256), 0, false);
        assertEquals(256, fast.update(run(256, 256), 1, false).progress());
        var slow = new ShieldedAssemblyAnimationClock();
        slow.update(run(0, 32), 0, false);
        assertEquals(256, slow.update(run(256, 32), 8, false).progress());
    }

    @Test void stopAndOverloadKeepPoseAndResumeWithoutAccumulatingStoppedTime() {
        var clock = new ShieldedAssemblyAnimationClock();
        clock.update(run(1024, 32), 0, false);
        assertEquals(1120, clock.update(run(1024, 32), 3, false).progress());
        var stopped = sample("manufacture", 1024, 0, false, false, true, false);
        assertEquals(1120, clock.update(stopped, 4, false).progress());
        assertEquals(1120, clock.update(stopped, 12, false).progress());
        assertEquals(1120, clock.update(run(1024, 32), 13, false).progress());
        assertEquals(1152, clock.update(run(1024, 32), 14, false).progress());
        var overloaded = sample("manufacture", 1024, 32, true, true, true, false);
        assertEquals(1152, clock.update(overloaded, 15, false).progress());
        assertEquals(1152, clock.update(overloaded, 16, false).progress());
    }

    @Test void truePauseFreezesEvenIfFractionalTimeMovesAndDoesNotCatchUp() {
        var clock = new ShieldedAssemblyAnimationClock();
        clock.update(run(1024, 64), 0, false);
        assertEquals(1056, clock.update(run(1024, 64), .5, false).progress());
        assertEquals(1056, clock.update(run(1024, 64), .9, true).progress());
        assertEquals(1056, clock.update(run(1024, 64), 10, true).progress());
        assertEquals(1056, clock.update(run(1024, 64), 11, false).progress());
        assertEquals(1088, clock.update(run(1024, 64), 11.5, false).progress());
    }

    @Test void reverseAndExcessiveRpmFollowAbsoluteCappedServerWork() {
        var reverse = new ShieldedAssemblyAnimationClock();
        reverse.update(run(0, -32), 0, false);
        assertEquals(128, reverse.update(run(0, -32), 4, false).progress());
        var excessive = new ShieldedAssemblyAnimationClock();
        excessive.update(run(0, 1024), 0, false);
        assertEquals(256, excessive.update(run(0, 1024), 1, false).progress());
        assertEquals(256, excessive.update(run(0, 1024), 2, false).progress());
        var fractional = new ShieldedAssemblyAnimationClock();
        fractional.update(run(0, 32.9f), 0, false);
        assertEquals(128, fractional.update(run(0, 32.9f), 4, false).progress());
    }

    @Test void outputWinsOverStaleWorkingAndPredictionCannotFinishTheBatch() {
        var clock = new ShieldedAssemblyAnimationClock();
        clock.update(run(25520, 256), 0, false);
        var frame = clock.update(run(25520, 256), 1, false);
        assertEquals(25599, frame.progress());
        assertTrue(frame.phase() < 1);
        assertEquals(0, clock.update(sample("manufacture", 0, 256, true, false, true, true),
                2, false).progress());
        assertEquals(0, clock.update(sample("manufacture", 0, 256, true, false, true, true),
                3, false).phase());
    }

    @Test void operationWorkAndAuthoritativeResetStartNewLocalTimeline() {
        var clock = new ShieldedAssemblyAnimationClock();
        clock.update(run(12000, 32), 0, false);
        clock.update(run(12000, 32), 2, false);
        assertEquals(256, clock.update(sample("sealing", 256, 32, true, false, true, false),
                3, false).progress());
        assertEquals(0, clock.update(sample("sealing", 0, 32, true, false, true, false),
                4, false).progress());
        assertEquals(32, clock.update(sample("sealing", 0, 32, true, false, true, false),
                5, false).progress());
        var changedWork = new ShieldedAssemblyAnimationClock.Sample("sealing", 256, 12800, 64,
                true, true, true, false, true, false);
        assertEquals(.02, clock.update(changedWork, 6, false).phase(), 1e-12);
    }

    @Test void missingInputsRemovedControllerAndUnformedMachineCannotRun() {
        for (int condition = 0; condition < 3; condition++) {
            var clock = new ShieldedAssemblyAnimationClock();
            clock.update(run(2000, 32), 0, false);
            var invalid = new ShieldedAssemblyAnimationClock.Sample("manufacture", 2000, 25600, 32,
                    condition != 0, condition != 1, true, false, condition != 2, false);
            assertEquals(0, clock.update(invalid, 1, false).phase());
        }
    }

    @Test void longGapAndBackwardTimeRebaseToReceivedProgressRatherThanChasingTime() {
        var clock = new ShieldedAssemblyAnimationClock();
        clock.update(run(512, 32), 0, false);
        clock.update(run(512, 32), 3, false);
        assertEquals(512, clock.update(run(512, 32), 100, false).progress());
        assertEquals(544, clock.update(run(512, 32), 101, false).progress());
        assertEquals(512, clock.update(run(512, 32), 1, false).progress());
        assertEquals(512, clock.update(run(512, 32), Double.NaN, false).progress());
    }

    @Test void nonFiniteSpeedFreezesAndSeparateEntitiesHaveSeparatePhase() {
        var first = new ShieldedAssemblyAnimationClock();
        var second = new ShieldedAssemblyAnimationClock();
        first.update(run(512, 32), 0, false);
        second.update(run(4096, 32), 0, false);
        assertEquals(640, first.update(run(512, 32), 4, false).progress());
        assertEquals(4224, second.update(run(4096, 32), 4, false).progress());
        assertEquals(640, first.update(run(512, Float.NaN), 5, false).progress());
        assertEquals(4224, second.update(run(4096, Float.POSITIVE_INFINITY), 5, false).progress());
        var freshWorldController = new ShieldedAssemblyAnimationClock();
        assertEquals(512, freshWorldController.update(run(512, 32), 5, false).progress());
    }
}
