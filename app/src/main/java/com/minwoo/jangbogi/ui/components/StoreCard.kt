package com.minwoo.jangbogi.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minwoo.jangbogi.domain.ListWithProgress
import com.minwoo.jangbogi.ui.theme.surfaceCard

@Composable
fun StoreCard(entry: ListWithProgress, onClick: () -> Unit, onRename: () -> Unit,
              onDelete: () -> Unit, modifier: Modifier = Modifier) {
    var menu by remember { mutableStateOf(false) }
    val largeText = LocalDensity.current.fontScale >= 1.5f
    val progress by animateFloatAsState(
        if (entry.totalCount == 0) 0f else entry.checkedCount.toFloat() / entry.totalCount,
        animationSpec = tween(260), label = "store-progress"
    )
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().testTag("store-${entry.list.id}"),
        shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceCard,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))
    ) {
        Column(Modifier.padding(start = 20.dp, end = 14.dp, top = 16.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StoreIllustration(Modifier.size(52.dp, 44.dp))
                if (largeText) Spacer(Modifier.weight(1f))
                else StoreTitle(entry, Modifier.weight(1f).padding(start = 12.dp))
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Rounded.MoreVert, "${entry.list.name} 더보기", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("마트 이름 변경") }, onClick = { menu = false; onRename() })
                        DropdownMenuItem(text = { Text("마트 삭제", color = MaterialTheme.colorScheme.error) },
                            onClick = { menu = false; onDelete() })
                    }
                }
            }
            if (largeText) StoreTitle(entry, Modifier.fillMaxWidth().padding(top = 8.dp, end = 6.dp))
            if (entry.totalCount > 0) {
                LinearProgressIndicator(progress = { progress },
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp, end = 6.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                    trackColor = MaterialTheme.colorScheme.primaryContainer)
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (entry.totalCount > 0) "${entry.checkedCount}/${entry.totalCount} 구매 완료" else "필요한 물건부터 담아보세요",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f))
                Text("장보기", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun StoreTitle(entry: ListWithProgress, modifier: Modifier) {
    Column(modifier) {
        Text(entry.list.name, style = MaterialTheme.typography.titleMedium)
        Text(when {
            entry.totalCount > entry.checkedCount -> "살 것 ${entry.totalCount - entry.checkedCount}개"
            entry.totalCount > 0 -> "장보기 완료!"
            else -> "장보기 준비됐어요"
        } + if (entry.consideringCount > 0) " · 고민 중 ${entry.consideringCount}개" else "",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp))
    }
}
