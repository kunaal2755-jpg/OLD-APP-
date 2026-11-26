package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Navy900
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSuccess

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val isConfirmed = status.equals("CONFIRMED", ignoreCase = true)
    val bgColor = if (isConfirmed) Color(0xFFDCFCE7) else Color(0xFFFFEDD5)
    val textColor = if (isConfirmed) StatusSuccess else StatusPending
    val label = if (isConfirmed) "CONFIRMED" else "PENDING"

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PdfPriceVisibilitySwitchCard(
    showPrice: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (showPrice) Color(0xFFEFF6FF) else Color(0xFFF1F5F9)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (showPrice) Color(0xFF93C5FD) else Color(0xFFCBD5E1)
            )
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Show Prices on Customer PDF",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Navy900
                )
                Text(
                    text = if (showPrice)
                        "ON: Bill shows rates, item line totals, and grand total"
                    else
                        "OFF: Quantity-only slip (Rates & amounts hidden from customer)",
                    fontSize = 12.sp,
                    color = if (showPrice) Color(0xFF1D4ED8) else Color(0xFF64748B)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = showPrice,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PureWhite,
                    checkedTrackColor = Color(0xFF2563EB)
                ),
                modifier = Modifier.testTag("pdf_price_switch")
            )
        }
    }
}

@Composable
fun ConfirmDeleteOrderDialog(
    isConfirmed: Boolean,
    onDismiss: () -> Unit,
    onConfirmDelete: (restoreStock: Boolean) -> Unit
) {
    var restoreStockChoice by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = StatusError,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Delete Order",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isConfirmed) {
                    Text(
                        text = "This is a Confirmed Sale with deducted inventory. Please choose how to handle inventory stock:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { restoreStockChoice = true }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = restoreStockChoice,
                            onClick = { restoreStockChoice = true }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("1. Restore sold quantities to stock", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Adds the boxes & pieces back into available inventory", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { restoreStockChoice = false }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = !restoreStockChoice,
                            onClick = { restoreStockChoice = false }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("2. Delete without restoring stock", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Keeps current warehouse stock unchanged", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }
                } else {
                    Text(
                        text = "This order is Pending. Stock has NOT been deducted yet, so deleting it will not change inventory stock.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmDelete(if (isConfirmed) restoreStockChoice else false)
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text("Confirm Delete", color = PureWhite)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(actionLabel)
            }
        }
    }
}
