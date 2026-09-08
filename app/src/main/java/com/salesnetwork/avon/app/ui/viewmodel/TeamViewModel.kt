package com.salesnetwork.avon.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.salesnetwork.avon.app.data.LeaderNetworkRepository
import com.salesnetwork.avon.app.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TeamUiState(
    val leaderUser: User? = null,
    val members: List<User> = emptyList(),
    val totalTeamCount: Int = 0
)

class TeamViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LeaderNetworkRepository.getInstance(application)

    private val _uiState = MutableStateFlow(TeamUiState())
    val uiState: StateFlow<TeamUiState> = _uiState.asStateFlow()

    fun loadTeamForLeader(leaderUser: User) {
        val membersList = repository.getMembersForLeader(leaderUser.referralCode)
        _uiState.value = TeamUiState(
            leaderUser = leaderUser,
            members = membersList,
            totalTeamCount = membersList.size
        )
    }
}
