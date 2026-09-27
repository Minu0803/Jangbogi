package com.minwoo.jangbogi.ui.viewmodel

import androidx.lifecycle.ViewModel
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
import kotlinx.coroutines.launch

class HomeViewModel(private val repo: JangbogiRepository) : ViewModel() {

    val lists: StateFlow<List<ListWithProgress>> =
        repo.observeListsWithProgress()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    fun deleteList(listId: Long, onDeleted: (DeletedListToken?) -> Unit) {
        viewModelScope.launch { onDeleted(repo.deleteList(listId)) }
    }

    fun undoDeleteList(token: DeletedListToken, onRestored: (Boolean) -> Unit) {
        viewModelScope.launch { onRestored(repo.undoDeleteList(token)) }
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
