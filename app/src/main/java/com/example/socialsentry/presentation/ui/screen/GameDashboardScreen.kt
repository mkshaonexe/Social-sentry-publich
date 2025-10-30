package com.example.socialsentry.presentation.ui.screen

import com.example.socialsentry.R
import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.socialsentry.ui.theme.*
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.socialsentry.presentation.viewmodel.SocialSentryViewModel
import org.koin.androidx.compose.koinViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.socialsentry.data.datastore.YouTubeTrackingDataStore
import com.example.socialsentry.data.model.YouTubeStats
import com.example.socialsentry.data.model.VideoCategory
import com.example.socialsentry.data.model.YouTubeSession
import org.koin.compose.koinInject
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.socialsentry.manager.AppUsageManager
import com.example.socialsentry.data.model.AppUsageStats
import com.example.socialsentry.data.model.AppCategory
import com.example.socialsentry.util.PermissionChecker
import android.content.Context
import androidx.compose.ui.platform.LocalContext

@Composable
fun GameDashboardScreen(
    viewModel: SocialSentryViewModel = koinViewModel(),
    youtubeDataStore: YouTubeTrackingDataStore = koinInject()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val dashboard = settings.gameDashboard
    val youtubeStats by youtubeDataStore.statsFlow.collectAsStateWithLifecycle(initialValue = YouTubeStats())
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .systemBarsPadding()
    ) {
        if (isLandscape) {
            // Landscape: Show Statistics and System sections
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Quote Section with animation
                AnimatedSection(visible = visible, delay = 0) {
                    QuoteSection(text = dashboard.quote)
                }
                
                // Statistics Section with Radar Chart
                AnimatedSection(visible = visible, delay = 100) {
                    StatisticsSection(
                        stats = listOf(
                            dashboard.statFit,
                            dashboard.statSoc,
                            dashboard.statInt,
                            dashboard.statDis,
                            dashboard.statFoc,
                            dashboard.statFin
                        )
                    )
                }
                
                // Screen Time Section
                AnimatedSection(visible = visible, delay = 200) {
                    ScreenTimeSection()
                }
                
                // System Section
                AnimatedSection(visible = visible, delay = 300) {
                    SystemSection()
                }
                
                // YouTube Stats Section
                AnimatedSection(visible = visible, delay = 400) {
                    YouTubeStatsSection(youtubeStats)
                }
            }
        } else {
            // Portrait: Show Player Profile prominently
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Player Section with animation
                AnimatedSection(visible = visible, delay = 0) {
                    PlayerSection(
                        playerName = dashboard.playerName,
                        title = dashboard.title,
                        rank = dashboard.rank,
                        currentXp = dashboard.currentXp,
                        maxXp = dashboard.maxXp,
                        goldCredits = dashboard.goldCredits
                    )
                }
                
                // Screen Time Section
                AnimatedSection(visible = visible, delay = 150) {
                    ScreenTimeSection()
                }
                
                // YouTube Stats Summary
                AnimatedSection(visible = visible, delay = 200) {
                    YouTubeQuickStatsSection(
                        quote = dashboard.quote,
                        youtubeStats = youtubeStats
                    )
                }
                
                // YouTube Stats Section
                AnimatedSection(visible = visible, delay = 250) {
                    YouTubeStatsSection(youtubeStats)
                }
            }
        }
    }
}

@Composable
fun AnimatedSection(
    visible: Boolean,
    delay: Long = 0,
    content: @Composable () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(visible) {
        if (visible) {
            delay(delay)
            isVisible = true
        }
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        ) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        ),
        exit = fadeOut()
    ) {
        content()
    }
}

