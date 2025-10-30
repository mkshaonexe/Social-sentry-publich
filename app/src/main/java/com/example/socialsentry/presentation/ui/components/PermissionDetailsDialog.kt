package com.example.socialsentry.presentation.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.socialsentry.util.PermissionStatus

@Composable
fun PermissionDetailsDialog(
    permissionStatus: PermissionStatus,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Permission Status",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Permission list
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(getAllPermissionItems(permissionStatus)) { item ->
                        PermissionItemCard(
                            permissionItem = item,
                            onClick = {
                                when (item.type) {
                                    PermissionType.ACCESSIBILITY -> {
                                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        context.startActivity(intent)
                                    }
                                }
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (permissionStatus.hasAllPermissions) 
                            Color(0xFF4CAF50).copy(alpha = 0.2f) 
                        else 
                            Color(0xFFFF6B6B).copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (permissionStatus.hasAllPermissions) 
                                Icons.Default.CheckCircle 
                            else 
                                Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (permissionStatus.hasAllPermissions) 
                                Color(0xFF4CAF50) 
                            else 
                                Color(0xFFFF6B6B),
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = if (permissionStatus.hasAllPermissions) 
                                "All permissions granted! App is fully functional." 
                            else 
                                "${permissionStatus.missingCount} permission(s) missing. Some features may not work.",
                            fontSize = 14.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionItemCard(
    permissionItem: PermissionItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A2A)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (permissionItem.isGranted) 
                                Color(0xFF4CAF50) 
                            else 
                                Color(0xFFFF6B6B)
                        )
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Icon
                Icon(
                    imageVector = permissionItem.icon,
                    contentDescription = null,
                    tint = if (permissionItem.isGranted) 
                        Color(0xFF4CAF50) 
                    else 
                        Color(0xFFFF6B6B),
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Text
                Column {
                    Text(
                        text = permissionItem.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = permissionItem.description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Status text
            Text(
                text = if (permissionItem.isGranted) "Granted" else "Missing",
                fontSize = 12.sp,
                color = if (permissionItem.isGranted) 
                    Color(0xFF4CAF50) 
                else 
                    Color(0xFFFF6B6B),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun getAllPermissionItems(permissionStatus: PermissionStatus): List<PermissionItem> {
    return listOf(
        PermissionItem(
            type = PermissionType.ACCESSIBILITY,
            title = "Accessibility Service",
            description = "Required for blocking apps and tracking usage",
            icon = Icons.Default.Person,
            isGranted = permissionStatus.isAccessibilityEnabled
        )
    )
}

private data class PermissionItem(
    val type: PermissionType,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isGranted: Boolean
)

private enum class PermissionType {
    ACCESSIBILITY
}
