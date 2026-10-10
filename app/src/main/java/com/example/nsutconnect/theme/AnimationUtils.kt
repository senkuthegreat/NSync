package com.example.nsutconnect.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material3.Surface
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * Apple Fluid Physics Constants.
 * Modeled after iOS SwiftUI `.spring(response: ..., dampingFraction: ...)` and UIKit dynamics.
 */
object AppleMotion {
    val SnappyFloat: SpringSpec<Float> = spring(
        dampingRatio = 0.74f,
        stiffness = 520f
    )

    val BouncyFloat: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val FluidFloat: SpringSpec<Float> = spring(
        dampingRatio = 0.84f,
        stiffness = 380f
    )

    val GentleFloat: SpringSpec<Float> = spring(
        dampingRatio = 0.90f,
        stiffness = 240f
    )

    val FluidOffset: SpringSpec<IntOffset> = spring(
        dampingRatio = 0.84f,
        stiffness = 380f
    )

    val FluidDp: SpringSpec<Dp> = spring(
        dampingRatio = 0.84f,
        stiffness = 380f
    )

    val FluidColor: AnimationSpec<Color> = tween(
        durationMillis = 200,
        easing = FastOutSlowInEasing
    )
}

/**
 * Surreal physics-based spring bounce click modifier.
 * Uses hardware layer scaling and optional subtle alpha with Apple spring dynamics
 * for tactile feedback without jank or relayout costs.
 *
 * Supports both standalone usage (e.g. on Buttons/IconButtons) and inline click handling.
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.95f,
    alphaDown: Float = 1f,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed && enabled) scaleDown else 1f,
            animationSpec = AppleMotion.BouncyFloat,
            label = "bounceScale"
        )
        val alpha by animateFloatAsState(
            targetValue = if (isPressed && enabled) alphaDown else 1f,
            animationSpec = AppleMotion.SnappyFloat,
            label = "bounceAlpha"
        )
        this
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    } else {
        var isPressed by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (isPressed && enabled) scaleDown else 1f,
            animationSpec = AppleMotion.BouncyFloat,
            label = "bounceScale"
        )
        val alpha by animateFloatAsState(
            targetValue = if (isPressed && enabled) alphaDown else 1f,
            animationSpec = AppleMotion.SnappyFloat,
            label = "bounceAlpha"
        )
        this
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    isPressed = true
                    waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    isPressed = false
                }
            }
    }
}

/**
 * Physics-based spring bounce press effect for components with explicit interaction sources.
 */
fun Modifier.bouncePress(
    interactionSource: MutableInteractionSource,
    scaleDown: Float = 0.96f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = AppleMotion.BouncyFloat,
        label = "bouncePress"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Apple Tab Selection Pop effect.
 * Produces a lively micro-pulse when an item or tab transitions to selected.
 */
fun Modifier.appleTabPop(selected: Boolean): Modifier = composed {
    var previousSelected by remember { mutableStateOf(selected) }
    val popScale = remember { Animatable(1f) }

    LaunchedEffect(selected) {
        if (selected && !previousSelected) {
            popScale.snapTo(0.85f)
            popScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.52f,
                    stiffness = 420f
                )
            )
        }
        previousSelected = selected
    }

    this.graphicsLayer {
        scaleX = popScale.value
        scaleY = popScale.value
    }
}

/**
 * Apple-grade directional fluid horizontal slide transition for tab switches and day changes.
 * Moving forward slides in from the right; moving backward slides in from the left.
 */
fun appleSlideHorizontally(forward: Boolean): ContentTransform {
    return if (forward) {
        (slideInHorizontally(
            animationSpec = AppleMotion.FluidOffset,
            initialOffsetX = { fullWidth -> (fullWidth * 0.85f).toInt() }
        ) + fadeIn(
            animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing)
        ) + scaleIn(
            initialScale = 0.97f,
            animationSpec = AppleMotion.FluidFloat
        )) togetherWith (slideOutHorizontally(
            animationSpec = AppleMotion.FluidOffset,
            targetOffsetX = { fullWidth -> -(fullWidth * 0.30f).toInt() }
        ) + fadeOut(
            animationSpec = tween(durationMillis = 150, easing = FastOutLinearInEasing)
        ) + scaleOut(
            targetScale = 0.97f,
            animationSpec = AppleMotion.FluidFloat
        ))
    } else {
        (slideInHorizontally(
            animationSpec = AppleMotion.FluidOffset,
            initialOffsetX = { fullWidth -> -(fullWidth * 0.85f).toInt() }
        ) + fadeIn(
            animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing)
        ) + scaleIn(
            initialScale = 0.97f,
            animationSpec = AppleMotion.FluidFloat
        )) togetherWith (slideOutHorizontally(
            animationSpec = AppleMotion.FluidOffset,
            targetOffsetX = { fullWidth -> (fullWidth * 0.30f).toInt() }
        ) + fadeOut(
            animationSpec = tween(durationMillis = 150, easing = FastOutLinearInEasing)
        ) + scaleOut(
            targetScale = 0.97f,
            animationSpec = AppleMotion.FluidFloat
        ))
    }
}

