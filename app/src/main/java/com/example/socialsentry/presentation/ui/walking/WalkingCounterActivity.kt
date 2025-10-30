package com.example.socialsentry.presentation.ui.walking

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.socialsentry.ui.theme.SocialSentryTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class WalkingCounterActivity : ComponentActivity() {
    
    private lateinit var sensorManager: SensorManager
    private var stepCounterSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var stepDetector: WalkingStepDetector? = null
    
    private var isWalking by mutableStateOf(false)
    private var stepCount by mutableStateOf(0)
    private var walkingTime by mutableStateOf(0L) // in seconds
    private var walkingDistance by mutableStateOf(0.0) // in meters
    private var startTime by mutableStateOf(0L)
    private var initialStepCount by mutableStateOf(0)
    
    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let { sensorEvent ->
                when (sensorEvent.sensor.type) {
                    Sensor.TYPE_STEP_COUNTER -> {
                        if (isWalking) {
                            val totalSteps = sensorEvent.values[0].toInt()
                            if (initialStepCount == 0) {
                                // First reading - set as baseline
                                initialStepCount = totalSteps
                            }
                            stepCount = totalSteps - initialStepCount
                            // Update distance based on steps (average step length ~0.7m)
                            walkingDistance = stepCount * 0.7
                        }
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        if (isWalking) {
                            stepDetector?.onAccelerometerData(sensorEvent.values)
                        }
                    }
                }
            }
        }
        
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        stepDetector = WalkingStepDetector()
        
        setContent {
            SocialSentryTheme {
                var hasPermission by remember { mutableStateOf(false) }
                val context = LocalContext.current
                
                LaunchedEffect(Unit) {
                    hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACTIVITY_RECOGNITION
                    ) == PackageManager.PERMISSION_GRANTED
                }
                
                if (hasPermission) {
                    WalkingCounterScreen(
                        isWalking = isWalking,
                        stepCount = stepCount,
                        walkingTime = walkingTime,
                        walkingDistance = walkingDistance,
                        onStartWalking = { startWalking() },
                        onStopWalking = { stopWalking() },
                        onBack = { finish() }
                    )
                } else {
                    PermissionRequestScreen(
                        onPermissionGranted = { hasPermission = true },
                        onBack = { finish() }
                    )
                }
            }
        }
    }
    
    private fun startWalking() {
        isWalking = true
        startTime = System.currentTimeMillis()
        stepCount = 0
        walkingTime = 0
        walkingDistance = 0.0
        
        // Get initial step count to calculate difference
        stepCounterSensor?.let { sensor ->
            sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_UI)
            // Get current step count as baseline
            val currentSteps = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            if (currentSteps != null) {
                // We'll get the initial count from the first sensor event
            }
        }
        accelerometerSensor?.let { sensor ->
            sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        
        // Start time tracking
        startTimeTracking()
    }
    
    private fun stopWalking() {
        isWalking = false
        
        // Unregister sensors
        sensorManager.unregisterListener(sensorEventListener)
        
        // Calculate earned time based on distance walked
        // 250 meters = 1 minute of reels time
        val earnedMinutes = (walkingDistance / 250.0).toInt()
        
        if (earnedMinutes > 0) {
            val resultIntent = Intent().apply {
                putExtra("walking_minutes", earnedMinutes)
                putExtra("step_count", stepCount)
                putExtra("walking_time", walkingTime)
                putExtra("walking_distance", walkingDistance)
            }
            setResult(Activity.RESULT_OK, resultIntent)
        }
        
        finish()
    }
    
    private fun startTimeTracking() {
        // Start a coroutine to track time
        kotlinx.coroutines.GlobalScope.launch {
            while (isWalking) {
                kotlinx.coroutines.delay(1000)
                walkingTime = (System.currentTimeMillis() - startTime) / 1000
            }
        }
    }
    
    
    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(sensorEventListener)
    }
}

@Composable
fun WalkingCounterScreen(
    isWalking: Boolean,
    stepCount: Int,
    walkingTime: Long,
    walkingDistance: Double,
    onStartWalking: () -> Unit,
    onStopWalking: () -> Unit,
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
                    text = "Walking Counter",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.width(48.dp))
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Walking Status
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isWalking) Color(0xFF00BCD4) else Color(0xFF2A2A2A)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isWalking) "🚶‍♂️ Walking..." else "⏸️ Not Walking",
                        fontSize = 24.sp,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = if (isWalking) "Keep walking to earn unblock time!" else "Tap start to begin walking",
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    title = "Steps",
                    value = stepCount.toString(),
                    icon = "👣"
                )
                
                StatCard(
                    title = "Time",
                    value = formatTime(walkingTime),
                    icon = "⏱️"
                )
                
                StatCard(
                    title = "Distance",
                    value = "${String.format("%.1f", walkingDistance)}m",
                    icon = "📏"
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Earned Time Display
            if (isWalking || stepCount > 0) {
                val earnedMinutes = (walkingDistance / 250.0).toInt()
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Earned Reels Time",
                            color = Color(0xFF4CAF50),
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
                            text = "250m walking = 1 min reels time",
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
                if (!isWalking) {
                    Button(
                        onClick = onStartWalking,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00BCD4)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Start Walking")
                    }
                } else {
                    Button(
                        onClick = onStopWalking,
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
                        Text("Stop Walking")
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: String
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A2A)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                fontSize = 24.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = value,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun PermissionRequestScreen(
    onPermissionGranted: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onPermissionGranted()
        } else {
            Toast.makeText(context, "Permission required for walking detection", Toast.LENGTH_LONG).show()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A1A))
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🚶‍♂️",
                fontSize = 64.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Walking Detection Permission",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "We need permission to detect your walking activity to earn unblock time.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00BCD4)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Text("Grant Permission")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(onClick = onBack) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

private fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}
