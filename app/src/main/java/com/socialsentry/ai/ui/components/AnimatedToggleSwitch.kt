package com.socialsentry.ai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.socialsentry.ai.ui.theme.BrightPink
import com.socialsentry.ai.ui.theme.BrightGreen
import com.socialsentry.ai.ui.theme.DarkGray
import com.socialsentry.ai.ui.theme.White
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedToggleSwitch(
    isEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val size = 200.dp
    val centerSize = 80.dp
    
    // Animation values
    val scale by animateFloatAsState(
        targetValue = if (isEnabled) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val glowIntensity by animateFloatAsState(
        targetValue = if (isEnabled) 1f else 0.3f,
        animationSpec = tween(500),
        label = "glow"
    )
    
    val ringRotation = remember { Animatable(0f) }
    
    LaunchedEffect(isEnabled) {
        ringRotation.animateTo(
            targetValue = if (isEnabled) 360f else 0f,
            animationSpec = tween(1000, easing = EaseInOutCubic)
        )
    }
    
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        // Outer glow effect
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawGlowEffect(
                center = Offset(this.size.width / 2, this.size.height / 2),
                radius = this.size.width / 2,
                intensity = glowIntensity,
                color = if (isEnabled) BrightGreen else Color(0xFF757575) // Gray instead of pink
            )
        }
        
        // Animated rings
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawAnimatedRings(
                center = Offset(this.size.width / 2, this.size.height / 2),
                radius = this.size.width / 2,
                rotation = ringRotation.value,
                isEnabled = isEnabled
            )
        }
        
        // Center button
        Box(
            modifier = Modifier
                .size(centerSize)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isEnabled) BrightGreen else Color(0xFF757575), // Gray instead of pink
                            if (isEnabled) BrightGreen.copy(alpha = 0.8f) else Color(0xFF757575).copy(alpha = 0.8f)
                        ),
                        radius = centerSize.value * density.density / 2
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isEnabled) Icons.Default.Close else Icons.Default.PlayArrow,
                contentDescription = if (isEnabled) "Close" else "Play",
                modifier = Modifier.size(32.dp),
                tint = White
            )
        }
    }
}

private fun DrawScope.drawGlowEffect(
    center: Offset,
    radius: Float,
    intensity: Float,
    color: Color
) {
    val glowRadius = radius * 1.5f
    val glowColor = color.copy(alpha = intensity * 0.3f)
    
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                glowColor,
                glowColor.copy(alpha = 0f)
            ),
            radius = glowRadius
        ),
        radius = glowRadius,
        center = center
    )
}

private fun DrawScope.drawAnimatedRings(
    center: Offset,
    radius: Float,
    rotation: Float,
    isEnabled: Boolean
) {
    val ringCount = 3
    val ringSpacing = radius / ringCount
    
    repeat(ringCount) { index ->
        val ringRadius = radius - (index * ringSpacing)
        val ringAlpha = if (isEnabled) 0.6f - (index * 0.1f) else 0.2f - (index * 0.05f)
        
        // Dashed ring
        drawDashedRing(
            center = center,
            radius = ringRadius,
            color = if (isEnabled) BrightGreen.copy(alpha = ringAlpha) else Color(0xFF757575).copy(alpha = ringAlpha), // Gray instead of pink
            strokeWidth = 4.dp.toPx(),
            dashLength = 8.dp.toPx(),
            gapLength = 4.dp.toPx(),
            rotation = rotation + (index * 30f)
        )
        
        // Solid ring
        if (index < 2) {
            drawCircle(
                color = if (isEnabled) BrightGreen.copy(alpha = ringAlpha * 0.5f) else Color(0xFF757575).copy(alpha = ringAlpha * 0.5f), // Gray instead of pink
                radius = ringRadius - 8.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

private fun DrawScope.drawDashedRing(
    center: Offset,
    radius: Float,
    color: Color,
    strokeWidth: Float,
    dashLength: Float,
    gapLength: Float,
    rotation: Float
) {
    val circumference = 2 * kotlin.math.PI * radius
    val dashCount = (circumference / (dashLength + gapLength)).toInt()
    val angleStep = 360f / dashCount
    
    for (i in 0 until dashCount) {
        val startAngle = (i * angleStep + rotation) * kotlin.math.PI / 180f
        val endAngle = ((i * angleStep + rotation) + (dashLength / circumference * 360f)) * kotlin.math.PI / 180f
        
        val startX = center.x + radius * cos(startAngle).toFloat()
        val startY = center.y + radius * sin(startAngle).toFloat()
        val endX = center.x + radius * cos(endAngle).toFloat()
        val endY = center.y + radius * sin(endAngle).toFloat()
        
        drawLine(
            color = color,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

