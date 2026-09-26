package com.minwoo.jangbogi.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minwoo.jangbogi.domain.ListUiState
import com.minwoo.jangbogi.domain.PurchaseIntent
import com.minwoo.jangbogi.ui.components.ListSwitcherSheet
import com.minwoo.jangbogi.ui.components.ShoppingContent
import com.minwoo.jangbogi.ui.viewmodel.ShoppingHomeViewModel
import kotlinx.coroutines.launch

@Composable
fun ShoppingHomeScreen(vm: ShoppingHomeViewModel, onOpenPlan: () -> Unit, onManageLists: () -> Unit) {
    val activeId by vm.activeListId.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val draft by vm.draft.collectAsStateWithLifecycle()
    val draftName by vm.draftName.collectAsStateWithLifecycle()
    val draftQuery by vm.draftQuery.collectAsStateWithLifecycle()
    val draftIntent by vm.draftIntent.collectAsStateWithLifecycle()
    val lists by vm.lists.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showLists by remember { mutableStateOf(false) }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else if (activeId != null && !draft) {
        ListScreen(
            listId = activeId!!,
            initialQuery = vm.savedQuery(activeId!!),
            onSaveQuery = { vm.saveQuery(activeId!!, it) },
            onOpenLists = { showLists = true },
            onOpenPlan = onOpenPlan
        )
    } else {
        ShoppingContent(
            state = ListUiState(listName = if (draft) draftName else "내 장보기", isLoading = false),
            selectedIntent = draftIntent,
            query = draftQuery,
            suggestions = emptyList(),
            snackbarHostState = snackbar,
            onQueryChange = vm::setDraftQuery,
            onSelectIntent = vm::setDraftIntent,
            onAdd = { raw -> vm.addToDraft(raw) { message -> scope.launch { snackbar.showSnackbar(message) } } },
            onToggle = {},
            onEdit = {},
            onDelete = {},
            onMove = { _, _ -> },
            onOpenLists = { showLists = true },
            onOpenPlan = onOpenPlan
        )
    }

    if (showLists) ListSwitcherSheet(
        lists = lists,
        activeListId = if (draft) null else activeId,
        onSelect = { vm.selectList(it); showLists = false },
        onNewList = { vm.startNewDraft(); showLists = false },
        onManage = { showLists = false; onManageLists() },
        onDismiss = { showLists = false }
    )
}
