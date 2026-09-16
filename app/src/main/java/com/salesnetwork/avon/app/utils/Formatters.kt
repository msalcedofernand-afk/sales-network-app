package com.salesnetwork.avon.app.utils

import java.util.Locale

/** Formats monetary amounts consistently across Android UI and messages. */
fun formatMoney(amount: Double): String = String.format(Locale.US, "%.2f", amount)