/**
 * Apple iOS Push / Pop navigation transition between screens.
 * When pushing: new view slides in from right, underlying view recedes slightly.
 * When popping: active view slides out to right, underlying view expands forward.
 */
fun applePushPopTransition(isPush: Boolean): ContentTransform {
    return if (isPush) {
        (slideInHorizontally(
            animationSpec = AppleMotion.FluidOffset,
            initialOffsetX = { fullWidth -> fullWidth }
        ) + fadeIn(
            animationSpec = tween(durationMillis = 200)
        )) togetherWith (slideOutHorizontally(
            animationSpec = AppleMotion.FluidOffset,
            targetOffsetX = { fullWidth -> -(fullWidth * 0.28f).toInt() }
        ) + fadeOut(
            animationSpec = tween(durationMillis = 180)
        ) + scaleOut(
            targetScale = 0.94f,
            animationSpec = AppleMotion.FluidFloat
        ))
    } else {
        (slideInHorizontally(
            animationSpec = AppleMotion.FluidOffset,
            initialOffsetX = { fullWidth -> -(fullWidth * 0.28f).toInt() }
        ) + fadeIn(
            animationSpec = tween(durationMillis = 200)
        ) + scaleIn(
            initialScale = 0.94f,
            animationSpec = AppleMotion.FluidFloat
        )) togetherWith (slideOutHorizontally(
            animationSpec = AppleMotion.FluidOffset,
            targetOffsetX = { fullWidth -> fullWidth }
        ) + fadeOut(
            animationSpec = tween(durationMillis = 180)
        ))
    }
}

/**
 * Apple Accordion Expand & Shrink transitions for cards, dropdowns, and target planners.
 */
fun appleAccordionEnter(): EnterTransition =
    expandVertically(
        animationSpec = spring(dampingRatio = 0.84f, stiffness = 380f),
        expandFrom = Alignment.Top
    ) + fadeIn(animationSpec = tween(durationMillis = 200))

fun appleAccordionExit(): ExitTransition =
    shrinkVertically(
        animationSpec = spring(dampingRatio = 0.84f, stiffness = 380f),
        shrinkTowards = Alignment.Top
    ) + fadeOut(animationSpec = tween(durationMillis = 160))

/**
 * Apple Sliding Segmented Control with fluid animated pill indicator.
 * Provides the signature iOS glass/elevated pill gliding across tabs with spring physics.
 */
@Composable
fun <T> AppleSlidingSegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = AppleCardSurface,
    pillColor: Color = AppleElevatedSurface,
    pillShape: Shape = RoundedCornerShape(9.dp),
    contentPadding: Dp = 3.dp,
    labelContent: @Composable (item: T, isSelected: Boolean) -> Unit
) {
    val selectedIndex = items.indexOf(selectedItem).coerceAtLeast(0)
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "apple_pill_slider"
    )

    Surface(
        modifier = modifier.defaultMinSize(minHeight = 40.dp),
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding)
        ) {
            val totalWidth = maxWidth
            val count = items.size.coerceAtLeast(1)
            val tabWidth = totalWidth / count

            // Sliding Pill
            Box(
                modifier = Modifier
                    .offset(x = tabWidth * animatedIndex)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .clip(pillShape)
                    .background(pillColor)
            )

            // Items Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .bounceClick(scaleDown = 0.94f) { onItemSelected(item) },
                        contentAlignment = Alignment.Center
                    ) {
                        labelContent(item, isSelected)
                    }
                }
            }
        }
    }
}
