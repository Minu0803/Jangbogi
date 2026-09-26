package com.minwoo.jangbogi.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.minwoo.jangbogi.R
import com.minwoo.jangbogi.data.ShoppingItem
import com.minwoo.jangbogi.domain.Category
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditItemSheet(
    item: ShoppingItem,
    onDismiss: () -> Unit,
    onSave: (name: String, quantity: Int, category: Category, plannedBuyAt: Long?, preferredStore: String, mustBuyBy: Long?, stockUpMonth: Int?, stockQuantity: Int) -> Unit,
    onDelete: () -> Unit,
    allowDelete: Boolean = true,
    showPlanInitially: Boolean = false
) {
    var name by remember(item.id) { mutableStateOf(item.name) }
    var quantity by remember(item.id) { mutableIntStateOf(item.quantity) }
    var category by remember(item.id) { mutableStateOf(item.category) }
    var plannedBuyAt by remember(item.id) { mutableStateOf(item.plannedBuyAt) }
    var mustBuyBy by remember(item.id) { mutableStateOf(item.mustBuyBy) }
    var preferredStore by remember(item.id) { mutableStateOf(item.preferredStore.orEmpty()) }
    var stockUpMonth by remember(item.id) { mutableStateOf(item.stockUpMonth) }
    var stockQuantity by remember(item.id) { mutableIntStateOf(item.stockQuantity) }
    var datePickerFor by remember { mutableStateOf<PlanDateField?>(null) }
    var showCategories by remember(item.id) { mutableStateOf(false) }
    var showPlan by remember(item.id) { mutableStateOf(showPlanInitially) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val decreaseDesc = stringResource(R.string.quantity_decrease)
    val increaseDesc = stringResource(R.string.quantity_increase)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.edit_item_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.item_name_label)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.quantity_label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalIconButton(
                    onClick = { if (quantity > 1) quantity-- },
                    enabled = quantity > 1,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = decreaseDesc }
                ) {
                    Text("−", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = quantity.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 48.dp)
                )
                FilledTonalIconButton(
                    onClick = { if (quantity < 99) quantity++ },
                    enabled = quantity < 99,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = increaseDesc }
                ) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
            Column {
                Text(
                    text = stringResource(R.string.category_label),
                    style = MaterialTheme.typography.titleSmall
                )
                TextButton(onClick = { showCategories = !showCategories }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(category.display, color = MaterialTheme.colorScheme.onSurface)
                        Text(if (showCategories) "접기" else "변경")
                    }
                }
                if (showCategories) FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Category.entries.forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c; showCategories = false },
                            label = { Text(c.display) }
                        )
                    }
                }
            }
            TextButton(onClick = { showPlan = !showPlan }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("구매 계획 · 재고", color = MaterialTheme.colorScheme.onSurface)
                    Text(if (showPlan) "접기" else "펼치기")
                }
            }
            if (showPlan) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PlanDateField(
                    label = "언제 사면 좋을까요?",
                    value = plannedBuyAt,
                    onClick = { datePickerFor = PlanDateField.BUY }
                )
                OutlinedTextField(
                    value = preferredStore,
                    onValueChange = { preferredStore = it },
                    label = { Text("주로 사는 곳") },
                    placeholder = { Text("예: 동네 마트, 온라인") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                PlanDateField(
                    label = "이 날짜 전에는 꼭 사요",
                    value = mustBuyBy,
                    onClick = { datePickerFor = PlanDateField.DEADLINE }
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("여유 있을 때 쟁일 달", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        FilterChip(selected = stockUpMonth == null, onClick = { stockUpMonth = null }, label = { Text("미정") })
                        (1..12).forEach { month ->
                            FilterChip(
                                selected = stockUpMonth == month,
                                onClick = { stockUpMonth = month },
                                label = { Text("${month}월") }
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("집에 있는 수량", style = MaterialTheme.typography.titleSmall)
                        Text("현재 비축분을 기록해요", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalIconButton(
                        onClick = { stockQuantity = (stockQuantity - 1).coerceAtLeast(0) },
                        enabled = stockQuantity > 0,
                        modifier = Modifier.size(48.dp)
                    ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                    Text(stockQuantity.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 48.dp))
                    FilledTonalIconButton(
                        onClick = { stockQuantity = (stockQuantity + 1).coerceAtMost(99) },
                        enabled = stockQuantity < 99,
                        modifier = Modifier.size(48.dp)
                    ) { Text("+", style = MaterialTheme.typography.titleLarge) }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (allowDelete) {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                Button(
                    onClick = { onSave(name.trim(), quantity, category, plannedBuyAt, preferredStore, mustBuyBy, stockUpMonth, stockQuantity) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(if (allowDelete) 1f else 2f).height(52.dp)
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
    datePickerFor?.let { field ->
        val current = if (field == PlanDateField.BUY) plannedBuyAt else mustBuyBy
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = current ?: LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { datePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    if (field == PlanDateField.BUY) plannedBuyAt = pickerState.selectedDateMillis
                    else mustBuyBy = pickerState.selectedDateMillis
                    datePickerFor = null
                }) { Text("선택") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        if (field == PlanDateField.BUY) plannedBuyAt = null else mustBuyBy = null
                        datePickerFor = null
                    }) { Text("날짜 지우기") }
                    TextButton(onClick = { datePickerFor = null }) { Text("취소") }
                }
            }
        ) { DatePicker(state = pickerState) }
    }
}

private enum class PlanDateField { BUY, DEADLINE }

@Composable
private fun PlanDateField(label: String, value: Long?, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value?.let(::formatPlanDate) ?: "날짜 정하기", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun formatPlanDate(value: Long): String {
    val date = LocalDate.ofEpochDay(value / 86_400_000L)
    return "${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일"
}
