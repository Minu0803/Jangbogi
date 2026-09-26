package com.minwoo.jangbogi.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.minwoo.jangbogi.data.ShoppingItem
import com.minwoo.jangbogi.domain.CategorySection
import com.minwoo.jangbogi.domain.ListUiState
import com.minwoo.jangbogi.domain.PurchaseIntent
import com.minwoo.jangbogi.domain.Suggestion
import com.minwoo.jangbogi.ui.theme.surfaceCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingContent(
    state: ListUiState,
    selectedIntent: PurchaseIntent,
    query: String,
    suggestions: List<Suggestion>,
    snackbarHostState: SnackbarHostState,
    onQueryChange: (String) -> Unit,
    onSelectIntent: (PurchaseIntent) -> Unit,
    onAdd: (String) -> Unit,
    onToggle: (Long) -> Unit,
    onEdit: (ShoppingItem) -> Unit,
    onDelete: (Long) -> Unit,
    onMove: (Long, PurchaseIntent) -> Unit,
    onOpenLists: () -> Unit,
    onOpenPlan: () -> Unit,
    onShare: (() -> Unit)? = null,
    onClearCompleted: (() -> Unit)? = null
) {
    var showCompleted by remember(state.listName) { mutableStateOf(false) }
    var moreExpanded by remember { mutableStateOf(false) }
    val remaining = state.totalCount - state.checkedCount
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    TextButton(onClick = onOpenLists, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(state.listName.ifBlank { "내 장보기" }, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall)
                        Text("  ⌄", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    if (onShare != null) {
                        IconButton(onClick = onShare) {
                            Icon(Icons.Rounded.Share, contentDescription = "살 것만 공유")
                        }
                    }
                    TextButton(onClick = onOpenPlan) { Text("살림 계획", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (onClearCompleted != null) {
                        Box {
                            IconButton(onClick = { moreExpanded = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "목록 더보기")
                            }
                            DropdownMenu(expanded = moreExpanded, onDismissRequest = { moreExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("완료 항목 비우기") },
                                    enabled = state.completedItems.isNotEmpty(),
                                    onClick = { moreExpanded = false; onClearCompleted() }
                                )
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
                    .imePadding().navigationBarsPadding().padding(top = 8.dp, bottom = 8.dp)
            ) {
                AnimatedVisibility(visible = suggestions.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item { Text("자주 담는 물건", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp)) }
                        items(suggestions.take(4), key = { it.name }) { suggestion ->
                            Surface(
                                onClick = { onAdd(suggestion.name) },
                                color = MaterialTheme.colorScheme.surfaceCard,
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("＋ ${suggestion.name}", style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) }
                        }
                    }
                }
                Text(
                    if (selectedIntent == PurchaseIntent.BUY) "살 것에 추가" else "고민 중에 보관",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 22.dp, top = 7.dp, bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = { Text(if (selectedIntent == PurchaseIntent.BUY) "살 물건을 적어보세요" else "고민되는 물건을 적어보세요") },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (query.isNotBlank()) onAdd(query) }),
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp)
                    )
                    Button(
                        onClick = { onAdd(query) },
                        enabled = query.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 15.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(56.dp)
                    ) { Text(if (selectedIntent == PurchaseIntent.BUY) "담기" else "보관") }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(11.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    Text("무엇을 살까요?", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    val summary = when {
                        remaining > 0 -> "${remaining}개 남았어요 · ${state.checkedCount}/${state.totalCount} 구매 완료"
                        state.totalCount > 0 -> "살 것은 다 샀어요" + if (state.consideringItems.isNotEmpty()) " · 고민 중 ${state.consideringItems.size}개" else ""
                        state.consideringItems.isNotEmpty() -> "고민 중인 물건 ${state.consideringItems.size}개가 있어요"
                        else -> "필요한 물건부터 담아보세요"
                    }
                    Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.totalCount > 0) {
                        LinearProgressIndicator(
                            progress = { state.checkedCount.toFloat() / state.totalCount },
                            modifier = Modifier.fillMaxWidth().height(4.dp)
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(17.dp)).padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(PurchaseIntent.BUY to "살 것 $remaining", PurchaseIntent.CONSIDER to "고민 중 ${state.consideringItems.size}").forEach { (intent, label) ->
                        val selected = selectedIntent == intent
                        Surface(
                            onClick = { onSelectIntent(intent); focusManager.clearFocus() },
                            modifier = Modifier.weight(1f).heightIn(min = 46.dp),
                            color = if (selected) MaterialTheme.colorScheme.surfaceCard else MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(label, style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            if (state.isLoading) {
                item { Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else if (selectedIntent == PurchaseIntent.BUY) {
                if (remaining == 0) item {
                    Text(if (state.totalCount > 0) "살 것은 모두 챙겼어요." else "아래에서 필요한 물건을 담아보세요.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp), style = MaterialTheme.typography.bodyMedium)
                }
                state.sections.forEach { section ->
                    item(key = "category-${section.category.name}") {
                        Text(section.category.display, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp, bottom = 2.dp))
                    }
                    item(key = "section-${section.category.name}") {
                        ShoppingRows(section.items, onToggle, onEdit, onDelete, onMove)
                    }
                }
                if (state.completedItems.isNotEmpty()) {
                    item {
                        TextButton(
                            onClick = { showCompleted = !showCompleted },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("구매 완료 ${state.completedItems.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (showCompleted) "접기" else "펼치기", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (showCompleted) item {
                        ShoppingRows(state.completedItems, onToggle, onEdit, onDelete, onMove)
                    }
                }
            } else {
                if (state.consideringItems.isEmpty()) item {
                    Text("살지 고민되는 물건을 보관해보세요.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp), style = MaterialTheme.typography.bodyMedium)
                } else {
                    item {
                        Text("천천히 결정해도 괜찮아요", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp, bottom = 2.dp))
                    }
                    item { ShoppingRows(state.consideringItems, onToggle, onEdit, onDelete, onMove) }
                }
            }
        }
    }
}

@Composable
private fun ShoppingRows(
    rows: List<ShoppingItem>,
    onToggle: (Long) -> Unit,
    onEdit: (ShoppingItem) -> Unit,
    onDelete: (Long) -> Unit,
    onMove: (Long, PurchaseIntent) -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surfaceCard, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
            rows.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ShoppingRow(item, onToggle, onEdit, onDelete, onMove)
            }
        }
    }
}

@Composable
private fun ShoppingRow(
    item: ShoppingItem,
    onToggle: (Long) -> Unit,
    onEdit: (ShoppingItem) -> Unit,
    onDelete: (Long) -> Unit,
    onMove: (Long, PurchaseIntent) -> Unit
) {
    var menuExpanded by remember(item.id) { mutableStateOf(false) }
    val considering = item.purchaseIntent == PurchaseIntent.CONSIDER
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (!considering) {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = { onToggle(item.id) },
                modifier = Modifier.size(48.dp).semantics {
                    contentDescription = "${item.name} ${if (item.isChecked) "구매 취소" else "구매 완료"}"
                }
            )
        }
        Column(
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).clickable { onEdit(item) }
                .padding(start = if (considering) 10.dp else 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            if (considering) Text("고민 중", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary)
            Text(item.name, style = MaterialTheme.typography.titleMedium,
                color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (item.quantity > 1) Text("×${item.quantity}", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (considering) {
            TextButton(onClick = { onMove(item.id, PurchaseIntent.BUY) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("살래요")
            }
        }
        Box {
            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "${item.name} 더보기")
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(text = { Text("수정") }, onClick = { menuExpanded = false; onEdit(item) })
                DropdownMenuItem(
                    text = { Text(if (considering) "살 것으로 이동" else "고민 중으로 이동") },
                    onClick = {
                        menuExpanded = false
                        onMove(item.id, if (considering) PurchaseIntent.BUY else PurchaseIntent.CONSIDER)
                    }
                )
                DropdownMenuItem(text = { Text("삭제", color = MaterialTheme.colorScheme.error) },
                    onClick = { menuExpanded = false; onDelete(item.id) })
            }
        }
    }
}
