package com.huanchengfly.tieba.post.ui.common.prefs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme

@Composable
fun GroupHeader(
    title: String,
    color: Color = ExtendedTheme.colors.accentText
) {
    Box(
        Modifier
            .padding(
                start = StartPadding,
            )
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            title,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}


private val StartPadding = 16.dp