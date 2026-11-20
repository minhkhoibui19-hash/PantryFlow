package com.example.data.util

import kotlin.math.round

object UnitConverter {
    /**
     * Returns compatible units that can be automatically converted to/from [baseUnit].
     * Example: "g" -> ["g", "kg"]; "kg" -> ["kg", "g"]; "ml" -> ["ml", "lít"]; "quả" -> ["quả"].
     */
    fun compatibleUnitsFor(baseUnit: String): List<String> {
        return when (baseUnit.trim().lowercase()) {
            "g" -> listOf("g", "kg")
            "kg" -> listOf("kg", "g")
            "ml" -> listOf("ml", "lít")
            "lít", "l" -> listOf("lít", "ml")
            else -> listOf(baseUnit)
        }
    }

    /**
     * Converts [amount] expressed in [fromUnit] into the equivalent quantity in [toUnit].
     */
    fun convert(amount: Double, fromUnit: String, toUnit: String): Double {
        val from = fromUnit.trim().lowercase()
        val to = toUnit.trim().lowercase()
        if (from == to) return amount

        return when {
            from == "g" && to == "kg" -> amount / 1000.0
            from == "kg" && to == "g" -> amount * 1000.0
            from == "ml" && (to == "lít" || to == "l") -> amount / 1000.0
            (from == "lít" || from == "l") && to == "ml" -> amount * 1000.0
            else -> amount
        }
    }

    /**
     * Calculates remaining quantity in [itemUnit] after deducting [usedAmount] in [usedUnit].
     * Returns 0.0 if fully consumed.
     */
    fun calculateRemaining(
        currentQuantity: Double,
        itemUnit: String,
        usedAmount: Double,
        usedUnit: String
    ): Double {
        val deductedInItemUnit = convert(usedAmount, usedUnit, itemUnit)
        val rawRemaining = (currentQuantity - deductedInItemUnit).coerceAtLeast(0.0)
        // Round cleanly to 2 decimal places to avoid floating point artifacts like 0.30000000000000004
        return round(rawRemaining * 100.0) / 100.0
    }

    fun formatPreciseQuantity(quantity: Double): String {
        val rounded = round(quantity * 100.0) / 100.0
        return when {
            rounded % 1.0 == 0.0 -> rounded.toLong().toString()
            (rounded * 10.0) % 1.0 == 0.0 -> String.format(java.util.Locale.US, "%.1f", rounded)
            else -> String.format(java.util.Locale.US, "%.2f", rounded)
        }
    }
}
