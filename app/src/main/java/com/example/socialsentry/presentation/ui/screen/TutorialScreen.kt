package com.example.socialsentry.presentation.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.example.socialsentry.ui.theme.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.socialsentry.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    var currentStep by remember { mutableStateOf(1) }
    val totalSteps = 6
    
    val tutorialSteps = listOf(
        TutorialStep(
            title = "Welcome to Social Sentry! 🛡️",
            description = "Your personal guardian against social media addiction. Let's learn how to take control of your digital life!",
            icon = Icons.Default.Home,
            color = BrightPink,
            content = "Social Sentry helps you block distracting content like reels, limit screen time, and level up your productivity through gamification."
        ),
        TutorialStep(
            title = "Block Reels & Shorts 🚫",
            description = "Stop endless scrolling! Block Instagram Reels, YouTube Shorts, Facebook Reels, and TikTok automatically.",
            icon = Icons.Default.Close,
            color = BrightRed,
            content = "When you try to access reels or shorts, Social Sentry will redirect you back to the main feed. You can customize which platforms to block in settings."
        ),
        TutorialStep(
            title = "Set Screen Time Limits ⏰",
            description = "Control when you can use social media. Set specific time windows for each platform.",
            icon = Icons.Default.Settings,
            color = BrightBlue,
            content = "Configure blocking schedules for each app. For example, block Instagram during study hours (9 AM - 5 PM) but allow it in the evening."
        ),
        TutorialStep(
            title = "Use Allowance Time 🎯",
            description = "Get limited daily access to blocked apps when you really need it.",
            icon = Icons.Default.PlayArrow,
            color = BrightGreen,
            content = "You get 10 minutes of allowance time per day. Use it wisely! The app tracks your usage and shows remaining time."
        ),
        TutorialStep(
            title = "Level Up & Earn Rewards 🏆",
            description = "Turn productivity into a game! Earn XP, level up, and unlock achievements.",
            icon = Icons.Default.Star,
            color = BrightPurple,
            content = "Study, exercise, and stay focused to earn XP. Watch your character grow from E-Rank Hunter to S-Rank Master!"
        ),
        TutorialStep(
            title = "Track Your Progress 📊",
            description = "Monitor your YouTube usage, study time, and productivity stats with beautiful charts.",
            icon = Icons.Default.Info,
            color = BrightOrange,
            content = "View detailed analytics of your social media usage, categorized by content type. See how much time you spend on educational vs entertainment content."
        )
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Progress indicator
            TutorialProgressIndicator(
                currentStep = currentStep,
                totalSteps = totalSteps,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Tutorial content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300)) togetherWith
                    slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                },
                label = "tutorial_content"
            ) { step ->
                val tutorialData = tutorialSteps[step - 1]
                TutorialStepContent(
                    step = tutorialData,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep > 1) {
                    Button(
                        onClick = { currentStep-- },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrightGreen
                        )
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Previous")
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                if (currentStep < totalSteps) {
                    Button(
                        onClick = { currentStep++ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrightGreen
                        )
                    ) {
                        Text("Next")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                } else {
                    Button(
                        onClick = onComplete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrightGreen
                        )
                    ) {
                        Text("Get Started!")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Skip button
            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.Cyan
                )
            ) {
                Text("Skip Tutorial")
            }
        }
    }
}

@Composable
private fun TutorialProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val stepNumber = index + 1
            val isActive = stepNumber == currentStep
            val isCompleted = stepNumber < currentStep
            
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when {
                            isCompleted -> BrightGreen
                            isActive -> BrightPink
                            else -> MaterialTheme.colorScheme.outline
                        }
                    )
            )
            
            if (index < totalSteps - 1) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun TutorialStepContent(
    step: TutorialStep,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(60.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            step.color.copy(alpha = 0.2f),
                            step.color.copy(alpha = 0.1f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = step.icon,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = step.color
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Title
        Text(
            text = step.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Description
        Text(
            text = step.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Content card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = step.color.copy(alpha = 0.1f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = step.content,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(20.dp),
                lineHeight = 22.sp
            )
        }
    }
}

private data class TutorialStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val content: String
)
