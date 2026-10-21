package com.huanchengfly.tieba.post.ui.widgets.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassicRefreshCycleTest {
    @Test fun duplicateGesturesCannotStartSecondRequest() {
        val cycle = ClassicRefreshCycle()
        assertTrue(cycle.begin(false))
        assertFalse(cycle.begin(false))
        assertTrue(cycle.pending)
        assertEquals(ClassicRefreshPhase.Armed, cycle.phase)
    }

    @Test fun successfulOrFailedRequestReturnsToIdle() {
        val cycle = ClassicRefreshCycle()
        cycle.begin(false)
        cycle.observe(true)
        assertEquals(ClassicRefreshPhase.Refreshing, cycle.phase)
        cycle.observe(false)
        assertFalse(cycle.pending)
        assertEquals(ClassicRefreshPhase.Settling, cycle.phase)
        cycle.settle()
        assertEquals(ClassicRefreshPhase.Idle, cycle.phase)
        assertTrue(cycle.begin(false))
    }

    @Test fun initialLoadingAndMissingStartAreSafe() {
        val cycle = ClassicRefreshCycle()
        assertFalse(cycle.begin(true))
        cycle.observe(false)
        assertFalse(cycle.pending)
        assertTrue(cycle.begin(false))
        cycle.expireIfNeverStarted()
        assertFalse(cycle.pending)
        assertEquals(ClassicRefreshPhase.Settling, cycle.phase)
    }
}
