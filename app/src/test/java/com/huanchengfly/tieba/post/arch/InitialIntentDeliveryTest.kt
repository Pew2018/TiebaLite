package com.huanchengfly.tieba.post.arch

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class InitialIntentDeliveryTest {
    @Test
    fun senderWaitsForFirstSubscriberInsteadOfDroppingInitialEvent() = runBlocking {
        val intentFlow = MutableSharedFlow<Int>()
        val sender = launch(start = CoroutineStart.UNDISPATCHED) {
            intentFlow.subscriptionCount.first { it > 0 }
            intentFlow.emit(42)
        }
        // The sender is already waiting when the first subscriber appears.
        val receiver = async(start = CoroutineStart.UNDISPATCHED) { intentFlow.first() }
        assertEquals(42, withTimeout(1_000) { receiver.await() })
        sender.join()
    }
}
