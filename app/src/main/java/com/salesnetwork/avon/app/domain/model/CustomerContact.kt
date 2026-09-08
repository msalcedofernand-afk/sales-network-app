package com.salesnetwork.avon.app.domain.model

data class CustomerContact(
    val id: String,
    val name: String,
    val phone: String,
    val whatsapp: String,
    val address: String,
    val city: String = "Chiclayo",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val notes: String = "",
    val addedByUserId: String = "",
    val estimatedMinutes: Int? = null,
    val estimatedDistanceKm: Double? = null
)
