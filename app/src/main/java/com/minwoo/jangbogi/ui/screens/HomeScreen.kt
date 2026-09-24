package com.minwoo.jangbogi.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minwoo.jangbogi.JangbogiApp
import com.minwoo.jangbogi.R
import com.minwoo.jangbogi.domain.ListWithProgress
import com.minwoo.jangbogi.ui.components.ConfirmDialog
import com.minwoo.jangbogi.ui.components.EmptyState
import com.minwoo.jangbogi.ui.components.ListCard
import com.minwoo.jangbogi.ui.components.NameDialog
import com.minwoo.jangbogi.ui.viewmodel.HomeViewModel
import java.time.LocalDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(onOpenList: (Long) -> Unit, onOpenPlan: () -> Unit) {
    val app = LocalContext.current.applicationContext as JangbogiApp
    val vm: HomeViewModel = viewModel(factory = app.container.homeViewModelFactory())
    val lists by vm.lists.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ListWithProgress?>(null) }
    var deleteTarget by remember { mutableStateOf<ListWithProgress?>(null) }

    val deletedMsg = stringResource(R.string.snackbar_list_deleted)
    val undoLabel = stringResource(R.string.undo)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
                },
                actions = {
                    TextButton(onClick = onOpenPlan) { Text("살림 계획") }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.new_list)) }
            )
        }
    ) { padding ->
        if (lists.isEmpty()) {
            EmptyState(
                emoji = "🛒",
                title = stringResource(R.string.home_empty_title),
                subtitle = stringResource(R.string.home_empty_subtitle),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(lists, key = { it.list.id }) { entry ->
                    ListCard(
                        entry = entry,
                        onClick = { onOpenList(entry.list.id) },
                        onRename = { renameTarget = entry },
                        onDelete = { deleteTarget = entry },
                        modifier = Modifier.animateItemPlacement()
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        NameDialog(
            title = stringResource(R.string.new_list),
            initialValue = defaultListName(),
            confirmLabel = stringResource(R.string.new_list_confirm),
            allowBlank = true,
            onConfirm = { name -> vm.createList(name) { id -> onOpenList(id) } },
            onDismiss = { showCreateDialog = false }
        )
    }
    renameTarget?.let { target ->
        NameDialog(
            title = stringResource(R.string.rename),
            initialValue = target.list.name,
            confirmLabel = stringResource(R.string.save),
            onConfirm = { vm.renameList(target.list.id, it) },
            onDismiss = { renameTarget = null }
        )
    }
    deleteTarget?.let { target ->
        ConfirmDialog(
            title = stringResource(R.string.delete_list_title),
            message = stringResource(R.string.delete_list_message, target.list.name),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                vm.deleteList(target.list.id)
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = deletedMsg,
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) vm.undoDeleteList()
                }
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun defaultListName(): String {
    val today = LocalDate.now()
    return stringResource(R.string.default_list_name, today.monthValue, today.dayOfMonth)
}
