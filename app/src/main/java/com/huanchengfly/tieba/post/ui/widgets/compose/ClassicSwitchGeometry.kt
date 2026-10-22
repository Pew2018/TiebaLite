package com.huanchengfly.tieba.post.ui.widgets.compose

/** Pixel geometry shared by track drawing and thumb anchors; width includes the stroke. */
internal data class ClassicSwitchGeometry(
    val width: Float,
    val height: Float,
    val trackWidth: Float,
    val trackHeight: Float,
    val strokeWidth: Float,
    val thumbDiameter: Float,
) {
    val trackLeft = (width - trackWidth + strokeWidth) / 2f
    val trackTop = (height - trackHeight + strokeWidth) / 2f
    val trackPathWidth = trackWidth - strokeWidth
    val trackPathHeight = trackHeight - strokeWidth
    val trackRadius = trackPathHeight / 2f
    val thumbStart = trackLeft + trackRadius - thumbDiameter / 2f
    val thumbEnd = trackLeft + trackPathWidth - trackRadius - thumbDiameter / 2f
}
