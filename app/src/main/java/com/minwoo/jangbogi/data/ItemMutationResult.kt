package com.minwoo.jangbogi.data

import com.minwoo.jangbogi.domain.PurchaseIntent

sealed interface AddItemResult {
    data object Added : AddItemResult
    data object Merged : AddItemResult
    data object Reopened : AddItemResult
    data object EmptyInput : AddItemResult
    data object QuantityLimit : AddItemResult
    data object Failed : AddItemResult
    data class OtherIntent(val itemId: Long, val intent: PurchaseIntent) : AddItemResult
}

data class UndoToken(val id: Long, val before: ShoppingItem, val after: ShoppingItem?)

sealed interface ItemMutationResult {
    data class Applied(val undoToken: UndoToken) : ItemMutationResult
    data object NoChange : ItemMutationResult
    data object Missing : ItemMutationResult
}

enum class UndoResult { RESTORED, CONFLICT, MISSING_PARENT }

enum class UpdateItemResult { UPDATED, DUPLICATE_NAME, INVALID_NAME, MISSING, FAILED }

data class DeletedListToken(val list: ShoppingList, val items: List<ShoppingItem>)

data class ClearedCompletedToken(val id: Long, val listId: Long, val items: List<ShoppingItem>)
