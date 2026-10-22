package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: SwitchColors = SwitchDefaults.colors(),
) {
    val density = LocalDensity.current
    val geometry = remember(density) {
        with(density) {
            ClassicSwitchGeometry(
                width = SwitchWidth.toPx(),
                height = SwitchHeight.toPx(),
                trackWidth = TrackWidth.toPx(),
                trackHeight = TrackHeight.toPx(),
                strokeWidth = TrackStrokeWidth.toPx(),
                thumbDiameter = ThumbDiameter.toPx(),
            )
        }
    }
    val swipeableState = rememberSwipeableStateFor(checked, onCheckedChange ?: {}, AnimationSpec)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val toggleableModifier =
        if (onCheckedChange != null) {
            Modifier.toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = false, radius = 24.dp)
            )
        } else {
            Modifier
        }

    Box(
        modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .then(toggleableModifier)
            .swipeable(
                state = swipeableState,
                // Relative thumb placement and reversed dragging mirror together in RTL.
                anchors = mapOf(geometry.thumbStart to false, geometry.thumbEnd to true),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal,
                enabled = enabled && onCheckedChange != null,
                reverseDirection = isRtl,
                interactionSource = interactionSource,
                resistance = null
            )
            .wrapContentSize(Alignment.Center)
            .requiredSize(SwitchWidth, SwitchHeight)
    ) {
        SwitchImpl(
            checked = checked,
            enabled = enabled,
            colors = colors,
            geometry = geometry,
            thumbValue = swipeableState.offset,
            interactionSource = interactionSource
        )
    }
}

@Composable
private fun BoxScope.SwitchImpl(
    checked: Boolean,
    enabled: Boolean,
    colors: SwitchColors,
    geometry: ClassicSwitchGeometry,
    thumbValue: State<Float>,
    interactionSource: InteractionSource
) {
    val pressed by interactionSource.collectIsPressedAsState()
    val dragged by interactionSource.collectIsDraggedAsState()
    val elevation by animateDpAsState(
        targetValue = if (enabled && (pressed || dragged)) 3.dp else 2.dp,
        animationSpec = TweenSpec(durationMillis = AnimationDurationMillis),
        label = "classicSwitchElevation",
    )
    val targetTrackColor by colors.trackColor(enabled, checked)
    val trackColor by animateColorAsState(
        targetValue = targetTrackColor,
        animationSpec = TweenSpec(durationMillis = AnimationDurationMillis),
        label = "classicSwitchTrackColor"
    )
    Canvas(Modifier.align(Alignment.Center).fillMaxSize()) {
        drawTrack(
            trackColor = trackColor,
            geometry = geometry,
        )
    }
    val targetThumbColor by colors.thumbColor(enabled, checked)
    val thumbColor by animateColorAsState(
        targetValue = targetThumbColor,
        animationSpec = TweenSpec(durationMillis = AnimationDurationMillis),
        label = "classicSwitchThumbColor"
    )
    val elevationOverlay = LocalElevationOverlay.current
    val absoluteElevation = LocalAbsoluteElevation.current + elevation
    val resolvedThumbColor =
        if (thumbColor == MaterialTheme.colors.surface && elevationOverlay != null) {
            elevationOverlay.apply(thumbColor, absoluteElevation)
        } else {
            thumbColor
        }
    Spacer(
        Modifier
            .align(Alignment.CenterStart)
            .offset { IntOffset(thumbValue.value.roundToInt(), 0) }
            .requiredSize(ThumbDiameter)
            .shadow(elevation, CircleShape, clip = false)
            .background(resolvedThumbColor, CircleShape)
    )
}

private fun DrawScope.drawTrack(
    trackColor: Color,
    geometry: ClassicSwitchGeometry,
) {
    drawRoundRect(
        color = trackColor,
        topLeft = Offset(geometry.trackLeft, geometry.trackTop),
        size = Size(width = geometry.trackPathWidth, height = geometry.trackPathHeight),
        cornerRadius = CornerRadius(geometry.trackRadius),
        style = Stroke(width = geometry.strokeWidth),
    )
}

internal val TrackWidth = 36.dp
internal val TrackHeight = 16.dp
internal val TrackStrokeWidth = 2.dp
internal val ThumbDiameter = 24.dp

private val SwitchWidth = 48.dp
private val SwitchHeight = 28.dp

private const val AnimationDurationMillis = 180
private val AnimationSpec = TweenSpec<Float>(durationMillis = AnimationDurationMillis)

@Stable
interface SwitchColors {
    @Composable
    fun thumbColor(enabled: Boolean, checked: Boolean): State<Color>

    @Composable
    fun trackColor(enabled: Boolean, checked: Boolean): State<Color>
}

/**
 * Contains the default values used by [Switch]
 */
