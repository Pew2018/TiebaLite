package com.huanchengfly.tieba.post.ui.page.reply

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplyDismissCoordinatorTest {
    @Test fun backHidesKeyboardWithoutClosingThenAnotherBackCanClose() {
        val coordinator = ReplyDismissCoordinator()
        assertTrue(coordinator.request(closeDialog = false))
        assertFalse(coordinator.imeHidden())
        assertEquals(ReplyDismissCoordinator.Phase.Open, coordinator.phase)
        assertTrue(coordinator.request(closeDialog = true))
        assertTrue(coordinator.imeHidden())
        assertEquals(ReplyDismissCoordinator.Phase.Closing, coordinator.phase)
    }

    @Test fun outsideTapDuringKeyboardBackUpgradesThePendingOperationToFullClose() {
        val coordinator = ReplyDismissCoordinator()
        assertTrue(coordinator.request(closeDialog = false))
        assertFalse(coordinator.request(closeDialog = true))
        assertEquals(ReplyDismissCoordinator.Phase.HidingIme, coordinator.phase)
        assertTrue(coordinator.imeHidden())
        assertFalse(coordinator.imeHidden())
    }

    @Test fun repeatedCloseAndSuccessCallbacksOnlyStartOneExit() {
        val coordinator = ReplyDismissCoordinator()
        assertTrue(coordinator.request(closeDialog = true))
        repeat(5) { assertFalse(coordinator.request(closeDialog = true)) }
        assertTrue(coordinator.imeHidden())
        assertFalse(coordinator.request(closeDialog = false))
        assertFalse(coordinator.request(closeDialog = true))
        assertFalse(coordinator.imeHidden())
    }
}
