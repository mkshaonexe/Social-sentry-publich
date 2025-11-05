package com.socialsentry.ai.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.socialsentry.ai.R
import com.socialsentry.ai.SocialSentryViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SocialSentryViewModel = koinViewModel(),
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val appSettings by viewModel.appSettings.collectAsState()
    var isReelsBlockExpanded by remember { mutableStateOf(false) }
    var isDeveloperModeExpanded by remember { mutableStateOf(false) }
    var isAccessibilityGranted by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var developerModeClicks by remember { mutableStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(lifecycleState) {
        when (lifecycleState) {
            Lifecycle.State.RESUMED -> {
                isAccessibilityGranted = context.isAccessibilityGranted()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header with back button and warning icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Warning icon (only show if accessibility not granted)
            if (!isAccessibilityGranted) {
                IconButton(
                    onClick = { showPermissionDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Missing Permissions",
                        tint = Color(0xFFFF9800), // Orange
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Reels Block Card (Expandable)
        ReelsBlockCard(
            isExpanded = isReelsBlockExpanded,
            onExpandToggle = { isReelsBlockExpanded = !isReelsBlockExpanded },
            appSettings = appSettings,
            viewModel = viewModel,
            isAccessibilityGranted = isAccessibilityGranted
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Developer Mode Card with unlock functionality
        DeveloperModeCard(
            isExpanded = isDeveloperModeExpanded,
            isUnlocked = appSettings.developerModeUnlocked,
            autoHideAdsEnabled = appSettings.autoHideAds,
            isAccessibilityGranted = isAccessibilityGranted,
            onExpandToggle = { 
                if (appSettings.developerModeUnlocked) {
                    isDeveloperModeExpanded = !isDeveloperModeExpanded
                }
            },
            onCardClick = {
                if (!appSettings.developerModeUnlocked) {
                    developerModeClicks++
                    
                    if (developerModeClicks >= 7) {
                        viewModel.updateDeveloperModeUnlocked(true)
                        isDeveloperModeExpanded = true
                        android.widget.Toast.makeText(
                            context,
                            "🎉 Developer Mode Unlocked!",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    isDeveloperModeExpanded = !isDeveloperModeExpanded
                }
            },
            onAutoHideAdsToggle = { viewModel.updateAutoHideAds(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // App Version Card
        AppVersionCard(
            versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        )
    }
    
    // Permission Dialog
    if (showPermissionDialog) {
        PermissionDialog(
            onDismiss = { showPermissionDialog = false },
            onGrantClick = {
                showPermissionDialog = false
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        )
    }
}

@Composable
fun PermissionDialog(
    onDismiss: () -> Unit,
    onGrantClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2C2C2E),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFFF6B6B), // Red
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Missing Permissions (1)",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "The following permissions are required for the app to function properly:",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Permission item
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFF3C3C3E),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color(0xFFFF6B6B), // Red dot
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Accessibility Service",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Button(
                        onClick = onGrantClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "Grant",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Close",
                    color = Color(0xFFFF6B6B) // Red
                )
            }
        }
    )
}

@Composable
fun DeveloperModeCard(
    isExpanded: Boolean,
    isUnlocked: Boolean,
    autoHideAdsEnabled: Boolean,
    isAccessibilityGranted: Boolean,
    onExpandToggle: () -> Unit,
    onCardClick: () -> Unit,
    onAutoHideAdsToggle: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2C2C2E)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Header - Simple text, no icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCardClick)
                    .padding(20.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Developer Mode",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }

            // Expandable content (only shown when unlocked)
            AnimatedVisibility(
                visible = isExpanded && isUnlocked,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    // Auto-Hide Ads Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = autoHideAdsEnabled,
                                enabled = isAccessibilityGranted,
                                role = Role.Switch,
                                onValueChange = onAutoHideAdsToggle
                            )
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Auto-Hide Ads",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                            Text(
                                text = if (autoHideAdsEnabled) 
                                    "Automatically hide Facebook ads" 
                                else 
                                    "Turn on to hide sponsored posts",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = autoHideAdsEnabled,
                            onCheckedChange = onAutoHideAdsToggle,
                            enabled = isAccessibilityGranted
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun AccessibilityWarningBanner(
    onEnableClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF9800).copy(alpha = 0.2f) // Orange warning background
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEnableClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = Color(0xFFFF9800), // Orange
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Accessibility Required",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap to enable accessibility service",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFFCC80) // Light orange
                )
            }
        }
    }
}

