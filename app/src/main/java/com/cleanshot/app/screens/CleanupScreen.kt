package com.cleanshot.app.screens

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.text.format.Formatter
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cleanshot.app.utils.estimateUrisTotalBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.roundToInt

// Soft Pixel-style action colors — theme-agnostic accents, not full-screen backgrounds.
private val KeepGreen = Color(0xFF6DBF7A)
private val KeepGreenContainer = Color(0xFFE3F5E8)
private val DeleteRed = Color(0xFFE07070)
private val DeleteRedContainer = Color(0xFFFFE8E8)
private val UndoNeutralContainer = Color(0xFFE8EAED)
private val UndoNeutralContent = Color(0xFF5F6368)

/** Shared horizontal inset for card stack and action buttons (matches mock). */
private val ScreenHorizontalPadding = 24.dp
private val ActionButtonGap = 12.dp

private const val CARD_CORNER_RADIUS_DP = 28
/** Phone-like screenshot width as a fraction of the stack container. */
private const val CARD_WIDTH_FRACTION = 0.80f
private const val CARD_MAX_HEIGHT_FRACTION = 0.90f
private const val CARD_ASPECT_WIDTH = 9f
private const val CARD_ASPECT_HEIGHT = 19.5f
private const val MAX_ROTATION_DEGREES = 5f
private const val BACK_CARD_BASE_SCALE = 0.90f
private const val BACK_CARD_SCALE_DRAG_BOOST = 0.15f
private const val BACK_CARD_BASE_ALPHA = 0.70f
private const val BACK_CARD_ALPHA_DRAG_BOOST = 0.18f
private val BACK_CARD_Y_OFFSET = 14.dp
private const val STACK_VISIBLE_COUNT = 2
private const val STACK_DEPTH_2_SCALE_DELTA = 0.04f
private const val STACK_DEPTH_2_ALPHA_DELTA = 0.08f
/**
 * Fraction of card width at which the back card begins promoting toward the front
 * during an active drag. Must be < 1f.
 */
private const val DRAG_PROMOTION_THRESHOLD = 0.35f

private val StackPromotionSpring = spring<Float>(
    dampingRatio = 0.82f,
    stiffness = 260f
)

private val CardDropShadowElevation = 42.dp
private val ButtonDropShadowElevation = 20.dp

private const val DELETE_ERROR_MESSAGE = "Couldn't delete screenshots"

/** Swipe direction for programmatic (button) and gesture-driven exits. */
internal enum class SwipeDirection {
    Left,
    Right
}

private enum class CleanupFlowPhase {
    Initializing,
    Reviewing,
    QueueReview,
    Completed
}

/** Single source of truth for which cleanup UI is shown — never render swipe deck otherwise. */
private sealed interface CleanupStage {
    data object Initializing : CleanupStage
    data object Empty : CleanupStage
    data class SwipeReview(val deckUris: List<Uri>) : CleanupStage
    data object ReviewSettling : CleanupStage
    data object QueueReview : CleanupStage
    data object Completed : CleanupStage
}

private const val POST_REVIEW_SETTLE_DELAY_MS = 130L

private fun resolveCleanupStage(
    flowPhase: CleanupFlowPhase,
    isPostReviewTransitioning: Boolean,
    originalTotal: Int,
    unreviewedCount: Int,
    deckUris: List<Uri>
): CleanupStage = when (flowPhase) {
    CleanupFlowPhase.Initializing -> CleanupStage.Initializing
    CleanupFlowPhase.Completed -> CleanupStage.Completed
    CleanupFlowPhase.QueueReview -> CleanupStage.QueueReview
    CleanupFlowPhase.Reviewing -> when {
        originalTotal == 0 -> CleanupStage.Empty
        isPostReviewTransitioning || unreviewedCount == 0 -> CleanupStage.ReviewSettling
        deckUris.isNotEmpty() -> CleanupStage.SwipeReview(deckUris = deckUris)
        else -> CleanupStage.ReviewSettling
    }
}

