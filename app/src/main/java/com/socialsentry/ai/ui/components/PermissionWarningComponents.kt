package com.socialsentry.ai.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
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
import com.socialsentry.ai.util.PermissionStatus

@Composable
fun PermissionWarningBanner(
    permissionStatus: PermissionStatus,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Only show if there are missing permissions
    AnimatedVisibility(
        visible = !permissionStatus.hasAllPermissions,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Warning header with blinking icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Blinking warning icon
                BlinkingWarningIcon()
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Missing Permissions (${permissionStatus.missingCount})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B6B)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // List of missing permissions with action buttons
            permissionStatus.missingPermissions.forEach { permission ->
                PermissionItem(
                    permissionName = permission,
                    onClick = {
                        when (permission) {
                            "Accessibility Service" -> {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun BlinkingWarningIcon() {
    // Infinite blinking animation
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Icon(
        imageVector = Icons.Default.Warning,
        contentDescription = "Warning",
        tint = Color(0xFFFF6B6B).copy(alpha = alpha),
        modifier = Modifier.size(20.dp)
    )
}

@Composable
private fun PermissionItem(
    permissionName: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2A2A2A))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Red dot indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFF6B6B))
            )
            
            Spacer(modifier = Modifier.width(10.dp))
            
            Text(
                text = permissionName,
                fontSize = 13.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Enable button
        Text(
            text = "Enable",
            fontSize = 12.sp,
            color = Color(0xFF4CAF50),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * Compact warning indicator for app bar
 */
@Composable
fun CompactPermissionWarning(
    permissionStatus: PermissionStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !permissionStatus.hasAllPermissions,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        // Blinking warning icon
        val infiniteTransition = rememberInfiniteTransition(label = "blink")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        
        Box(
            modifier = modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFFF6B6B).copy(alpha = 0.3f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            // Badge with count
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Missing Permissions",
                    tint = Color(0xFFFF6B6B).copy(alpha = alpha),
                    modifier = Modifier.size(28.dp)
                )
                
                // Count badge
                if (permissionStatus.missingCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(18.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color(0xFFFF6B6B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = permissionStatus.missingCount.toString(),
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Popup dialog that shows when user clicks the warning icon
 * Contains tutorial, email, and website information
 */
@Composable
fun SetupHelpDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFFF6B6B),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "App Not Set Up Correctly",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "The app is not set up correctly. Please check the tutorial for proper setup instructions.",
                    fontSize = 14.sp,
                    color = Color(0xFF757575)
                )
                
                // Tutorial section
                ContactInfoItem(
                    icon = Icons.Default.PlayArrow,
                    title = "YouTube Tutorial",
                    subtitle = "@mkshaon7",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=_ZM__jHfrl0&feature=youtu.be"))
                        context.startActivity(intent)
                    }
                )
                
                // Email section
                ContactInfoItem(
                    icon = Icons.Default.Email,
                    title = "Email Support",
                    subtitle = "mkshaon2024@gmail.com",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:mkshaon2024@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Social Sentry Setup Help")
                        }
                        context.startActivity(intent)
                    }
                )
                
                // Website section
                ContactInfoItem(
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    title = "Website",
                    subtitle = "mkshaon.com/social_sentry",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://mkshaon.com/social_sentry"))
                        context.startActivity(intent)
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        },
        containerColor = Color(0xFF1A1A1A),
        titleContentColor = Color.White,
        textContentColor = Color(0xFF757575)
    )
}

@Composable
private fun ContactInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2A2A2A))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF00BCD4),
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF757575)
            )
        }
        
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Open",
            tint = Color(0xFF757575),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun PermissionDetailsDialog(
    permissionStatus: PermissionStatus,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Permission Status",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (permissionStatus.hasAllPermissions) {
                    Text(
                        text = "All permissions granted! App is fully functional.",
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "${permissionStatus.missingCount} permission(s) missing. Some features may not work.",
                        color = Color(0xFFFF6B6B),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        containerColor = Color(0xFF1A1A1A),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

