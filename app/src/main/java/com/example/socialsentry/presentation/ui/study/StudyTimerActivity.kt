package com.example.socialsentry.presentation.ui.study

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.socialsentry.ui.theme.SocialSentryTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StudyTimerActivity : ComponentActivity() {
    
    private var isStudying by mutableStateOf(false)
    private var studyTime by mutableStateOf(0L) // in seconds
    private var startTime by mutableStateOf(0L)
    private var isPaused by mutableStateOf(false)
    private var pausedTime by mutableStateOf(0L)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            SocialSentryTheme {
                StudyTimerScreen(
                    isStudying = isStudying,
                    studyTime = studyTime,
                    isPaused = isPaused,
                    onStartStudy = { startStudy() },
                    onPauseStudy = { pauseStudy() },
                    onResumeStudy = { resumeStudy() },
                    onStopStudy = { stopStudy() },
                    onBack = { finish() }
                )
            }
        }
    }
    
    private fun startStudy() {
        isStudying = true
        isPaused = false
        startTime = System.currentTimeMillis()
        studyTime = 0
        pausedTime = 0
        
        // Start time tracking
        startTimeTracking()
    }
    
    private fun pauseStudy() {
        isPaused = true
        pausedTime = studyTime
    }
    
    private fun resumeStudy() {
        isPaused = false
        startTime = System.currentTimeMillis() - (studyTime * 1000)
    }
    
    private fun stopStudy() {
        isStudying = false
        isPaused = false
        
        // Calculate earned time based on study time
        // 10 minutes of study = 1 minute of reels time
        val earnedMinutes = (studyTime / 600).toInt() // 600 seconds = 10 minutes
        
        if (earnedMinutes > 0) {
            val resultIntent = Intent().apply {
                putExtra("study_minutes", earnedMinutes)
                putExtra("study_time", studyTime)
            }
            setResult(Activity.RESULT_OK, resultIntent)
        }
        
        finish()
    }
    
    private fun startTimeTracking() {
        // Start a coroutine to track time
        kotlinx.coroutines.GlobalScope.launch {
            while (isStudying) {
                kotlinx.coroutines.delay(1000)
                if (!isPaused) {
                    studyTime = (System.currentTimeMillis() - startTime) / 1000
                }
            }
        }
    }
}

@Composable
fun StudyTimerScreen(
    isStudying: Boolean,
    studyTime: Long,
    isPaused: Boolean,
    onStartStudy: () -> Unit,
    onPauseStudy: () -> Unit,
    onResumeStudy: () -> Unit,
    onStopStudy: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A1A))
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                
                Text(
                    text = "Study Timer",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.width(48.dp))
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Study Status
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isStudying && !isPaused -> Color(0xFF9C27B0)
                        isPaused -> Color(0xFFFF9800)
                        else -> Color(0xFF2A2A2A)
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when {
                            isStudying && !isPaused -> "📚 Studying..."
                            isPaused -> "⏸️ Paused"
                            else -> "📖 Ready to Study"
                        },
                        fontSize = 24.sp,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = when {
                            isStudying && !isPaused -> "Keep studying to earn unblock time!"
                            isPaused -> "Study session is paused"
                            else -> "Tap start to begin studying"
                        },
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Study Timer Display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2A2A2A)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Study Time",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = formatTime(studyTime),
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Earned Time Display
            if (isStudying || studyTime > 0) {
                val earnedMinutes = (studyTime / 600).toInt() // 600 seconds = 10 minutes
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF9C27B0).copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Earned Reels Time",
                            color = Color(0xFF9C27B0),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "$earnedMinutes minutes",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "10 minutes study = 1 minute reels time",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when {
                    !isStudying -> {
                        Button(
                            onClick = onStartStudy,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF9C27B0)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Start Study")
                        }
                    }
                    isPaused -> {
                        Button(
                            onClick = onResumeStudy,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Resume")
                        }
                    }
                    else -> {
                        Button(
                            onClick = onPauseStudy,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9800)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                            Text("Pause")
                        }
                    }
                }
                
                if (isStudying) {
                    Button(
                        onClick = onStopStudy,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Stop")
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, remainingSeconds)
    } else {
        String.format("%02d:%02d", minutes, remainingSeconds)
    }
}
