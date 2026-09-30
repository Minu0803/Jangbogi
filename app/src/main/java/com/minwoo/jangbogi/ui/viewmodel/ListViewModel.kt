package com.minwoo.jangbogi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minwoo.jangbogi.data.JangbogiRepository
import com.minwoo.jangbogi.data.AddItemResult
import com.minwoo.jangbogi.data.ItemMutationResult
import com.minwoo.jangbogi.data.UndoToken
import com.minwoo.jangbogi.data.UndoResult
import com.minwoo.jangbogi.data.UpdateItemResult
import com.minwoo.jangbogi.data.ClearedCompletedToken
import com.minwoo.jangbogi.domain.Category
import com.minwoo.jangbogi.domain.ItemOrganizer
import com.minwoo.jangbogi.domain.ListUiState
import com.minwoo.jangbogi.domain.ShareTextBuilder
import com.minwoo.jangbogi.domain.Suggestion
import com.minwoo.jangbogi.domain.PurchaseIntent
import com.minwoo.jangbogi.domain.ShoppingSummary
import com.minwoo.jangbogi.domain.DecisionOutcome
import com.minwoo.jangbogi.domain.DecisionPhase
import com.minwoo.jangbogi.domain.DecisionRoulette
import com.minwoo.jangbogi.domain.DecisionSession
import com.minwoo.jangbogi.data.ShoppingItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class ListViewModel(
    private val repo: JangbogiRepository,
    private val listId: Long,
    private val random: Random = Random.Default
) : ViewModel() {

    private var restoredSelectionEpoch: Long? = null

    fun restoreEntry(epoch: Long, intent: PurchaseIntent, initialQuery: String) {
        if (restoredSelectionEpoch == epoch) return
        restoredSelectionEpoch = epoch
        selectIntent(intent)
        onQueryChange(initialQuery)
    }
    private var adding = false
    private var nextDecisionId = 1L
    private val _decisionSession = MutableStateFlow<DecisionSession?>(null)
    val decisionSession: StateFlow<DecisionSession?> = _decisionSession.asStateFlow()

    fun openDecision(item: ShoppingItem) {
        if (_decisionSession.value != null || item.listId != listId || item.purchaseIntent != PurchaseIntent.CONSIDER) return
        _decisionSession.value = DecisionSession(nextDecisionId++, item)
    }

    fun startDecision() {
        val current = _decisionSession.value ?: return
        if (current.phase != DecisionPhase.READY) return
        _decisionSession.value = current.start(DecisionRoulette.spin(random)) ?: return
    }

    fun finishDecisionAnimation(sessionId: Long) {
        val current = _decisionSession.value ?: return
        if (current.id != sessionId) return
        val applying = current.finishAnimation() ?: return
        _decisionSession.value = applying
        applyDecision(applying)
    }

    fun retryDecision() {
        val retry = _decisionSession.value?.retry() ?: return
        _decisionSession.value = retry
        applyDecision(retry)
    }

    private fun applyDecision(session: DecisionSession) {
        viewModelScope.launch {
            try {
                val result = repo.applyDecision(session.item, requireNotNull(session.spin).outcome)
                if (_decisionSession.value?.id != session.id || _decisionSession.value?.phase != DecisionPhase.APPLYING) return@launch
                _decisionSession.value = when (result) {
                    is ItemMutationResult.Applied -> session.succeed(result.undoToken)
                    ItemMutationResult.Missing, ItemMutationResult.NoChange -> session.fail("물건이 변경되었어요. 목록에서 다시 확인해 주세요")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (_decisionSession.value?.id == session.id && _decisionSession.value?.phase == DecisionPhase.APPLYING) {
                    _decisionSession.value = session.fail("저장하지 못했어요. 다시 시도해 주세요")
                }
            }
        }
    }

    fun dismissDecision(): DecisionDismissal? {
        val current = _decisionSession.value ?: return null
        if (current.phase != DecisionPhase.READY && current.phase != DecisionPhase.ERROR && current.phase != DecisionPhase.RESULT) return null
        _decisionSession.value = null
        val token = current.undoToken ?: return null
        return DecisionDismissal(requireNotNull(current.spin).outcome, token)
    }

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
        viewModelScope.launch {
            val restored = try { repo.undoMutation(token) == UndoResult.RESTORED }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { false }
            onResult(restored)
        }
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

    fun renameList(newName: String) {
        viewModelScope.launch { repo.renameList(listId, newName) }
    }

    fun clearCompleted(onResult: (ClearedCompletedToken?) -> Unit) {
        viewModelScope.launch {
            val token = try {
                repo.clearCompleted(listId)
            } catch (_: Exception) {
                null
            }
            onResult(token)
        }
    }

    fun undoClearCompleted(token: ClearedCompletedToken, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val restored = try {
                repo.undoClearCompleted(token) == UndoResult.RESTORED
            } catch (_: Exception) {
                false
            }
            onResult(restored)
        }
    }

    fun buildShareText(): String {
        val state = uiState.value
        val items = state.sections.flatMap { it.items } + state.completedItems
        return ShareTextBuilder.build(state.listName, items)
    }
}

data class DecisionDismissal(val outcome: DecisionOutcome, val undoToken: UndoToken)
