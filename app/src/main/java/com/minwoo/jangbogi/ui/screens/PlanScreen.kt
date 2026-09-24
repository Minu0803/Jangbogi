package com.minwoo.jangbogi.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.minwoo.jangbogi.JangbogiApp
import com.minwoo.jangbogi.data.PlannedShoppingItem
import com.minwoo.jangbogi.data.ShoppingItem
import com.minwoo.jangbogi.ui.components.EditItemSheet
import com.minwoo.jangbogi.ui.theme.surfaceCard
import com.minwoo.jangbogi.ui.viewmodel.HomeViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PlanScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as JangbogiApp
    val vm: HomeViewModel = viewModel(factory = app.container.homeViewModelFactory())
    val entries by vm.plannedItems.collectAsStateWithLifecycle()
    var editTarget by remember { mutableStateOf<ShoppingItem?>(null) }
    val today = LocalDate.now()
    val soon = entries.count { entry ->
        entry.item.mustBuyBy?.let { dateFromMillis(it) }?.let { !it.isBefore(today) && !it.isAfter(today.plusDays(7)) } == true
    }
    val stocked = entries.count { it.item.stockQuantity > 0 }
    val stockup = entries.count { it.item.stockUpMonth != null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("살림 계획", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("필요한 순간에, 알뜰하게", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("살 시기와 장소, 꼭 사야 할 날과 집 재고를 한곳에서 챙겨요.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PlanStat("7일 내 기한", "${soon}개", Modifier.weight(1f))
                    PlanStat("재고 있는 품목", "${stocked}개", Modifier.weight(1f))
                    PlanStat("쟁임 달 기록", "${stockup}개", Modifier.weight(1f))
                }
            }
            item {
                Text("내 품목", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
            }
            if (entries.isEmpty()) {
                item {
                    Surface(color = MaterialTheme.colorScheme.surfaceCard, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🧺", style = MaterialTheme.typography.headlineLarge)
                            Text("아직 관리할 품목이 없어요", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("장보기 목록에 물건을 추가하면 구매 계획과 집 재고를 기록할 수 있어요.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(entries, key = { it.item.id }) { entry ->
                    PlanItemCard(
                        entry = entry,
                        onEdit = { editTarget = entry.item },
                        onStockChange = { value ->
                            val item = entry.item
                            vm.updateShoppingPlan(item.name, item.quantity, item.category, item.plannedBuyAt, item.preferredStore.orEmpty(), item.mustBuyBy, item.stockUpMonth, value)
                        }
                    )
                }
            }
        }
    }

    editTarget?.let { item ->
        EditItemSheet(
            item = item,
            onDismiss = { editTarget = null },
            onSave = { name, quantity, category, plannedBuyAt, preferredStore, mustBuyBy, stockUpMonth, stockQuantity ->
                vm.updateShoppingPlan(name, quantity, category, plannedBuyAt, preferredStore, mustBuyBy, stockUpMonth, stockQuantity)
                editTarget = null
            },
            onDelete = { editTarget = null },
            allowDelete = false
        )
    }
}

@Composable
private fun PlanStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceCard, shape = RoundedCornerShape(18.dp), modifier = modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlanItemCard(entry: PlannedShoppingItem, onEdit: () -> Unit, onStockChange: (Int) -> Unit) {
    val item = entry.item
    val today = LocalDate.now()
    val deadline = item.mustBuyBy?.let(::dateFromMillis)
    val overdue = deadline?.isBefore(today) == true
    Surface(
        color = MaterialTheme.colorScheme.surfaceCard,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${item.category.emoji} ${item.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${entry.listName.takeIf { it.isNotBlank() }?.let { "$it · " }.orEmpty()}${item.quantity}개 살 예정",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("수정 ›", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
            }
            Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("🗓  살 때  ${item.plannedBuyAt?.let { dateLabel(dateFromMillis(it)) } ?: "날짜 미정"}", style = MaterialTheme.typography.bodyMedium)
                    Text("📍  구매처  ${item.preferredStore?.takeIf { it.isNotBlank() } ?: "미정"}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "⏰  꼭 살 날  ${deadline?.let { dateLabel(it) } ?: "기한 미정"}${if (overdue) "  ·  기한 지남" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    Text("💰  미리 쟁일 달  ${item.stockUpMonth?.let { "${it}월" } ?: "미정"}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("집에 있어요", style = MaterialTheme.typography.labelLarge)
                    Text("현재 수량", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalIconButton(onClick = { onStockChange(item.stockQuantity - 1) }, enabled = item.stockQuantity > 0, modifier = Modifier.size(48.dp)) {
                    Text("−", style = MaterialTheme.typography.titleLarge)
                }
                Text("${item.stockQuantity}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                FilledTonalIconButton(onClick = { onStockChange(item.stockQuantity + 1) }, enabled = item.stockQuantity < 99, modifier = Modifier.size(48.dp)) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

private fun dateFromMillis(value: Long): LocalDate =
    Instant.ofEpochMilli(value).atZone(ZoneId.of("UTC")).toLocalDate()

private fun dateLabel(date: LocalDate): String = "${date.monthValue}월 ${date.dayOfMonth}일"
