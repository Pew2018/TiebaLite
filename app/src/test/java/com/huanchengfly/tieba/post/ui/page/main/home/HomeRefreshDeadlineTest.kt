package com.huanchengfly.tieba.post.ui.page.main.home

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRefreshDeadlineTest {
    @Test
    fun firstSuccessfulValueTerminatesWithoutWaitingForMore() = runBlocking {
        assertEquals(1, flowOf(1, 2).firstOrFailWithin(1_000).single())
    }

    @Test
    fun upstreamCompletingWithoutAnEmissionBecomesAnError() = runBlocking {
        val state = emptyFlow<Int>()
            .firstOrFailWithin(1_000)
            .catch { assertTrue(it is IllegalStateException); emit(-1) }
            .single()
        assertEquals(-1, state)
    }

    @Test
    fun permanentlySuspendedUpstreamBecomesRecoverableFailure() = runBlocking {
        val state = flow<Int> { awaitCancellation() }
            .firstOrFailWithin(100)
            .catch { emit(-1) }
            .single()
        assertEquals(-1, state)
    }
}
