package com.huanchengfly.tieba.post.ui.common.prefs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Simple divider used to separate individual preferences
 */
@Composable
fun Divider(
    modifier: Modifier = Modifier,
    color: Color = ExtendedTheme.colors.divider,
    thickness: Dp = 1.dp,
    indent: Dp = 0.dp
) {
    Box(
        modifier
            .then(Modifier.padding(start = indent, end = indent))
            .fillMaxWidth()
            .height(thickness)
            .background(color = color)
    )
}

