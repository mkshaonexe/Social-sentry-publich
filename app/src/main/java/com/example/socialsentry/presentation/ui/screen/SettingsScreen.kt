package com.example.socialsentry.presentation.ui.screen

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.socialsentry.data.model.SocialApp
import com.example.socialsentry.presentation.ui.components.AccessibilityServiceCard
import com.example.socialsentry.presentation.ui.components.SocialAppCard
import com.example.socialsentry.presentation.ui.components.SettingsPermissionDialog
import com.example.socialsentry.presentation.viewmodel.SocialSentryViewModel
import com.example.socialsentry.util.PermissionChecker
import org.koin.androidx.compose.koinViewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.rememberCoroutineScope
import android.widget.Toast
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import android.content.SharedPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SocialSentryViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    val scope = rememberCoroutineScope()
    
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var isManagedAppsExpanded by remember { mutableStateOf(false) }
    var isGameDashboardExpanded by remember { mutableStateOf(false) }
    var showAccessibilityPanel by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // Developer mode state
    var developerClickCount by remember { mutableStateOf(0) }
    var isDeveloperMode by remember { mutableStateOf(false) }
    var showDeveloperDropdown by remember { mutableStateOf(false) }
    var showDeveloperDialog by remember { mutableStateOf(false) }
    
    // Manual time entry state
    var manualTimeText by remember { mutableStateOf("") }
    var showManualTimeDialog by remember { mutableStateOf(false) }
    
    // Check permission status
    var permissionStatus by remember { mutableStateOf(PermissionChecker.getPermissionStatus(context)) }
    
    // Check accessibility service status and permissions
    LaunchedEffect(lifecycleState) {
        when (lifecycleState) {
            Lifecycle.State.RESUMED -> {
                isAccessibilityEnabled = context.isAccessibilityServiceEnabled()
                permissionStatus = PermissionChecker.getPermissionStatus(context)
            }
            else -> {}
        }
    }
    
    // Load developer mode state from SharedPreferences
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("developer_mode", Context.MODE_PRIVATE)
        isDeveloperMode = prefs.getBoolean("is_developer_mode", false)
    }
    
    // Developer mode click handler
    fun handleDeveloperClick() {
        developerClickCount++
        if (developerClickCount >= 7) {
            isDeveloperMode = true
            developerClickCount = 0
            Toast.makeText(context, "You are now a developer", Toast.LENGTH_SHORT).show()
            
            // Save developer mode state
            val prefs = context.getSharedPreferences("developer_mode", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("is_developer_mode", true).apply()
        }
    }
    
    val socialApps = listOf(
        settings.youtube,
        settings.facebook,
        settings.instagram,
        settings.tiktok,
        settings.pinterest
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        // Back button in top left with proper spacing
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp)
            )
        }

        // Top-right warning icon to show permission dialog when permissions are missing
        if (!permissionStatus.hasAllPermissions) {
            IconButton(
                onClick = { showPermissionDialog = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = "Missing Permissions",
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Accessibility Service panel shown on demand via the top-right icon
        if (showAccessibilityPanel && !isAccessibilityEnabled) {
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    AccessibilityServiceCard(
                        isServiceEnabled = isAccessibilityEnabled
                    )
                }
            }
        }
        
        item {
            // Reels Block Header - Clickable to expand/collapse
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { isManagedAppsExpanded = !isManagedAppsExpanded },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reels Block",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Icon(
                        imageVector = if (isManagedAppsExpanded) 
                            Icons.Default.KeyboardArrowUp 
                        else 
                            Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isManagedAppsExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
        
        // Expandable Apps List
        if (isManagedAppsExpanded) {
            items(socialApps) { app ->
                SocialAppCard(
                    app = app,
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    onToggleBlocking = { isBlocked ->
                        viewModel.toggleAppBlocking(app.name, isBlocked)
                    }
                )
            }
        }
        // Temporary Unblock section is hidden as requested
        
        // Scroll Limiter section is hidden as requested

        // Developer Mode - Clickable to expand/collapse and access developer features
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { 
                        if (isDeveloperMode) {
                            isGameDashboardExpanded = !isGameDashboardExpanded
                        } else {
                            handleDeveloperClick()
                        }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Developer Mode",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    if (isDeveloperMode) {
                        Icon(
                            imageVector = if (isGameDashboardExpanded)
                                Icons.Default.KeyboardArrowUp
                            else
                                Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isGameDashboardExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        // App Version Information
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "App Version",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "0.0.99",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00BCD4)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Last updated: ${getLastUpdateDate()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Developer Mode Expanded Content - Game Dashboard Editor
        if (isGameDashboardExpanded && isDeveloperMode) {
            item {
                val dash = settings.gameDashboard
                val bannerPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        viewModel.updateGameDashboard { it.copy(bannerImageUri = uri.toString()) }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Basic info
                        OutlinedTextField(
                            value = dash.playerName,
                            onValueChange = { v -> viewModel.updateGameDashboard { it.copy(playerName = v) } },
                            label = { Text("Player name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = dash.title,
                            onValueChange = { v -> viewModel.updateGameDashboard { it.copy(title = v) } },
                            label = { Text("Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = dash.rank,
                            onValueChange = { v -> viewModel.updateGameDashboard { it.copy(rank = v) } },
                            label = { Text("Rank") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = dash.quote,
                            onValueChange = { v -> viewModel.updateGameDashboard { it.copy(quote = v) } },
                            label = { Text("Quote") },
                            singleLine = false,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Numbers
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = dash.currentXp.toString(),
                                onValueChange = { v -> v.toIntOrNull()?.let { n -> viewModel.updateGameDashboard { it.copy(currentXp = n) } } },
                                label = { Text("Current XP") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = dash.maxXp.toString(),
                                onValueChange = { v -> v.toIntOrNull()?.let { n -> viewModel.updateGameDashboard { it.copy(maxXp = n) } } },
                                label = { Text("Max XP") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = dash.goldCredits.toString(),
                            onValueChange = { v -> v.toIntOrNull()?.let { n -> viewModel.updateGameDashboard { it.copy(goldCredits = n) } } },
                            label = { Text("Gold credits") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Stats (0..1)
                        @Composable
                        fun StatSlider(label: String, value: Float, onChange: (Float) -> Unit) {
                            Column {
                                Text(label, color = MaterialTheme.colorScheme.onBackground)
                                Slider(
                                    value = value,
                                    onValueChange = onChange,
                                    valueRange = 0f..1f
                                )
                            }
                        }
                        StatSlider("FIT", dash.statFit) { v -> viewModel.updateGameDashboard { it.copy(statFit = v) } }
                        StatSlider("SOC", dash.statSoc) { v -> viewModel.updateGameDashboard { it.copy(statSoc = v) } }
                        StatSlider("INT", dash.statInt) { v -> viewModel.updateGameDashboard { it.copy(statInt = v) } }
                        StatSlider("DIS", dash.statDis) { v -> viewModel.updateGameDashboard { it.copy(statDis = v) } }
                        StatSlider("FOC", dash.statFoc) { v -> viewModel.updateGameDashboard { it.copy(statFoc = v) } }
                        StatSlider("FIN", dash.statFin) { v -> viewModel.updateGameDashboard { it.copy(statFin = v) } }

                        // Photo Selection - Single button for changing photo
                        val photoPicker = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.GetContent()
                        ) { uri ->
                            if (uri != null) {
                                // Set as banner (full background) by default, user can change preference
                                viewModel.updateGameDashboard { it.copy(bannerImageUri = uri.toString()) }
                            }
                        }
                        
                        OutlinedButton(
                            onClick = { photoPicker.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📷 Change Photo")
                        }
                        
                        // Manual Time Entry Section
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Text(
                            text = "Manual Time Entry",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = manualTimeText,
                                onValueChange = { input ->
                                    manualTimeText = input.filter { it.isDigit() }.take(3)
                                },
                                label = { Text("Minutes") },
                                placeholder = { Text("0") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            
                            Button(
                                onClick = {
                                    val minutes = manualTimeText.toIntOrNull() ?: 0
                                    if (minutes > 0) {
                                        showManualTimeDialog = true
                                    } else {
                                        Toast.makeText(context, "Enter minutes > 0", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00BCD4)
                                )
                            ) {
                                Text("Add Time")
                            }
                        }
                        
                        // Quick add buttons for manual time
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(1, 5, 10, 30).forEach { quick ->
                                OutlinedButton(
                                    onClick = {
                                        val current = manualTimeText.toIntOrNull() ?: 0
                                        manualTimeText = (current + quick).toString()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF00BCD4)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00BCD4))
                                ) {
                                    Text("+${quick}m")
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Tutorial Reset Option (Developer Mode)
        if (isDeveloperMode) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { 
                            scope.launch {
                                viewModel.resetTutorial()
                                Toast.makeText(context, "Tutorial reset! Restart app to see tutorial again.", Toast.LENGTH_LONG).show()
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reset Tutorial",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Show tutorial again for first-time users",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Tutorial",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        }

        // Developer Advanced Settings Dialog
        if (showDeveloperDialog) {
            AlertDialog(
                onDismissRequest = { showDeveloperDialog = false },
                title = { Text("Developer Settings") },
                text = { 
                    Text("Advanced developer options:\n\n• Reset all settings\n• Clear app data\n• Force accessibility restart\n• Debug logging\n• Export logs\n• Performance metrics") 
                },
                confirmButton = {
                    Button(
                        onClick = { 
                            showDeveloperDialog = false
                            Toast.makeText(context, "Developer settings accessed", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeveloperDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Manual Time Entry Confirmation Dialog
        if (showManualTimeDialog) {
            AlertDialog(
                onDismissRequest = { showManualTimeDialog = false },
                title = { Text("Confirm Manual Time Entry") },
                text = { 
                    Text("Are you sure you want to add ${manualTimeText} minutes of unblock time?") 
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val minutes = manualTimeText.toIntOrNull() ?: 0
                            scope.launch {
                                try {
                                    viewModel.addManualUnblockMinutes(minutes)
                                    Toast.makeText(context, "Added $minutes minutes successfully!", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error adding minutes: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                            showManualTimeDialog = false
                            manualTimeText = ""
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00BCD4)
                        )
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showManualTimeDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Permission Dialog
        if (showPermissionDialog) {
            SettingsPermissionDialog(
                permissionStatus = permissionStatus,
                onDismiss = { showPermissionDialog = false }
            )
        }
    }
}

private fun Context.isAccessibilityServiceEnabled(): Boolean {
    val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
        AccessibilityEvent.TYPE_VIEW_CLICKED
    )
    
    // Debug: Log all enabled services
    enabledServices.forEach { service ->
        android.util.Log.d("SocialSentry", "Enabled accessibility service: ${service.id}")
    }
    
    val targetServiceId = "com.example.socialsentry/.service.SocialSentryAccessibilityService"
    val isEnabled = enabledServices.any { serviceInfo ->
        serviceInfo.id == targetServiceId
    }
    
    android.util.Log.d("SocialSentry", "Looking for service: $targetServiceId, found: $isEnabled")
    return isEnabled
}

private fun getLastUpdateDate(): String {
    // Return the last update date - you can update this when you release new versions
    return "5:24 PM, October 29, 2025"
}
