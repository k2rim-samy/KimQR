package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.QRMasterViewModel
import com.google.accompanist.permissions.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: QRMasterViewModel,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToPasscode: () -> Unit,
    onNavigateToMain: () -> Unit
) {
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()
    val passcode by viewModel.passcode.collectAsState()

    // Animation configs
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Laser sweep scan animation
    val laserYOffset by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    LaunchedEffect(Unit) {
        delay(2500) // Beautiful intro delay
        if (!onboardingCompleted) {
            onNavigateToOnboarding()
        } else if (passcode != null) {
            onNavigateToPasscode()
        } else {
            onNavigateToMain()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Stylized glowing Scanner Ring
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val padding = 20f

                    // Draw QR code exterior corner corners (Metallic look with Neon cyan/indigo brush)
                    val strokeWidth = 10f
                    val brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF6366F1), Color(0xFF06B6D4))
                    )

                    // Top-Left corner
                    drawPath(
                        path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(padding, padding + 40f)
                            lineTo(padding, padding)
                            lineTo(padding + 40f, padding)
                        },
                        brush = brush,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Top-Right corner
                    drawPath(
                        path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(w - padding, padding + 40f)
                            lineTo(w - padding, padding)
                            lineTo(w - padding - 40f, padding)
                        },
                        brush = brush,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Bottom-Left corner
                    drawPath(
                        path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(padding, h - padding - 40f)
                            lineTo(padding, h - padding)
                            lineTo(padding + 40f, h - padding)
                        },
                        brush = brush,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Bottom-Right corner
                    drawPath(
                        path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(w - padding, h - padding - 40f)
                            lineTo(w - padding, h - padding)
                            lineTo(w - padding - 40f, h - padding)
                        },
                        brush = brush,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Centered tiny QR Finder eye
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(w / 3f, h / 3f),
                        size = Size(w / 3f, h / 3f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
                        style = Stroke(width = 6f)
                    )
                    drawRect(
                        brush = brush,
                        topLeft = Offset(w / 2.5f, h / 2.5f),
                        size = Size(w / 5f, h / 5f)
                    )

                    // Glowing Sweeping Laser bar
                    val currentLaserLineY = padding + (h - 2 * padding) * laserYOffset
                    drawLine(
                        color = Color(0xFF06B6D4),
                        start = Offset(padding + 10f, currentLaserLineY),
                        end = Offset(w - padding - 10f, currentLaserLineY),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Text Typography branding
            Text(
                text = "KimQR",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Elegant Creator & Scanner",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
