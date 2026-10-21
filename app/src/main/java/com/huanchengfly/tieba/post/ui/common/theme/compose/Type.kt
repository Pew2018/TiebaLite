package com.huanchengfly.tieba.post.ui.common.theme.compose

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal val DefaultTextStyle = TextStyle.Default.copy()

// Classic Material / OxygenOS 7.1.1 typography.
val Typography = Typography(
    h6 = DefaultTextStyle.copy(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 20.sp, letterSpacing = 0.sp),
    subtitle1 = DefaultTextStyle.copy(fontWeight = FontWeight.Normal, fontSize = 16.sp, letterSpacing = 0.sp),
    subtitle2 = DefaultTextStyle.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.sp),
    body1 = DefaultTextStyle.copy(fontWeight = FontWeight.Normal, fontSize = 16.sp, letterSpacing = 0.sp),
    body2 = DefaultTextStyle.copy(fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.sp),
    caption = DefaultTextStyle.copy(fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.sp),
    button = DefaultTextStyle.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.sp)
)