object SwitchDefaults {
    /**
     * Creates a [SwitchColors] that represents the different colors used in a [Switch] in
     * different states.
     *
     * @param checkedThumbColor the color used for the thumb when enabled and checked
     * @param checkedTrackColor the color used for the track when enabled and checked
     * @param checkedTrackAlpha the alpha applied to [checkedTrackColor] and
     * [disabledCheckedTrackColor]
     * @param uncheckedThumbColor the color used for the thumb when enabled and unchecked
     * @param uncheckedTrackColor the color used for the track when enabled and unchecked
     * @param uncheckedTrackAlpha the alpha applied to [uncheckedTrackColor] and
     * [disabledUncheckedTrackColor]
     * @param disabledCheckedThumbColor the color used for the thumb when disabled and checked
     * @param disabledCheckedTrackColor the color used for the track when disabled and checked
     * @param disabledUncheckedThumbColor the color used for the thumb when disabled and unchecked
     * @param disabledUncheckedTrackColor the color used for the track when disabled and unchecked
     */
    @Composable
    fun colors(
        checkedThumbColor: Color = ExtendedTheme.colors.switchOn,
        checkedTrackColor: Color = ExtendedTheme.colors.switchOn,
        checkedTrackAlpha: Float = 0.45f,
        uncheckedThumbColor: Color = Color(0xFFE6E6E6),
        uncheckedTrackColor: Color = ExtendedTheme.colors.switchOff,
        uncheckedTrackAlpha: Float = 1f,
        disabledCheckedThumbColor: Color = checkedThumbColor
            .copy(alpha = ContentAlpha.disabled)
            .compositeOver(MaterialTheme.colors.surface),
        disabledCheckedTrackColor: Color = checkedTrackColor
            .copy(alpha = ContentAlpha.disabled)
            .compositeOver(MaterialTheme.colors.surface),
        disabledUncheckedThumbColor: Color = uncheckedThumbColor
            .copy(alpha = ContentAlpha.disabled)
            .compositeOver(MaterialTheme.colors.surface),
        disabledUncheckedTrackColor: Color = uncheckedTrackColor
            .copy(alpha = ContentAlpha.disabled)
            .compositeOver(MaterialTheme.colors.surface)
    ): SwitchColors = DefaultSwitchColors(
        checkedThumbColor = checkedThumbColor,
        checkedTrackColor = checkedTrackColor.copy(alpha = checkedTrackAlpha),
        uncheckedThumbColor = uncheckedThumbColor,
        uncheckedTrackColor = uncheckedTrackColor.copy(alpha = uncheckedTrackAlpha),
        disabledCheckedThumbColor = disabledCheckedThumbColor,
        disabledCheckedTrackColor = disabledCheckedTrackColor.copy(alpha = checkedTrackAlpha),
        disabledUncheckedThumbColor = disabledUncheckedThumbColor,
        disabledUncheckedTrackColor = disabledUncheckedTrackColor.copy(alpha = uncheckedTrackAlpha)
    )
}

/**
 * Default [SwitchColors] implementation.
 */
@Immutable
private class DefaultSwitchColors(
    private val checkedThumbColor: Color,
    private val checkedTrackColor: Color,
    private val uncheckedThumbColor: Color,
    private val uncheckedTrackColor: Color,
    private val disabledCheckedThumbColor: Color,
    private val disabledCheckedTrackColor: Color,
    private val disabledUncheckedThumbColor: Color,
    private val disabledUncheckedTrackColor: Color
) : SwitchColors {
    @Composable
    override fun thumbColor(enabled: Boolean, checked: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (checked) checkedThumbColor else uncheckedThumbColor
            } else {
                if (checked) disabledCheckedThumbColor else disabledUncheckedThumbColor
            }
        )
    }

    @Composable
    override fun trackColor(enabled: Boolean, checked: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (checked) checkedTrackColor else uncheckedTrackColor
            } else {
                if (checked) disabledCheckedTrackColor else disabledUncheckedTrackColor
            }
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultSwitchColors

        if (checkedThumbColor != other.checkedThumbColor) return false
        if (checkedTrackColor != other.checkedTrackColor) return false
        if (uncheckedThumbColor != other.uncheckedThumbColor) return false
        if (uncheckedTrackColor != other.uncheckedTrackColor) return false
        if (disabledCheckedThumbColor != other.disabledCheckedThumbColor) return false
        if (disabledCheckedTrackColor != other.disabledCheckedTrackColor) return false
        if (disabledUncheckedThumbColor != other.disabledUncheckedThumbColor) return false
        if (disabledUncheckedTrackColor != other.disabledUncheckedTrackColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedThumbColor.hashCode()
        result = 31 * result + checkedTrackColor.hashCode()
        result = 31 * result + uncheckedThumbColor.hashCode()
        result = 31 * result + uncheckedTrackColor.hashCode()
        result = 31 * result + disabledCheckedThumbColor.hashCode()
        result = 31 * result + disabledCheckedTrackColor.hashCode()
        result = 31 * result + disabledUncheckedThumbColor.hashCode()
        result = 31 * result + disabledUncheckedTrackColor.hashCode()
        return result
    }
}
