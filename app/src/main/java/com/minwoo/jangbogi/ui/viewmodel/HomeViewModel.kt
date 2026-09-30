package com.minwoo.jangbogi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.minwoo.jangbogi.data.JangbogiRepository
import com.minwoo.jangbogi.domain.ListWithProgress
import com.minwoo.jangbogi.data.PlannedShoppingItem
import com.minwoo.jangbogi.data.UpdateItemResult
import com.minwoo.jangbogi.data.DeletedListToken
import com.minwoo.jangbogi.domain.Category
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

enum class StoreAction { CREATE, RENAME, OPEN }
data class StoreOperation(val action: StoreAction, val storeId: Long? = null, val error: String? = null)

class HomeViewModel(private val repo: JangbogiRepository, private val savedState: SavedStateHandle = SavedStateHandle()) : ViewModel() {
    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()
    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()
    private val _operation = MutableStateFlow<StoreOperation?>(null)
    val operation = _operation.asStateFlow()

    fun consumeOperation(result: StoreOperation) {
        if (_operation.value === result) _operation.value = null
    }

    val lists: StateFlow<List<ListWithProgress>> =
        repo.observeListsWithProgress()
            .onEach { _loading.value = false }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun savedQuery(id: Long): String = savedState["store_query_$id"] ?: ""
    fun saveQuery(id: Long, query: String) { savedState["store_query_$id"] = query }

    fun saveStore(name: String, id: Long? = null) {
        if (_saving.value || _operation.value != null || name.isBlank()) return
        _saving.value = true
        val action = if (id == null) StoreAction.CREATE else StoreAction.RENAME
        viewModelScope.launch {
            try {
                val storeId = if (id == null) repo.createStore(name) else { repo.renameList(id, name); id }
                check(storeId > 0)
                _operation.value = StoreOperation(action, storeId)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _operation.value = StoreOperation(action, error = "저장하지 못했어요. 다시 시도해 주세요") }
            finally { _saving.value = false }
        }
    }

    fun selectStore(id: Long) {
        if (_saving.value || _operation.value != null) return
        _saving.value = true
        viewModelScope.launch {
            try {
                _operation.value = if (repo.selectList(id)) StoreOperation(StoreAction.OPEN, id)
                    else StoreOperation(StoreAction.OPEN, error = "마트를 찾을 수 없어요")
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _operation.value = StoreOperation(StoreAction.OPEN, error = "마트를 열지 못했어요. 다시 시도해 주세요") }
            finally { _saving.value = false }
        }
    }

    val plannedItems: StateFlow<List<PlannedShoppingItem>> =
        repo.observeItemsForPlanning()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createList(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            onCreated(repo.createList(name))
        }
    }

    fun renameList(listId: Long, newName: String) {
        viewModelScope.launch { repo.renameList(listId, newName) }
    }

    fun deleteList(listId: Long, onDeleted: (DeletedListToken?) -> Unit, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try { onDeleted(repo.deleteList(listId)) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { onError("삭제하지 못했어요. 다시 시도해 주세요") }
        }
    }

    fun undoDeleteList(token: DeletedListToken, onRestored: (Boolean) -> Unit) {
        viewModelScope.launch {
            try { onRestored(repo.undoDeleteList(token)) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { onRestored(false) }
        }
    }

    fun updateShoppingPlan(
        name: String,
        quantity: Int,
        category: Category,
        plannedBuyAt: Long?,
        preferredStore: String,
        mustBuyBy: Long?,
        stockUpMonth: Int?,
        stockQuantity: Int
    ) {
        viewModelScope.launch {
            repo.updateShoppingPlan(name, quantity, category, plannedBuyAt, preferredStore, mustBuyBy, stockUpMonth, stockQuantity)
        }
    }

    fun updateItemAndPlan(
        itemId: Long,
        name: String,
        quantity: Int,
        category: Category,
        plannedBuyAt: Long?,
        preferredStore: String,
        mustBuyBy: Long?,
        stockUpMonth: Int?,
        stockQuantity: Int,
        onResult: (UpdateItemResult) -> Unit
    ) {
        viewModelScope.launch {
            try {
                onResult(repo.updateItemAndPlan(itemId, name, quantity, category, plannedBuyAt, preferredStore, mustBuyBy, stockUpMonth, stockQuantity))
            } catch (_: Exception) {
                onResult(UpdateItemResult.FAILED)
            }
        }
    }
}