@Composable
fun PlayerSection(
    playerName: String,
    title: String,
    rank: String,
    currentXp: Int,
    maxXp: Int,
    goldCredits: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = BrightPink.copy(alpha = 0.15f),
                spotColor = BrightPink.copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .padding(20.dp)
    ) {
        // Player Name Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Player",
                tint = White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = playerName,
                fontSize = 18.sp,
                color = White,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Player Photo Card - Shows single photo (banner or avatar)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val context = LocalContext.current
            val settingsViewModel = koinViewModel<SocialSentryViewModel>()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            val bannerUri = settings.gameDashboard.bannerImageUri
            val avatarUri = settings.gameDashboard.avatarImageUri
            
            // Priority: Banner first, then avatar, then default
            when {
                bannerUri != null -> {
                    // Show banner as full background
                    AsyncImage(
                        model = bannerUri,
                        contentDescription = "Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                avatarUri != null -> {
                    // Show avatar as profile picture in center
                    AsyncImage(
                        model = avatarUri,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(60.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    // Show default profile picture as full background
                    AsyncImage(
                        model = R.drawable.default_profile_pic,
                        contentDescription = "Default Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Level and Title with glowing icon
        val infiniteTransition = rememberInfiniteTransition(label = "icon glow")
        val iconGlow by infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "icon alpha"
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(8.dp),
                        ambientColor = Color(0xFF00BCD4).copy(alpha = iconGlow * 0.5f),
                        spotColor = Color(0xFF00BCD4).copy(alpha = iconGlow * 0.5f)
                    )
                    .background(Color(0xFF00BCD4).copy(alpha = iconGlow), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚔",
                    fontSize = 18.sp,
                    color = White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = White,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rank: $rank",
                        fontSize = 13.sp,
                        color = TextGray,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "⭐",
                        fontSize = 12.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // XP Progress Bar with pulsing animation
        Column {
            Text(
                text = "${currentXp}/${maxXp} XP",
                fontSize = 12.sp,
                color = BrightPink,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val progress = (currentXp.toFloat() / maxXp.coerceAtLeast(1)).coerceIn(0f, 1f)
            AnimatedProgressBar(progress = progress)
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Stats with better styling
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "You require 100 XP to complete this level!",
                fontSize = 13.sp,
                color = Color(0xFFFF5722),
                fontWeight = FontWeight.Medium
            )
            
            StatItem(
                label = "XP earned:",
                value = "${currentXp} XP",
                valueColor = BrightPink
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gold earned ",
                    fontSize = 13.sp,
                    color = White
                )
                Text(
                    text = "💰",
                    fontSize = 13.sp
                )
                Text(
                    text = " : ",
                    fontSize = 13.sp,
                    color = White
                )
                Text(
                    text = "$goldCredits Credits",
                    fontSize = 13.sp,
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // New Page Button with press animation
        var isPressed by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.95f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "button scale"
        )
        
        OutlinedButton(
            onClick = { },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .scale(scale),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = TextGray
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2A2A)),
            shape = RoundedCornerShape(8.dp),
            interactionSource = remember { MutableInteractionSource() }
                .also { interactionSource ->
                    LaunchedEffect(interactionSource) {
                        interactionSource.interactions.collect { interaction ->
                            when (interaction) {
                                is androidx.compose.foundation.interaction.PressInteraction.Press -> {
                                    isPressed = true
                                }
                                is androidx.compose.foundation.interaction.PressInteraction.Release,
                                is androidx.compose.foundation.interaction.PressInteraction.Cancel -> {
                                    isPressed = false
                                }
                            }
                        }
                    }
                }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = TextGray.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "New page",
                fontSize = 13.sp,
                color = TextGray.copy(alpha = 0.7f),
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun AnimatedProgressBar(progress: Float) {
    // Pulsing animation for the progress bar
    val infiniteTransition = rememberInfiniteTransition(label = "progress pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress alpha"
    )
    
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Background track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF2A2A2A))
        )
        
        // Animated progress
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(BrightPink.copy(alpha = alpha))
        )
    }
}

@Composable
fun QuoteSection(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = TextGray.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            letterSpacing = 1.5.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun StatisticsSection(stats: List<Float>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Radar Chart - Larger and more spacious
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            ImprovedRadarChart(stats)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Creator credit
        Text(
            text = "yt@mkshaon7",
            fontSize = 11.sp,
            color = TextGray.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun YouTubeQuickStatsSection(
    quote: String, 
    youtubeStats: YouTubeStats,
    youtubeDataStore: YouTubeTrackingDataStore = koinInject()
) {
    var showDetailedTable by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .padding(24.dp)
    ) {
        // Quote
        Text(
            text = quote,
            fontSize = 11.sp,
            color = TextGray,
            textAlign = TextAlign.Center,
            letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // YouTube Category Radar Chart - Bigger and more spacious
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            YouTubeCategoryRadarChart(youtubeStats)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Creator credit
        Text(
            text = "yt@mkshaon7",
            fontSize = 11.sp,
            color = TextGray.copy(alpha = 0.6f),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        HorizontalDivider(color = TextGray.copy(alpha = 0.2f))
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // See More Button
        var isPressed by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.95f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "button scale"
        )
        
        OutlinedButton(
            onClick = { showDetailedTable = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .scale(scale),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = BrightPink
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, BrightPink),
            shape = RoundedCornerShape(8.dp),
            interactionSource = remember { MutableInteractionSource() }
                .also { interactionSource ->
                    LaunchedEffect(interactionSource) {
                        interactionSource.interactions.collect { interaction ->
                            when (interaction) {
                                is androidx.compose.foundation.interaction.PressInteraction.Press -> {
                                    isPressed = true
                                }
                                is androidx.compose.foundation.interaction.PressInteraction.Release,
                                is androidx.compose.foundation.interaction.PressInteraction.Cancel -> {
                                    isPressed = false
                                }
                            }
                        }
                    }
                }
        ) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = null,
                tint = BrightPink,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "See All Videos",
                fontSize = 13.sp,
                color = BrightPink,
                fontWeight = FontWeight.Bold
            )
        }
    }
    
    // Detailed Videos Table Dialog
    if (showDetailedTable) {
        DetailedVideosTableDialog(
            youtubeStats = youtubeStats,
            onDismiss = { showDetailedTable = false }
        )
    }
}

@Composable
fun QuickStatsSection(quote: String, stats: List<Float>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .padding(24.dp)
    ) {
        // Quote
        Text(
            text = quote,
            fontSize = 11.sp,
            color = TextGray,
            textAlign = TextAlign.Center,
            letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Radar Chart - Bigger and more spacious
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            ImprovedRadarChart(stats)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Creator credit
        Text(
            text = "yt@mkshaon7",
            fontSize = 11.sp,
            color = TextGray.copy(alpha = 0.6f),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        HorizontalDivider(color = TextGray.copy(alpha = 0.2f))
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // The System Status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = BrightPink,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "The System",
                fontSize = 16.sp,
                color = White,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Status message
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF8B0000).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = Color(0xFFFF5722),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "No new quests detected.",
                fontSize = 13.sp,
                color = Color(0xFFFF5722),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SystemSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .padding(24.dp)
    ) {
        // Power Button with hover animation
        var powerPressed by remember { mutableStateOf(false) }
        val powerScale by animateFloatAsState(
            targetValue = if (powerPressed) 0.85f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "power button scale"
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .scale(powerScale)
                    .background(Color(0xFF0F0F0F), RoundedCornerShape(28.dp))
                    .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(28.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Power button action
                    }
                    .then(
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() }
                                .also { interactionSource ->
                                    LaunchedEffect(interactionSource) {
                                        interactionSource.interactions.collect { interaction ->
                                            when (interaction) {
                                                is androidx.compose.foundation.interaction.PressInteraction.Press -> {
                                                    powerPressed = true
                                                }
                                                is androidx.compose.foundation.interaction.PressInteraction.Release,
                                                is androidx.compose.foundation.interaction.PressInteraction.Cancel -> {
                                                    powerPressed = false
                                                }
                                            }
                                        }
                                    }
                                },
                            indication = null,
                            onClick = {}
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Power",
                    tint = TextGray.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // The System Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = BrightPink,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "The System",
                fontSize = 18.sp,
                color = White,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // No new quests message
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF8B0000).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = Color(0xFFFF5722),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "No new quests detected.",
                fontSize = 14.sp,
                color = Color(0xFFFF5722),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label ",
            fontSize = 13.sp,
            color = White
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = valueColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun YouTubeStatsSection(
    stats: YouTubeStats,
    youtubeDataStore: YouTubeTrackingDataStore = koinInject()
) {
    var selectedSessionId by remember { mutableStateOf<String?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .padding(24.dp)
    ) {
        // Header with icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFFFF0000), // YouTube red
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "YouTube Usage Tracker",
                fontSize = 18.sp,
                color = White,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Total watch time
        val totalMinutes = (stats.totalWatchTimeMs / 60000).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F0F0F), RoundedCornerShape(12.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Today's Watch Time",
                    fontSize = 12.sp,
                    color = TextGray,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    if (hours > 0) {
                        Text(
                            text = "${hours}h ",
                            fontSize = 32.sp,
                            color = Color(0xFFFF0000),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${minutes}m",
                        fontSize = 32.sp,
                        color = Color(0xFFFF0000),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Category breakdown with pie chart
        if (stats.categoryBreakdown.isNotEmpty()) {
            Text(
                text = "Category Breakdown",
                fontSize = 14.sp,
                color = White,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Pie chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CategoryPieChart(stats.categoryBreakdown, stats.totalWatchTimeMs)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Category legend with percentages
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                stats.categoryBreakdown.entries.sortedByDescending { it.value }.forEach { (category, time) ->
                    val percentage = if (stats.totalWatchTimeMs > 0) {
                        ((time.toFloat() / stats.totalWatchTimeMs) * 100).toInt()
                    } else 0
                    val categoryMinutes = (time / 60000).toInt()
                    
                    CategoryItem(
                        category = category,
                        timeMinutes = categoryMinutes,
                        percentage = percentage
                    )
                }
            }
        } else {
            // No data yet
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📊",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No YouTube usage data yet",
                        fontSize = 14.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Watch some videos to see your stats!",
                        fontSize = 12.sp,
                        color = TextGray.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        HorizontalDivider(color = TextGray.copy(alpha = 0.2f))
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Top channels (if available)
        if (stats.topChannels.isNotEmpty()) {
            Text(
                text = "Top Channels",
                fontSize = 14.sp,
                color = White,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                stats.topChannels.entries.take(5).forEach { (channel, time) ->
                    val channelMinutes = (time / 60000).toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = channel,
                            fontSize = 13.sp,
                            color = TextGray,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${channelMinutes}m",
                            fontSize = 13.sp,
                            color = BrightPink,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = TextGray.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Recent videos with category correction
        if (stats.sessions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Videos",
                    fontSize = 14.sp,
                    color = White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap to correct category",
                    fontSize = 11.sp,
                    color = TextGray.copy(alpha = 0.6f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Show last 5 completed sessions
            val recentSessions = stats.sessions
                .filter { it.endTime != null && it.durationMs >= 5000 }
                .sortedByDescending { it.startTime }
                .take(5)
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recentSessions.forEach { session ->
                    VideoSessionItem(
                        session = session,
                        onClick = {
                            selectedSessionId = session.id
                            showCategoryDialog = true
                        }
                    )
                }
            }
        }
    }
    
    // Category correction dialog
    if (showCategoryDialog && selectedSessionId != null) {
        val session = stats.sessions.find { it.id == selectedSessionId }
        if (session != null) {
            CategoryCorrectionDialog(
                session = session,
                onCategorySelected = { newCategory ->
                    // Update the session category
                    scope.launch {
                        youtubeDataStore.updateSessionCategory(session.id, newCategory)
                        youtubeDataStore.saveUserCorrection(session.title, newCategory)
                    }
                    showCategoryDialog = false
                    selectedSessionId = null
                },
                onDismiss = {
                    showCategoryDialog = false
                    selectedSessionId = null
                }
            )
        }
    }
}

@Composable
fun VideoSessionItem(
    session: YouTubeSession,
    onClick: () -> Unit
) {
    val (categoryColor, categoryIcon, _) = when (session.category) {
        VideoCategory.STUDY -> Triple(Color(0xFF4CAF50), "📚", "Study")
        VideoCategory.ENTERTAINMENT -> Triple(Color(0xFFE91E63), "🎬", "Entertainment")
        VideoCategory.GAMING -> Triple(Color(0xFF9C27B0), "🎮", "Gaming")
        VideoCategory.PRODUCTIVE -> Triple(Color(0xFF2196F3), "💼", "Productive")
        VideoCategory.OTHER -> Triple(Color(0xFF757575), "❓", "Other")
    }
    
    val durationMinutes = (session.durationMs / 60000).toInt()
    val durationSeconds = ((session.durationMs % 60000) / 1000).toInt()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F0F0F))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category icon
        Text(
            text = categoryIcon,
            fontSize = 20.sp
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Video info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.title,
                fontSize = 13.sp,
                color = White,
                maxLines = 2,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                session.channelName?.let { channel ->
                    Text(
                        text = channel,
                        fontSize = 11.sp,
                        color = TextGray.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = TextGray.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Text(
                    text = if (durationMinutes > 0) "${durationMinutes}m ${durationSeconds}s" else "${durationSeconds}s",
                    fontSize = 11.sp,
                    color = TextGray.copy(alpha = 0.7f)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Category indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(categoryColor, RoundedCornerShape(4.dp))
        )
    }
}

@Composable
fun CategoryCorrectionDialog(
    session: YouTubeSession,
    onCategorySelected: (VideoCategory) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Correct Category",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = session.title,
                    fontSize = 13.sp,
                    color = TextGray,
                    maxLines = 2
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Select the correct category for this video:",
                    fontSize = 13.sp,
                    color = TextGray
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Category options
                VideoCategory.values().forEach { category ->
                    val (color, icon, label) = when (category) {
                        VideoCategory.STUDY -> Triple(Color(0xFF4CAF50), "📚", "Study")
                        VideoCategory.ENTERTAINMENT -> Triple(Color(0xFFE91E63), "🎬", "Entertainment")
                        VideoCategory.GAMING -> Triple(Color(0xFF9C27B0), "🎮", "Gaming")
                        VideoCategory.PRODUCTIVE -> Triple(Color(0xFF2196F3), "💼", "Productive")
                        VideoCategory.OTHER -> Triple(Color(0xFF757575), "❓", "Other")
                    }
                    
                    val isSelected = category == session.category
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) color.copy(alpha = 0.2f)
                                else Color(0xFF2A2A2A)
                            )
                            .clickable { onCategorySelected(category) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = icon,
                            fontSize = 20.sp
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            color = if (isSelected) color else White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = color,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = Color(0xFF1A1A1A),
        titleContentColor = White,
        textContentColor = TextGray
    )
}

@Composable
fun CategoryItem(
    category: VideoCategory,
    timeMinutes: Int,
    percentage: Int
) {
    val (color, icon, label) = when (category) {
        VideoCategory.STUDY -> Triple(Color(0xFF4CAF50), "📚", "Study")
        VideoCategory.ENTERTAINMENT -> Triple(Color(0xFFE91E63), "🎬", "Entertainment")
        VideoCategory.GAMING -> Triple(Color(0xFF9C27B0), "🎮", "Gaming")
        VideoCategory.PRODUCTIVE -> Triple(Color(0xFF2196F3), "💼", "Productive")
        VideoCategory.OTHER -> Triple(Color(0xFF757575), "❓", "Other")
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = icon,
            fontSize = 16.sp
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = label,
            fontSize = 13.sp,
            color = White,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = "${timeMinutes}m",
            fontSize = 13.sp,
            color = TextGray,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = "${percentage}%",
            fontSize = 13.sp,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CategoryPieChart(
    categoryBreakdown: Map<VideoCategory, Long>,
    totalTime: Long
) {
    val categoryColors = mapOf(
        VideoCategory.STUDY to Color(0xFF4CAF50),
        VideoCategory.ENTERTAINMENT to Color(0xFFE91E63),
        VideoCategory.GAMING to Color(0xFF9C27B0),
        VideoCategory.PRODUCTIVE to Color(0xFF2196F3),
        VideoCategory.OTHER to Color(0xFF757575)
    )
    
    // Animation for pie chart
    val infiniteTransition = rememberInfiniteTransition(label = "pie pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pie scale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size.minDimension
            val radius = canvasSize / 2.5f
            val centerX = size.width / 2
            val centerY = size.height / 2
            
            var startAngle = -90f // Start from top
            
            categoryBreakdown.forEach { (category, time) ->
                val sweepAngle = ((time.toFloat() / totalTime) * 360f)
                val color = categoryColors[category] ?: Color.Gray
                
                // Draw pie slice
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        centerX - radius,
                        centerY - radius
                    ),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )
                
                // Draw border between slices
                drawArc(
                    color = Color(0xFF0D0D0D),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        centerX - radius,
                        centerY - radius
                    ),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                )
                
                startAngle += sweepAngle
            }
            
            // Draw center circle (donut effect)
            drawCircle(
                color = Color(0xFF1A1A1A),
                radius = radius * 0.5f,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY)
            )
        }
    }
}

@Composable
fun YouTubeCategoryRadarChart(youtubeStats: YouTubeStats) {
    // Convert YouTube categories to radar chart values
    val categoryValues = listOf(
        youtubeStats.categoryBreakdown[VideoCategory.STUDY]?.let { it.toFloat() / 3600000f } ?: 0f, // Convert to hours
        youtubeStats.categoryBreakdown[VideoCategory.ENTERTAINMENT]?.let { it.toFloat() / 3600000f } ?: 0f,
        youtubeStats.categoryBreakdown[VideoCategory.GAMING]?.let { it.toFloat() / 3600000f } ?: 0f,
        youtubeStats.categoryBreakdown[VideoCategory.PRODUCTIVE]?.let { it.toFloat() / 3600000f } ?: 0f,
        youtubeStats.categoryBreakdown[VideoCategory.OTHER]?.let { it.toFloat() / 3600000f } ?: 0f,
        0f // 6th axis for future use
    )
    
    // Normalize values to 0-1 range (max 4 hours per category)
    val normalizedValues = categoryValues.map { (it / 4f).coerceIn(0f, 1f) }
    
    // Subtle breathing animation for the radar chart
    val infiniteTransition = rememberInfiniteTransition(label = "radar pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radar scale"
    )
    
    // YouTube Category radar chart
    Box(
        modifier = Modifier
            .fillMaxSize()
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.minDimension / 2.8f
            
            // Draw hexagon radar grid (6 points)
            val points = 6
            val angleStep = (2 * Math.PI / points).toFloat()
            val startAngle = -Math.PI.toFloat() / 2 // Start from top
            
            // Draw concentric hexagons (grid) - 5 levels
            for (level in 1..5) {
                val currentRadius = radius * level / 5
                val path = androidx.compose.ui.graphics.Path()
                
                for (i in 0 until points) {
                    val angle = startAngle + angleStep * i
                    val x = centerX + currentRadius * kotlin.math.cos(angle)
                    val y = centerY + currentRadius * kotlin.math.sin(angle)
                    
                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                path.close()
                
                drawPath(
                    path = path,
                    color = Color(0xFF00BCD4).copy(alpha = 0.2f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
            }
            
            // Draw axis lines from center to each point
            for (i in 0 until points) {
                val angle = startAngle + angleStep * i
                val x = centerX + radius * kotlin.math.cos(angle)
                val y = centerY + radius * kotlin.math.sin(angle)
                
                drawLine(
                    color = Color(0xFF00BCD4).copy(alpha = 0.25f),
                    start = androidx.compose.ui.geometry.Offset(centerX, centerY),
                    end = androidx.compose.ui.geometry.Offset(x, y),
                    strokeWidth = 2f
                )
            }
            
            // Draw data polygon (filled) - YouTube categories
            val dataPath = androidx.compose.ui.graphics.Path()
            
            for (i in normalizedValues.indices) {
                val angle = startAngle + angleStep * i
                val currentRadius = radius * normalizedValues[i]
                val x = centerX + currentRadius * kotlin.math.cos(angle)
                val y = centerY + currentRadius * kotlin.math.sin(angle)
                
                if (i == 0) {
                    dataPath.moveTo(x, y)
                } else {
                    dataPath.lineTo(x, y)
                }
            }
            dataPath.close()
            
            // Fill with YouTube red gradient effect
            drawPath(
                path = dataPath,
                color = Color(0xFFFF0000).copy(alpha = 0.6f)
            )
            
            // Stroke outline
            drawPath(
                path = dataPath,
                color = Color(0xFFFF0000).copy(alpha = 0.9f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
            )
            
            // Draw data points as circles
            for (i in normalizedValues.indices) {
                val angle = startAngle + angleStep * i
                val currentRadius = radius * normalizedValues[i]
                val x = centerX + currentRadius * kotlin.math.cos(angle)
                val y = centerY + currentRadius * kotlin.math.sin(angle)
                
                // Outer glow circle
                drawCircle(
                    color = Color(0xFFFF0000).copy(alpha = 0.3f),
                    radius = 8f,
                    center = androidx.compose.ui.geometry.Offset(x, y)
                )
                // Inner solid circle
                drawCircle(
                    color = Color(0xFFFF0000),
                    radius = 5f,
                    center = androidx.compose.ui.geometry.Offset(x, y)
                )
            }
        }
        
        // Labels positioned around the hexagon - YouTube categories
        val labels = listOf("STUDY", "ENT", "GAME", "PROD", "OTHER", "FUTURE")
        Box(modifier = Modifier.fillMaxSize()) {
            // Top label (STUDY)
            Text(
                text = labels[0],
                fontSize = 12.sp,
                color = TextGray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 0.dp)
            )
            
            // Top-right label (ENT)
            Text(
                text = labels[1],
                fontSize = 12.sp,
                color = TextGray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp, top = 32.dp)
            )
            
            // Bottom-right label (GAME)
            Text(
                text = labels[2],
                fontSize = 12.sp,
                color = TextGray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 32.dp)
            )
            
            // Bottom label (PROD)
            Text(
                text = labels[3],
                fontSize = 12.sp,
                color = TextGray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 0.dp)
            )
            
            // Bottom-left label (OTHER)
            Text(
                text = labels[4],
                fontSize = 12.sp,
                color = TextGray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 32.dp)
            )
            
            // Top-left label (FUTURE)
            Text(
                text = labels[5],
                fontSize = 12.sp,
                color = TextGray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 32.dp)
            )
        }
    }
}

@Composable
fun DetailedVideosTableDialog(
    youtubeStats: YouTubeStats,
    onDismiss: () -> Unit
) {
    val completedSessions = youtubeStats.sessions
        .filter { it.endTime != null && it.durationMs >= 5000 }
        .sortedByDescending { it.startTime }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    tint = BrightPink,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "All Watched Videos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Video Title",
                        fontSize = 12.sp,
                        color = BrightPink,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(2f)
                    )
                    Text(
                        text = "Duration",
                        fontSize = 12.sp,
                        color = BrightPink,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Category",
                        fontSize = 12.sp,
                        color = BrightPink,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Videos list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(completedSessions) { session ->
                        DetailedVideoRow(session = session)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        containerColor = Color(0xFF1A1A1A),
        titleContentColor = White,
        textContentColor = TextGray
    )
}

@Composable
fun DetailedVideoRow(session: YouTubeSession) {
    val (categoryColor, categoryIcon, categoryLabel) = when (session.category) {
        VideoCategory.STUDY -> Triple(Color(0xFF4CAF50), "📚", "Study")
        VideoCategory.ENTERTAINMENT -> Triple(Color(0xFFE91E63), "🎬", "Entertainment")
        VideoCategory.GAMING -> Triple(Color(0xFF9C27B0), "🎮", "Gaming")
        VideoCategory.PRODUCTIVE -> Triple(Color(0xFF2196F3), "💼", "Productive")
        VideoCategory.OTHER -> Triple(Color(0xFF757575), "❓", "Other")
    }
    
    val durationMinutes = (session.durationMs / 60000).toInt()
    val durationSeconds = ((session.durationMs % 60000) / 1000).toInt()
    val durationText = if (durationMinutes > 0) "${durationMinutes}m ${durationSeconds}s" else "${durationSeconds}s"
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F0F0F), RoundedCornerShape(6.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Video title
        Text(
            text = session.title,
            fontSize = 11.sp,
            color = White,
            maxLines = 2,
            modifier = Modifier.weight(2f)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Duration
        Text(
            text = durationText,
            fontSize = 11.sp,
            color = TextGray,
            modifier = Modifier.weight(1f)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Category
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = categoryIcon,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = categoryLabel,
                fontSize = 10.sp,
                color = categoryColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ImprovedRadarChart(stats: List<Float>) {
    // Subtle breathing animation for the radar chart
    val infiniteTransition = rememberInfiniteTransition(label = "radar pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radar scale"
    )
    
    // Hexagon radar chart matching the reference image
    Box(
        modifier = Modifier
            .fillMaxSize()
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            // Make the radar chart bigger - use more of the available space
            val radius = size.minDimension / 2.8f
            
            // Draw hexagon radar grid (6 points)
            val points = 6
            val angleStep = (2 * Math.PI / points).toFloat()
            val startAngle = -Math.PI.toFloat() / 2 // Start from top
            
            // Draw concentric hexagons (grid) - 5 levels with better visibility
            for (level in 1..5) {
                val currentRadius = radius * level / 5
                val path = androidx.compose.ui.graphics.Path()
                
                for (i in 0 until points) {
                    val angle = startAngle + angleStep * i
                    val x = centerX + currentRadius * kotlin.math.cos(angle)
                    val y = centerY + currentRadius * kotlin.math.sin(angle)
                    
                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                path.close()
                
                // Make grid lines more visible
                drawPath(
                    path = path,
                    color = Color(0xFF00BCD4).copy(alpha = 0.2f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
            }
            
            // Draw axis lines from center to each point - thicker
            for (i in 0 until points) {
                val angle = startAngle + angleStep * i
                val x = centerX + radius * kotlin.math.cos(angle)
                val y = centerY + radius * kotlin.math.sin(angle)
                
                drawLine(
                    color = Color(0xFF00BCD4).copy(alpha = 0.25f),
                    start = androidx.compose.ui.geometry.Offset(centerX, centerY),
                    end = androidx.compose.ui.geometry.Offset(x, y),
                    strokeWidth = 2f
                )
            }
            
            // Draw data polygon (filled) - Sample stats values
            val dataValues = stats.takeIf { it.size == 6 } ?: listOf(0.85f, 0.95f, 0.78f, 0.65f, 0.72f, 0.88f)
            val dataPath = androidx.compose.ui.graphics.Path()
            
            for (i in dataValues.indices) {
                val angle = startAngle + angleStep * i
                val currentRadius = radius * dataValues[i]
                val x = centerX + currentRadius * kotlin.math.cos(angle)
                val y = centerY + currentRadius * kotlin.math.sin(angle)
                
                if (i == 0) {
                    dataPath.moveTo(x, y)
                } else {
                    dataPath.lineTo(x, y)
                }
            }
            dataPath.close()
            
            // Fill with purple gradient effect - more visible
            drawPath(
                path = dataPath,
                color = Color(0xFF9C27B0).copy(alpha = 0.6f)
            )
            
            // Stroke outline - thicker
            drawPath(
                path = dataPath,
                color = Color(0xFF9C27B0).copy(alpha = 0.9f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
            )
            
            // Draw data points as circles - bigger
            for (i in dataValues.indices) {
                val angle = startAngle + angleStep * i
                val currentRadius = radius * dataValues[i]
                val x = centerX + currentRadius * kotlin.math.cos(angle)
                val y = centerY + currentRadius * kotlin.math.sin(angle)
                
                // Outer glow circle
                drawCircle(
                    color = Color(0xFF9C27B0).copy(alpha = 0.3f),
                    radius = 8f,
                    center = androidx.compose.ui.geometry.Offset(x, y)
                )
                // Inner solid circle
                drawCircle(
                    color = Color(0xFF9C27B0),
                    radius = 5f,
                    center = androidx.compose.ui.geometry.Offset(x, y)
                )
            }
        }
        
        // Labels positioned around the hexagon - better spacing
        val labels = listOf("FIT", "SOC", "INT", "DIS", "FOC", "FIN")
        Box(modifier = Modifier.fillMaxSize()) {
            // Top label (FIT)
            Text(
                text = labels[0],
                fontSize = 12.sp,
                color = TextGray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 0.dp)
            )
            
            // Top-right label (SOC)
            Text(
                text = labels[1],
                fontSize = 12.sp,
                color = TextGray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp, top = 32.dp)
            )
            
            // Bottom-right label (INT)
            Text(
                text = labels[2],
                fontSize = 12.sp,
                color = TextGray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 32.dp)
            )
            
            // Bottom label (DIS)
            Text(
                text = labels[3],
                fontSize = 12.sp,
                color = TextGray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 0.dp)
            )
            
            // Bottom-left label (FOC)
            Text(
                text = labels[4],
                fontSize = 12.sp,
                color = TextGray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 32.dp)
            )
            
            // Top-left label (FIN)
            Text(
                text = labels[5],
                fontSize = 12.sp,
                color = TextGray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 32.dp)
            )
        }
    }
}

@Composable
fun ScreenTimeSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val youtubeDataStore = koinInject<YouTubeTrackingDataStore>()
    
    var usageStats by remember { mutableStateOf<AppUsageStats?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasPermission by remember { mutableStateOf(false) }
    
    // Check permission status - Usage stats functionality disabled
    LaunchedEffect(Unit) {
        hasPermission = false // Usage stats functionality removed
        isLoading = false
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .padding(24.dp)
    ) {
        // Header with icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF00BCD4), // Cyan color for screen time
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Screen Time",
                fontSize = 18.sp,
                color = White,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        if (isLoading) {
            // Loading state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF00BCD4),
                    strokeWidth = 3.dp
                )
            }
        } else if (!hasPermission) {
            // Permission not granted
            PermissionRequiredContent()
        } else if (usageStats == null || usageStats?.totalScreenTime == 0L) {
            // No data available
            NoDataContent()
        } else {
            // Show screen time data
            usageStats?.let { stats ->
                ScreenTimeContent(stats = stats)
            }
        }
    }
}

@Composable
fun PermissionRequiredContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val youtubeDataStore = koinInject<YouTubeTrackingDataStore>()
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = Color(0xFFFF9800),
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Permission Required",
            fontSize = 16.sp,
            color = White,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Grant usage stats permission to view your screen time",
            fontSize = 12.sp,
            color = TextGray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                scope.launch {
                    val appUsageManager = AppUsageManager(context, youtubeDataStore)
                    appUsageManager.openUsageStatsSettings()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00BCD4)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Grant Permission",
                color = White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun NoDataContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "📱",
            fontSize = 48.sp
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "No screen time data yet",
            fontSize = 14.sp,
            color = TextGray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "Use your phone to see usage statistics!",
            fontSize = 12.sp,
            color = TextGray.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ScreenTimeContent(stats: AppUsageStats) {
    // Total screen time display
    val totalHours = (stats.totalScreenTime / 3600000).toInt()
    val totalMinutes = ((stats.totalScreenTime % 3600000) / 60000).toInt()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F0F0F), RoundedCornerShape(12.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Today's Screen Time",
                fontSize = 12.sp,
                color = TextGray,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                if (totalHours > 0) {
                    Text(
                        text = "${totalHours}h ",
                        fontSize = 32.sp,
                        color = Color(0xFF00BCD4),
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${totalMinutes}m",
                    fontSize = 32.sp,
                    color = Color(0xFF00BCD4),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    
    Spacer(modifier = Modifier.height(20.dp))
    
    // Category breakdown
    if (stats.categoryBreakdown.isNotEmpty()) {
        Text(
            text = "App Categories",
            fontSize = 14.sp,
            color = White,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Show top 5 categories
        val sortedCategories = stats.categoryBreakdown.entries
            .sortedByDescending { it.value }
            .take(5)
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sortedCategories.forEach { (category, time) ->
                val percentage = if (stats.totalScreenTime > 0) {
                    ((time.toFloat() / stats.totalScreenTime) * 100).toInt()
                } else 0
                val categoryMinutes = (time / 60000).toInt()
                
                AppCategoryItem(
                    category = category,
                    timeMinutes = categoryMinutes,
                    percentage = percentage
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = TextGray.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    // Top apps
    if (stats.topApps.isNotEmpty()) {
        Text(
            text = "Most Used Apps",
            fontSize = 14.sp,
            color = White,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Show top 3 apps
        val topApps = stats.topApps.entries.take(3)
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            topApps.forEach { (packageName, time) ->
                val appMinutes = (time / 60000).toInt()
                val appName = getAppDisplayName(packageName)
                
                TopAppItem(
                    appName = appName,
                    timeMinutes = appMinutes
                )
            }
        }
    }
}

@Composable
fun AppCategoryItem(
    category: AppCategory,
    timeMinutes: Int,
    percentage: Int
) {
    val (color, icon, label) = when (category) {
        AppCategory.STUDY -> Triple(Color(0xFF4CAF50), "📚", "Study")
        AppCategory.SOCIAL_MEDIA -> Triple(Color(0xFFE91E63), "📱", "Social")
        AppCategory.ENTERTAINMENT -> Triple(Color(0xFF9C27B0), "🎬", "Entertainment")
        AppCategory.PRODUCTIVITY -> Triple(Color(0xFF2196F3), "💼", "Productivity")
        AppCategory.COMMUNICATION -> Triple(Color(0xFFFF9800), "💬", "Communication")
        AppCategory.UTILITIES -> Triple(Color(0xFF607D8B), "🔧", "Utilities")
        AppCategory.OTHER -> Triple(Color(0xFF757575), "❓", "Other")
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = icon,
            fontSize = 16.sp
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = label,
            fontSize = 13.sp,
            color = White,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = "${timeMinutes}m",
            fontSize = 13.sp,
            color = TextGray,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = "${percentage}%",
            fontSize = 13.sp,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TopAppItem(
    appName: String,
    timeMinutes: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F0F0F), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon placeholder
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📱",
                fontSize = 16.sp
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appName,
                fontSize = 13.sp,
                color = White,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${timeMinutes} minutes",
                fontSize = 11.sp,
                color = TextGray
            )
        }
    }
}

private fun getAppDisplayName(packageName: String): String {
    return when (packageName) {
        "com.google.android.youtube" -> "YouTube"
        "com.instagram.android" -> "Instagram"
        "com.facebook.katana" -> "Facebook"
        "com.zhiliaoapp.musically" -> "TikTok"
        "com.pinterest" -> "Pinterest"
        "com.twitter.android" -> "Twitter"
        "com.snapchat.android" -> "Snapchat"
        "com.whatsapp" -> "WhatsApp"
        "com.spotify.music" -> "Spotify"
        "com.netflix.mediaclient" -> "Netflix"
        else -> packageName.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: packageName
    }
}

