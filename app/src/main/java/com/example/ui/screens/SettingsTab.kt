package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.QRMasterViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    viewModel: QRMasterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val themeDark by viewModel.themeDark.collectAsState()
    val playBeep by viewModel.beepEnabled.collectAsState()
    val playVibrate by viewModel.vibrateEnabled.collectAsState()
    val autoSaveScanned by viewModel.autoSaveScanned.collectAsState()
    val qrExportQuality by viewModel.qrExportQuality.collectAsState()
    val passcodePin by viewModel.passcode.collectAsState()

    // Dialog controllers
    var showPinDialog by remember { mutableStateOf(false) }
    var pinValue by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(top = 16.dp, bottom = 100.dp)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Settings Header
        Text(
            text = stringResource(R.string.settings_title),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // 1. GENERAL/VISUAL PANEL CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Style/Theme toggler
                SettingsRow(
                    label = stringResource(R.string.settings_theme_title),
                    desc = stringResource(R.string.settings_theme_desc),
                    icon = Icons.Default.DarkMode,
                    color = MaterialTheme.colorScheme.tertiary,
                    action = {
                        Switch(
                            checked = themeDark,
                            onCheckedChange = { viewModel.toggleTheme(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.secondary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        )
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Language explanation indicator (fully automatic locale adaptation)
                SettingsRow(
                    label = stringResource(R.string.settings_current_language_title),
                    desc = stringResource(R.string.settings_current_language_desc),
                    icon = Icons.Default.Language,
                    color = MaterialTheme.colorScheme.primary,
                    action = {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = java.util.Locale.getDefault().displayLanguage,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )
            }
        }

        // 2. SCANNER & EXPORTS CONFIGURATION CARD
        Text(
            text = if (java.util.Locale.getDefault().language == "ar") "المسح الفحص وصناعة الأكواد" else "Scanner & Code Generator",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sound beep feedback toggle
                SettingsRow(
                    label = stringResource(R.string.settings_beep_title),
                    desc = stringResource(R.string.settings_beep_desc),
                    icon = Icons.Default.VolumeUp,
                    color = MaterialTheme.colorScheme.secondary,
                    action = {
                        Switch(
                            checked = playBeep,
                            onCheckedChange = { viewModel.toggleBeep(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.secondary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        )
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Vibration feedback toggle
                SettingsRow(
                    label = stringResource(R.string.settings_vibrate_title),
                    desc = stringResource(R.string.settings_vibrate_desc),
                    icon = Icons.Default.Vibration,
                    color = Color(0xFFFF9800),
                    action = {
                        Switch(
                            checked = playVibrate,
                            onCheckedChange = { viewModel.toggleVibrate(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFF9800),
                                checkedTrackColor = Color(0xFFFFB74D).copy(alpha = 0.5f)
                            )
                        )
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Scan autosave toggle
                SettingsRow(
                    label = stringResource(R.string.settings_autosave_title),
                    desc = stringResource(R.string.settings_autosave_desc),
                    icon = Icons.Default.Save,
                    color = Color(0xFF4CAF50),
                    action = {
                        Switch(
                            checked = autoSaveScanned,
                            onCheckedChange = { viewModel.toggleAutoSave(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF4CAF50),
                                checkedTrackColor = Color(0xFF81C784).copy(alpha = 0.5f)
                            )
                        )
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Export/Generate resolution quality settings
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AspectRatio, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text(stringResource(R.string.settings_export_title), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.settings_export_desc), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 14.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isArabic = java.util.Locale.getDefault().language == "ar"
                        listOf(
                            256 to if (isArabic) "منخفضة (256)" else "Low (256)",
                            512 to if (isArabic) "متوسطة (512)" else "Medium (512)",
                            1024 to if (isArabic) "عالية (1024)" else "High (1024)"
                        ).forEach { (size, label) ->
                            val isSelected = qrExportQuality == size
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setQrExportQuality(size) },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 3. SECURITY & CACHE ACTIONS CARD
        Text(
            text = stringResource(R.string.settings_security_title),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Passcode PIN lock/toggle
                val pinSet = passcodePin != null
                SettingsRow(
                    label = stringResource(R.string.settings_passcode_title),
                    desc = if (pinSet) stringResource(R.string.settings_passcode_desc_set) else stringResource(R.string.settings_passcode_desc_unset),
                    icon = Icons.Default.Security,
                    color = MaterialTheme.colorScheme.primary,
                    action = {
                        Button(
                            onClick = {
                                if (pinSet) {
                                    viewModel.setPasscode(null)
                                    val msg = if (java.util.Locale.getDefault().language == "ar") "تم إزالة قفل الحماية بنجاح!" else "Security lock removed successfully!"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                } else {
                                    pinValue = ""
                                    showPinDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (pinSet) Color.Red.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(
                                text = if (pinSet) stringResource(R.string.settings_passcode_btn_disable) else stringResource(R.string.settings_passcode_btn_enable),
                                color = if (pinSet) Color.Red else MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Onboarding Reset control
                SettingsRow(
                    label = stringResource(R.string.settings_onboarding_reset_title),
                    desc = stringResource(R.string.settings_onboarding_reset_desc),
                    icon = Icons.Default.RotateLeft,
                    color = MaterialTheme.colorScheme.secondary,
                    action = {
                        Button(
                            onClick = {
                                viewModel.resetOnboarding()
                                val msg = if (java.util.Locale.getDefault().language == "ar") "تم تصفير حالة التشغيل بنجاح! سيتم إظهار الدليل مجدداً عند تشغيل التطبيق." else "Welcome guide reset! It will show again upon next launch."
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_onboarding_reset_btn),
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Clear history option row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showClearDialog = true }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Red.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(stringResource(R.string.settings_clear_history_title), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.settings_clear_history_desc), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 14.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ABOUT DETAILS SECTION
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("KimQR", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(stringResource(R.string.settings_gold_version), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.settings_app_desc),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    }

    // Passcode Configuration popup dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.settings_pin_dialog_title), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.settings_pin_dialog_desc), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    OutlinedTextField(
                        value = pinValue,
                        onValueChange = { newValue ->
                            if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                                pinValue = newValue
                            }
                        },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinValue.length == 4) {
                            viewModel.setPasscode(pinValue)
                            showPinDialog = false
                            val msg = if (java.util.Locale.getDefault().language == "ar") "تم تمكين رمز المرور PIN بنجاح لتأمين الذاكرة والسجل!" else "Security passcode PIN set successfully!"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        } else {
                            val msg = if (java.util.Locale.getDefault().language == "ar") "يرجى كتابة 4 أرقام لرمز PIN السري!" else "Please write exactly 4 numbers for security PIN!"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.settings_pin_dialog_btn), color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text(stringResource(R.string.settings_dialog_cancel), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }

    // Clear confirmation popup dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.settings_clear_dialog_title), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text(stringResource(R.string.settings_clear_dialog_desc), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearDialog = false
                        val msg = if (java.util.Locale.getDefault().language == "ar") "تم تصفير وإفراغ السجل بنجاح تام!" else "History and favorites database wiped out completely!"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(stringResource(R.string.settings_clear_confirm_btn), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.settings_dialog_cancel), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }
}

@Composable
fun SettingsRow(
    label: String,
    desc: String,
    icon: ImageVector,
    color: Color,
    action: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(desc, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 14.sp)
            }
        }
        action()
    }
}
