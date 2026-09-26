package com.minwoo.jangbogi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minwoo.jangbogi.data.JangbogiRepository
import com.minwoo.jangbogi.data.AddItemResult
import com.minwoo.jangbogi.data.ItemMutationResult
import com.minwoo.jangbogi.data.UndoToken
import com.minwoo.jangbogi.data.UndoResult
import com.minwoo.jangbogi.domain.Category
import com.minwoo.jangbogi.domain.ItemOrganizer
import com.minwoo.jangbogi.domain.ListUiState
import com.minwoo.jangbogi.domain.ShareTextBuilder
import com.minwoo.jangbogi.domain.Suggestion
import com.minwoo.jangbogi.domain.PurchaseIntent
import com.minwoo.jangbogi.domain.ShoppingSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ListViewModel(
    private val repo: JangbogiRepository,
    private val listId: Long
) : ViewModel() {

    private var adding = false

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _selectedIntent = MutableStateFlow(PurchaseIntent.BUY)
    val selectedIntent: StateFlow<PurchaseIntent> = _selectedIntent.asStateFlow()

    val uiState: StateFlow<ListUiState> =
        combine(repo.observeList(listId), repo.observeItems(listId)) { list, items ->
            val summary = ShoppingSummary.from(items)
            ListUiState(
                listName = list?.name.orEmpty(),
                sections = ItemOrganizer.sections(items),
                completedItems = ItemOrganizer.completed(items),
                consideringItems = ItemOrganizer.considering(items),
                totalCount = summary.purchaseCount,
                checkedCount = summary.completedCount,
                isLoading = false
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListUiState())

    val suggestions: StateFlow<List<Suggestion>> =
        combine(_query, _selectedIntent) { q, intent -> q.trim() to intent }
            .flatMapLatest { (q, intent) -> repo.observeSuggestions(listId, q, intent) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun selectIntent(intent: PurchaseIntent) { _selectedIntent.value = intent }

    fun addItem(rawInput: String, onResult: (AddItemResult) -> Unit = {}) {
        if (adding) return
        adding = true
        viewModelScope.launch {
            try {
                val result = repo.addItem(listId, rawInput, _selectedIntent.value)
                if (result == AddItemResult.Added || result == AddItemResult.Merged || result == AddItemResult.Reopened) _query.value = ""
                onResult(result)
            } catch (_: Exception) {
                onResult(AddItemResult.Failed)
            } finally {
                adding = false
            }
        }
    }

    fun toggleItem(itemId: Long) {
        viewModelScope.launch { repo.toggleItem(itemId) }
    }

    fun moveItem(itemId: Long, intent: PurchaseIntent, onResult: (ItemMutationResult) -> Unit) {
        viewModelScope.launch { onResult(repo.moveItem(itemId, intent)) }
    }

    fun deleteItemWithUndo(itemId: Long, onResult: (ItemMutationResult) -> Unit) {
        viewModelScope.launch { onResult(repo.deleteItemWithUndo(itemId)) }
    }

    fun undoMutation(token: UndoToken, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(repo.undoMutation(token) == UndoResult.RESTORED) }
    }

    fun deleteItem(itemId: Long) {
        viewModelScope.launch { repo.deleteItem(itemId) }
    }

    fun undoDeleteItem() {
        viewModelScope.launch { repo.undoDeleteItem() }
    }

    fun updateItem(itemId: Long, name: String, quantity: Int, category: Category) {
        viewModelScope.launch { repo.updateItem(itemId, name, quantity, category) }
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
        stockQuantity: Int
    ) {
        viewModelScope.launch {
            repo.updateItemAndPlan(itemId, name, quantity, category, plannedBuyAt, preferredStore, mustBuyBy, stockUpMonth, stockQuantity)
        }
    }

    fun renameList(newName: String) {
        viewModelScope.launch { repo.renameList(listId, newName) }
    }

    fun clearCompleted() {
        viewModelScope.launch { repo.clearCompleted(listId) }
    }

    fun undoClearCompleted() {
        viewModelScope.launch { repo.undoClearCompleted() }
    }

    fun buildShareText(): String {
        val state = uiState.value
        val items = state.sections.flatMap { it.items } + state.completedItems
        return ShareTextBuilder.build(state.listName, items)
    }
}
