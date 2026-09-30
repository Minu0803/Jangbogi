package com.minwoo.jangbogi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minwoo.jangbogi.domain.PurchaseIntent
import com.minwoo.jangbogi.ui.components.ListSwitcherSheet
import com.minwoo.jangbogi.ui.viewmodel.ShoppingHomeViewModel

@Composable
fun ShoppingHomeScreen(vm: ShoppingHomeViewModel, onOpenPlan: () -> Unit, onBack: () -> Unit,
                       onSelectStore: (Long) -> Unit, onNewStore: () -> Unit,
                       initialQuery: String, onSaveQuery: (String) -> Unit) {
    val loading by vm.loading.collectAsStateWithLifecycle()
    val exists by vm.exists.collectAsStateWithLifecycle()
    val stores by vm.lists.collectAsStateWithLifecycle()
    var showStores by remember { mutableStateOf(false) }
    if (loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    else if (!exists) Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text("마트를 찾을 수 없어요", style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = onBack) { Text("마트 홈으로") }
    } else ListScreen(listId = vm.storeId, selectionEpoch = 0L, entryIntent = PurchaseIntent.BUY,
        initialQuery = initialQuery, onSaveQuery = onSaveQuery, onOpenLists = { showStores = true },
        onOpenPlan = onOpenPlan, onBackToStores = onBack)
    if (showStores) ListSwitcherSheet(lists = stores, activeListId = vm.storeId,
        onSelect = { showStores = false; if (it != vm.storeId) onSelectStore(it) },
        onNewList = { showStores = false; onNewStore() }, onManage = { showStores = false; onBack() },
        onDismiss = { showStores = false })
}