/**
 * Cleanup flow: Tinder-style swipe cards to keep or delete screenshots.
 *
 * @param screenshots Ordered list of screenshot URIs to review.
 * @param onKeep Called when the user swipes right or taps Keep.
 * @param onDelete Called when the user swipes left or taps Delete.
 * @param onBack Navigates back to the previous screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanupScreen(
    screenshots: List<Uri>,
    onKeep: (Uri) -> Unit,
    onDelete: (Uri) -> Unit,
    onBack: () -> Unit,
    onBackToLibrary: () -> Unit = onBack
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val reviewItems = remember(screenshots) { mutableStateListOf<CleanupReviewItem>() }
    val undoStack = remember(screenshots) { mutableStateListOf<CleanupUndoEntry>() }
    var isAnimating by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var flowPhase by remember(screenshots) { mutableStateOf(CleanupFlowPhase.Initializing) }
    var isPostReviewTransitioning by remember(screenshots) { mutableStateOf(false) }
    var postReviewTransitionNonce by remember(screenshots) { mutableIntStateOf(0) }
    var showQueueReview by remember { mutableStateOf(false) }
    var isDeletingQueue by remember { mutableStateOf(false) }
    var queuedBytesEstimate by remember { mutableStateOf(0L) }
    var recoveredBytes by remember { mutableStateOf(0L) }
    var confirmedDeletedCount by remember { mutableIntStateOf(0) }
    var reviewSessionGeneration by remember(screenshots) { mutableIntStateOf(0) }

    LaunchedEffect(screenshots) {
        flowPhase = CleanupFlowPhase.Initializing
        isPostReviewTransitioning = false
        postReviewTransitionNonce++
        reviewItems.clear()
        reviewItems.addAll(screenshots.toCleanupReviewItems())
        undoStack.clear()
        showQueueReview = false
        isDeletingQueue = false
        queuedBytesEstimate = 0L
        recoveredBytes = 0L
        confirmedDeletedCount = 0
        reviewSessionGeneration = 0
        isAnimating = false
        flowPhase = CleanupFlowPhase.Reviewing
    }

    val reviewSnapshot = reviewItems.toList()
    val unreviewedDeck = reviewSnapshot.unreviewedDeck()
    val queuedUris = reviewSnapshot.queuedUris()
    val keepCount = reviewSnapshot.keepCount()
    val queuedCount = reviewSnapshot.queuedCount()
    val hasQueuedDeletes = queuedUris.isNotEmpty()

    LaunchedEffect(queuedUris) {
        queuedBytesEstimate = estimateUrisTotalBytes(context, queuedUris)
    }

    LaunchedEffect(queuedCount, showQueueReview) {
        if (showQueueReview && queuedCount == 0) {
            showQueueReview = false
        }
    }

    var deleteConsentContinuation by remember {
        mutableStateOf<((Boolean) -> Unit)?>(null)
    }

    val deleteConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val approved = result.resultCode == Activity.RESULT_OK
        deleteConsentContinuation?.invoke(approved)
        deleteConsentContinuation = null
    }

    val onKeepState by rememberUpdatedState(onKeep)
    val onDeleteState by rememberUpdatedState(onDelete)

    val originalTotal = screenshots.size
    val stackDeckUris = remember(reviewSnapshot) {
        unreviewedDeck.take(STACK_VISIBLE_COUNT).map { it.uri }
    }
    val cleanupStage = resolveCleanupStage(
        flowPhase = flowPhase,
        isPostReviewTransitioning = isPostReviewTransitioning,
        originalTotal = originalTotal,
        unreviewedCount = unreviewedDeck.size,
        deckUris = stackDeckUris
    )
    val progressIndex = when (flowPhase) {
        CleanupFlowPhase.Initializing -> 0
        else -> when {
            originalTotal == 0 -> 0
            unreviewedDeck.isEmpty() -> originalTotal
            else -> (reviewSnapshot.reviewedCount() + 1).coerceAtMost(originalTotal)
        }
    }

    fun schedulePostReviewTransition() {
        if (flowPhase != CleanupFlowPhase.Reviewing) return
        if (originalTotal <= 0) return
        if (reviewItems.any { it.reviewState == ScreenshotReviewState.Unreviewed }) return

        val targetPhase = if (hasQueuedDeletes) {
            CleanupFlowPhase.QueueReview
        } else {
            CleanupFlowPhase.Completed
        }
        val nonce = postReviewTransitionNonce + 1
        postReviewTransitionNonce = nonce
        isPostReviewTransitioning = true
        scope.launch {
            delay(POST_REVIEW_SETTLE_DELAY_MS)
            if (postReviewTransitionNonce != nonce) return@launch
            if (flowPhase != CleanupFlowPhase.Reviewing) {
                isPostReviewTransitioning = false
                return@launch
            }
            if (reviewItems.any { it.reviewState == ScreenshotReviewState.Unreviewed }) {
                isPostReviewTransitioning = false
                return@launch
            }
            isAnimating = false
            reviewSessionGeneration++
            flowPhase = targetPhase
            isPostReviewTransitioning = false
        }
    }

    fun applyReviewState(itemId: String, newState: ScreenshotReviewState) {
        val index = reviewItems.indexOfFirst { it.id == itemId }
        if (index < 0) return
        val item = reviewItems[index]
        if (item.reviewState != ScreenshotReviewState.Unreviewed) return

        undoStack.add(CleanupUndoEntry(itemId = item.id, previousState = item.reviewState))
        reviewItems[index] = item.copy(reviewState = newState)
        isAnimating = false
        if (reviewItems.none { it.reviewState == ScreenshotReviewState.Unreviewed }) {
            schedulePostReviewTransition()
        }
    }

    fun markKept(uri: Uri) {
        applyReviewState(uri.toString(), ScreenshotReviewState.Kept)
        onKeepState(uri)
    }

    fun markQueued(uri: Uri) {
        applyReviewState(uri.toString(), ScreenshotReviewState.Queued)
    }

    fun performUndo() {
        val entry = undoStack.removeLastOrNull() ?: return
        val index = reviewItems.indexOfFirst { it.id == entry.itemId }
        if (index < 0) return

        postReviewTransitionNonce++
        isPostReviewTransitioning = false
        reviewItems[index] = reviewItems[index].copy(reviewState = entry.previousState)
        if (flowPhase != CleanupFlowPhase.Reviewing && reviewItems.unreviewedDeck().isNotEmpty()) {
            flowPhase = CleanupFlowPhase.Reviewing
            reviewSessionGeneration++
        }
    }

    fun removeFromQueue(uri: Uri) {
        val itemId = uri.toString()
        val index = reviewItems.indexOfFirst { it.id == itemId && it.reviewState == ScreenshotReviewState.Queued }
        if (index < 0) return

        reviewItems[index] = reviewItems[index].copy(reviewState = ScreenshotReviewState.Unreviewed)
        undoStack.removeAll { it.itemId == itemId }

        if (flowPhase == CleanupFlowPhase.QueueReview &&
            reviewItems.none { it.reviewState == ScreenshotReviewState.Queued }
        ) {
            flowPhase = if (reviewItems.none { it.reviewState == ScreenshotReviewState.Unreviewed }) {
                CleanupFlowPhase.Completed
            } else {
                CleanupFlowPhase.Reviewing
            }
            showQueueReview = false
            reviewSessionGeneration++
        }
    }

    fun flushPendingDeletes() {
        if (queuedUris.isEmpty() || isDeletingQueue) return
        scope.launch {
            val urisToDelete = queuedUris
            val deleteCountRequested = urisToDelete.size
            val bytesToRecover = estimateUrisTotalBytes(context, urisToDelete)
            isDeletingQueue = true
            val deleted = deleteQueuedScreenshots(
                context = context,
                uris = urisToDelete,
                requestUserConsent = { intentRequest ->
                    suspendCancellableCoroutine { continuation ->
                        deleteConsentContinuation = { approved ->
                            if (continuation.isActive) {
                                continuation.resume(approved)
                            }
                        }
                        deleteConsentLauncher.launch(intentRequest)
                    }
                }
            )
            isDeletingQueue = false
            if (deleted) {
                val deletedIds = reviewItems
                    .filter { it.reviewState == ScreenshotReviewState.Queued }
                    .map { it.id }
                    .toSet()
                reviewItems.removeAll { it.id in deletedIds }
                undoStack.clear()
                confirmedDeletedCount = deleteCountRequested
                recoveredBytes = bytesToRecover
                flowPhase = CleanupFlowPhase.Completed
                showQueueReview = false
                showBatchDeleteDialog = false
                reviewSessionGeneration++
                onDeleteState(urisToDelete.first())
            } else {
                snackbarHostState.showSnackbar(DELETE_ERROR_MESSAGE)
            }
        }
    }

    val swipeSessionGeneration by rememberUpdatedState(reviewSessionGeneration)
    val onSwipeResolved: (SwipeDirection, Uri) -> Unit = swipeResolved@{ direction, uri ->
        if (flowPhase != CleanupFlowPhase.Reviewing) {
            isAnimating = false
            return@swipeResolved
        }
        when (direction) {
            SwipeDirection.Right -> markKept(uri)
            SwipeDirection.Left -> markQueued(uri)
        }
    }
    val onSwipeResolvedState by rememberUpdatedState(onSwipeResolved)

    val colorScheme = MaterialTheme.colorScheme
    val isLightSurface = colorScheme.surface.luminance() > 0.5f
    val deleteContainer = if (isLightSurface) DeleteRedContainer else DeleteRed.copy(alpha = 0.22f)
    val undoContainer = if (isLightSurface) {
        UndoNeutralContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    val undoContent = if (isLightSurface) {
        UndoNeutralContent
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CleanupHeader(
                reviewedIndex = progressIndex,
                total = originalTotal,
                flowPhase = flowPhase,
                onBack = onBack,
                enabled = flowPhase != CleanupFlowPhase.Initializing &&
                        !isAnimating &&
                        !isDeletingQueue
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = cleanupStage,
                    transitionSpec = {
                        val swipeHandoff =
                            initialState is CleanupStage.SwipeReview &&
                                    targetState is CleanupStage.SwipeReview
                        val reviewHandoffToPostReview =
                            (initialState is CleanupStage.SwipeReview ||
                                    initialState is CleanupStage.ReviewSettling) &&
                                    (targetState is CleanupStage.QueueReview ||
                                            targetState is CleanupStage.Completed)
                        val initToReview =
                            initialState is CleanupStage.Initializing &&
                                    targetState is CleanupStage.SwipeReview
                        val swipeToSettling =
                            initialState is CleanupStage.SwipeReview &&
                                    targetState is CleanupStage.ReviewSettling
                        when {
                            swipeHandoff -> EnterTransition.None togetherWith ExitTransition.None
                            swipeToSettling ->
                                fadeIn(tween(160)) togetherWith fadeOut(tween(240))
                            reviewHandoffToPostReview ->
                                fadeIn(tween(380, delayMillis = 100)) togetherWith
                                        fadeOut(tween(280))
                            initToReview ->
                                fadeIn(tween(320)) togetherWith fadeOut(tween(180))
                            else ->
                                (fadeIn(tween(320)) + slideInVertically { it / 8 }) togetherWith
                                        fadeOut(tween(220))
                        }
                    },
                    label = "cleanup_flow",
                    contentKey = { stage ->
                        when (stage) {
                            CleanupStage.Initializing -> "init"
                            CleanupStage.Empty -> "empty"
                            is CleanupStage.SwipeReview -> "swipe_deck"
                            CleanupStage.ReviewSettling -> "settling"
                            CleanupStage.QueueReview -> "queue"
                            CleanupStage.Completed -> "completed"
                        }
                    }
                ) { stage ->
                    when (stage) {
                        CleanupStage.Initializing -> CleanupStackSkeleton()
                        CleanupStage.Empty -> CleanupEmptyState()
                        CleanupStage.ReviewSettling -> CleanupReviewSettlingState()
                        is CleanupStage.SwipeReview -> SwipeCardStack(
                            deckUris = stage.deckUris,
                            isAnimating = isAnimating,
                            sessionGeneration = swipeSessionGeneration,
                            onSwipeComplete = onSwipeResolvedState,
                            onAnimatingChanged = { isAnimating = it }
                        )
                        CleanupStage.QueueReview -> CleanupReviewCompleteState(
                            keepCount = keepCount,
                            queuedCount = queuedCount,
                            queuedBytes = queuedBytesEstimate,
                            isDeleting = isDeletingQueue,
                            onDeleteQueued = { showBatchDeleteDialog = true },
                            onReviewQueue = { showQueueReview = true }
                        )
                        CleanupStage.Completed -> CleanupFinishedState(
                            keepCount = keepCount,
                            deletedCount = confirmedDeletedCount,
                            recoveredBytes = recoveredBytes,
                            onBackToLibrary = onBackToLibrary
                        )
                    }
                }
            }

            if (cleanupStage is CleanupStage.SwipeReview) {
                CleanupActionButtons(
                    enabled = !isAnimating,
                    showUndo = undoStack.isNotEmpty(),
                    pendingDeleteCount = queuedCount,
                    deleteContainerColor = deleteContainer,
                    undoContainerColor = undoContainer,
                    undoContentColor = undoContent,
                    onDeleteQueue = {
                        if (queuedCount > 0) {
                            showQueueReview = true
                        }
                    },
                    onUndo = { performUndo() }
                )
            } else {
                Spacer(modifier = Modifier.height(96.dp))
            }
        }

        if (showBatchDeleteDialog) {
            val dialogQueuedCount = queuedCount
            AlertDialog(
                onDismissRequest = { showBatchDeleteDialog = false },
                title = { Text("Delete queued screenshots?") },
                text = {
                    Text(
                        "Permanently delete $dialogQueuedCount screenshot" +
                                "${if (dialogQueuedCount == 1) "" else "s"} from your device."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showBatchDeleteDialog = false
                            flushPendingDeletes()
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBatchDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showQueueReview && queuedUris.isNotEmpty()) {
            CleanupQueueReviewSheet(
                queuedUris = queuedUris,
                queuedCount = queuedCount,
                isDeleting = isDeletingQueue,
                onDismiss = { showQueueReview = false },
                onRemoveFromQueue = { removeFromQueue(it) },
                onConfirmDelete = {
                    showQueueReview = false
                    showBatchDeleteDialog = true
                }
            )
        }

        if (isDeletingQueue) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}

/** Top bar: back on the left, title + subtitle truly centered. */
@Composable
private fun CleanupHeader(
    reviewedIndex: Int,
    total: Int,
    flowPhase: CleanupFlowPhase,
    onBack: () -> Unit,
    enabled: Boolean
) {
    val subtitle = when (flowPhase) {
        CleanupFlowPhase.Initializing -> "Preparing…"
        CleanupFlowPhase.Reviewing ->
            if (total == 0) "No screenshots" else "$reviewedIndex / $total reviewed"
        CleanupFlowPhase.QueueReview -> "Review complete"
        CleanupFlowPhase.Completed -> "Cleanup complete"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        IconButton(
            onClick = onBack,
            enabled = enabled,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Cleanup",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Placeholder stack shown while review state initializes or during post-review settle. */
@Composable
private fun CleanupStackSkeleton(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val shape = RoundedCornerShape(CARD_CORNER_RADIUS_DP.dp)
    val backPlaceholder = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val frontPlaceholder = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    val backCardYOffsetPx = with(density) { BACK_CARD_Y_OFFSET.toPx() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = ScreenHorizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(CARD_WIDTH_FRACTION)
                .aspectRatio(CARD_ASPECT_WIDTH / CARD_ASPECT_HEIGHT)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = BACK_CARD_BASE_SCALE
                        scaleY = BACK_CARD_BASE_SCALE
                        translationY = backCardYOffsetPx
                        alpha = BACK_CARD_BASE_ALPHA
                    }
                    .clip(shape)
                    .background(backPlaceholder)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(frontPlaceholder)
            )
        }
    }
}

