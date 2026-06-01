package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.QRMasterViewModel

@Composable
fun OnboardingScreen(
    viewModel: QRMasterViewModel,
    onNavigateToMain: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }
    val totalPages = 3

    val primaryCol = MaterialTheme.colorScheme.primary
    val secondaryCol = MaterialTheme.colorScheme.secondary
    val tertiaryCol = MaterialTheme.colorScheme.tertiary
    val onPrimaryCol = MaterialTheme.colorScheme.onPrimary

    val slides = listOf(
        OnboardingSlide(
            title = stringResource(R.string.onboarding_slide1_title),
            description = stringResource(R.string.onboarding_slide1_desc),
            icon = Icons.Default.QrCodeScanner,
            color = secondaryCol,
            artDraw = { color ->
                // Custom scanner visual representation
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawCircle(
                        color = color.copy(alpha = 0.15f),
                        radius = w / 2.5f,
                        center = Offset(w/2f, h/2f)
                    )
                    drawRect(
                        color = color,
                        topLeft = Offset(w/3f, h/3f),
                        size = androidx.compose.ui.geometry.Size(w/3f, h/3f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
                    )
                    drawLine(
                        color = color,
                        start = Offset(w/3f - 10f, h/2f),
                        end = Offset(2*w/3f + 10f, h/2f),
                        strokeWidth = 6f
                    )
                }
            }
        ),
        OnboardingSlide(
            title = stringResource(R.string.onboarding_slide2_title),
            description = stringResource(R.string.onboarding_slide2_desc),
            icon = Icons.Default.Palette,
            color = primaryCol,
            artDraw = { color ->
                // Creative gradient palette visual
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val gradient = Brush.linearGradient(
                        colors = listOf(color, tertiaryCol)
                    )
                    drawRoundRect(
                        brush = gradient,
                        topLeft = Offset(w/3.5f, h/3.5f),
                        size = androidx.compose.ui.geometry.Size(w/2.2f, h/2.2f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f)
                    )
                    // Draw micro circles on the side
                    drawCircle(Color.White.copy(alpha = 0.8f), radius = 12f, center = Offset(w/2f - 30f, h/2f))
                    drawCircle(Color.White.copy(alpha = 0.8f), radius = 12f, center = Offset(w/2f + 30f, h/2f))
                }
            }
        ),
        OnboardingSlide(
            title = stringResource(R.string.onboarding_slide3_title),
            description = stringResource(R.string.onboarding_slide3_desc),
            icon = Icons.Default.Security,
            color = tertiaryCol,
            artDraw = { color ->
                // Safeguard representations
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawCircle(
                        color = color.copy(alpha = 0.15f),
                        radius = w / 2.5f,
                        center = Offset(w/2f, h/2f)
                    )
                    // Draw a lock representation
                    val stroke = 6.dp.toPx()
                    drawArc(
                        color = color,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(w/2.6f, h/3.2f),
                        size = androidx.compose.ui.geometry.Size(w/4.3f, h/4f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                    )
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(w/2.9f, h/2.1f),
                        size = androidx.compose.ui.geometry.Size(w/3.2f, h/4.5f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                    )
                }
            }
        )
    )

    val currentSlide = slides[currentPage]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Upper Skip Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart // Arabic text aligned neatly
        ) {
            Text(
                text = stringResource(R.string.btn_skip),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier
                    .clickable {
                        viewModel.completeOnboarding()
                        onNavigateToMain()
                    }
                    .padding(8.dp)
            )
        }

        // Mid Card Panel with animations
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Visual Art Frame (Glassmorphic Container representation)
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .shadow(16.dp, RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                currentSlide.artDraw(currentSlide.color)
                
                // Overlay center tiny vector icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(currentSlide.color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = currentSlide.icon,
                        contentDescription = null,
                        tint = currentSlide.color,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Slide Title Text
            Text(
                text = currentSlide.title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Slide Description Text
            Text(
                text = currentSlide.description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Bottom Navigation Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            // Left (Indicator dots)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                for (i in 0 until totalPages) {
                    val active = i == currentPage
                    val wState = animateScalar(if (active) 24f else 8f)
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(wState.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) currentSlide.color else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            // Right (CTA Navigate Button)
            FloatingActionButton(
                onClick = {
                    if (currentPage < totalPages - 1) {
                        currentPage++
                    } else {
                        viewModel.completeOnboarding()
                        onNavigateToMain()
                    }
                },
                containerColor = currentSlide.color,
                contentColor = onPrimaryCol,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentPage == totalPages - 1) stringResource(R.string.btn_start_now) else stringResource(R.string.btn_next),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.btn_next),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun animateScalar(targetValue: Float): Float {
    val state = animateFloatAsState(
        targetValue = targetValue,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scalar"
    )
    return state.value
}

data class OnboardingSlide(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val artDraw: @Composable (Color) -> Unit
)
