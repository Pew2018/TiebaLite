package com.huanchengfly.tieba.post.ui.page.reply

/** One IME-hide operation can serve back, outside-tap and programmatic close requests. */
internal class ReplyDismissCoordinator {
    enum class Phase { Open, HidingIme, Closing }
    var phase = Phase.Open
        private set
    private var closeAfterHide = false

    /** Returns true only to the caller that must start hiding the IME. */
    fun request(closeDialog: Boolean): Boolean {
        if (phase == Phase.Closing) return false
        closeAfterHide = closeAfterHide || closeDialog
        if (phase == Phase.HidingIme) return false
        phase = Phase.HidingIme
        return true
    }

    /** Returns true once when the full close should start, otherwise back only hid the IME. */
    fun imeHidden(): Boolean {
        if (phase != Phase.HidingIme) return false
        phase = if (closeAfterHide) Phase.Closing else Phase.Open
        return closeAfterHide
    }
}