/** Brief fade-out between the swipe deck and queue / completion screens. */
@Composable
private fun CleanupReviewSettlingState() {
    val alpha by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(durationMillis = 260),
        label = "review_settling_fade"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha },
        contentAlignment = Alignment.Center
    ) {
        CleanupStackSkeleton()
    }
}

private data class StackLayerVisuals(
    val scale: Float,
    val alpha: Float,
    val translationY: Float
)

private fun lerpFloat(a: Float, b: Float, t: Float): Float =
    a + (b - a) * t.coerceIn(0f, 1f)

/** Interpolate scale/alpha/Y between front (0) and stacked-back (1–2) depths. */
private fun stackVisualsForDepth(
    depth: Float,
    dragProgress: Float,
    backCardYOffsetPx: Float
): StackLayerVisuals {
    val frontScale = 1f - dragProgress * 0.025f
    val backScale = BACK_CARD_BASE_SCALE + dragProgress * BACK_CARD_SCALE_DRAG_BOOST
    val backAlpha = BACK_CARD_BASE_ALPHA + dragProgress * BACK_CARD_ALPHA_DRAG_BOOST
    val back2Scale = BACK_CARD_BASE_SCALE - STACK_DEPTH_2_SCALE_DELTA
    val back2Alpha = (BACK_CARD_BASE_ALPHA - STACK_DEPTH_2_ALPHA_DELTA).coerceAtLeast(0.35f)
    val back2Y = backCardYOffsetPx * 1.35f

    return when {
        depth <= 0f -> StackLayerVisuals(frontScale, 1f, 0f)
        depth <= 1f -> {
            val t = depth.coerceIn(0f, 1f)
            StackLayerVisuals(
                scale = lerpFloat(frontScale, backScale, t),
                alpha = lerpFloat(1f, backAlpha, t),
                translationY = lerpFloat(0f, backCardYOffsetPx, t)
            )
        }
        else -> {
            val t = (depth - 1f).coerceIn(0f, 1f)
            StackLayerVisuals(
                scale = lerpFloat(backScale, back2Scale, t),
                alpha = lerpFloat(0f, back2Alpha, t),
                translationY = lerpFloat(backCardYOffsetPx, back2Y, t)
            )
        }
    }
}

