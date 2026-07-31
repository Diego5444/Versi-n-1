package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ContinueWatchingEntry
import com.example.data.model.WatchHistoryEntry
import com.example.data.repository.AuthRepository
import com.example.data.repository.UserDataRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val userDataRepository: UserDataRepository = UserDataRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val currentUserId = authRepository.observeAuthState()

    val watchHistory: StateFlow<List<WatchHistoryEntry>> = currentUserId.flatMapLatest { user ->
        if (user != null) {
            userDataRepository.getWatchHistoryFlow(user.uid)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val continueWatchingList: StateFlow<List<ContinueWatchingEntry>> = currentUserId.flatMapLatest { user ->
        if (user != null) {
            userDataRepository.getContinueWatchingFlow(user.uid)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun recordWatchHistory(entry: WatchHistoryEntry) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            userDataRepository.addOrUpdateHistory(uid, entry)
        }
    }

    fun saveContinueWatching(entry: ContinueWatchingEntry) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            userDataRepository.saveContinueWatching(uid, entry)
        }
    }

    fun removeContinueWatching(contentId: String) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            userDataRepository.removeContinueWatching(uid, contentId)
        }
    }

    fun clearHistory() {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            userDataRepository.clearWatchHistory(uid)
        }
    }
}
