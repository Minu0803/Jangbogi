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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import com.minwoo.jangbogi.JangbogiApp
import com.minwoo.jangbogi.data.AddItemResult
import com.minwoo.jangbogi.data.ItemMutationResult
import com.minwoo.jangbogi.data.ShoppingItem
import com.minwoo.jangbogi.data.UndoToken
import com.minwoo.jangbogi.data.UpdateItemResult
import com.minwoo.jangbogi.domain.PurchaseIntent
import com.minwoo.jangbogi.domain.DecisionOutcome
import com.minwoo.jangbogi.ui.components.DecisionRouletteDialog
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
    selectionEpoch: Long,
    entryIntent: PurchaseIntent,
    initialQuery: String,
    onSaveQuery: (String) -> Unit,
    onOpenLists: () -> Unit,
    onOpenPlan: () -> Unit,
    onBackToStores: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as JangbogiApp
    val vm: ListViewModel = viewModel(key = "shopping-list-$listId", factory = app.container.listViewModelFactory(listId))
    val state by vm.uiState.collectAsStateWithLifecycle()
    val decision by vm.decisionSession.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val query by vm.query.collectAsStateWithLifecycle()
    val intent by vm.selectedIntent.collectAsStateWithLifecycle()
    val suggestions by vm.suggestions.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editTarget by remember(listId) { mutableStateOf<ShoppingItem?>(null) }
    var editError by remember(listId) { mutableStateOf<String?>(null) }
    var clearCompleted by remember(listId) { mutableStateOf(false) }

    LaunchedEffect(listId, selectionEpoch, entryIntent) {
        vm.restoreEntry(selectionEpoch, entryIntent, initialQuery)
    }

    fun info(message: String) { scope.launch { snackbar.showSnackbar(message) } }

    fun showUndo(message: String, token: UndoToken, duration: SnackbarDuration = SnackbarDuration.Short) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(message, actionLabel = "실행 취소", duration = duration, withDismissAction = true)
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
        onQueryChange = {
            // A disposed Compose text field can emit its old buffer on blur during recreation.
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                vm.onQueryChange(it)
                onSaveQuery(it)
            }
        },
        onSelectIntent = vm::selectIntent,
        onAdd = { raw ->
            vm.addItem(raw) { result ->
                when (result) {
                    AddItemResult.Added, AddItemResult.Merged, AddItemResult.Reopened -> onSaveQuery("")
                    AddItemResult.EmptyInput -> Unit
                    AddItemResult.QuantityLimit -> info("수량은 99개까지 담을 수 있어요")
                    AddItemResult.Failed -> info("저장하지 못했어요. 다시 시도해 주세요")
                    is AddItemResult.OtherIntent -> scope.launch {
                        val message = if (result.intent == PurchaseIntent.BUY) "이미 살 것에 있어요" else "이미 고민 중에 있어요"
                        if (snackbar.showSnackbar(message, actionLabel = "보기", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) {
                            vm.selectIntent(result.intent)
                        }
                    }
                }
            }
        },
        onToggle = vm::toggleItem,
        onEdit = { editError = null; editTarget = it },
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
        onBackToStores = onBackToStores,
        onShare = {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, vm.buildShareText())
            }
            context.startActivity(Intent.createChooser(sendIntent, "살 것만 공유 · 고민 중 제외"))
        },
        onClearCompleted = { clearCompleted = true },
        onDecide = {
            focusManager.clearFocus()
            keyboard?.hide()
            snackbar.currentSnackbarData?.dismiss()
            vm.openDecision(it)
        }
    )

    decision?.let { session ->
        DecisionRouletteDialog(session, vm::startDecision, vm::finishDecisionAnimation, onDismiss = {
            vm.dismissDecision()?.let { result ->
                val message = result.undoToken.before.name + if (result.outcome == DecisionOutcome.BUY) " · 살 것으로 옮겼어요" else " · 삭제했어요"
                showUndo(message, result.undoToken, SnackbarDuration.Long)
            }
        }, onRetry = vm::retryDecision)
    }

    if (clearCompleted) {
        ConfirmDialog(
            title = "완료 항목 비우기",
            message = "구매 완료한 물건 ${state.completedItems.size}개를 비울까요?",
            confirmLabel = "비우기",
            onConfirm = {
                vm.clearCompleted { token ->
                    if (token == null) {
                        info("비울 완료 항목이 없거나 비우지 못했어요")
                    } else scope.launch {
                        val result = snackbar.showSnackbar("완료 항목을 비웠어요", actionLabel = "실행 취소", duration = SnackbarDuration.Short)
                        if (result == SnackbarResult.ActionPerformed) vm.undoClearCompleted(token) { restored ->
                            if (!restored) info("되돌릴 수 없어요. 목록이 변경되었는지 확인해 주세요")
                        }
                    }
                }
                clearCompleted = false
            },
            onDismiss = { clearCompleted = false }
        )
    }
    editTarget?.let { item ->
        EditItemSheet(
            item = item,
            errorMessage = editError,
            onDismiss = { editTarget = null },
            onSave = { name, quantity, category, plannedBuyAt, preferredStore, mustBuyBy, stockUpMonth, stockQuantity ->
                vm.updateItemAndPlan(item.id, name, quantity, category, plannedBuyAt, preferredStore, mustBuyBy, stockUpMonth, stockQuantity) { result ->
                    when (result) {
                        UpdateItemResult.UPDATED -> editTarget = null
                        UpdateItemResult.DUPLICATE_NAME -> editError = "이 목록에 같은 이름의 물건이 있어요"
                        UpdateItemResult.INVALID_NAME -> editError = "물건 이름을 적어주세요"
                        UpdateItemResult.MISSING -> { editTarget = null; info("이 물건을 찾을 수 없어요") }
                        UpdateItemResult.FAILED -> editError = "저장하지 못했어요. 다시 시도해 주세요"
                    }
                }
            },
            onDelete = { editTarget = null; removeItem(item.id) }
        )
    }
}
