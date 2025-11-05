package com.socialsentry.ai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.*
import androidx.compose.runtime.getValue
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.socialsentry.ai.SocialSentryViewModel
import com.socialsentry.ai.ui.settings.SettingsScreen
import com.socialsentry.ai.ui.settings.isAccessibilityGranted
import com.socialsentry.ai.ui.theme.SocialSentryDarkGray
import com.socialsentry.ai.ui.theme.SocialSentryGreen
import com.socialsentry.ai.ui.theme.SocialSentryLightGray
import com.socialsentry.ai.ui.theme.SocialSentryPink
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SocialSentryScreen(
    viewModel: SocialSentryViewModel,
    modifier: Modifier = Modifier
) {
    val appState by viewModel.appState.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var showWarningDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Check accessibility permission
    var isAccessibilityGranted by remember { mutableStateOf(context.isAccessibilityGranted()) }
    
    // Update permission status when activity resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityGranted = context.isAccessibilityGranted()
                // Hide dialog if permission was granted
                if (isAccessibilityGranted) {
                    showWarningDialog = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Hide dialog when going to settings
    LaunchedEffect(showSettings) {
        if (showSettings) {
            showWarningDialog = false
        }
    }
    
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    
    // Responsive sizing
    val circularButtonSize = if (screenWidth < 400.dp) {
        screenWidth * 0.6f
    } else {
        screenWidth * 0.5f
    }
    
    val titleSize = if (screenWidth < 400.dp) 32.sp else 42.sp
    val instructionSize = if (screenWidth < 400.dp) 14.sp else 16.sp
    val statusSize = if (screenWidth < 400.dp) 20.sp else 28.sp

    if (showSettings) {
        SettingsScreen(
            viewModel = viewModel,
            onClose = { showSettings = false }
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section with menu and warning icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            showSettings = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Settings",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    // Warning icon - only show if accessibility permission not granted
                    if (!isAccessibilityGranted) {
                        IconButton(
                            onClick = {
                                showWarningDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Accessibility Permission Warning",
                                tint = Color(0xFFFFA500), // Orange color for warning
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

            Spacer(modifier = Modifier.weight(0.1f))

            // Title and instruction
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                // App Title with colored text and smooth transitions
                val socialColorTarget = if (appState.isBlocked) Color.White else SocialSentryPink
                val sentryColorTarget = if (appState.isBlocked) SocialSentryGreen else Color.White
                
                val socialColor by animateColorAsState(
                    targetValue = socialColorTarget,
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    label = "socialColor"
                )
                val sentryColor by animateColorAsState(
                    targetValue = sentryColorTarget,
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    label = "sentryColor"
                )
                
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Social",
                        fontSize = titleSize,
                        fontWeight = FontWeight.Bold,
                        color = socialColor,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = "Sentry",
                        fontSize = titleSize,
                        fontWeight = FontWeight.Bold,
                        color = sentryColor
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Instruction text
                Text(
                    text = if (appState.isBlocked) "Tap To Turn Off" else "Tap To Turn On",
                    fontSize = instructionSize,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(0.2f))

            // Central circular button with animations
            Box(
                modifier = Modifier
                    .size(circularButtonSize)
                    .clickable { viewModel.toggleBlockState() },
                contentAlignment = Alignment.Center
            ) {
                AnimatedCircularButton(
                    isBlocked = appState.isBlocked,
                    size = circularButtonSize
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Status Card - Matching reference design
            androidx.compose.animation.AnimatedVisibility(
                visible = true,
                enter = androidx.compose.animation.slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    initialOffsetY = { it }
                ) + androidx.compose.animation.fadeIn(animationSpec = tween(500))
            ) {
                androidx.compose.material3.Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = SocialSentryDarkGray.copy(alpha = 0.8f)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        SocialSentryPink.copy(alpha = 0.1f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(24.dp)
                            .heightIn(min = 120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.animation.AnimatedContent(
                            targetState = appState.isBlocked,
                            transitionSpec = {
                                androidx.compose.animation.fadeIn(animationSpec = tween(300)) + 
                                androidx.compose.animation.slideInHorizontally(
                                    animationSpec = tween(300),
                                    initialOffsetX = { -it }
                                ) togetherWith androidx.compose.animation.fadeOut(animationSpec = tween(300)) + 
                                androidx.compose.animation.slideOutHorizontally(
                                    animationSpec = tween(300),
                                    targetOffsetX = { it }
                                )
                            },
                            label = "status"
                        ) { isBlocked ->
                            StatusText(isBlocked = isBlocked, fontSize = statusSize)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.3f))

            // Status message when unblocking
            if (appState.isUnblocking && appState.statusMessage.isNotEmpty()) {
                StatusIndicator(
                    message = appState.statusMessage,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }
        }
        
        // Warning Dialog
        if (showWarningDialog) {
            AccessibilityWarningDialog(
                onDismiss = { showWarningDialog = false },
                context = context
            )
        }
    }
    }
}

@Composable
fun AccessibilityWarningDialog(
    onDismiss: () -> Unit,
    context: Context
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(horizontal = 16.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            color = Color(0xFF2A2A2A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Warning icon and title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                // Warning triangle icon
                Canvas(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp)
                ) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width / 2, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFFE91E63)
                    )
                    // Draw exclamation mark
                    drawLine(
                        color = Color.White,
                        start = Offset(size.width / 2, size.height * 0.3f),
                        end = Offset(size.width / 2, size.height * 0.6f),
                        strokeWidth = 2.2.dp.toPx()
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 1.5.dp.toPx(),
                        center = Offset(size.width / 2, size.height * 0.75f)
                    )
                }
                
                Text(
                    text = "App Not Set Up Correctly",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            // Description text
            Text(
                text = "The app is not set up correctly. Please check the tutorial for proper setup instructions.",
                fontSize = 12.sp,
                color = Color(0xFFB0B0B0),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // YouTube Tutorial Card
            SetupOptionCard(
                icon = {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF00D9FF),
                        modifier = Modifier.size(20.dp)
                    )
                },
                title = "YouTube Tutorial",
                subtitle = "@mkshaon7",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=_ZM__jHfrl0&feature=youtu.be"))
                    context.startActivity(intent)
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Email Support Card
            SetupOptionCard(
                icon = {
                    Canvas(modifier = Modifier.size(20.dp)) {
                        // Simple email icon
                        drawRect(
                            color = Color(0xFF00D9FF),
                            topLeft = Offset(0f, size.height * 0.2f),
                            size = Size(size.width, size.height * 0.6f)
                        )
                        // Envelope flap
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, size.height * 0.2f)
                            lineTo(size.width / 2, size.height * 0.6f)
                            lineTo(size.width, size.height * 0.2f)
                        }
                        drawPath(
                            path = path,
                            color = Color(0xFF2A2A2A)
                        )
                    }
                },
                title = "Email Support",
                subtitle = "mkshaon2024@gmail.com",
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:mkshaon2024@gmail.com")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback if no email app is available
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Email", "mkshaon2024@gmail.com")
                        clipboard.setPrimaryClip(clip)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Website Card
            SetupOptionCard(
                icon = {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF00D9FF),
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                rotationZ = -90f
                            }
                    )
                },
                title = "Website",
                subtitle = "mkshaon.com/social_sentry",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://mkshaon.com/social_sentry"))
                    context.startActivity(intent)
                }
            )
            
            // Got it button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextButton(
                    onClick = onDismiss,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = SocialSentryPink
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Got it",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
    }
}

