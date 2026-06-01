package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.GenTabType
import com.example.ui.QRMasterViewModel
import com.example.util.BarcodeGenerator
import com.example.util.QRFormatUtils
import com.example.util.QRGenerator
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GenerateTab(
    viewModel: QRMasterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Tab Type: QR Code vs Barcode
    val genTabType by viewModel.genTabType.collectAsState()

    // 1. QR Code States from VM
    val genType by viewModel.genType.collectAsState()
    val previewBitmap by viewModel.previewBitmap.collectAsState()
    val primaryColorHex by viewModel.primaryColorHex.collectAsState()
    val secondaryColorHex by viewModel.secondaryColorHex.collectAsState()
    val isGradient by viewModel.isGradient.collectAsState()
    val qrStyle by viewModel.qrStyle.collectAsState()
    val eyeStyle by viewModel.eyeStyle.collectAsState()
    val logoType by viewModel.logoType.collectAsState()

    // 2. Barcode States from VM
    val barcodeContent by viewModel.barcodeContent.collectAsState()
    val barcodeType by viewModel.barcodeType.collectAsState()
    val barcodePrimaryColorHex by viewModel.barcodePrimaryColorHex.collectAsState()
    val barcodeBgColorHex by viewModel.barcodeBgColorHex.collectAsState()
    val barcodeShowText by viewModel.barcodeShowText.collectAsState()
    val barcodePreviewBitmap by viewModel.barcodePreviewBitmap.collectAsState()

    // Template Input values for QR
    var urlInput by remember { mutableStateOf("https://google.com") }
    var textInput by remember { mutableStateOf("") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPass by remember { mutableStateOf("") }
    var wifiSecurity by remember { mutableStateOf("WPA") }
    var phoneInput by remember { mutableStateOf("") }
    var smsPhone by remember { mutableStateOf("") }
    var smsBody by remember { mutableStateOf("") }
    var emailAddr by remember { mutableStateOf("") }
    var emailSubject by remember { mutableStateOf("") }
    var emailBody by remember { mutableStateOf("") }
    var vcardName by remember { mutableStateOf("") }
    var vcardPhone by remember { mutableStateOf("") }
    var vcardEmail by remember { mutableStateOf("") }
    var vcardOrg by remember { mutableStateOf("") }
    var vcardTitle by remember { mutableStateOf("") }
    var vcardUrl by remember { mutableStateOf("") }
    var locLat by remember { mutableStateOf("") }
    var locLon by remember { mutableStateOf("") }

    // Save history names dialogs
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveTitle by remember { mutableStateOf("") }

    var showSaveBarcodeDialog by remember { mutableStateOf(false) }
    var saveBarcodeTitle by remember { mutableStateOf("") }

    // Auto update content formatted output when inputs of the active template alters
    LaunchedEffect(
        genType, urlInput, textInput, wifiSsid, wifiPass, wifiSecurity,
        phoneInput, smsPhone, smsBody, emailAddr, emailSubject, emailBody,
        vcardName, vcardPhone, vcardEmail, vcardOrg, vcardTitle, vcardUrl,
        locLat, locLon
    ) {
        val payload = when (genType) {
            "URL" -> urlInput
            "TEXT" -> textInput
            "WIFI" -> QRFormatUtils.formatWiFi(wifiSsid, wifiPass, wifiSecurity)
            "PHONE" -> QRFormatUtils.formatPhone(phoneInput)
            "SMS" -> QRFormatUtils.formatSMS(smsPhone, smsBody)
            "EMAIL" -> QRFormatUtils.formatEmail(emailAddr, emailSubject, emailBody)
            "VCARD" -> QRFormatUtils.formatvCard(vcardName, vcardPhone, vcardEmail, vcardOrg, vcardTitle, vcardUrl)
            "LOCATION" -> QRFormatUtils.formatLocation(locLat, locLon)
            else -> textInput
        }
        viewModel.updateGenContent(payload)
    }

    // Available categories for QR
    val categories = listOf(
        GenCategory(R.string.cat_url, "URL", Icons.Default.Link),
        GenCategory(R.string.cat_text, "TEXT", Icons.Default.TextSnippet),
        GenCategory(R.string.cat_wifi, "WIFI", Icons.Default.Wifi),
        GenCategory(R.string.cat_phone, "PHONE", Icons.Default.Phone),
        GenCategory(R.string.cat_sms, "SMS", Icons.Default.Sms),
        GenCategory(R.string.cat_email, "EMAIL", Icons.Default.Email),
        GenCategory(R.string.cat_contact, "VCARD", Icons.Default.ContactPage),
        GenCategory(R.string.cat_location, "LOCATION", Icons.Default.LocationOn)
    )

    // Palette presets for custom design colors
    val colorPresets = listOf(
        "#000000", // Dark slate black (standard)
        "#6366F1", // Indigo
        "#06B6D4", // Cyan
        "#8B5CF6", // Purple
        "#10B981", // Emerald Green
        "#F59E0B", // Amber Gold
        "#EF4444"  // Crimson Red
    )

    val backgroundPresets = listOf(
        "#FFFFFF", // Classic white
        "#F8FAFC", // Slate white
        "#FFF7ED", // Warm peach/cream
        "#F0FDF4"  // Mint green transparency
    )

    // Layout Root Container
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(top = 16.dp, bottom = 120.dp) // Cushion nav items overlap
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        
        // App top tab switcher slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp)
        ) {
            val qrSelected = genTabType == GenTabType.QR_CODE
            // QR Tab Option
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (qrSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { viewModel.setGenTabType(GenTabType.QR_CODE) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        tint = if (qrSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.gen_tab_qr_custom),
                        color = if (qrSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            val barcodeSelected = genTabType == GenTabType.BARCODE
            // Barcode Tab Option
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (barcodeSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { viewModel.setGenTabType(GenTabType.BARCODE) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ViewWeek,
                        contentDescription = null,
                        tint = if (barcodeSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.gen_tab_barcode),
                        color = if (barcodeSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // RENDERING MAIN TAB CONTENT BODY
        if (genTabType == GenTabType.QR_CODE) {
            // ==========================================
            // QR CODE GENERATOR BRANCH
            // ==========================================

            // App top label branding
            Text(
                text = stringResource(R.string.gen_header_qr),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // LazyRow of categories selects
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val active = cat.id == genType
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .clickable { viewModel.updateGenContent("", cat.id) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = cat.icon,
                                contentDescription = null,
                                tint = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(cat.labelRes),
                                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // INPUT FIELDS CONTAINER CARD (Material 3 Surface elevated looks)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (genType) {
                        "URL" -> {
                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                label = { Text(stringResource(R.string.input_url), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = getTextFieldColors(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                            )
                        }
                        "TEXT" -> {
                            OutlinedTextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                label = { Text(stringResource(R.string.input_text), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = getTextFieldColors(),
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "WIFI" -> {
                            OutlinedTextField(
                                value = wifiSsid,
                                onValueChange = { wifiSsid = it },
                                label = { Text(stringResource(R.string.input_wifi_ssid), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = getTextFieldColors(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = wifiPass,
                                onValueChange = { wifiPass = it },
                                label = { Text(stringResource(R.string.input_wifi_pass), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = getTextFieldColors(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                            )
                            // Security Selector row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("WPA", "WEP", "None").forEach { sec ->
                                    val selected = sec == wifiSecurity
                                    Button(
                                        onClick = { wifiSecurity = sec },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(sec, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        "PHONE" -> {
                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = { phoneInput = it },
                                label = { Text(stringResource(R.string.input_phone), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = getTextFieldColors(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )
                        }
                        "SMS" -> {
                            OutlinedTextField(
                                value = smsPhone,
                                onValueChange = { smsPhone = it },
                                label = { Text(stringResource(R.string.input_sms_phone), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = getTextFieldColors(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )
                            OutlinedTextField(
                                value = smsBody,
                                onValueChange = { smsBody = it },
                                label = { Text(stringResource(R.string.input_sms_body), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = getTextFieldColors(),
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "EMAIL" -> {
                            OutlinedTextField(
                                value = emailAddr,
                                onValueChange = { emailAddr = it },
                                label = { Text(stringResource(R.string.input_email_addr), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = getTextFieldColors(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )
                            OutlinedTextField(
                                value = emailSubject,
                                onValueChange = { emailSubject = it },
                                label = { Text(stringResource(R.string.input_email_subject), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = getTextFieldColors(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = emailBody,
                                onValueChange = { emailBody = it },
                                label = { Text(stringResource(R.string.input_email_body), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = getTextFieldColors(),
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "VCARD" -> {
                            OutlinedTextField(
                                value = vcardName,
                                onValueChange = { vcardName = it },
                                label = { Text(stringResource(R.string.input_vcard_name), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = getTextFieldColors(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = vcardPhone,
                                onValueChange = { vcardPhone = it },
                                label = { Text(stringResource(R.string.input_vcard_phone), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = getTextFieldColors(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )
                            OutlinedTextField(
                                value = vcardEmail,
                                onValueChange = { vcardEmail = it },
                                label = { Text(stringResource(R.string.input_vcard_email), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = getTextFieldColors(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )
                            OutlinedTextField(
                                value = vcardOrg,
                                onValueChange = { vcardOrg = it },
                                label = { Text(stringResource(R.string.input_vcard_org), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = getTextFieldColors(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "LOCATION" -> {
                            OutlinedTextField(
                                value = locLat,
                                onValueChange = { locLat = it },
                                label = { Text(stringResource(R.string.input_loc_lat), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = getTextFieldColors(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = locLon,
                                onValueChange = { locLon = it },
                                label = { Text(stringResource(R.string.input_loc_lon), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = getTextFieldColors(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // DYNAMIC QR PREVIEW CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .width(280.dp)
                    .wrapContentHeight()
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
                                )
                            )
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (previewBitmap != null) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.size(210.dp)
                        ) {
                            Image(
                                bitmap = previewBitmap!!.asImageBitmap(),
                                contentDescription = "QR Code Preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                 Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    stringResource(R.string.preview_placeholder),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // DESIGN STYLE CUSTOMIZER SECTION HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.custom_features_header), color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }

            // Custom QR styles options card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Gradient options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.custom_gradient_toggle), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = isGradient,
                            onCheckedChange = { viewModel.updateDesign(gradient = it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.secondary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        )
                    }

                    // Choose colors
                    Text(stringResource(R.string.custom_primary_color), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(colorPresets) { hex ->
                            val selected = hex == primaryColorHex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .border(
                                        if (selected) 3.dp else 0.dp,
                                        if (selected) Color.White else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { viewModel.updateDesign(primaryHex = hex) }
                            )
                        }
                    }

                    if (isGradient) {
                        Text(stringResource(R.string.custom_secondary_color), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(colorPresets) { hex ->
                                val selected = hex == secondaryColorHex
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                        .border(
                                            if (selected) 3.dp else 0.dp,
                                            if (selected) Color.White else Color.Transparent,
                                            CircleShape
                                        )
                                        .clickable { viewModel.updateDesign(secondaryHex = hex) }
                                )
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Node patterns style picker
                    Text(stringResource(R.string.custom_style_dots), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            QRGenerator.QrStyle.SQUARE to R.string.custom_style_sq,
                            QRGenerator.QrStyle.DOTS to R.string.custom_style_dt,
                            QRGenerator.QrStyle.ROUNDED to R.string.custom_style_rd
                        ).forEach { pair ->
                            val selected = pair.first == qrStyle
                            Button(
                                onClick = { viewModel.updateDesign(style = pair.first) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(pair.second), color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Eye Borders corners custom selector
                    Text(stringResource(R.string.custom_eye_style_label), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            QRGenerator.EyeStyle.SQUARE to R.string.custom_eye_sq,
                            QRGenerator.EyeStyle.CIRCLE to R.string.custom_eye_ci,
                            QRGenerator.EyeStyle.ROUNDED to R.string.custom_eye_rd
                        ).forEach { pair ->
                            val selected = pair.first == eyeStyle
                            Button(
                                onClick = { viewModel.updateDesign(eye = pair.first) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(pair.second), color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Logo custom overlay badge center
                    Text(stringResource(R.string.custom_logo_label), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            QRGenerator.LogoType.NONE to R.string.logo_none,
                            QRGenerator.LogoType.WIFI to null,
                            QRGenerator.LogoType.FACEBOOK to null,
                            QRGenerator.LogoType.INSTAGRAM to null,
                            QRGenerator.LogoType.TELEGRAM to null,
                            QRGenerator.LogoType.WHATSAPP to null,
                            QRGenerator.LogoType.GOOGLE to null
                        ).forEach { pair ->
                            val selected = pair.first == logoType
                            val textLabel = if (pair.second != null) stringResource(pair.second!!) else pair.first.name.lowercase().replaceFirstChar { it.uppercase() }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { viewModel.updateDesign(logo = pair.first) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(textLabel, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ACTION BUTTONS SECTION LAYOUT FOR QR
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Save PNG
                Button(
                    onClick = {
                        if (previewBitmap != null) {
                            saveBitmapToGallery(context, previewBitmap!!)
                        } else {
                            Toast.makeText(context, context.getString(R.string.toast_input_required), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_save_png), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }

                // Export PDF
                Button(
                    onClick = {
                        if (previewBitmap != null) {
                            saveQRAsPdf(context, previewBitmap!!)
                        } else {
                            Toast.makeText(context, context.getString(R.string.toast_input_required), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_export_pdf), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }

                // Native Share
                Button(
                    onClick = {
                        if (previewBitmap != null) {
                            shareBitmap(context, previewBitmap!!)
                        } else {
                            Toast.makeText(context, context.getString(R.string.toast_input_required), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_share_direct), color = MaterialTheme.colorScheme.onTertiary, fontWeight = FontWeight.Bold)
                }

                // Save in database history
                Button(
                    onClick = {
                        if (previewBitmap != null) {
                            saveTitle = when (genType) {
                                "WIFI" -> context.getString(R.string.type_wifi, wifiSsid)
                                "URL" -> context.getString(R.string.type_url, urlInput)
                                "TEXT" -> context.getString(R.string.type_text, textInput.take(20))
                                else -> context.getString(R.string.gen_tab_qr_custom)
                            }
                            showSaveDialog = true
                        } else {
                            Toast.makeText(context, context.getString(R.string.toast_input_required), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Icon(Icons.Default.LibraryAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حفظ الباركود وتثبيته في السجل", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }

        } else {
            // ==========================================
            // BARCODE GENERATOR BRANCH
            // ==========================================

            Text(
                text = "إنشاء باركود Barcode مخصص",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic live validation bounds checks
            val validationError = remember(barcodeContent, barcodeType) {
                BarcodeGenerator.validateInput(barcodeContent, barcodeType)
            }

            // INPUT CARD FOR BARCODE
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("محتوى الرمز وصيغته", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = barcodeContent,
                        onValueChange = { viewModel.updateBarcodeContent(it) },
                        label = { Text("أدخل الكود المراد تشفيره", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = getTextFieldColors(),
                        singleLine = true,
                        isError = validationError != null,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                    )

                    // Dynamic Warning Message displayed instantly
                    if (validationError != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = validationError,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        val formatGuidance = when (barcodeType) {
                            BarcodeGenerator.BarcodeType.CODE_128 -> "يدعم جميع الحروف والرموز الإنجليزية القياسية (مثالي)"
                            BarcodeGenerator.BarcodeType.CODE_39 -> "يدعم الحروف الأبجدية الكبيرة والأرقام ورموز محدودة"
                            BarcodeGenerator.BarcodeType.EAN_13 -> "يتطلب 12 أو 13 رقمًا من الأرقام فقط"
                            BarcodeGenerator.BarcodeType.UPC_A -> "يتطلب 11 أو 12 رقمًا من الأرقام فقط"
                            BarcodeGenerator.BarcodeType.ISBN -> "يتطلب 10 أو 13 رقمًا مخصصًا للكتب والمقالات"
                        }
                        Text(
                            text = "صيغة مقبولة: $formatGuidance",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Barcode Format Option Grid
                    Text("اختر صيغة كود الباركود", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            BarcodeGenerator.BarcodeType.CODE_128 to "Code 128",
                            BarcodeGenerator.BarcodeType.CODE_39 to "Code 39",
                            BarcodeGenerator.BarcodeType.EAN_13 to "EAN-13",
                            BarcodeGenerator.BarcodeType.UPC_A to "UPC-A",
                            BarcodeGenerator.BarcodeType.ISBN to "ISBN"
                        ).forEach { pair ->
                            val selected = pair.first == barcodeType
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { viewModel.updateBarcodeType(pair.first) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = pair.second,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // LIVE BARCODE PREVIEW DISPLAY CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
                                )
                            )
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (barcodePreviewBitmap != null) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            Image(
                                bitmap = barcodePreviewBitmap!!.asImageBitmap(),
                                contentDescription = "Barcode Preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.ViewWeek,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "اكتب شيئًا بتنسيق صالح للمعاينة...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CUSTOMIZATION CUSTOMER CARD FOR BARCODE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تخصيص ألوان وتصميم الباركود", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Show text under code toggle option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("إظهار النص التوضيحي أسفل الباركود", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = barcodeShowText,
                            onCheckedChange = { viewModel.updateBarcodeDesign(showText = it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

                    // Bar customized colors
                    Text("اللون الرئيسي للخطوط", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(colorPresets) { hex ->
                            val selected = hex == barcodePrimaryColorHex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .border(
                                        if (selected) 3.dp else 0.dp,
                                        if (selected) Color.White else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { viewModel.updateBarcodeDesign(primaryHex = hex) }
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

                    // Background customized colors
                    Text("لون خلفية الباركود", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(backgroundPresets) { hex ->
                            val selected = hex == barcodeBgColorHex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .border(
                                        if (selected) 3.dp else 4.dp,
                                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                        CircleShape
                                    )
                                    .clickable { viewModel.updateBarcodeDesign(bgHex = hex) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ACTION BUTTONS SECTION LAYOUT FOR BARCODE
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Save PNG
                Button(
                    onClick = {
                        if (barcodePreviewBitmap != null) {
                            saveBitmapToGallery(context, barcodePreviewBitmap!!)
                        } else {
                            Toast.makeText(context, "الرجاء كتابة محتوى باركود صالح أولاً!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حفظ بجودة عالية في المعرض (PNG)", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }

                // Export PDF
                Button(
                    onClick = {
                        if (barcodePreviewBitmap != null) {
                            saveQRAsPdf(context, barcodePreviewBitmap!!)
                        } else {
                            Toast.makeText(context, "الرجاء كتابة محتوى باركود صالح أولاً!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تصدير كملف للطباعة (PDF)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }

                // Native Share
                Button(
                    onClick = {
                        if (barcodePreviewBitmap != null) {
                            shareBitmap(context, barcodePreviewBitmap!!)
                        } else {
                            Toast.makeText(context, "الرجاء كتابة محتوى أولاً!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مشاركة الباركود مباشرة", color = MaterialTheme.colorScheme.onTertiary, fontWeight = FontWeight.Bold)
                }

                // Save in database history
                Button(
                    onClick = {
                        if (barcodePreviewBitmap != null) {
                            saveBarcodeTitle = "باركود (${barcodeType.name}): ${barcodeContent.take(15)}"
                            showSaveBarcodeDialog = true
                        } else {
                            Toast.makeText(context, "الرجاء كتابة محتوى أولاً!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Icon(Icons.Default.LibraryAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حفظ وتثبيت الباركود في السجل", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Save Title Dialog Popup for QRs
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("حفظ رمز الـ QR في السجل", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("أدخل عنواناً معبراً ليسهل البحث عنه مستقبلاً:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    OutlinedTextField(
                        value = saveTitle,
                        onValueChange = { saveTitle = it },
                        singleLine = true,
                        colors = getTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCurrentGeneratedCode(saveTitle)
                        showSaveDialog = false
                        Toast.makeText(context, "تم الحفظ بالسجل بنجاح!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("حفظ", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("إلغاء", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }

    // Save Dialog Popup for Barcode
    if (showSaveBarcodeDialog) {
        AlertDialog(
            onDismissRequest = { showSaveBarcodeDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("حفظ رمز الباركود في السجل", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("أدخل عنواناً معبراً للباركود:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    OutlinedTextField(
                        value = saveBarcodeTitle,
                        onValueChange = { saveBarcodeTitle = it },
                        singleLine = true,
                        colors = getTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCurrentGeneratedBarcode(saveBarcodeTitle)
                        showSaveBarcodeDialog = false
                        Toast.makeText(context, "تم الحفظ بالسجل بنجاح!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("حفظ", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveBarcodeDialog = false }) {
                    Text("إلغاء", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }
}

// Helper Text fields options modifiers
@Composable
private fun getTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
)

data class GenCategory(
    val labelRes: Int,
    val id: String,
    val icon: ImageVector
)

// LOCAL UTILITY FOR SAVING CHUNKS PNG TO GALLERY
private fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
    try {
        val filename = "QRMaster_${System.currentTimeMillis()}.png"
        var fos: OutputStream? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/QRMaster")
            }
            val imageUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                fos = context.contentResolver.openOutputStream(imageUri)
            }
        } else {
            val imagesDir = context.getExternalFilesDir("Pictures") ?: context.filesDir
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }

        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            Toast.makeText(context, "تم حفظ الصورة بنجاح في مجلد الصور (Pictures/QRMaster)!", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "فشل حفظ الملف!", Toast.LENGTH_SHORT).show()
    }
}

// LOCAL UTILITY FOR SHARING BITMAP
private fun shareBitmap(context: Context, bitmap: Bitmap) {
    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "shared_qr.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri = FileProvider.getUriForFile(context, "com.aistudio.qrmaster.vktqz.fileprovider", file)
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة الرمز"))
    } catch (e: Exception) {
        Toast.makeText(context, "فشل مشاركة الرمز!", Toast.LENGTH_SHORT).show()
    }
}

// LOCAL UTILITY FOR SAVING GENERATED PDF DOCUMENT
private fun saveQRAsPdf(context: Context, bitmap: Bitmap) {
    var document: PdfDocument? = null
    try {
        document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = document.startPage(pageInfo)
        
        val canvas = page.canvas
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        document.finishPage(page)

        val filename = "QRMaster_${System.currentTimeMillis()}.pdf"
        var fos: OutputStream? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/QRMaster")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                fos = context.contentResolver.openOutputStream(uri)
                fos?.use {
                    document?.writeTo(it)
                }
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
                Toast.makeText(context, "تم حفظ PDF بنجاح في مجلد Downloads/QRMaster", Toast.LENGTH_LONG).show()
            }
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val appDir = File(dir, "QRMaster")
            if (!appDir.exists()) appDir.mkdirs()
            val file = File(appDir, filename)
            fos = FileOutputStream(file)
            fos?.use {
                document?.writeTo(it)
                Toast.makeText(context, "تم حفظ PDF بنجاح في Downloads/QRMaster", Toast.LENGTH_LONG).show()
            }
            // Notify MediaScanner for older Android
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = Uri.fromFile(file)
            context.sendBroadcast(intent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "فشل تصدير PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    } finally {
        document?.close()
    }
}
