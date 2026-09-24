package com.minwoo.jangbogi.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minwoo.jangbogi.JangbogiApp
import com.minwoo.jangbogi.R
import com.minwoo.jangbogi.data.ShoppingItem
import com.minwoo.jangbogi.ui.components.ConfirmDialog
import com.minwoo.jangbogi.ui.components.EditItemSheet
import com.minwoo.jangbogi.ui.components.EmptyState
import com.minwoo.jangbogi.ui.components.ItemRow
import com.minwoo.jangbogi.ui.components.NameDialog
import com.minwoo.jangbogi.ui.components.ProgressHeader
import com.minwoo.jangbogi.ui.components.QuickAddBar
import com.minwoo.jangbogi.ui.components.SectionHeader
import com.minwoo.jangbogi.ui.components.SuggestionChips
import com.minwoo.jangbogi.ui.viewmodel.ListViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ListScreen(listId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as JangbogiApp
    val vm: ListViewModel = viewModel(
        key = "list-$listId",
        factory = app.container.listViewModelFactory(listId)
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val suggestions by vm.suggestions.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var menuExpanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<ShoppingItem?>(null) }

    val itemDeletedMsg = stringResource(R.string.snackbar_item_deleted)
    val clearedMsg = stringResource(R.string.snackbar_completed_cleared)
    val undoLabel = stringResource(R.string.undo)
    val shareTitle = stringResource(R.string.share_chooser_title)

    fun showUndoSnackbar(message: String, onUndo: () -> Unit) {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) onUndo()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.listName,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showRenameDialog = true }
                            .padding(4.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, vm.buildShareText())
                        }
                        context.startActivity(Intent.createChooser(sendIntent, shareTitle))
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = stringResource(R.string.share)
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.more)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.clear_completed)) },
                                enabled = uiState.completedItems.isNotEmpty(),
                                onClick = { menuExpanded = false; showClearDialog = true }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedVisibility(visible = uiState.totalCount > 0) {
                ProgressHeader(
                    totalCount = uiState.totalCount,
                    checkedCount = uiState.checkedCount
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                    uiState.totalCount == 0 -> {
                        EmptyState(
                            emoji = "🧺",
                            title = stringResource(R.string.list_detail_empty_title),
                            subtitle = stringResource(R.string.list_detail_empty_subtitle),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            uiState.sections.forEach { section ->
                                stickyHeader(key = "h-${section.category.name}") {
                                    SectionHeader(
                                        emoji = section.category.emoji,
                                        label = section.category.display,
                                        count = section.items.size
                                    )
                                }
                                items(section.items, key = { it.id }) { item ->
                                    ItemRow(
                                        item = item,
                                        onToggle = { vm.toggleItem(item.id) },
                                        onEdit = { editTarget = item },
                                        onDelete = {
                                            vm.deleteItem(item.id)
                                            showUndoSnackbar(itemDeletedMsg) { vm.undoDeleteItem() }
                                        },
                                        modifier = Modifier
                                            .animateItemPlacement()
                                            .padding(horizontal = 16.dp)
                                    )
                                }
                            }
                            if (uiState.completedItems.isNotEmpty()) {
                                stickyHeader(key = "h-completed") {
                                    SectionHeader(
                                        emoji = "✅",
                                        label = stringResource(R.string.completed_section_label),
                                        count = uiState.completedItems.size
                                    )
                                }
                                items(uiState.completedItems, key = { it.id }) { item ->
                                    ItemRow(
                                        item = item,
                                        onToggle = { vm.toggleItem(item.id) },
                                        onEdit = { editTarget = item },
                                        onDelete = {
                                            vm.deleteItem(item.id)
                                            showUndoSnackbar(itemDeletedMsg) { vm.undoDeleteItem() }
                                        },
                                        modifier = Modifier
                                            .animateItemPlacement()
                                            .padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(visible = suggestions.isNotEmpty()) {
                SuggestionChips(
                    suggestions = suggestions,
                    onPick = { vm.addItem(it.name) }
                )
            }
            QuickAddBar(
                query = query,
                onQueryChange = vm::onQueryChange,
                onAdd = { if (query.isNotBlank()) vm.addItem(query) },
                modifier = Modifier
                    .imePadding()
                    .navigationBarsPadding()
            )
        }
    }

    if (showRenameDialog) {
        NameDialog(
            title = stringResource(R.string.rename_list_title),
            initialValue = uiState.listName,
            confirmLabel = stringResource(R.string.save),
            onConfirm = { vm.renameList(it) },
            onDismiss = { showRenameDialog = false }
        )
    }
    if (showClearDialog) {
        ConfirmDialog(
            title = stringResource(R.string.clear_completed),
            message = stringResource(R.string.clear_completed_message, uiState.completedItems.size),
            confirmLabel = stringResource(R.string.clear_confirm),
            onConfirm = {
                vm.clearCompleted()
                showUndoSnackbar(clearedMsg) { vm.undoClearCompleted() }
            },
            onDismiss = { showClearDialog = false }
        )
    }
    editTarget?.let { item ->
        EditItemSheet(
            item = item,
            onDismiss = { editTarget = null },
            onSave = { name, quantity, category ->
                vm.updateItem(item.id, name, quantity, category)
                editTarget = null
            },
            onDelete = {
                vm.deleteItem(item.id)
                editTarget = null
                showUndoSnackbar(itemDeletedMsg) { vm.undoDeleteItem() }
            }
        )
    }
}
