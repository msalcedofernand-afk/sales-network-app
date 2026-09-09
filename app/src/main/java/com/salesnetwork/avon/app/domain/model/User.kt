package com.salesnetwork.avon.app.domain.model

enum class UserRole {
    ROOT_ADMIN,
    LIDER,
    MIEMBRO
}

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val referralCode: String,
    val leaderCode: String? = null,
    val registrationDate: Long = System.currentTimeMillis(),
    val isActiveInCampaign: Boolean = true
)
