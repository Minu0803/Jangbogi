package com.minwoo.jangbogi.ui.screens

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minwoo.jangbogi.JangbogiApp
import com.minwoo.jangbogi.data.AddItemResult
import com.minwoo.jangbogi.data.ItemMutationResult
import com.minwoo.jangbogi.data.ShoppingItem
import com.minwoo.jangbogi.data.UndoToken
import com.minwoo.jangbogi.domain.PurchaseIntent
import com.minwoo.jangbogi.ui.components.ConfirmDialog
import com.minwoo.jangbogi.ui.components.EditItemSheet
import com.minwoo.jangbogi.ui.components.ShoppingContent
import com.minwoo.jangbogi.ui.viewmodel.ListViewModel
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.launch

@Composable
fun ListScreen(
    listId: Long,
    initialQuery: String,
    onSaveQuery: (String) -> Unit,
    onOpenLists: () -> Unit,
    onOpenPlan: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as JangbogiApp
    val vm: ListViewModel = viewModel(key = "shopping-list-$listId", factory = app.container.listViewModelFactory(listId))
    val state by vm.uiState.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val intent by vm.selectedIntent.collectAsStateWithLifecycle()
    val suggestions by vm.suggestions.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editTarget by remember(listId) { mutableStateOf<ShoppingItem?>(null) }
    var clearCompleted by remember(listId) { mutableStateOf(false) }

    LaunchedEffect(listId) { vm.onQueryChange(initialQuery) }

    fun info(message: String) { scope.launch { snackbar.showSnackbar(message) } }

    fun showUndo(message: String, token: UndoToken) {
        scope.launch {
            val result = snackbar.showSnackbar(message, actionLabel = "실행 취소", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) vm.undoMutation(token) { restored ->
                if (!restored) info("되돌릴 수 없어요. 물건이 변경되었는지 확인해 주세요")
            }
        }
    }

    fun removeItem(id: Long) {
        vm.deleteItemWithUndo(id) { result ->
            if (result is ItemMutationResult.Applied) showUndo("삭제했어요", result.undoToken)
        }
    }

    ShoppingContent(
        state = state,
        selectedIntent = intent,
        query = query,
        suggestions = suggestions,
        snackbarHostState = snackbar,
        onQueryChange = { vm.onQueryChange(it); onSaveQuery(it) },
        onSelectIntent = vm::selectIntent,
        onAdd = { raw ->
            vm.addItem(raw) { result ->
                when (result) {
                    AddItemResult.Added, AddItemResult.Merged, AddItemResult.Reopened -> onSaveQuery("")
                    AddItemResult.EmptyInput -> Unit
                    AddItemResult.QuantityLimit -> info("수량은 99개까지 담을 수 있어요")
                    AddItemResult.Failed -> info("저장하지 못했어요. 다시 시도해 주세요")
                    is AddItemResult.OtherIntent -> info(
                        if (result.intent == PurchaseIntent.BUY) "이미 살 것에 있어요" else "이미 고민 중에 있어요"
                    )
                }
            }
        },
        onToggle = vm::toggleItem,
        onEdit = { editTarget = it },
        onDelete = ::removeItem,
        onMove = { id, target ->
            vm.moveItem(id, target) { result ->
                if (result is ItemMutationResult.Applied) {
                    showUndo(if (target == PurchaseIntent.BUY) "살 것으로 옮겼어요" else "고민 중으로 옮겼어요", result.undoToken)
                }
            }
        },
        onOpenLists = onOpenLists,
        onOpenPlan = onOpenPlan,
        onShare = {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, vm.buildShareText())
            }
            context.startActivity(Intent.createChooser(sendIntent, "살 것만 공유 · 고민 중 제외"))
        },
        onClearCompleted = { clearCompleted = true }
    )

    if (clearCompleted) {
        ConfirmDialog(
            title = "완료 항목 비우기",
            message = "구매 완료한 물건 ${state.completedItems.size}개를 비울까요?",
            confirmLabel = "비우기",
            onConfirm = {
                vm.clearCompleted()
                scope.launch {
                    val result = snackbar.showSnackbar("완료 항목을 비웠어요", actionLabel = "실행 취소", duration = SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) vm.undoClearCompleted()
                }
                clearCompleted = false
            },
            onDismiss = { clearCompleted = false }
        )
    }
    editTarget?.let { item ->
        EditItemSheet(
            item = item,
            onDismiss = { editTarget = null },
            onSave = { name, quantity, category, plannedBuyAt, preferredStore, mustBuyBy, stockUpMonth, stockQuantity ->
                vm.updateItemAndPlan(item.id, name, quantity, category, plannedBuyAt, preferredStore, mustBuyBy, stockUpMonth, stockQuantity)
                editTarget = null
            },
            onDelete = { editTarget = null; removeItem(item.id) }
        )
    }
}
