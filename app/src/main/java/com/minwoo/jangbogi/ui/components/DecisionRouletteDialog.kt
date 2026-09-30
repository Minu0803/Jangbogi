package com.minwoo.jangbogi.ui.components

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.minwoo.jangbogi.domain.*
import com.minwoo.jangbogi.ui.theme.*

@Composable
fun DecisionRouletteDialog(
    session: DecisionSession,
    onStart: () -> Unit,
    onAnimationFinished: (Long) -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    val busy = session.phase == DecisionPhase.SPINNING || session.phase == DecisionPhase.APPLYING
    val finished = session.phase == DecisionPhase.RESULT
    val rotation = remember(session.id) { Animatable(if (session.phase == DecisionPhase.SPINNING) 0f else session.spin?.targetRotation ?: 0f) }
    val finishCallback by rememberUpdatedState(onAnimationFinished)
    LaunchedEffect(session.id, session.phase) {
        val spin = session.spin ?: return@LaunchedEffect
        if (session.phase == DecisionPhase.SPINNING) {
            rotation.animateTo(spin.targetRotation, tween(2800, easing = CubicBezierEasing(.16f, .7f, .12f, 1f)))
            finishCallback(session.id)
        } else rotation.snapTo(spin.targetRotation)
    }
    val maxHeight = (LocalConfiguration.current.screenHeightDp * .88f).dp
    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = !busy, usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.padding(horizontal = 16.dp).widthIn(max = 340.dp).fillMaxWidth().heightIn(max = maxHeight),
            shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surfaceCard, tonalElevation = 0.dp
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("가볍게 결정해볼까요?", style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
                val itemLabel = session.item.name + if (session.item.quantity > 1) " ×${session.item.quantity}" else ""
                Text(itemLabel, style = MaterialTheme.typography.titleMedium, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { contentDescription = itemLabel })
                Text("각 50% · 결과에 따라 이동하거나 삭제해요", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                RouletteWheel(rotation.value)
                Column(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    when (session.phase) {
                        DecisionPhase.READY -> Text("결정이 어려울 때, 한 번만 돌려봐요", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        DecisionPhase.SPINNING -> Text("어떤 결과가 나올까요?", style = MaterialTheme.typography.bodyMedium)
                        DecisionPhase.APPLYING -> Text("결과를 반영하고 있어요", style = MaterialTheme.typography.bodyMedium)
                        DecisionPhase.RESULT -> {
                            val buy = session.spin?.outcome == DecisionOutcome.BUY
                            Text(if (buy) "살래요!" else "안 살래요", style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(if (buy) "살 것으로 옮겼어요" else "고민 중에서 삭제했어요",
                                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        }
                        DecisionPhase.ERROR -> Text(session.error ?: "반영하지 못했어요", textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                val retry = session.phase == DecisionPhase.ERROR && session.error?.startsWith("저장") == true
                Button(
                    onClick = when { finished -> onDismiss; retry -> onRetry; session.phase == DecisionPhase.ERROR -> onDismiss; else -> onStart },
                    enabled = !busy, shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text(when {
                        finished -> "확인"
                        retry -> "다시 저장"
                        session.phase == DecisionPhase.ERROR -> "닫기"
                        session.phase == DecisionPhase.SPINNING -> "돌리는 중…"
                        session.phase == DecisionPhase.APPLYING -> "반영 중…"
                        else -> "돌리기"
                    })
                }
                if (session.phase == DecisionPhase.READY || retry) TextButton(onClick = onDismiss,
                    modifier = Modifier.heightIn(min = 48.dp)) { Text("지금은 닫기") }
                Text("팝업을 닫은 뒤 실행 취소할 수 있어요.",
                    style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RouletteWheel(rotation: Float) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    // The wheel is a diagram: its complete labels are also exposed as one accessibility node.
    val labelSize = with(density) { 14.dp.toPx() } * density.fontScale.coerceAtMost(1.4f)
    Box(Modifier.size(width = 200.dp, height = 216.dp).semantics {
        contentDescription = "룰렛, 살래요 50%, 안 살래요 50%"
    }) {
        Canvas(Modifier.align(Alignment.BottomCenter).size(200.dp).graphicsLayer { rotationZ = rotation }) {
            val inset = 4.dp.toPx()
            val discSize = Size(size.width - inset * 2, size.height - inset * 2)
            drawCircle(colors.surfaceCard)
            drawArc(colors.rouletteBuyContainer, -90f, 180f, true, Offset(inset, inset), discSize)
            drawArc(colors.rouletteSkipContainer, 90f, 180f, true, Offset(inset, inset), discSize)
            drawLine(colors.surfaceCard, Offset(size.width / 2, inset), Offset(size.width / 2, size.height - inset), 2.dp.toPx())
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER; textSize = labelSize; isFakeBoldText = true
            }
            val baseline = size.height / 2 - labelSize * .3f
            for ((x, label, color) in listOf(
                Triple(size.width * .75f, "살래요", colors.rouletteOnBuy),
                Triple(size.width * .25f, "안 살래요", colors.rouletteOnSkip)
            )) {
                paint.color = color.toArgb(); paint.textSize = labelSize; paint.isFakeBoldText = true
                drawContext.canvas.nativeCanvas.drawText(label, x, baseline, paint)
                paint.textSize = labelSize * .85f; paint.isFakeBoldText = false
                drawContext.canvas.nativeCanvas.drawText("50%", x, baseline + labelSize * 1.5f, paint)
            }
            drawCircle(colors.surfaceCard, 7.dp.toPx())
        }
        Canvas(Modifier.align(Alignment.TopCenter).size(20.dp, 22.dp)) {
            drawPath(Path().apply { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2, size.height); close() }, colors.onSurface)
        }
    }
}
