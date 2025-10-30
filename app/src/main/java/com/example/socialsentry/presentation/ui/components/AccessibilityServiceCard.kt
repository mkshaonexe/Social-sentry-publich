package com.example.socialsentry.presentation.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AccessibilityServiceCard(
    isServiceEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isServiceEnabled) {
                Color(0xFF1A472A) // Dark green for enabled state
            } else {
                Color(0xFF4A1A1A) // Dark red for disabled state
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isServiceEnabled) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isServiceEnabled) {
                    Color(0xFF4CAF50) // Green for enabled state
                } else {
                    Color(0xFFFF6B6B) // Red for disabled state
                }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Accessibility Service",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    text = if (isServiceEnabled) {
                        "Service is enabled. Tap to open system settings to disable."
                    } else {
                        "Service is disabled. Tap to enable in system settings."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Open Settings",
                tint = if (isServiceEnabled) {
                    Color(0xFF4CAF50) // Green settings icon
                } else {
                    Color(0xFFFF6B6B) // Red settings icon
                }
            )
        }
        
        AnimatedVisibility(visible = !isServiceEnabled) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2A2A2A) // Dark gray for description card
                )
            ) {
                Text(
                    text = "Social Sentry needs accessibility permission to detect when you enter Reels, Shorts, or Stories and redirect you back to the main feed. No personal data is collected.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}
