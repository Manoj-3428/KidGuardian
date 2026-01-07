package com.example.phonelock.ui.parent.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import android.graphics.Paint
import android.graphics.Typeface
import com.example.phonelock.ui.parent.models.LabelPosition
import com.example.phonelock.ui.parent.models.SliceData
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CustomPieChartWithLabels(
    slices: List<SliceData>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val size = constraints.maxWidth.toFloat().coerceAtMost(constraints.maxHeight.toFloat())
        val centerX = size / 2f
        val centerY = size / 2f
        val radius = size * 0.3f
        val labelRadius = size * 0.48f
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            var startAngle = -90f
            val totalPercentage = slices.sumOf { it.percentage.toDouble() }.toFloat()
            
            val labelPositions = mutableListOf<LabelPosition>()
            
            slices.forEach { slice ->
                val sweepAngle = if (totalPercentage > 0) {
                    (slice.percentage / totalPercentage) * 360f
                } else {
                    0f
                }
                
                if (sweepAngle > 0) {
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = Offset(centerX - radius, centerY - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                    )
                    
                    val midAngle = startAngle + sweepAngle / 2f
                    val angleRad = Math.toRadians(midAngle.toDouble())
                    
                    val edgeX = centerX + (radius * cos(angleRad).toFloat())
                    val edgeY = centerY + (radius * sin(angleRad).toFloat())
                    
                    val labelX = centerX + (labelRadius * cos(angleRad).toFloat())
                    val labelY = centerY + (labelRadius * sin(angleRad).toFloat())
                    
                    val textSizePx = 16.dp.toPx()
                    val text = "${slice.percentage.toInt()}%"
                    val tempTextPaint = Paint().apply {
                        isAntiAlias = true
                        textSize = textSizePx
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    
                    val textAlign = when {
                        midAngle > -45 && midAngle < 45 -> Paint.Align.LEFT
                        midAngle > 135 || midAngle < -135 -> Paint.Align.RIGHT
                        else -> Paint.Align.CENTER
                    }
                    
                    val gap = 4.dp.toPx()
                    val lineEndX = when (textAlign) {
                        Paint.Align.LEFT -> labelX - gap
                        Paint.Align.RIGHT -> labelX + gap
                        else -> labelX
                    }
                    val lineEndY = labelY
                    
                    drawLine(
                        color = Color(0xFF6B7280),
                        start = Offset(edgeX, edgeY),
                        end = Offset(lineEndX, lineEndY),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    
                    labelPositions.add(
                        LabelPosition(
                            x = labelX,
                            y = labelY,
                            angle = midAngle,
                            text = text,
                            color = slice.color
                        )
                    )
                    
                    startAngle += sweepAngle
                }
            }
            
            drawIntoCanvas { canvas ->
                val textPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 16.dp.toPx()
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = android.graphics.Color.parseColor("#1F2937")
                }
                
                labelPositions.forEach { labelPos ->
                    val textAlign = when {
                        labelPos.angle > -45 && labelPos.angle < 45 -> Paint.Align.LEFT
                        labelPos.angle > 135 || labelPos.angle < -135 -> Paint.Align.RIGHT
                        else -> Paint.Align.CENTER
                    }
                    textPaint.textAlign = textAlign
                    
                    val textY = labelPos.y - (textPaint.descent() + textPaint.ascent()) / 2
                    
                    canvas.nativeCanvas.drawText(
                        labelPos.text,
                        labelPos.x,
                        textY,
                        textPaint
                    )
                }
            }
        }
    }
}

