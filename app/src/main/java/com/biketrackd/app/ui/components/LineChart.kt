package com.biketrackd.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biketrackd.app.data.FontSizePreferences
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun LineChart(
    data: List<Pair<String, Float>>,
    color: Color,
    unit: String = "",
    labelCount: Int = 4,
    height: Dp = 160.dp,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) return

    val maxVal = (data.maxOf { it.second } * 1.15f).coerceAtLeast(1f)
    val roundUp = pow10(ceil(log10(maxVal)).toInt()) / 2f
    val yMax = (ceil(maxVal / roundUp) * roundUp).coerceAtLeast(roundUp)
    val ySteps = (0..(yMax / roundUp).toInt()).map { it * roundUp }

    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800),
        label = "line-progress",
    )

    val fontScale = FontSizePreferences.getFontScale(LocalContext.current)
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = (8 * fontScale).sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().height(height)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
                val leftPadding = 32.dp.toPx()
                val bottomPadding = 24.dp.toPx()
                val topPadding = 8.dp.toPx()
                val rightPadding = 8.dp.toPx()

                val chartWidth = size.width - leftPadding - rightPadding
                val chartHeight = size.height - topPadding - bottomPadding

                // Horizontal grid lines + Y labels
                for (step in ySteps) {
                    val y = topPadding + chartHeight * (1f - step / yMax)
                    drawLine(
                        color = outlineVariant.copy(alpha = 0.4f),
                        start = Offset(leftPadding, y),
                        end = Offset(size.width - rightPadding, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
                    )
                    val label = "${step.toInt()}${if (unit.isNotEmpty()) " $unit" else ""}"
                    val measured = textMeasurer.measure(label, labelStyle)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(leftPadding - measured.size.width - 4.dp.toPx(), y - measured.size.height / 2f),
                    )
                }

                // Data points
                val pointCount = data.size
                if (pointCount >= 2) {
                    val path = Path()
                    val points = data.mapIndexed { i, (_, value) ->
                        val x = leftPadding + chartWidth * i / (pointCount - 1).coerceAtLeast(1)
                        val y = topPadding + chartHeight * (1f - value / yMax)
                        Offset(x, y)
                    }

                    // Fill under line
                    val fillPath = Path().apply {
                        moveTo(points.first().x, topPadding + chartHeight)
                        for (p in points) lineTo(p.x, p.y)
                        lineTo(points.last().x, topPadding + chartHeight)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        color = color.copy(alpha = 0.12f * animatedProgress),
                    )

                    // Line
                    path.moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val endIndex = (i.toFloat() * animatedProgress).roundToInt().coerceAtMost(i)
                        if (endIndex > 0) {
                            path.lineTo(points[endIndex - 1].x, points[endIndex - 1].y)
                        }
                        if (endIndex >= i) {
                            path.lineTo(points[i].x, points[i].y)
                        }
                    }

                    val visibleCount = (pointCount * animatedProgress).toInt().coerceAtLeast(1)
                    val linePath = Path()
                    for (i in 0 until visibleCount.coerceAtMost(pointCount)) {
                        if (i == 0) {
                            linePath.moveTo(points[i].x, points[i].y)
                        } else {
                            linePath.lineTo(points[i].x, points[i].y)
                        }
                    }
                    drawPath(
                        path = linePath,
                        color = color,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )

                    // Dots
                    for (i in 0 until visibleCount.coerceAtMost(pointCount)) {
                        drawCircle(
                            color = color,
                            radius = 3.dp.toPx(),
                            center = points[i],
                        )
                        drawCircle(
                            color = surfaceColor,
                            radius = 1.5.dp.toPx(),
                            center = points[i],
                        )
                    }

                    // X labels (show evenly spaced subset)
                    val maxLabels = labelCount.coerceAtMost(pointCount)
                    val step = (pointCount - 1).coerceAtLeast(1) / (maxLabels - 1).coerceAtLeast(1)
                    for (i in 0 until pointCount step step.coerceAtLeast(1)) {
                        val x = leftPadding + chartWidth * i / (pointCount - 1).coerceAtLeast(1)
                        val measured = textMeasurer.measure(data[i].first, labelStyle)
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(x - measured.size.width / 2f, topPadding + chartHeight + 6.dp.toPx()),
                        )
                    }
                } else if (pointCount == 1) {
                    val x = leftPadding + chartWidth / 2f
                    val y = topPadding + chartHeight * (1f - data[0].second / yMax)
                    drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(x, y))
                    val measured = textMeasurer.measure(data[0].first, labelStyle)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(x - measured.size.width / 2f, topPadding + chartHeight + 6.dp.toPx()),
                    )
                }
            }
        }
    }
}

private fun log10(x: Float): Float = kotlin.math.ln(x.toDouble()).toFloat() / kotlin.math.ln(10.0).toFloat()

private fun pow10(exp: Int): Float = when {
    exp >= 0 -> (1..exp).fold(1f) { acc, _ -> acc * 10f }
    else -> (1..(-exp)).fold(1f) { acc, _ -> acc / 10f }
}
