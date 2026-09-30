package com.minwoo.jangbogi.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/** A resolution-independent storefront, using the app's light and dark palette. */
@Composable
fun StoreIllustration(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Canvas(modifier) {
        withTransform({ scale(size.width / 200f, size.height / 160f, Offset.Zero) }) {
            drawCircle(colors.primaryContainer, 70f, Offset(100f, 78f))
            drawOval(colors.primary.copy(alpha = .09f), Offset(30f, 136f), Size(140f, 12f))
            drawRoundRect(colors.surfaceContainerLowest, Offset(43f, 58f), Size(114f, 80f), CornerRadius(9f))
            drawRoundRect(colors.outlineVariant, Offset(43f, 58f), Size(114f, 80f), CornerRadius(9f), style = Stroke(2f))
            drawRoundRect(colors.primary, Offset(63f, 25f), Size(74f, 24f), CornerRadius(8f))
            drawLine(colors.onPrimary, Offset(84f, 37f), Offset(116f, 37f), 3f)
            val roof = Path().apply {
                moveTo(43f, 49f); lineTo(157f, 49f); lineTo(167f, 71f); lineTo(33f, 71f); close()
            }
            drawPath(roof, colors.primary)
            for (index in 0..6) {
                val x = 33f + index * 19.15f
                val stripe = Path().apply {
                    moveTo(43f + index * 16.3f, 49f)
                    lineTo(43f + (index + 1) * 16.3f, 49f)
                    lineTo(x + 19.2f, 71f); lineTo(x, 71f); close()
                }
                val tint = if (index % 2 == 0) colors.primary else colors.primaryContainer
                drawPath(stripe, tint)
                drawRoundRect(tint, Offset(x, 68f), Size(19.2f, 14f), CornerRadius(7f))
            }
            drawRoundRect(colors.primaryContainer, Offset(56f, 92f), Size(43f, 30f), CornerRadius(4f))
            drawLine(colors.surfaceContainerLowest, Offset(77.5f, 93f), Offset(77.5f, 121f), 3f)
            drawRoundRect(colors.secondaryContainer, Offset(111f, 90f), Size(31f, 48f), CornerRadius(4f))
            drawRoundRect(colors.primary.copy(alpha = .24f), Offset(116f, 95f), Size(21f, 26f), CornerRadius(2f))
            drawCircle(colors.primary, 2f, Offset(135f, 126f))
            drawLine(colors.primary.copy(alpha = .3f), Offset(39f, 138f), Offset(161f, 138f), 3f)
            drawCircle(colors.primary.copy(alpha = .24f), 4f, Offset(27f, 43f))
            drawLine(colors.primary.copy(alpha = .5f), Offset(173f, 99f), Offset(173f, 111f), 2f)
            drawLine(colors.primary.copy(alpha = .5f), Offset(167f, 105f), Offset(179f, 105f), 2f)
        }
    }
}
