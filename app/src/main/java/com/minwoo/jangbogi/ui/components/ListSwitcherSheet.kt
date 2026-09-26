package com.minwoo.jangbogi.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minwoo.jangbogi.domain.ListWithProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListSwitcherSheet(
    lists: List<ListWithProgress>,
    activeListId: Long?,
    onSelect: (Long) -> Unit,
    onNewList: () -> Unit,
    onManage: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("내 장보기 목록", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp))
            lists.forEach { entry ->
                TextButton(onClick = { onSelect(entry.list.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.list.name, style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text("살 것 ${entry.totalCount - entry.checkedCount} · 고민 중 ${entry.consideringCount}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (entry.list.id == activeListId) Text("선택됨", style = MaterialTheme.typography.labelMedium)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            TextButton(onClick = onNewList, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("＋ 새 목록으로 시작하기")
            }
            if (lists.isNotEmpty()) {
                TextButton(onClick = onManage, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text("목록 이름 변경 · 삭제")
                }
            }
        }
    }
}
