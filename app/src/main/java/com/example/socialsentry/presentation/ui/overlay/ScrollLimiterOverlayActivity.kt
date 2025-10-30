package com.example.socialsentry.presentation.ui.overlay

import android.app.Activity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.socialsentry.ui.theme.SocialSentryTheme

class ScrollLimiterOverlayActivity : ComponentActivity() {
    
    private var countDownTimer: CountDownTimer? = null
    private var remainingSeconds = mutableStateOf(30)
    private var canClose = mutableStateOf(false)
    private var appName = mutableStateOf("Social Media")
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make this activity appear on top of everything, even lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        
        // Get app name and remaining time from intent
        val appNameFromIntent = intent.getStringExtra("APP_NAME") ?: "Social Media"
        appName.value = appNameFromIntent
        val initialSeconds = intent.getIntExtra("REMAINING_SECONDS", 30)
        remainingSeconds.value = initialSeconds
        
        // Start countdown timer
        startCountdown(initialSeconds)
        
        setContent {
            SocialSentryTheme {
                ScrollLimiterOverlay(
                    appName = appName.value,
                    remainingSeconds = remainingSeconds.value,
                    canClose = canClose.value,
                    onClose = { 
                        // User can always close the popup
                        // But app will remain blocked until break ends
                        finish()
                    }
                )
            }
        }
    }
    
    private fun startCountdown(seconds: Int) {
        val milliseconds = (seconds * 1000).toLong()
        countDownTimer = object : CountDownTimer(milliseconds, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds.value = (millisUntilFinished / 1000).toInt() + 1
            }
            
            override fun onFinish() {
                remainingSeconds.value = 0
                canClose.value = true
                // Auto-close after countdown finishes
                finish()
            }
        }.start()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
    
    // Allow back button to close the popup at any time
    // User can close, but Facebook will be blocked until 30 seconds complete
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
    }
}

@Composable
fun ScrollLimiterOverlay(
    appName: String,
    remainingSeconds: Int,
    canClose: Boolean,
    onClose: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F3460)
                    )
                )
            )
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated timer icon
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale),
                shape = CircleShape,
                color = Color(0xFFE94560).copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "Break Time",
                        tint = Color(0xFFE94560),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
            
            // Title
            Text(
                text = "Take a Break!",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            // Message
            Text(
                text = "You've been scrolling $appName for over ${if (appName == "Facebook") "2 minutes" else "1 minute"}.\nLet's give your mind a rest.",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            
            // Countdown
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE94560).copy(alpha = 0.15f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$remainingSeconds",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE94560)
                    )
                    Text(
                        text = "seconds remaining",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Motivational quote
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF533483).copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = "\"The best way to take care of the future is to take care of the present moment.\" - Thích Nhất Hạnh",
                    fontSize = 14.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(20.dp),
                    lineHeight = 20.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Close button (always visible - user can dismiss but Facebook stays blocked)
            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canClose) Color(0xFF00D9FF) else Color(0xFF888888)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (canClose) "Continue" else "Close (Break continues)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

