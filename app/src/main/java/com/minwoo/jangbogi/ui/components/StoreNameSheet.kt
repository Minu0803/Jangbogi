package com.minwoo.jangbogi.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StoreNameSheet(initialName: String, isRename: Boolean, saving: Boolean, error: String?,
                   onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    val focus = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    fun save() { if (name.isNotBlank() && !saving) { focus.clearFocus(); onSave(name.trim()) } }
    ModalBottomSheet(onDismissRequest = { if (!saving) onDismiss() }, sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (isRename) "마트 이름 변경" else "어떤 마트에 가시나요?", style = MaterialTheme.typography.headlineSmall)
            Text(if (isRename) "알아보기 쉬운 이름으로 바꿔보세요." else "자주 가는 마트를 이름으로 기억해둘게요.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("마트 이름") }, placeholder = { Text("예: 이마트 성수점, 동네 마트") },
                enabled = !saving, singleLine = true, shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { save() }),
                modifier = Modifier.fillMaxWidth().testTag("store-name-input"),
                isError = error != null, supportingText = error?.let { message -> { Text(message) } })
            if (!isRename) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("동네 마트", "이마트", "홈플러스", "하나로마트", "코스트코").forEach { suggestion ->
                        SuggestionChip(onClick = { name = suggestion }, enabled = !saving, label = { Text(suggestion) })
                    }
                }
            }
            Button(onClick = { save() }, enabled = name.isNotBlank() && !saving,
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("save-store"),
                contentPadding = PaddingValues(16.dp)) {
                if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(if (isRename) "이름 저장" else "추가하고 장보기")
            }
            TextButton(onClick = onDismiss, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("취소") }
        }
    }
}
