package com.example.socialsentry.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.socialsentry.R
import com.example.socialsentry.data.model.SocialApp

@Composable
fun SocialAppCard(
    app: SocialApp,
    isAccessibilityEnabled: Boolean,
    onToggleBlocking: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // Dark card background
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(getAppIcon(app.name)),
                    contentDescription = "${app.name} icon",
                    modifier = Modifier.size(40.dp),
                    tint = getAppColor(app.name)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Switch(
                checked = app.isBlocked,
                onCheckedChange = { isBlocked ->
                    if (isAccessibilityEnabled) {
                        onToggleBlocking(isBlocked)
                    }
                },
                enabled = isAccessibilityEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF4CAF50), // Green for enabled/blocked
                    checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.5f),
                    uncheckedThumbColor = Color(0xFF9E9E9E), // Gray for disabled/unblocked
                    uncheckedTrackColor = Color(0xFF9E9E9E).copy(alpha = 0.3f),
                    disabledCheckedThumbColor = Color(0xFF4CAF50).copy(alpha = 0.5f),
                    disabledCheckedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.3f),
                    disabledUncheckedThumbColor = Color(0xFF757575),
                    disabledUncheckedTrackColor = Color(0xFF757575).copy(alpha = 0.3f)
                )
            )
        }
    }
}

private fun getAppIcon(appName: String): Int {
    return when (appName) {
        "Instagram" -> R.drawable.ic_instagram
        "YouTube" -> R.drawable.ic_youtube
        "TikTok" -> R.drawable.ic_tiktok
        "Facebook" -> R.drawable.ic_facebook
        else -> R.drawable.ic_shield
    }
}

private fun getAppColor(appName: String): Color {
    return when (appName) {
        "Instagram" -> Color(0xFFE4405F)
        "YouTube" -> Color(0xFFFF0000)
        "TikTok" -> Color(0xFF000000)
        "Facebook" -> Color(0xFF1877F2)
        else -> Color.Gray
    }
}
