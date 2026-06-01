package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.database.QRCodeEntity
import com.example.ui.QRMasterViewModel
import com.example.ui.theme.AccentGold

@Composable
fun HistoryTab(
    viewModel: QRMasterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val listItems by viewModel.historyQRCodes.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp, bottom = 100.dp) // Leave blank padding for navbar
            .padding(horizontal = 24.dp)
    ) {
        // Tab Page Header with Clear All Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.history_header_title),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )

            if (listItems.isNotEmpty()) {
                var showClearConfirm by remember { mutableStateOf(false) }

                IconButton(
                    onClick = { showClearConfirm = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = stringResource(R.string.desc_clear_all),
                        tint = Color.Red
                    )
                }

                if (showClearConfirm) {
                    AlertDialog(
                        onDismissRequest = { showClearConfirm = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        title = {
                            Text(
                                text = stringResource(R.string.dialog_clear_all_confirm_title),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.dialog_clear_all_confirm_desc),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.clearAllHistory()
                                    showClearConfirm = false
                                    Toast.makeText(context, context.getString(R.string.toast_history_clear_success), Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text(stringResource(R.string.dialog_clear_all_confirm_btn), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearConfirm = false }) {
                                Text(stringResource(R.string.btn_cancel), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    )
                }
            }
        }

        // Glassy Search Input Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.search(it) },
            placeholder = { Text(stringResource(R.string.search_history_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.search("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Horizontal Category Filter Row chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            val filters = listOf(
                FilterPreset(stringResource(R.string.filter_all), "ALL"),
                FilterPreset(stringResource(R.string.filter_scanned), "SCANNED"),
                FilterPreset(stringResource(R.string.filter_generated), "GENERATED"),
                FilterPreset(stringResource(R.string.filter_favorites), "FAVORITES")
            )

            filters.forEach { flt ->
                val active = flt.id == filterType
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .clickable { viewModel.setFilter(flt.id) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = flt.label,
                        color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // History list viewer
        if (listItems.isEmpty()) {
            // Elegant empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 48.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.history_empty_state_title),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.history_empty_state_desc),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(listItems, key = { it.id }) { qr ->
                    HistoryItemCard(
                        qr = qr,
                        onDeleteClick = { viewModel.deleteQRCode(qr) },
                        onPinClick = { 
                            val updated = qr.copy(isPinned = !qr.isPinned)
                            viewModel.updateQRCode(updated)
                        },
                        onFavClick = {
                            val updated = qr.copy(isFavorite = !qr.isFavorite)
                            viewModel.updateQRCode(updated)
                        },
                        onUseAgain = {
                            if (qr.type == "BARCODE") {
                                viewModel.setGenTabType(com.example.ui.GenTabType.BARCODE)
                                viewModel.updateBarcodeContent(qr.content)
                                try {
                                    val formatEnum = com.example.util.BarcodeGenerator.BarcodeType.valueOf(qr.qrStyle ?: "CODE_128")
                                    viewModel.updateBarcodeType(formatEnum)
                                } catch (e: Exception) {}
                            } else {
                                viewModel.setGenTabType(com.example.ui.GenTabType.QR_CODE)
                                viewModel.updateGenContent(qr.content, qr.type)
                            }
                            Toast.makeText(context, context.getString(R.string.toast_code_loaded_editor), Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    qr: QRCodeEntity,
    onDeleteClick: () -> Unit,
    onPinClick: () -> Unit,
    onFavClick: () -> Unit,
    onUseAgain: () -> Unit
) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (qr.isPinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Type Icon & Pin/delete options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (qr.type == "BARCODE") MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else if (qr.isScanned) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (qr.type == "BARCODE") Icons.Default.ViewWeek
                            else if (qr.isScanned) Icons.Default.QrCodeScanner
                            else Icons.Default.Palette,
                            contentDescription = null,
                            tint = if (qr.type == "BARCODE") MaterialTheme.colorScheme.primary
                            else if (qr.isScanned) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Content Type Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(qr.type, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Control quick utilities
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Pin option trigger
                    IconButton(onClick = onPinClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (qr.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                            contentDescription = "Pin",
                            tint = if (qr.isPinned) AccentGold else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Favorite trigger
                    IconButton(onClick = onFavClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (qr.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (qr.isFavorite) Color.Red else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Delete item
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Central layout details titles
            Column {
                Text(
                    text = qr.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = qr.content,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // Footer quick actions text triggers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary use layout detail
                Text(
                    text = if (qr.isScanned) stringResource(R.string.scanned_automatically_footer) else stringResource(R.string.generated_locally_footer),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Copy text trigger
                    Text(
                        text = stringResource(R.string.copy_text_action),
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("QR Master", qr.content)
                                cb.setPrimaryClip(clip)
                                Toast.makeText(context, context.getString(R.string.toast_code_copied_success), Toast.LENGTH_SHORT).show()
                            }
                            .padding(4.dp)
                    )

                    // Re-use trigger
                    Text(
                        text = stringResource(R.string.reuse_action),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onUseAgain() }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

data class FilterPreset(
    val label: String,
    val id: String
)
