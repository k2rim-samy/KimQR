package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.QRMasterViewModel
import com.example.util.QRReader
import com.google.accompanist.permissions.*
import java.io.InputStream
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanTab(
    viewModel: QRMasterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var hasCameraPermission by remember { mutableStateOf(false) }
    LaunchedEffect(permissionState.status) {
        hasCameraPermission = permissionState.status is PermissionStatus.Granted
    }

    val scanResult by viewModel.scanResult.collectAsState()

    val scope = rememberCoroutineScope()
    var isDecodingFromGallery by remember { mutableStateOf(false) }

    // Flash/Torch state
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControl: CameraControl? by remember { mutableStateOf<CameraControl?>(null) }

    // Visual animation for scanner boundary lines
    val infiniteTransition = rememberInfiniteTransition(label = "scan_laser")
    val laserYOffset by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    // Gallery Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                isDecodingFromGallery = true
                scope.launch {
                    try {
                        val decodedData = withContext(Dispatchers.IO) {
                            QRReader.decodeBitmapWithRotation(context, uri)
                        }
                        if (decodedData != null) {
                            viewModel.setScanResult(decodedData)
                        } else {
                            Toast.makeText(context, context.getString(R.string.gallery_no_code_toast), Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, context.getString(R.string.gallery_error_toast), Toast.LENGTH_SHORT).show()
                    } finally {
                        isDecodingFromGallery = false
                    }
                }
            }
        }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!hasCameraPermission) {
            // Permission request layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.camera_permission_required_title),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.camera_permission_required_desc),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { permissionState.launchPermissionRequest() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.camera_permission_grant_btn), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp))
                }
            }
        } else {
            // Camera scanner view bounds
            val cameraProviderState = remember { mutableStateOf<ProcessCameraProvider?>(null) }
            val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
            val executor = remember { Executors.newSingleThreadExecutor() }
            val isDisposed = remember { mutableStateOf(false) }
            val previewView = remember { 
                PreviewView(context).apply { 
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                } 
            }

            val preview = remember { Preview.Builder().build() }
            val imageAnalysis = remember {
                ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
            }

            LaunchedEffect(hasCameraPermission, lifecycleOwner) {
                if (hasCameraPermission) {
                    cameraProviderFuture.addListener({
                        if (isDisposed.value) return@addListener
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            cameraProviderState.value = cameraProvider

                            preview.setSurfaceProvider(previewView.surfaceProvider)

                            imageAnalysis.clearAnalyzer()
                            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                try {
                                    if (viewModel.scanResult.value == null) {
                                        val decodedData = QRReader.decodeImageProxy(imageProxy)
                                        if (decodedData != null) {
                                            viewModel.setScanResult(decodedData)
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    imageProxy.close()
                                }
                            }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            cameraProvider.unbindAll()
                            val cameraObj = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                            cameraControl = cameraObj.cameraControl
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            }

            DisposableEffect(lifecycleOwner) {
                onDispose {
                    isDisposed.value = true
                    try {
                        val provider = cameraProviderState.value
                        provider?.unbindAll()
                        preview.setSurfaceProvider(null)
                        imageAnalysis.clearAnalyzer()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    cameraControl = null
                    try {
                        executor.shutdown()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Transparent Mask covering layout with custom scanning target reticle
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Dim overlays around high-contrast viewing center
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black.copy(alpha = 0.5f)))
                    Row(modifier = Modifier.height(260.dp).fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.Black.copy(alpha = 0.5f)))
                        Box(
                            modifier = Modifier
                                .size(260.dp)
                                .border(2.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        ) {
                            // Moving Laser ray line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .offset(y = (260.dp * laserYOffset) - 2.dp)
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.Black.copy(alpha = 0.5f)))
                    }
                    Box(modifier = Modifier.weight(1.2f).fillMaxWidth().background(Color.Black.copy(alpha = 0.5f)))
                }

                // Top instructions banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp)
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.scan_instruction),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 20.dp)
                    )
                }

                // Bottom utility controller bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp)
                        .padding(horizontal = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Flash Light toggle trigger button
                    IconButton(
                        onClick = {
                            val target = !isFlashOn
                            isFlashOn = target
                            try {
                                cameraControl?.enableTorch(target)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = stringResource(R.string.desc_flash_toggle),
                            tint = if (isFlashOn) Color.Yellow else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Scan from gallery controller button
                    IconButton(
                        onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = stringResource(R.string.desc_gallery_sec),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        if (isDecodingFromGallery) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.gallery_loading_msg),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Animated Result action bottom sheet dialog
        AnimatedVisibility(
            visible = scanResult != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val contentStr = scanResult?.content ?: ""
            val formatStr = scanResult?.format ?: ""
            val isQr = formatStr == "QR_CODE"
            val isWifi = contentStr.startsWith("WIFI:", ignoreCase = true)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Grab handle aesthetic line representation
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = if (isQr) stringResource(R.string.scan_success_qr) else stringResource(R.string.scan_success_barcode),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Code type badge chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isQr) stringResource(R.string.scan_result_type_qr_label) else stringResource(R.string.scan_result_type_barcode_label, formatStr),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Text values box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = contentStr,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Dialog actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Copy Button
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Code Result", contentStr)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, context.getString(R.string.toast_copied), Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.btn_copy), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_copy), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }

                        // Open external intent action link / Connect WiFi
                        val isLink = !isWifi && (contentStr.startsWith("http://", ignoreCase = true) || contentStr.startsWith("https://", ignoreCase = true))
                        Button(
                            onClick = {
                                when {
                                    isWifi -> {
                                        val ssid = Regex("S:([^;]+)").find(contentStr)?.groupValues?.get(1) ?: ""
                                        val password = Regex("P:([^;]+)").find(contentStr)?.groupValues?.get(1) ?: ""
                                        val security = Regex("T:([^;]+)").find(contentStr)?.groupValues?.get(1) ?: ""
                                        
                                        if (ssid.isBlank()) {
                                            Toast.makeText(context, "Invalid WiFi QR data", Toast.LENGTH_SHORT).show()
                                        } else {
                                            try {
                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                                    val builder = WifiNetworkSuggestion.Builder()
                                                        .setSsid(ssid)
                                                    
                                                    if (password.isNotBlank()) {
                                                        try {
                                                            builder.setWpa2Passphrase(password)
                                                        } catch (ie: IllegalArgumentException) {
                                                            Toast.makeText(context, "Password must be at least 8 characters long", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                    
                                                    val wifiSuggestion = builder.build()
                                                    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                                                    val status = wifiManager.addNetworkSuggestions(listOf(wifiSuggestion))
                                                    
                                                    if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                                                        Toast.makeText(context, "Requested connection to $ssid (system suggestion)", Toast.LENGTH_LONG).show()
                                                    } else {
                                                        Toast.makeText(context, "Failed to suggest WiFi code: $status", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    @Suppress("DEPRECATION")
                                                    val wifiConfig = android.net.wifi.WifiConfiguration().apply {
                                                        SSID = "\"$ssid\""
                                                        if (password.isNotBlank()) {
                                                            preSharedKey = "\"$password\""
                                                        } else {
                                                            allowedKeyManagement.set(android.net.wifi.WifiConfiguration.KeyMgmt.NONE)
                                                        }
                                                    }
                                                    @Suppress("DEPRECATION")
                                                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                                                    @Suppress("DEPRECATION")
                                                    val netId = wifiManager.addNetwork(wifiConfig)
                                                    if (netId != -1) {
                                                        @Suppress("DEPRECATION")
                                                        wifiManager.disconnect()
                                                        @Suppress("DEPRECATION")
                                                        wifiManager.enableNetwork(netId, true)
                                                        @Suppress("DEPRECATION")
                                                        wifiManager.reconnect()
                                                        Toast.makeText(context, "Connecting to $ssid...", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Failed to configure WiFi network", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } catch (t: Throwable) {
                                                Toast.makeText(context, "Error: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                    isLink -> {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(contentStr))
                                        context.startActivity(intent)
                                    }
                                    else -> {
                                        // General send intent text share
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, contentStr)
                                        }
                                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_text_chooser)))
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            val icon = when {
                                isWifi -> Icons.Default.Wifi
                                isLink -> Icons.Default.OpenInBrowser
                                else -> Icons.Default.Share
                            }
                            val description = when {
                                isWifi -> "Connect to WiFi" // Should really be a resource string
                                isLink -> stringResource(R.string.btn_open_link)
                                else -> stringResource(R.string.btn_share)
                            }
                            val textStr = when {
                                isWifi -> "Connect" // Should really be a resource string
                                isLink -> stringResource(R.string.btn_open_link)
                                else -> stringResource(R.string.btn_share)
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = description,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = textStr,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary close dismiss scanner
                    TextButton(
                        onClick = { viewModel.clearScanResult() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.btn_scan_new), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
