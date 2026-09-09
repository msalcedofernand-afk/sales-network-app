package com.salesnetwork.avon.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.salesnetwork.avon.app.data.LeaderNetworkRepository
import com.salesnetwork.avon.app.data.OrderRepository
import com.salesnetwork.avon.app.domain.model.User
import com.salesnetwork.avon.app.domain.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LeaderSupervisionData(
    val leader: User,
    val members: List<User>,
    val activeMembersCount: Int,
    val totalTeamSales: Double,
    val networkCommission: Double
)

data class TeamUiState(
    val currentUser: User? = null,
    val isRootAdmin: Boolean = false,
    val members: List<User> = emptyList(),
    val totalTeamCount: Int = 0,
    val activeMembersCount: Int = 0,
    val networkCommissionTotal: Double = 0.0,
    val allLeadersData: List<LeaderSupervisionData> = emptyList(),
    val globalTotalSales: Double = 0.0,
    val globalTotalMembers: Int = 0
)

class TeamViewModel(application: Application) : AndroidViewModel(application) {

    private val leaderRepo = LeaderNetworkRepository.getInstance(application)
    private val orderRepo = OrderRepository.getInstance(application)

    private val _uiState = MutableStateFlow(TeamUiState())
    val uiState: StateFlow<TeamUiState> = _uiState.asStateFlow()

    fun loadTeamForLeader(currentUser: User) = loadTeamForUser(currentUser)

    fun loadTeamForUser(currentUser: User) {
        if (currentUser.role == UserRole.ROOT_ADMIN) {
            // El Super Admin supervisa a todos los líderes de la red
            val allLeaders = leaderRepo.getAllUsers().filter { it.role == UserRole.LIDER }
            var totalSalesAll = 0.0
            var totalMembersAll = 0

            val supervisionList = allLeaders.map { leader ->
                val team = leaderRepo.getMembersForLeader(leader.referralCode)
                val activeCount = team.count { it.isActiveInCampaign }
                val orders = orderRepo.getOrdersForLeader(leader.id)
                // Never manufacture sales for an account that has not synced orders yet.
                val sales = orders.sumOf { it.totalAmount }
                val comm = orders.sumOf { it.networkCommissionLeader }

                totalSalesAll += sales
                totalMembersAll += team.size

                LeaderSupervisionData(
                    leader = leader,
                    members = team,
                    activeMembersCount = activeCount,
                    totalTeamSales = sales,
                    networkCommission = comm
                )
            }

            _uiState.value = TeamUiState(
                currentUser = currentUser,
                isRootAdmin = true,
                allLeadersData = supervisionList,
                globalTotalSales = totalSalesAll,
                globalTotalMembers = totalMembersAll
            )
        } else {
            // Vista de Líder individual
            val membersList = leaderRepo.getMembersForLeader(currentUser.referralCode)
            val activeCount = membersList.count { it.isActiveInCampaign }
            val netComm = orderRepo.calculateTotalNetworkCommission(currentUser.id)

            _uiState.value = TeamUiState(
                currentUser = currentUser,
                isRootAdmin = false,
                members = membersList,
                totalTeamCount = membersList.size,
                activeMembersCount = activeCount,
                networkCommissionTotal = netComm
            )
        }
    }
}
