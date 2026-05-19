package com.cleanshot.app.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sign

/** Slight drag — card moves with the finger, minimal chrome. */
internal const val SWIPE_HINT_THRESHOLD_FRACTION = 0.08f

/** Medium drag — visible keep/delete preview. */
internal const val SWIPE_PREVIEW_THRESHOLD_FRACTION = 0.20f

/** Release beyond this distance commits keep/delete. */
internal const val SWIPE_COMMIT_THRESHOLD_FRACTION = 0.32f

/** Fling speed to commit when paired with minimum displacement. */
internal const val SWIPE_VELOCITY_THRESHOLD_PX_PER_SEC = 1_150f

internal const val SWIPE_VELOCITY_MIN_OFFSET_FRACTION = 0.12f

/**
 * Horizontal movement must clearly exceed vertical before locking (scroll-friendly).
 * Also requires horizontal displacement past touch slop so tiny diagonals do not lock.
 */
internal const val SWIPE_HORIZONTAL_LOCK_RATIO = 1.32f

internal const val SWIPE_HORIZONTAL_MIN_SLOP_MULTIPLIER = 1.15f

/** Follow-through beyond the soft zone (higher = lighter, less sticky). */
internal const val SWIPE_DRAG_RESISTANCE = 0.74f

/** Near 1:1 tracking inside the soft drag zone. */
internal const val SWIPE_SOFT_ZONE_FRACTION = 0.38f

internal const val SWIPE_SOFT_TRACKING = 0.97f

internal const val SWIPE_MAX_OVERSCROLL_FRACTION = 1.18f

internal const val SWIPE_OFFSCREEN_MULTIPLIER = 1.4f

internal val SwipeResetSpring = spring<Float>(
    dampingRatio = 0.82f,
    stiffness = 300f
)

internal val SwipeExitSpring = spring<Float>(
    dampingRatio = 0.84f,
    stiffness = 195f
)

internal val SwipeBackCardSpring = spring<Float>(
    dampingRatio = 0.82f,
    stiffness = Spring.StiffnessMediumLow
)

/** Progressive swipe phase for overlay and feedback. */
internal enum class SwipeProgressPhase {
    Idle,
    Slight,
    Preview,
    CommitReady
}

internal fun swipeProgressPhase(offsetPx: Float, cardWidthPx: Float): SwipeProgressPhase {
    if (cardWidthPx <= 0f) return SwipeProgressPhase.Idle
    val progress = abs(offsetPx) / cardWidthPx
    return when {
        progress < SWIPE_HINT_THRESHOLD_FRACTION -> SwipeProgressPhase.Idle
        progress < SWIPE_PREVIEW_THRESHOLD_FRACTION -> SwipeProgressPhase.Slight
        progress < SWIPE_COMMIT_THRESHOLD_FRACTION -> SwipeProgressPhase.Preview
        else -> SwipeProgressPhase.CommitReady
    }
}

/**
 * Soft zone with near 1:1 tracking, then eased resistance — fluid but bounded.
 */
internal fun resistSwipeOffset(rawOffsetPx: Float, cardWidthPx: Float): Float {
    if (cardWidthPx <= 0f) return 0f
    val sign = rawOffsetPx.sign
    val absOffset = abs(rawOffsetPx)
    val maxOverscroll = cardWidthPx * SWIPE_MAX_OVERSCROLL_FRACTION
    val softZone = cardWidthPx * SWIPE_SOFT_ZONE_FRACTION

    val resisted = if (absOffset <= softZone) {
        absOffset * SWIPE_SOFT_TRACKING
    } else {
        val softComponent = softZone * SWIPE_SOFT_TRACKING
        val beyond = absOffset - softZone
        val resistSpan = cardWidthPx * (SWIPE_MAX_OVERSCROLL_FRACTION - SWIPE_SOFT_ZONE_FRACTION)
        val t = (beyond / resistSpan).coerceIn(0f, 1f)
        val easedBeyond = beyond * (SWIPE_DRAG_RESISTANCE + (1f - SWIPE_DRAG_RESISTANCE) * (1f - t * t))
        softComponent + easedBeyond
    }
    return sign * resisted.coerceAtMost(maxOverscroll)
}

internal fun swipeCommitThresholdPx(cardWidthPx: Float): Float =
    cardWidthPx * SWIPE_COMMIT_THRESHOLD_FRACTION

internal fun resolveSwipeDirection(
    offsetPx: Float,
    velocityXPxPerSec: Float,
    cardWidthPx: Float
): SwipeDirection? {
    val commitThreshold = swipeCommitThresholdPx(cardWidthPx)
    val velocityMinOffset = cardWidthPx * SWIPE_VELOCITY_MIN_OFFSET_FRACTION

    return when {
        offsetPx >= commitThreshold -> SwipeDirection.Right
        offsetPx <= -commitThreshold -> SwipeDirection.Left
        velocityXPxPerSec >= SWIPE_VELOCITY_THRESHOLD_PX_PER_SEC &&
            offsetPx >= velocityMinOffset -> SwipeDirection.Right
        velocityXPxPerSec <= -SWIPE_VELOCITY_THRESHOLD_PX_PER_SEC &&
            offsetPx <= -velocityMinOffset -> SwipeDirection.Left
        else -> null
    }
}

/**
 * Horizontal swipe only after touch slop and clear horizontal intent — vertical movement
 * is allowed until horizontal dominance is obvious.
 */
internal suspend fun PointerInputScope.detectControlledHorizontalSwipe(
    onDragStart: () -> Unit,
    onHorizontalDrag: (deltaX: Float, change: PointerInputChange) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    val touchSlop = viewConfiguration.touchSlop
    val horizontalMinPx = touchSlop * SWIPE_HORIZONTAL_MIN_SLOP_MULTIPLIER

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val pointerId = down.id
        var accumulatedX = 0f
        var accumulatedY = 0f
        var horizontalLocked = false
        var dragStarted = false

        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == pointerId }
                ?: event.changes.firstOrNull()
                ?: break

            if (!change.pressed) {
                if (horizontalLocked) onDragEnd() else onDragCancel()
                break
            }

            val delta = change.positionChange()
            if (delta == Offset.Zero) continue

            if (!horizontalLocked) {
                accumulatedX += delta.x
                accumulatedY += delta.y
                val distance = hypot(accumulatedX.toDouble(), accumulatedY.toDouble()).toFloat()
                if (distance < touchSlop) continue

                val horizontalDominant =
                    abs(accumulatedX) >= horizontalMinPx &&
                        abs(accumulatedX) >= abs(accumulatedY) * SWIPE_HORIZONTAL_LOCK_RATIO
                horizontalLocked = horizontalDominant
                if (!horizontalLocked) {
                    onDragCancel()
                    break
                }
                if (!dragStarted) {
                    dragStarted = true
                    onDragStart()
                }
                change.consume()
                onHorizontalDrag(accumulatedX, change)
                continue
            }

            if (!dragStarted) {
                dragStarted = true
                onDragStart()
            }
            change.consume()
            onHorizontalDrag(delta.x, change)
        }
    }
}