@Composable
fun ReelsBlockCard(
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    appSettings: com.socialsentry.ai.datastore.AppSettings,
    viewModel: SocialSentryViewModel,
    isAccessibilityGranted: Boolean
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "arrow rotation"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2C2C2E)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpandToggle)
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reels Block",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(rotationAngle)
                )
            }

            // Expandable content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    // Instagram
                    AppToggleCard(
                        appName = "Instagram",
                        isBlocked = appSettings.instagram.blocked,
                        enabled = isAccessibilityGranted,
                        iconRes = R.drawable.ic_instagram,
                        onToggle = { viewModel.updateInstagram(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // YouTube
                    AppToggleCard(
                        appName = "YouTube",
                        isBlocked = appSettings.youtube.blocked,
                        enabled = isAccessibilityGranted,
                        iconRes = R.drawable.ic_youtube,
                        onToggle = { viewModel.updateYoutube(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // TikTok
                    AppToggleCard(
                        appName = "TikTok",
                        isBlocked = appSettings.tiktok.blocked,
                        enabled = isAccessibilityGranted,
                        iconRes = R.drawable.ic_tiktok,
                        onToggle = { viewModel.updateTikTok(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Facebook
                    AppToggleCard(
                        appName = "Facebook",
                        isBlocked = appSettings.facebook.blocked,
                        enabled = isAccessibilityGranted,
                        iconRes = R.drawable.ic_facebook,
                        onToggle = { viewModel.updateFacebook(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Facebook Lite
                    AppToggleCard(
                        appName = "Facebook Lite",
                        isBlocked = appSettings.facebookLite.blocked,
                        enabled = isAccessibilityGranted,
                        iconRes = R.drawable.ic_facebook_lite,
                        onToggle = { viewModel.updateFacebookLite(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun AppVersionCard(versionName: String) {
    // Fixed last update time for all users
    val lastUpdateTime = "9:38 pm 04 Nov 2025"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2C2C2E)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "App Version",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = versionName,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = Color(0xFF00BCD4)
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "Last updated: $lastUpdateTime",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun AccessibilityServiceCard(isAccessibilityGranted: Boolean) {
    val context = LocalContext.current

    val update: () -> Unit = {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (!isAccessibilityGranted)
                MaterialTheme.colorScheme.errorContainer
            else
                Color(0xFF4CAF50).copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .toggleable(
                value = isAccessibilityGranted,
                role = Role.Switch,
                onValueChange = { update() }
            ),
        shape = RoundedCornerShape(15.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = Icons.Rounded.Accessibility,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Accessibility Service",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (isAccessibilityGranted) {
                        "Granted. Click to view Settings."
                    } else {
                        "Not granted. Click to enable blocking."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Switch(
                checked = isAccessibilityGranted,
                onCheckedChange = { update() }
            )
        }
    }
}

@Composable
fun AppToggleCard(
    appName: String,
    isBlocked: Boolean,
    enabled: Boolean,
    iconRes: Int,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = isBlocked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onToggle
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                Text(
                    text = if (isBlocked) "Blocking enabled" else "Not blocking",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        Switch(
            checked = isBlocked,
            onCheckedChange = onToggle,
            enabled = enabled
        )
    }
}

@Composable
fun MasterBlockingCard(
    isBlocked: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isBlocked)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .toggleable(
                value = isBlocked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onToggle
            ),
        shape = RoundedCornerShape(15.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = Icons.Default.Block,
                contentDescription = null,
                tint = if (isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Block All Reels",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (isBlocked) {
                        "All selected apps are being blocked"
                    } else {
                        if (enabled) "Turn on to block all selected apps" else "Enable accessibility service first"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Switch(
                checked = isBlocked,
                onCheckedChange = onToggle,
                enabled = enabled
            )
        }
    }
}

fun Context.isAccessibilityGranted(): Boolean {
    val am = this.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val runningServices =
        am.getEnabledAccessibilityServiceList(AccessibilityEvent.TYPE_VIEW_CLICKED)
    return runningServices.any {
        it.id == "com.socialsentry.ai/.accessibility.SocialSentryAccessibilityService"
    }
}

