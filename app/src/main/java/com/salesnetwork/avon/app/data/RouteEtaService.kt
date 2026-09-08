package com.salesnetwork.avon.app.data

import com.salesnetwork.avon.app.domain.model.CustomerContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*

data class RouteEtaResult(
    val distanceKm: Double,
    val durationMinutes: Int,
    val formattedSummary: String
)

class RouteEtaService {

    suspend fun calculateRouteEta(
        originLat: Double,
        originLng: Double,
        destinationLat: Double?,
        destinationLng: Double?
    ): RouteEtaResult = withContext(Dispatchers.IO) {
        if (destinationLat == null || destinationLng == null ||
            originLat !in -90.0..90.0 || originLng !in -180.0..180.0 ||
            destinationLat !in -90.0..90.0 || destinationLng !in -180.0..180.0
        ) {
            return@withContext RouteEtaResult(0.0, 0, "Ruta sin ubicación confirmada")
        }

        try {
            val urlString = "https://router.project-osrm.org/route/v1/driving/$originLng,$originLat;$destinationLng,$destinationLat?overview=false"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.requestMethod = "GET"

            if (conn.responseCode == 200) {
                val stream = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(stream)
                val routes = json.getJSONArray("routes")
                if (routes.length() > 0) {
                    val primaryRoute = routes.getJSONObject(0)
                    val distanceMeters = primaryRoute.getDouble("distance")
                    val durationSeconds = primaryRoute.getDouble("duration")

                    val km = (distanceMeters / 1000.0 * 10).roundToInt() / 10.0
                    val mins = max(1, (durationSeconds / 60.0).roundToInt())

                    return@withContext RouteEtaResult(
                        distanceKm = km,
                        durationMinutes = mins,
                        formattedSummary = "A $mins min ($km km) por calles"
                    )
                }
            }
        } catch (e: Exception) {
            // Offline fallback
        }

        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(destinationLat - originLat)
        val dLng = Math.toRadians(destinationLng - originLng)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(originLat)) * cos(Math.toRadians(destinationLat)) *
                sin(dLng / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val straightDistance = earthRadiusKm * c
        val streetDistanceKm = (straightDistance * 1.4 * 10).roundToInt() / 10.0
        val minutes = max(2, (streetDistanceKm / 25.0 * 60).roundToInt())

        RouteEtaResult(
            distanceKm = streetDistanceKm,
            durationMinutes = minutes,
            formattedSummary = "A ~$minutes min ($streetDistanceKm km) por ruta de ciudad"
        )
    }

    suspend fun enrichCustomerWithEta(
        customer: CustomerContact,
        userLat: Double = -6.7714, // Centro de Chiclayo
        userLng: Double = -79.8409
    ): CustomerContact {
        val lat = customer.latitude
        val lng = customer.longitude
        if (lat == null || lng == null) {
            return customer.copy(
                estimatedMinutes = null,
                estimatedDistanceKm = null
            )
        }
        val eta = calculateRouteEta(userLat, userLng, lat, lng)
        return customer.copy(
            estimatedMinutes = eta.durationMinutes,
            estimatedDistanceKm = eta.distanceKm
        )
    }
}