/**
 * Stacked swipe deck — up to [STACK_VISIBLE_COUNT] cards stay mounted with stable URI keys.
 * The card underneath promotes in place after the top card exits (no remount pop-in).
 */
@Composable
private fun SwipeCardStack(
    deckUris: List<Uri>,
    isAnimating: Boolean,
    sessionGeneration: Int,
    onSwipeComplete: (SwipeDirection, Uri) -> Unit,
    onAnimatingChanged: (Boolean) -> Unit
) {
    val topUri = deckUris.firstOrNull() ?: return

    val scope = rememberCoroutineScope()
    val onSwipeCompleteState by rememberUpdatedState(onSwipeComplete)
    val onAnimatingChangedState by rememberUpdatedState(onAnimatingChanged)
    val liveSessionGeneration by rememberUpdatedState(sessionGeneration)
    val sessionAtMount = remember { liveSessionGeneration }

    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val offsetX = remember { Animatable(0f) }
    val promoteProgress = remember { Animatable(0f) }
    val velocityTracker = remember { VelocityTracker() }
    var exitingUri by remember { mutableStateOf<Uri?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    var containerHeightPx by remember { mutableFloatStateOf(0f) }

    DisposableEffect(sessionGeneration) {
        onDispose {
            isDragging = false
            dragOffsetX = 0f
            exitingUri = null
            scope.launch {
                offsetX.stop()
                offsetX.snapTo(0f)
                promoteProgress.stop()
                promoteProgress.snapTo(0f)
            }
        }
    }

    val displayOffsetX = if (isDragging) dragOffsetX else offsetX.value
    val promotion = promoteProgress.value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = ScreenHorizontalPadding)
            .onSizeChanged {
                containerWidthPx = it.width.toFloat()
                containerHeightPx = it.height.toFloat()
            },
        contentAlignment = Alignment.Center
    ) {
        if (containerWidthPx <= 0f || containerHeightPx <= 0f) return@Box

        val aspectRatio = CARD_ASPECT_HEIGHT / CARD_ASPECT_WIDTH
        var cardWidthPx = containerWidthPx * CARD_WIDTH_FRACTION
        var cardHeightPx = cardWidthPx * aspectRatio
        val maxCardHeightPx = containerHeightPx * CARD_MAX_HEIGHT_FRACTION
        if (cardHeightPx > maxCardHeightPx) {
            cardHeightPx = maxCardHeightPx
            cardWidthPx = cardHeightPx / aspectRatio
        }

        val cardWidthDp = with(density) { cardWidthPx.toDp() }
        val cardHeightDp = with(density) { cardHeightPx.toDp() }
        val cardSizeModifier = Modifier.width(cardWidthDp).height(cardHeightDp)
        val backCardYOffsetPx = with(density) { BACK_CARD_Y_OFFSET.toPx() }
        val commitThresholdPx = swipeCommitThresholdPx(cardWidthPx)
        val dragProgress = (abs(displayOffsetX) / cardWidthPx).coerceIn(0f, 1f)

        // How far the back card has promoted during the current drag gesture.
        // Starts ramping once dragProgress crosses DRAG_PROMOTION_THRESHOLD and reaches 1.0
        // at full swipe distance — so the second card is already in the front position
        // before the exit animation even finishes.
        val dragBasedPromotion = if (dragProgress > DRAG_PROMOTION_THRESHOLD) {
            ((dragProgress - DRAG_PROMOTION_THRESHOLD) /
                    (1f - DRAG_PROMOTION_THRESHOLD)).coerceIn(0f, 1f)
        } else 0f

        // Back → front: third, second, then interactive top (exiting card drawn on top).
        deckUris.take(STACK_VISIBLE_COUNT).asReversed().forEach { uri ->
            if (uri == exitingUri) return@forEach
            val stackIndex = deckUris.indexOf(uri)
            if (stackIndex < 0) return@forEach

            val depth = when (stackIndex) {
                0 -> 0f
                // Fold drag-based promotion in so the second card starts moving toward
                // the front at DRAG_PROMOTION_THRESHOLD — before the exit animation starts.
                // `promotion` (post-exit spring) handles the remainder for fast flicks.
                1 -> (1f - dragBasedPromotion - promotion).coerceAtLeast(0f)
                else -> (stackIndex - promotion).coerceAtLeast(0f)
            }
            // Feed the live drag progress to the back card too so its backScale / backAlpha
            // targets grow with the gesture, giving a continuous "growing out of the stack" feel.
            val layerDragProgress = if (uri == topUri || stackIndex == 1) dragProgress else 0f
            val visuals = stackVisualsForDepth(depth, layerDragProgress, backCardYOffsetPx)

            key(uri) {
                if (stackIndex == 0 && uri == topUri) {
                    val rotation =
                        (displayOffsetX / cardWidthPx).coerceIn(-1f, 1f) * MAX_ROTATION_DEGREES
                    ScreenshotCard(
                        uri = uri,
                        modifier = cardSizeModifier
                            .graphicsLayer {
                                translationX = displayOffsetX
                                rotationZ = rotation
                                scaleX = visuals.scale
                                scaleY = visuals.scale
                                alpha = visuals.alpha
                                translationY = visuals.translationY
                                clip = false
                            }
                            .pointerInput(uri, isAnimating, cardWidthPx, sessionGeneration) {
                                if (isAnimating) return@pointerInput
                                detectControlledHorizontalSwipe(
                                    onDragStart = {
                                        velocityTracker.resetTracking()
                                        isDragging = true
                                    },
                                    onHorizontalDrag = { deltaX, change ->
                                        velocityTracker.addPosition(
                                            timeMillis = change.uptimeMillis,
                                            position = change.position
                                        )
                                        val proposed = dragOffsetX + deltaX
                                        dragOffsetX = resistSwipeOffset(proposed, cardWidthPx)
                                    },
                                    onDragEnd = {
                                        scope.launch {
                                            if (sessionAtMount != liveSessionGeneration) return@launch
                                            isDragging = false
                                            offsetX.snapTo(dragOffsetX)
                                            handleStackSwipeRelease(
                                                offsetX = offsetX,
                                                promoteProgress = promoteProgress,
                                                velocityTracker = velocityTracker,
                                                cardWidthPx = cardWidthPx,
                                                uri = uri,
                                                onExitingChanged = { exitingUri = it },
                                                isSessionActive = {
                                                    sessionAtMount == liveSessionGeneration
                                                },
                                                onAnimatingChanged = onAnimatingChangedState,
                                                onSwipeComplete = onSwipeCompleteState
                                            )
                                            dragOffsetX = 0f
                                        }
                                    },
                                    onDragCancel = {
                                        scope.launch {
                                            isDragging = false
                                            dragOffsetX = 0f
                                            if (offsetX.value != 0f) {
                                                resetCardPosition(offsetX)
                                            }
                                        }
                                    }
                                )
                            },
                        swipeOverlay = {
                            SwipeOverlay(
                                offsetX = displayOffsetX,
                                cardWidthPx = cardWidthPx,
                                commitThresholdPx = commitThresholdPx
                            )
                        }
                    )
                } else {
                    ScreenshotCard(
                        uri = uri,
                        modifier = cardSizeModifier.graphicsLayer {
                            scaleX = visuals.scale
                            scaleY = visuals.scale
                            alpha = visuals.alpha
                            translationY = visuals.translationY
                            clip = false
                        }
                    )
                }
            }
        }

        exitingUri?.let { uri ->
            key("exiting_$uri") {
                val rotation = (offsetX.value / cardWidthPx).coerceIn(-1f, 1f) * MAX_ROTATION_DEGREES
                val exitVisuals = stackVisualsForDepth(0f, dragProgress = 0f, backCardYOffsetPx)
                ScreenshotCard(
                    uri = uri,
                    modifier = cardSizeModifier.graphicsLayer {
                        translationX = offsetX.value
                        rotationZ = rotation
                        scaleX = exitVisuals.scale
                        scaleY = exitVisuals.scale
                        alpha = exitVisuals.alpha
                        clip = false
                    }
                )
            }
        }
    }
}

