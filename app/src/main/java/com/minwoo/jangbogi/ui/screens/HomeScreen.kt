package com.minwoo.jangbogi.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minwoo.jangbogi.ui.components.ConfirmDialog
import com.minwoo.jangbogi.ui.components.StoreCard
import com.minwoo.jangbogi.ui.components.StoreIllustration
import com.minwoo.jangbogi.ui.components.StoreNameSheet
import com.minwoo.jangbogi.ui.viewmodel.HomeViewModel
import com.minwoo.jangbogi.ui.viewmodel.StoreAction
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(vm: HomeViewModel, onOpenList: (Long) -> Unit, onOpenPlan: () -> Unit,
               createRequested: Boolean = false, onCreateRequestConsumed: () -> Unit = {}) {
    val stores by vm.lists.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()
    val operation by vm.operation.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var create by rememberSaveable { mutableStateOf(false) }
    var renameId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    fun info(message: String) { scope.launch { snackbar.showSnackbar(message) } }
    fun add() { error = null; create = true }
    LaunchedEffect(createRequested) {
        if (createRequested) { add(); onCreateRequestConsumed() }
    }
    LaunchedEffect(operation) {
        operation?.let { result ->
            vm.consumeOperation(result)
            if (result.error != null) {
                if (result.action == StoreAction.OPEN) info(result.error) else error = result.error
            } else {
                create = false; renameId = null; error = null
                if (result.action != StoreAction.RENAME) onOpenList(requireNotNull(result.storeId))
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StoreIllustration(Modifier.size(32.dp, 28.dp))
                Text("장보기", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }, actions = { TextButton(onClick = onOpenPlan) { Text("살림 계획") } }) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (!loading) Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp)) {
                Button(onClick = ::add, enabled = !saving, shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp)); Text("마트 추가")
                }
            }
        }
    ) { padding ->
        if (loading) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else if (stores.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                StoreHero(empty = true)
            }
        } else LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("store-home-list"),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "hero") { StoreHero(empty = false) }
            item(key = "label") {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("내 마트", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text("${stores.size}곳", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(stores, key = { it.list.id }) { store ->
                StoreCard(store, onClick = { vm.selectStore(store.list.id) },
                    onRename = { error = null; renameId = store.list.id }, onDelete = { deleteId = store.list.id },
                    modifier = Modifier.animateItemPlacement())
            }
        }
    }
    val rename = stores.firstOrNull { it.list.id == renameId }
    if (create || rename != null) StoreNameSheet(
        initialName = rename?.list?.name.orEmpty(), isRename = rename != null, saving = saving, error = error,
        onSave = { name ->
            error = null
            vm.saveStore(name, rename?.list?.id)
        }, onDismiss = { create = false; renameId = null; error = null }
    )
    stores.firstOrNull { it.list.id == deleteId }?.let { target ->
        ConfirmDialog(title = "마트를 삭제할까요?", message = "‘${target.list.name}’와 담아둔 물건이 함께 삭제돼요.",
            confirmLabel = "삭제", onDismiss = { deleteId = null }, onConfirm = {
                vm.deleteList(target.list.id, onDeleted = { token ->
                    if (token != null) scope.launch {
                        snackbar.currentSnackbarData?.dismiss()
                        if (snackbar.showSnackbar("마트를 삭제했어요", actionLabel = "실행 취소", duration = SnackbarDuration.Long)
                            == SnackbarResult.ActionPerformed) vm.undoDeleteList(token) { restored ->
                            if (!restored) info("마트를 되돌릴 수 없어요")
                        }
                    }
                }, onError = ::info)
            })
    }
}

@Composable
private fun StoreHero(empty: Boolean) {
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, tween(280)) }
    Column(Modifier.fillMaxWidth().padding(top = if (empty) 0.dp else 8.dp, bottom = 16.dp)
        .graphicsLayer { alpha = entrance.value; translationY = (1f - entrance.value) * 12.dp.toPx() },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StoreIllustration(Modifier.size(if (empty) 188.dp else 124.dp, if (empty) 150.dp else 100.dp))
        Text("오늘의 장보기", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text("어디서 장을 볼까요?", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        Text(if (empty) "자주 가는 마트를 추가하고\n살 것들을 차근차근 담아보세요." else "마트를 고르면, 장보기가 시작돼요.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
