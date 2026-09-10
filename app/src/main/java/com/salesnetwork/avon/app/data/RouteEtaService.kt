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
    val formattedSummary: String,
    val isRouteAvailable: Boolean = true
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
            return@withContext RouteEtaResult(0.0, 0, "Sin calcular", isRouteAvailable = false)
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

        // A straight-line estimate is intentionally not presented as a route.
        // The UI will show “Sin calcular” until a routing provider responds.
        RouteEtaResult(0.0, 0, "Sin calcular", isRouteAvailable = false)
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
        if (!eta.isRouteAvailable) {
            return customer.copy(estimatedMinutes = null, estimatedDistanceKm = null)
        }
        return customer.copy(
            estimatedMinutes = eta.durationMinutes,
            estimatedDistanceKm = eta.distanceKm
        )
    }
}