private suspend fun handleStackSwipeRelease(
    offsetX: Animatable<Float, *>,
    promoteProgress: Animatable<Float, *>,
    velocityTracker: VelocityTracker,
    cardWidthPx: Float,
    uri: Uri,
    onExitingChanged: (Uri?) -> Unit,
    isSessionActive: () -> Boolean,
    onAnimatingChanged: (Boolean) -> Unit,
    onSwipeComplete: (SwipeDirection, Uri) -> Unit
) {
    if (!isSessionActive()) {
        onAnimatingChanged(false)
        return
    }
    val velocityX = velocityTracker.calculateVelocity().x
    val direction = resolveSwipeDirection(
        offsetPx = offsetX.value,
        velocityXPxPerSec = velocityX,
        cardWidthPx = cardWidthPx
    )
    if (direction != null) {
        completeStackSwipe(
            direction = direction,
            uri = uri,
            cardWidthPx = cardWidthPx,
            offsetX = offsetX,
            promoteProgress = promoteProgress,
            onExitingChanged = onExitingChanged,
            isSessionActive = isSessionActive,
            onAnimatingChanged = onAnimatingChanged,
            onSwipeComplete = onSwipeComplete
        )
    } else {
        resetCardPosition(offsetX)
    }
}

/** Horizontal exit, then in-place promotion of the stack — then advance review state. */
private suspend fun completeStackSwipe(
    direction: SwipeDirection,
    uri: Uri,
    cardWidthPx: Float,
    offsetX: Animatable<Float, *>,
    promoteProgress: Animatable<Float, *>,
    onExitingChanged: (Uri?) -> Unit,
    isSessionActive: () -> Boolean,
    onAnimatingChanged: (Boolean) -> Unit,
    onSwipeComplete: (SwipeDirection, Uri) -> Unit
) {
    if (!isSessionActive()) {
        onAnimatingChanged(false)
        return
    }
    if (offsetX.isRunning) offsetX.stop()
    onAnimatingChanged(true)

    val targetX = when (direction) {
        SwipeDirection.Right -> cardWidthPx * SWIPE_OFFSCREEN_MULTIPLIER
        SwipeDirection.Left -> -cardWidthPx * SWIPE_OFFSCREEN_MULTIPLIER
    }
    offsetX.animateTo(targetX, SwipeExitSpring)

    if (!isSessionActive()) {
        onExitingChanged(null)
        onAnimatingChanged(false)
        offsetX.snapTo(0f)
        return
    }

    onExitingChanged(uri)
    if (promoteProgress.isRunning) promoteProgress.stop()
    promoteProgress.snapTo(0f)
    promoteProgress.animateTo(1f, StackPromotionSpring)

    if (!isSessionActive()) {
        onExitingChanged(null)
        promoteProgress.snapTo(0f)
        onAnimatingChanged(false)
        offsetX.snapTo(0f)
        return
    }

    onSwipeComplete(direction, uri)
    onExitingChanged(null)
    promoteProgress.snapTo(0f)
    offsetX.snapTo(0f)
    onAnimatingChanged(false)
}

