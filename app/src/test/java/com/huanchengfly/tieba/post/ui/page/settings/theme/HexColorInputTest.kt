package com.huanchengfly.tieba.post.ui.page.settings.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HexColorInputTest {
    @Test fun validRgbIsNormalized() {
        assertEquals("#2196F3", normalizedHexRgb("#2196f3"))
        assertEquals("#FFFFFF", normalizedHexRgb(" #ffffff "))
        assertEquals("#000000", normalizedHexRgb("#000000"))
    }

    @Test fun invalidOrEditedDraftCannotBeCommitted() {
        listOf("#12", "#GGGGGG", "123456", "", "#1234567", "#12345678")
            .forEach { assertNull(normalizedHexRgb(it)) }
        assertEquals("#AABBCC", normalizedHexRgb("#aabbcc"))
        assertNull(normalizedHexRgb("#aabbc"))
    }
}
