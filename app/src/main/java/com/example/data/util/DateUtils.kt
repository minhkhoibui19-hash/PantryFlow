package com.example.data.util

import com.example.data.model.ExpiryUrgency
import com.example.data.model.StorageLocation
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object DateUtils {
    private const val ISO_PATTERN = "yyyy-MM-dd"
    private const val DISPLAY_PATTERN = "dd/MM/yyyy"

    private fun isoFormat(): SimpleDateFormat =
        SimpleDateFormat(ISO_PATTERN, Locale.US).apply {
            isLenient = false
        }

    private fun displayFormat(): SimpleDateFormat =
        SimpleDateFormat(DISPLAY_PATTERN, Locale.forLanguageTag("vi-VN"))

    private fun startOfTodayMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun todayIso(): String {
        return isoFormat().format(Calendar.getInstance().time)
    }

    fun plusDaysIso(daysOffset: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, daysOffset)
        return isoFormat().format(cal.time)
    }

    fun addDaysToIso(baseIso: String, daysOffset: Int): String {
        val parsed = runCatching { isoFormat().parse(baseIso) }.getOrNull()
        val cal = Calendar.getInstance()
        if (parsed != null) {
            cal.time = parsed
        }
        cal.add(Calendar.DAY_OF_YEAR, daysOffset)
        return isoFormat().format(cal.time)
    }

    fun isoToUtcMillis(iso: String): Long {
        val parsed = runCatching { isoFormat().parse(iso) }.getOrNull()
        val localCal = Calendar.getInstance()
        if (parsed != null) {
            localCal.time = parsed
        }
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utcCal.clear()
        utcCal.set(
            localCal.get(Calendar.YEAR),
            localCal.get(Calendar.MONTH),
            localCal.get(Calendar.DAY_OF_MONTH)
        )
        return utcCal.timeInMillis
    }

    fun utcMillisToIso(utcMillis: Long): String {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        val localCal = Calendar.getInstance().apply {
            clear()
            set(
                utcCal.get(Calendar.YEAR),
                utcCal.get(Calendar.MONTH),
                utcCal.get(Calendar.DAY_OF_MONTH)
            )
        }
        return isoFormat().format(localCal.time)
    }

    fun daysUntil(expiryIso: String): Int {
        val parsed = runCatching { isoFormat().parse(expiryIso) }.getOrNull() ?: return 0
        val targetCal = Calendar.getInstance().apply {
            time = parsed
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMs = targetCal.timeInMillis - startOfTodayMillis()
        return (diffMs.toDouble() / TimeUnit.DAYS.toMillis(1).toDouble()).roundToInt()
    }

    fun formatDisplayDate(iso: String): String {
        val parsed = runCatching { isoFormat().parse(iso) }.getOrNull() ?: return iso
        return displayFormat().format(parsed)
    }

    fun urgencyOf(expiryIso: String, soonThresholdDays: Int = 3): ExpiryUrgency {
        val days = daysUntil(expiryIso)
        return when {
            days <= 1 -> ExpiryUrgency.URGENT
            days <= soonThresholdDays.coerceAtLeast(2) -> ExpiryUrgency.SOON
            else -> ExpiryUrgency.FRESH
        }
    }

    fun formatDaysRemainingLabel(days: Int): String {
        return when {
            days < 0 -> "Quá hạn ${-days} ngày"
            days == 0 -> "Hết hạn hôm nay"
            days == 1 -> "Còn 1 ngày"
            else -> "Còn $days ngày"
        }
    }

    fun formatQuantity(quantity: Double): String {
        return if (quantity % 1.0 == 0.0) {
            quantity.toLong().toString()
        } else {
            String.format(Locale.US, "%.1f", quantity)
        }
    }

    /**
     * Heuristic shelf-life estimator based on common Vietnamese food names and storage location.
     * Returns estimated days from purchase date.
     */
    fun estimateShelfLifeDays(name: String, location: StorageLocation): Int {
        val normalized = name.trim().lowercase(Locale.ROOT)
        return when (location) {
            StorageLocation.FREEZER -> when {
                containsAny(normalized, "cá", "tôm", "mực", "hải sản") -> 30
                containsAny(normalized, "gà", "thịt", "heo", "bò", "sườn") -> 60
                containsAny(normalized, "kem", "chả", "xúc xích", "há cảo") -> 45
                else -> 30
            }
            StorageLocation.FRIDGE -> when {
                containsAny(normalized, "gà", "thịt", "heo", "bò", "cá", "tôm", "mực") -> 2
                containsAny(normalized, "rau", "cải", "xà lách", "hành", "ngò", "nấm", "giá") -> 3
                containsAny(normalized, "sữa", "yaourt", "sữa chua", "phô mai", "đậu hũ", "đậu phụ") -> 5
                containsAny(normalized, "cà chua", "dưa leo", "cà rốt", "bông cải", "ớt", "chanh") -> 7
                containsAny(normalized, "trứng") -> 14
                containsAny(normalized, "trái cây", "táo", "cam", "nho", "chuối", "bơ", "xoài") -> 5
                else -> 5
            }
            StorageLocation.PANTRY -> when {
                containsAny(normalized, "bánh mì", "bánh bao") -> 3
                containsAny(normalized, "khoai", "hành tây", "tỏi", "bí đỏ") -> 14
                containsAny(normalized, "gạo", "mì", "bún khô", "miến", "yến mạch", "đậu") -> 90
                containsAny(normalized, "nước mắm", "dầu ăn", "muối", "đường", "xì dầu", "tương") -> 180
                else -> 30
            }
            StorageLocation.OTHER -> 7
        }
    }

    fun suggestStorageLocation(name: String): StorageLocation {
        val normalized = name.trim().lowercase(Locale.ROOT)
        return when {
            normalized.isEmpty() -> StorageLocation.FRIDGE
            containsAny(normalized, "đông lạnh", "kem", "há cảo", "viên thả lẩu", "cá hồi đông") -> StorageLocation.FREEZER
            containsAny(normalized, "gạo", "mì", "bún khô", "miến", "bánh mì", "dầu ăn", "nước mắm", "đường", "muối", "yến mạch", "hành tây", "tỏi", "khoai") -> StorageLocation.PANTRY
            containsAny(normalized, "gà", "thịt", "heo", "bò", "cá", "tôm", "mực", "rau", "cải", "trứng", "sữa", "phô mai", "đậu hũ", "nấm", "cà chua", "dưa", "trái cây", "táo", "cam") -> StorageLocation.FRIDGE
            else -> StorageLocation.FRIDGE
        }
    }

    fun suggestUnitAndQuantity(name: String): Pair<String, Double> {
        val normalized = name.trim().lowercase(Locale.ROOT)
        return when {
            containsAny(normalized, "thịt", "gà", "heo", "bò", "tôm", "cá", "mực") -> "g" to 500.0
            containsAny(normalized, "rau", "cải", "hành lá", "ngò") -> "bó" to 1.0
            containsAny(normalized, "trứng") -> "quả" to 10.0
            containsAny(normalized, "sữa tươi", "sữa chua", "đậu hũ", "phô mai") -> "hộp" to 1.0
            containsAny(normalized, "sữa bịch", "bánh mì", "bịch") -> "bịch" to 1.0
            containsAny(normalized, "gạo", "khoai", "táo", "cam") -> "kg" to 1.0
            containsAny(normalized, "nước", "dầu", "mật ong") -> "ml" to 500.0
            containsAny(normalized, "cà chua", "dưa leo", "cà rốt", "chanh") -> "g" to 300.0
            else -> "g" to 250.0
        }
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it) }
    }
}