@Composable
fun SetupOptionCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .background(Color(0xFF383838))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier.padding(end = 10.dp)
        ) {
            icon()
        }
        
        // Title and subtitle
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF888888)
            )
        }
        
        // Arrow icon
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color(0xFF666666),
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer {
                    rotationZ = -90f
                }
        )
    }
}

@Composable
fun AnimatedCircularButton(
    isBlocked: Boolean,
    size: androidx.compose.ui.unit.Dp
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val centerSize = size * 0.4f
    
    // Animation values
    val scale by animateFloatAsState(
        targetValue = if (isBlocked) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val glowIntensity by animateFloatAsState(
        targetValue = if (isBlocked) 1f else 0.3f,
        animationSpec = tween(500),
        label = "glow"
    )
    
    val ringRotation = remember { Animatable(0f) }
    
    LaunchedEffect(isBlocked) {
        ringRotation.animateTo(
            targetValue = if (isBlocked) 360f else 0f,
            animationSpec = tween(1000, easing = EaseInOutCubic)
        )
    }
    
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
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
                color = if (isBlocked) SocialSentryGreen else Color(0xFF757575)
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
                isBlocked = isBlocked
            )
        }
        
        // Center button
        Box(
            modifier = Modifier
                .size(centerSize)
                .clip(CircleShape)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            if (isBlocked) SocialSentryGreen else Color(0xFF757575),
                            if (isBlocked) SocialSentryGreen.copy(alpha = 0.8f) else Color(0xFF757575).copy(alpha = 0.8f)
                        ),
                        radius = centerSize.value * density.density / 2
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isBlocked) Icons.Default.Close else Icons.Default.PlayArrow,
                contentDescription = if (isBlocked) "Turn Off" else "Turn On",
                modifier = Modifier.size(centerSize * 0.4f),
                tint = Color.White
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
        brush = androidx.compose.ui.graphics.Brush.radialGradient(
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
    isBlocked: Boolean
) {
    val ringCount = 3
    val ringSpacing = radius / ringCount
    
    repeat(ringCount) { index ->
        val ringRadius = radius - (index * ringSpacing)
        val ringAlpha = if (isBlocked) 0.6f - (index * 0.1f) else 0.2f - (index * 0.05f)
        
        // Dashed ring
        drawDashedRing(
            center = center,
            radius = ringRadius,
            color = if (isBlocked) SocialSentryGreen.copy(alpha = ringAlpha) else Color(0xFF757575).copy(alpha = ringAlpha),
            strokeWidth = 4.dp.toPx(),
            dashLength = 8.dp.toPx(),
            gapLength = 4.dp.toPx(),
            rotation = rotation + (index * 30f)
        )
        
        // Solid ring
        if (index < 2) {
            drawCircle(
                color = if (isBlocked) SocialSentryGreen.copy(alpha = ringAlpha * 0.5f) else Color(0xFF757575).copy(alpha = ringAlpha * 0.5f),
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
        
        val startX = center.x + radius * kotlin.math.cos(startAngle).toFloat()
        val startY = center.y + radius * kotlin.math.sin(startAngle).toFloat()
        val endX = center.x + radius * kotlin.math.cos(endAngle).toFloat()
        val endY = center.y + radius * kotlin.math.sin(endAngle).toFloat()
        
        drawLine(
            color = color,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}


@Composable
fun StatusText(
    isBlocked: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    val text = if (isBlocked) "Scrolling is Blocked" else "Scrolling is Unblocked"
    val words = text.split(" ")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        words.forEach { word ->
            androidx.compose.animation.AnimatedContent(
                targetState = word,
                transitionSpec = {
                    androidx.compose.animation.scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) togetherWith androidx.compose.animation.scaleOut(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                },
                label = "word"
            ) { currentWord ->
                Text(
                    text = currentWord,
                    color = if (currentWord == "Blocked" || currentWord == "Unlocked") SocialSentryPink else Color.White,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StatusIndicator(
    message: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = SocialSentryDarkGray,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(SocialSentryGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow, // Using play as shield substitute
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(10.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = message,
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

