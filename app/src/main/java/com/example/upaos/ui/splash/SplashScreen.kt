package com.example.upaos.ui.splash

import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.upaos.R
import com.example.upaos.data.local.TokenManager
import com.example.upaos.ui.theme.LocalUpaoDarkTheme
import kotlinx.coroutines.delay

internal object SplashColors {
    val NavyTop = Color(0xFF031238)
    val NavyCenter = Color(0xFF072260)
    val NavyBottom = Color(0xFF020B24)
    val CurveFillStart = Color(0xFF0B3284)
    val CurveFillEnd = Color(0xFF041747)
    val CurveStroke = Color(0xFF1D4ED8)
    val LogoFrame = Color(0xFF094CA7)
    val LogoBorder = Color(0xFF3B82F6)
    val ProgressActive = Color(0xFF1A73F8)
    val ProgressTrack = Color(0xFF1A3466)
    val TitleWhite = Color(0xFFFFFFFF)
}

@Composable
internal fun UpaoSplashScreen(onTimeout: () -> Unit) {
    val splashContext = LocalContext.current
    val view = LocalView.current
    val isDarkTheme = LocalUpaoDarkTheme.current
    val restoreSystemBarColor = MaterialTheme.colorScheme.background.toArgb()

    DisposableEffect(view, isDarkTheme, restoreSystemBarColor) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        if (window != null && controller != null && !view.isInEditMode) {
            window.statusBarColor = SplashColors.NavyTop.toArgb()
            window.navigationBarColor = SplashColors.NavyBottom.toArgb()
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
        onDispose {
            if (window != null && controller != null && !view.isInEditMode) {
                window.statusBarColor = restoreSystemBarColor
                window.navigationBarColor = restoreSystemBarColor
                controller.isAppearanceLightStatusBars = !isDarkTheme
                controller.isAppearanceLightNavigationBars = !isDarkTheme
            }
        }
    }

    var startAnim by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.92f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "splashScale"
    )

    LaunchedEffect(Unit) {
        startAnim = true
        val tokens = TokenManager(splashContext)
        if (!tokens.shouldKeepLoggedIn() || tokens.getSavedUser().isNullOrBlank()) delay(1200)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SplashColors.NavyTop,
                        SplashColors.NavyCenter,
                        SplashColors.NavyBottom
                    )
                )
            )
            .testTag("splash_root")
    ) {
        SplashAmbientCurves(modifier = Modifier.fillMaxSize())

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            val isCompactHeight = maxHeight < 520.dp
            val logoSize = if (isCompactHeight) 82.dp else 94.dp
            val titleSize = if (isCompactHeight) 26.sp else 30.sp
            val progressWidth = if (maxWidth > 500.dp) 180.dp else 148.dp

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.scale(scale)
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = SplashColors.LogoFrame,
                    border = BorderStroke(1.dp, SplashColors.LogoBorder.copy(alpha = 0.35f)),
                    shadowElevation = 14.dp,
                    modifier = Modifier
                        .size(logoSize)
                        .testTag("splash_logo")
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                        contentDescription = "Logo UPAO",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(22.dp))
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "UPAO",
                    fontSize = titleSize,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp,
                    color = SplashColors.TitleWhite,
                    modifier = Modifier.testTag("splash_title")
                )

                Spacer(modifier = Modifier.height(20.dp))

                LinearProgressIndicator(
                    modifier = Modifier
                        .width(progressWidth)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .testTag("splash_progress"),
                    color = SplashColors.ProgressActive,
                    trackColor = SplashColors.ProgressTrack.copy(alpha = 0.78f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun SplashAmbientCurves(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // Subtle center ambient glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    SplashColors.CurveFillStart.copy(alpha = 0.28f),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.44f),
                radius = maxOf(w, h) * 0.45f
            ),
            radius = maxOf(w, h) * 0.45f,
            center = Offset(w * 0.5f, h * 0.44f)
        )

        // Top-left subtle curve
        val topLeftPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(w * 0.36f, 0f)
            cubicTo(
                w * 0.28f, h * 0.08f,
                w * 0.14f, h * 0.135f,
                0f, h * 0.165f
            )
            close()
        }
        drawPath(
            path = topLeftPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    SplashColors.CurveFillStart.copy(alpha = 0.46f),
                    SplashColors.CurveFillEnd.copy(alpha = 0.28f)
                ),
                start = Offset.Zero,
                end = Offset(w * 0.36f, h * 0.17f)
            )
        )
        val topLeftEdge = Path().apply {
            moveTo(w * 0.36f, 0f)
            cubicTo(
                w * 0.28f, h * 0.08f,
                w * 0.14f, h * 0.135f,
                0f, h * 0.165f
            )
        }
        drawPath(
            path = topLeftEdge,
            color = SplashColors.CurveStroke.copy(alpha = 0.42f),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Bottom-right subtle curve
        val bottomRightPath = Path().apply {
            moveTo(w, h)
            lineTo(w * 0.53f, h)
            cubicTo(
                w * 0.60f, h * 0.87f,
                w * 0.77f, h * 0.77f,
                w, h * 0.72f
            )
            close()
        }
        drawPath(
            path = bottomRightPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    SplashColors.CurveFillEnd.copy(alpha = 0.28f),
                    SplashColors.CurveFillStart.copy(alpha = 0.44f)
                ),
                start = Offset(w * 0.53f, h),
                end = Offset(w, h * 0.72f)
            )
        )
        val bottomRightEdge = Path().apply {
            moveTo(w * 0.53f, h)
            cubicTo(
                w * 0.60f, h * 0.87f,
                w * 0.77f, h * 0.77f,
                w, h * 0.72f
            )
        }
        drawPath(
            path = bottomRightEdge,
            color = SplashColors.CurveStroke.copy(alpha = 0.38f),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}
