package com.salesnetwork.avon.app.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ContactActionHelper {

    fun openPhoneDialer(context: Context, phoneNumber: String) {
        val cleanPhone = phoneNumber.replace("[^0-9+]".toRegex(), "")
        if (cleanPhone.isEmpty()) {
            Toast.makeText(context, "Numero de telefono no valido", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanPhone")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir el marcador telefonico", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsAppChat(context: Context, phoneNumber: String, initialMessage: String = "") {
        val digitsOnly = phoneNumber.replace("[^0-9]".toRegex(), "")
        if (digitsOnly.isEmpty()) {
            Toast.makeText(context, "Numero de WhatsApp no valido", Toast.LENGTH_SHORT).show()
            return
        }
        val formattedPhone = if (!digitsOnly.startsWith("51") && digitsOnly.length == 9) {
            "51$digitsOnly"
        } else {
            digitsOnly
        }

        try {
            val encodedMsg = URLEncoder.encode(initialMessage, StandardCharsets.UTF_8.name())
            val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedMsg"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp no esta instalado o no se puede abrir", Toast.LENGTH_SHORT).show()
        }
    }

    fun openTurnByTurnNavigation(context: Context, address: String, lat: Double? = null, lng: Double? = null) {
        val hasCoords = lat != null && lng != null && lat != 0.0 && lng != 0.0
        val navUri = if (hasCoords) {
            Uri.parse("google.navigation:q=$lat,$lng&mode=d")
        } else if (address.isNotBlank()) {
            val encodedDest = Uri.encode(address)
            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$encodedDest&travelmode=driving")
        } else {
            Toast.makeText(context, "Direccion o ubicacion insuficiente para calcular ruta", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val mapsIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
                setPackage("com.google.android.apps.maps")
            }
            context.startActivity(mapsIntent)
        } catch (e: ActivityNotFoundException) {
            val destString = if (hasCoords) "$lat,$lng" else Uri.encode(address)
            val webDirUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destString&travelmode=driving")
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, webDirUri))
            } catch (ex: Exception) {
                Toast.makeText(context, "No se pudo abrir la navegacion de ruta", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo iniciar la navegacion de ruta", Toast.LENGTH_SHORT).show()
        }
    }
}
