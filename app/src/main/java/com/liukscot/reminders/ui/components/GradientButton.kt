package com.liukscot.reminders.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liukscot.reminders.ui.theme.EmberFlowA
import com.liukscot.reminders.ui.theme.EmberFlowB
import com.liukscot.reminders.ui.theme.EmberTextOnAccent
import com.liukscot.reminders.ui.theme.InkPressed

// Ref: Material-Flow-Design-System `tokens/motion.css` — read verbatim, not
// paraphrased, after two wrong guesses:
//   @keyframes flow-drift { 0%,100% { background-position: 0% 50% } 50% { background-position: 100% 50% } }
//   @keyframes flow-surge { 0% { background-position: 0% 50% } 100% { background-position: 100% 50% } }
//   .flow-surface { animation: flow-drift 9s var(--ease-standard) infinite; background-size: 220% 220%; }
//   .flow-surface:active { animation: flow-surge 900ms var(--ease-emphasized) forwards; }
// So: idle IS a there-and-back slide (not one-directional) across an
// oversized (220%) gradient, eased with cubic-bezier(0.2,0,0,1) — the
// easing (not the direction) is what makes it read as organic drift rather
// than a mechanical ping-pong. Press is a one-shot forward surge that
// holds at the end while pressed, dropping back to the idle loop on
// release (matching CSS re-applying the base animation, which restarts
// at 0%).
private val EaseStandard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val EaseEmphasized = CubicBezierEasing(0.3f, 0f, 0f, 1f)

// The design system's idle "flow-drift" as a reusable background for non-button shapes (the voice
// mic FAB and orb). Same oversized (220%) Ember gradient sliding there-and-back that GradientButton
// uses at rest — no press surge, since these aren't the primary CTA.
@Composable
fun Modifier.flowGradientBackground(shape: Shape): Modifier {
    var sizePx by remember { mutableStateOf(IntSize.Zero) }
    val position = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            position.animateTo(1f, tween(durationMillis = 4500, easing = EaseStandard))
            position.animateTo(0f, tween(durationMillis = 4500, easing = EaseStandard))
        }
    }
    val width = sizePx.width.toFloat()
    val height = sizePx.height.toFloat()
    val originX = -1.2f * width * position.value
    val originY = -0.6f * height
    val brush = if (width > 0f) {
        Brush.linearGradient(
            0f to EmberFlowA,
            0.45f to EmberFlowB,
            0.9f to EmberFlowA,
            start = Offset(originX, originY),
            end = Offset(originX + width * 2.2f, originY + height * 2.2f),
        )
    } else {
        Brush.linearGradient(listOf(EmberFlowA, EmberFlowB))
    }
    return this
        .onSizeChanged { sizePx = it }
        .clip(shape)
        .background(brush)
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    var sizePx by remember { mutableStateOf(IntSize.Zero) }

    val position = remember { Animatable(0f) }
    LaunchedEffect(pressed) {
        if (pressed) {
            position.animateTo(1f, tween(durationMillis = 900, easing = EaseEmphasized))
        } else {
            position.snapTo(0f)
            while (true) {
                position.animateTo(1f, tween(durationMillis = 4500, easing = EaseStandard))
                position.animateTo(0f, tween(durationMillis = 4500, easing = EaseStandard))
            }
        }
    }

    val width = sizePx.width.toFloat()
    val height = sizePx.height.toFloat()
    // background-size: 220% 220%, background-position: {position*100}% 50%.
    val originX = -1.2f * width * position.value
    val originY = -0.6f * height
    val gradientBrush = if (width > 0f) {
        Brush.linearGradient(
            0f to EmberFlowA,
            0.45f to EmberFlowB,
            0.9f to EmberFlowA,
            start = Offset(originX, originY),
            end = Offset(originX + width * 2.2f, originY + height * 2.2f),
        )
    } else {
        Brush.linearGradient(listOf(EmberFlowA, EmberFlowB))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .onSizeChanged { sizePx = it }
            .scale(if (pressed) 0.97f else 1f)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (enabled) gradientBrush else Brush.linearGradient(listOf(InkPressed, InkPressed)))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.16.sp,
            color = if (enabled) EmberTextOnAccent else MaterialTheme.colorScheme.outline,
        )
    }
}
