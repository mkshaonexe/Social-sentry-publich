package com.example.socialsentry.presentation.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.socialsentry.ui.theme.*

@Composable
fun AboutDeveloperScreen() {
    val context = LocalContext.current
    
    // Animation states
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkGray,
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E)
                    )
                )
            )
            .systemBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Animated Header
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(800)) + slideInVertically(
                    animationSpec = tween(800, easing = FastOutSlowInEasing),
                    initialOffsetY = { -100 }
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar Circle
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        BrightPink,
                                        Color(0xFFFF6B9D),
                                        Color(0xFFFEA47F)
                                    )
                                )
                            )
                            .border(4.dp, White.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "MK",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    }
                    
                    // Developer Name
                    Text(
                        text = "MK Shaon",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = White,
                        textAlign = TextAlign.Center
                    )
                    
                    // Title Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        BrightGreen.copy(alpha = 0.3f),
                                        Color(0xFF00BCD4).copy(alpha = 0.3f)
                                    )
                                )
                            )
                            .border(1.dp, BrightGreen.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Lead Developer",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrightGreen,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Contact Cards Section
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(1000, delayMillis = 300)) + slideInVertically(
                    animationSpec = tween(1000, delayMillis = 300, easing = FastOutSlowInEasing),
                    initialOffsetY = { 100 }
                )
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Get in Touch",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Email Card
                    ContactCard(
                        icon = Icons.Rounded.Email,
                        label = "Email",
                        value = "mkshaon2024@gmail.com",
                        gradientColors = listOf(
                            Color(0xFF667eea),
                            Color(0xFF764ba2)
                        ),
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:mkshaon2024@gmail.com")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    
                    // YouTube Card
                    ContactCard(
                        icon = Icons.Rounded.PlayArrow,
                        label = "YouTube",
                        value = "@mkshaon7",
                        gradientColors = listOf(
                            Color(0xFFFF0000),
                            Color(0xFFCC0000)
                        ),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/@mkshaon7"))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open YouTube", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    
                    // Facebook Card
                    ContactCard(
                        icon = Icons.Rounded.Person,
                        label = "Facebook",
                        value = "mkshaon777",
                        gradientColors = listOf(
                            Color(0xFF1877F2),
                            Color(0xFF0D47A1)
                        ),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://facebook.com/mkshaon777"))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open Facebook", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Footer
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(1200, delayMillis = 600))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .padding(vertical = 8.dp),
                        thickness = 1.dp,
                        color = White.copy(alpha = 0.2f)
                    )
                    
                    Text(
                        text = "Made with ❤️ for Social Sentry",
                        fontSize = 14.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "© 2024 MK Shaon",
                        fontSize = 12.sp,
                        color = TextGray.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactCard(
    icon: ImageVector,
    label: String,
    value: String,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale",
        finishedListener = {
            // Reset pressed state after animation completes
            if (isPressed) {
                isPressed = false
            }
        }
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                onClick = {
                    isPressed = true
                    onClick()
                }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = gradientColors.map { it.copy(alpha = 0.15f) }
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = gradientColors.map { it.copy(alpha = 0.5f) }
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Icon Circle
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(colors = gradientColors)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Text Info
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextGray,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = value,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = White
                        )
                    }
                }
                
                // Arrow Icon
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Open",
                    tint = gradientColors.first(),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

