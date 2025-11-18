package com.example.bankloginsystem.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

@Composable
fun ScanLines(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanline")
    val y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanline_y"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasHeight = size.height
        val lineHeight = 2.dp.toPx()
        val lineSpacing = 4.dp.toPx()
        val lineCount = (canvasHeight / (lineHeight + lineSpacing)).toInt()

        for (i in 0..lineCount) {
            val lineY = (i * (lineHeight + lineSpacing)) + (y * canvasHeight)
            if(lineY > canvasHeight) {
                val adjustedY = lineY - canvasHeight
                drawLine(
                    color = AlienGreen.copy(alpha = 0.1f),
                    start = Offset(0f, adjustedY),
                    end = Offset(size.width, adjustedY),
                    strokeWidth = lineHeight
                )
            } else {
                 drawLine(
                    color = AlienGreen.copy(alpha = 0.1f),
                    start = Offset(0f, lineY),
                    end = Offset(size.width, lineY),
                    strokeWidth = lineHeight
                )
            }
        }
    }
}