private suspend fun resetCardPosition(offsetX: Animatable<Float, *>) {
    if (offsetX.isRunning) offsetX.stop()
    offsetX.animateTo(
        targetValue = 0f,
        animationSpec = SwipeResetSpring
    )
}

/** Soft drop shadow behind [shape]; [clip] false keeps the halo visible outside rounded bounds. */
private fun Modifier.softDropShadow(
    shape: Shape,
    elevation: Dp,
    ambientColor: Color,
    spotColor: Color
): Modifier = shadow(
    elevation = elevation,
    shape = shape,
    clip = false,
    ambientColor = ambientColor,
    spotColor = spotColor
)

/** Rounded screenshot card with Coil image and soft shadow. */
@Composable
private fun ScreenshotCard(
    uri: Uri,
    modifier: Modifier = Modifier,
    swipeOverlay: @Composable (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(CARD_CORNER_RADIUS_DP.dp)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val shadowTint = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier.softDropShadow(
            shape = shape,
            elevation = CardDropShadowElevation,
            ambientColor = shadowTint.copy(alpha = 0.24f),
            spotColor = shadowTint.copy(alpha = 0.32f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(surfaceColor)
        ) {
            AsyncImage(
                model = uri,
                contentDescription = "Screenshot",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            swipeOverlay?.invoke()
        }
    }
}

/** Progressive keep/delete preview: slight → preview → commit-ready. */
@Composable
private fun SwipeOverlay(
    offsetX: Float,
    cardWidthPx: Float,
    commitThresholdPx: Float
) {
    if (cardWidthPx <= 0f) return
    val phase = swipeProgressPhase(offsetX, cardWidthPx)
    if (phase == SwipeProgressPhase.Idle) return

    val progress = abs(offsetX) / cardWidthPx
    val isKeep = offsetX > 0
    val tint = if (isKeep) KeepGreen else DeleteRed

    val overlayAlpha = when (phase) {
        SwipeProgressPhase.Slight -> {
            val t = (progress - SWIPE_HINT_THRESHOLD_FRACTION) /
                    (SWIPE_PREVIEW_THRESHOLD_FRACTION - SWIPE_HINT_THRESHOLD_FRACTION)
            t.coerceIn(0f, 1f) * 0.08f
        }
        SwipeProgressPhase.Preview -> {
            val t = (progress - SWIPE_PREVIEW_THRESHOLD_FRACTION) /
                    (SWIPE_COMMIT_THRESHOLD_FRACTION - SWIPE_PREVIEW_THRESHOLD_FRACTION)
            0.08f + t.coerceIn(0f, 1f) * 0.16f
        }
        SwipeProgressPhase.CommitReady -> {
            val commitFraction = commitThresholdPx / cardWidthPx
            val t = ((progress - commitFraction) / (1f - commitFraction).coerceAtLeast(0.01f))
                .coerceIn(0f, 1f)
            0.24f + t * 0.1f
        }
        SwipeProgressPhase.Idle -> 0f
    }

    val label = when (phase) {
        SwipeProgressPhase.Preview -> if (isKeep) "Keep" else "Delete"
        SwipeProgressPhase.CommitReady -> if (isKeep) "KEEP" else "DELETE"
        else -> null
    }

    val labelTargetAlpha = when (phase) {
        SwipeProgressPhase.Preview -> 0.55f
        SwipeProgressPhase.CommitReady -> 1f
        else -> 0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tint.copy(alpha = overlayAlpha)),
        contentAlignment = Alignment.Center
    ) {
        if (label != null) {
            val labelAlpha by animateFloatAsState(
                targetValue = labelTargetAlpha,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "swipeLabelAlpha"
            )
            Text(
                text = label,
                style = when (phase) {
                    SwipeProgressPhase.CommitReady ->
                        MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    else ->
                        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                },
                color = Color.White.copy(alpha = 0.92f * labelAlpha),
                modifier = Modifier.graphicsLayer {
                    val scale = 0.94f + labelAlpha * 0.06f
                    scaleX = scale
                    scaleY = scale
                }
            )
        }
    }
}

/** Swipe-first bottom controls: Undo (left) and delete queue (right). */
@Composable
private fun CleanupActionButtons(
    enabled: Boolean,
    showUndo: Boolean,
    pendingDeleteCount: Int,
    deleteContainerColor: Color,
    undoContainerColor: Color,
    undoContentColor: Color,
    onDeleteQueue: () -> Unit,
    onUndo: () -> Unit
) {
    val animatedQueueCount by animateIntAsState(
        targetValue = pendingDeleteCount,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "deleteQueueCount"
    )
    val showDeleteQueue = pendingDeleteCount > 0
    val deleteQueueLabel = "Delete ($animatedQueueCount)"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenHorizontalPadding)
            .padding(top = 8.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(ActionButtonGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(if (showDeleteQueue) 1f else 2f)
                .height(58.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = showUndo,
                enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.92f),
                exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.92f)
            ) {
                UndoPillButton(
                    enabled = enabled,
                    containerColor = undoContainerColor,
                    contentColor = undoContentColor,
                    onClick = onUndo,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            androidx.compose.animation.AnimatedVisibility(
                visible = showDeleteQueue,
                enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.88f),
                exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.88f),
                modifier = Modifier.fillMaxWidth()
            ) {
                DeleteQueuePillButton(
                    label = deleteQueueLabel,
                    queueCount = animatedQueueCount,
                    containerColor = deleteContainerColor,
                    contentColor = DeleteRed,
                    enabled = enabled,
                    onClick = onDeleteQueue,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UndoPillButton(
    enabled: Boolean,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(percent = 50)

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Undo",
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Undo",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteQueuePillButton(
    label: String,
    queueCount: Int,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(percent = 50)
    val shadowTint = MaterialTheme.colorScheme.onSurface
    val shadowModifier = if (enabled) {
        Modifier.softDropShadow(
            shape = shape,
            elevation = ButtonDropShadowElevation,
            ambientColor = shadowTint.copy(alpha = 0.22f),
            spotColor = shadowTint.copy(alpha = 0.30f)
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .height(58.dp)
            .then(shadowModifier)
    ) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxSize(),
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                BadgedBox(
                    badge = {
                        if (queueCount > 0) {
                            Badge(containerColor = DeleteRed) {
                                Text(
                                    text = queueCount.coerceAtMost(99).toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionPillButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(percent = 50)
    val shadowTint = MaterialTheme.colorScheme.onSurface
    val shadowModifier = if (enabled) {
        Modifier.softDropShadow(
            shape = shape,
            elevation = ButtonDropShadowElevation,
            ambientColor = shadowTint.copy(alpha = 0.22f),
            spotColor = shadowTint.copy(alpha = 0.30f)
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .height(58.dp)
            .fillMaxWidth()
            .then(shadowModifier)
    ) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxSize(),
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CleanupEmptyState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Text(
            text = "No screenshots to review",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CleanupReviewCompleteState(
    keepCount: Int,
    queuedCount: Int,
    queuedBytes: Long,
    isDeleting: Boolean,
    onDeleteQueued: () -> Unit,
    onReviewQueue: () -> Unit
) {
    val context = LocalContext.current
    val recoveryLabel = remember(queuedBytes) {
        Formatter.formatShortFileSize(context, queuedBytes.coerceAtLeast(0L))
    }
    val queuedLabel = if (queuedCount == 1) {
        "1 screenshot queued"
    } else {
        "$queuedCount screenshots queued"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Icon(
            imageVector = Icons.Default.DeleteSweep,
            contentDescription = null,
            modifier = Modifier.size(52.dp),
            tint = DeleteRed.copy(alpha = 0.85f)
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Ready to clean up",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = "You've finished reviewing. Nothing is deleted until you confirm.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        CleanupStatsCard(
            rows = listOf(
                "Kept" to "$keepCount",
                "Queued" to queuedLabel,
                "Recover" to recoveryLabel
            )
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onDeleteQueued,
                enabled = queuedCount > 0 && !isDeleting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(percent = 50)
            ) {
                Text("Delete queued screenshots")
            }
            Surface(
                onClick = onReviewQueue,
                enabled = queuedCount > 0 && !isDeleting,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(percent = 50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = "Review queue",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun CleanupFinishedState(
    keepCount: Int,
    deletedCount: Int,
    recoveredBytes: Long,
    onBackToLibrary: () -> Unit
) {
    val context = LocalContext.current
    var animateStats by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateStats = true }

    val animatedDeleted by animateIntAsState(
        targetValue = if (animateStats) deletedCount else 0,
        animationSpec = tween(durationMillis = 700, delayMillis = 120),
        label = "deletedCount"
    )
    val animatedRecoveryProgress by animateFloatAsState(
        targetValue = if (animateStats) 1f else 0f,
        animationSpec = tween(durationMillis = 900, delayMillis = 200),
        label = "recoveryProgress"
    )
    val displayedRecoveryBytes = (recoveredBytes * animatedRecoveryProgress).toLong()
    val recoveryLabel = remember(displayedRecoveryBytes) {
        Formatter.formatShortFileSize(context, displayedRecoveryBytes)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = animateStats,
            enter = scaleIn(
                initialScale = 0.88f,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(tween(400))
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = KeepGreen
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "All screenshots reviewed",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Cleanup is complete. Your library is up to date.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        CleanupStatsCard(
            rows = buildList {
                add("Kept" to "$keepCount")
                if (deletedCount > 0) {
                    add("Deleted" to "$animatedDeleted")
                    add("Storage recovered" to recoveryLabel)
                }
            }
        )
        Button(
            onClick = onBackToLibrary,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(percent = 50)
        ) {
            Text("Back to Library")
        }
    }
}

@Composable
private fun CleanupStatsCard(rows: List<Pair<String, String>>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rows.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CleanupQueueReviewSheet(
    queuedUris: List<Uri>,
    queuedCount: Int,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onRemoveFromQueue: (Uri) -> Unit,
    onConfirmDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val queueTitle = if (queuedCount == 1) {
        "1 screenshot queued"
    } else {
        "$queuedCount screenshots queued"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Deletion queue",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            Text(
                text = queueTitle,
                style = MaterialTheme.typography.titleSmall,
                color = DeleteRed.copy(alpha = 0.9f),
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Text(
                text = "Tap × to remove from the queue. Nothing is deleted until you confirm.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(queuedUris, key = { it.toString() }) { uri ->
                    CleanupQueueThumbnail(
                        uri = uri,
                        onRemove = { onRemoveFromQueue(uri) }
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isDeleting,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back to review")
                }
                Button(
                    onClick = onConfirmDelete,
                    enabled = queuedCount > 0 && !isDeleting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (queuedCount > 0) {
                            "Delete ($queuedCount)"
                        } else {
                            "Delete"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanupQueueThumbnail(
    uri: Uri,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(108.dp)
            .height(180.dp)
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(32.dp)
                .background(
                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove from queue",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Deletes queued MediaStore images in one batch.
 * Android 11+ uses a single [MediaStore.createDeleteRequest] consent sheet.
 */
private suspend fun deleteQueuedScreenshots(
    context: Context,
    uris: List<Uri>,
    requestUserConsent: suspend (IntentSenderRequest) -> Boolean
): Boolean {
    if (uris.isEmpty()) return true

    return try {
        val resolver = context.contentResolver

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val consentRequest = withContext(Dispatchers.IO) {
                val pendingIntent = MediaStore.createDeleteRequest(resolver, uris)
                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            }
            return requestUserConsent(consentRequest)
        }

        withContext(Dispatchers.IO) {
            uris.all { uri ->
                try {
                    resolver.delete(uri, null, null) > 0
                } catch (_: SecurityException) {
                    false
                }
            }
        }
    } catch (_: Exception) {
        false
    }
}


/** Approximate relative luminance for light/dark container picks. */
private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}
