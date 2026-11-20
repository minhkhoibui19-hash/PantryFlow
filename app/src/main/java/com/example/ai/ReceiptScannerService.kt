package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.data.model.StorageLocation
import com.example.data.util.DateUtils
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID
import kotlin.math.max

private const val TAG = "ReceiptScannerService"

data class ReceiptDraftItem(
    val draftId: String = UUID.randomUUID().toString(),
    val rawLabel: String = "",
    val name: String,
    val quantityText: String,
    val unit: String,
    val storageLocation: StorageLocation,
    val expiryDate: String,
    val isExpiryEstimated: Boolean = true
)

class ReceiptScannerService {

    private val receiptSchema = Schema.obj(
        properties = mapOf(
            "items" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "rawLabel" to Schema.string(
                            description = "Original raw text printed on the receipt line if available"
                        ),
                        "name" to Schema.string(
                            description = "Natural accented Vietnamese food name (e.g. 'Ức gà phi lê', 'Trứng gà', 'Rau cải ngọt')"
                        ),
                        "quantity" to Schema.double(
                            description = "Numeric quantity parsed from the receipt (e.g. 500, 10, 1, 0.5)"
                        ),
                        "unit" to Schema.enumeration(
                            values = listOf("g", "kg", "quả", "hộp", "bó", "bịch", "ml", "lít", "cái", "vỉ", "gói"),
                            description = "Measurement unit matching the item"
                        ),
                        "storageLocation" to Schema.enumeration(
                            values = listOf("Ngăn mát", "Ngăn đông", "Tủ bếp", "Khác"),
                            description = "Recommended home storage location in Vietnamese"
                        ),
                        "expiryDate" to Schema.string(
                            description = "Expiry date in YYYY-MM-DD format; prioritize date printed on receipt, otherwise estimate from today's purchase date"
                        ),
                        "isExpiryEstimated" to Schema.boolean(
                            description = "true if expiryDate was estimated by AI, false if an explicit expiry date was printed on the receipt"
                        )
                    ),
                    optionalProperties = listOf("rawLabel")
                ),
                description = "Filtered list of edible food and beverage grocery items only"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = receiptSchema
                temperature = 0.2f
            },
            systemInstruction = content {
                text(
                    """
                    Bạn là chuyên gia đọc và chuẩn hóa hóa đơn siêu thị Việt Nam (WinMart, Bách Hóa Xanh, Co.opmart, Lotte Mart, GO!/Big C, Mega Market, AEON...).
                    Nhiệm vụ của bạn khi phân tích ảnh hóa đơn:
                    1. CHỈ trích xuất các mặt hàng thực phẩm, đồ uống, gia vị ăn được.
                    2. TỰ ĐỘNG LOẠI BỎ hoàn toàn các dòng không phải thực phẩm: túi nilon, túi tự hủy, xà phòng, nước rửa chén, giấy ăn, dầu gội, khuyến mãi, giảm giá, chiết khấu, VAT, tổng tiền, tiền khách đưa, tiền thừa, điểm tích lũy.
                    3. TỰ ĐỘNG DỊCH tên viết tắt không dấu trên hóa đơn sang tên tiếng Việt có dấu tự nhiên, ngắn gọn, dễ hiểu và tách đúng số lượng + đơn vị:
                       - VD: "UC GA PHI LE 500G" -> name: "Ức gà phi lê", quantity: 500, unit: "g"
                       - VD: "TRUNG GA CP V10" -> name: "Trứng gà", quantity: 10, unit: "quả"
                       - VD: "RAU CAI NGOT 300G" -> name: "Rau cải ngọt", quantity: 300, unit: "g"
                       - VD: "SUA TUOI K/D TH 1L" -> name: "Sữa tươi không đường", quantity: 1, unit: "hộp"
                       - VD: "TOM SU DONG LANH 400G" -> name: "Tôm sú đông lạnh", quantity: 400, unit: "g"
                    4. Gợi ý storageLocation phù hợp nhất trong 4 giá trị: "Ngăn mát", "Ngăn đông", "Tủ bếp", "Khác".
                    5. Xác định expiryDate theo định dạng YYYY-MM-DD: nếu trên hóa đơn có in hạn sử dụng thì ưu tiên lấy ngày đó (isExpiryEstimated = false); nếu không có thì tự ước tính hợp lý dựa trên ngày mua là ngày hôm nay được cung cấp trong prompt (isExpiryEstimated = true).
                    6. Trả về đúng JSON schema, không thêm giải thích bên ngoài.
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun analyzeReceiptBitmap(bitmap: Bitmap): List<ReceiptDraftItem> {
        val todayIso = DateUtils.todayIso()
        val scaledBitmap = downscaleBitmapIfNeeded(bitmap, maxDimension = 1600)

        val response = withTimeout(60_000L) {
            model.generateContent(
                content {
                    image(scaledBitmap)
                    text(
                        "Ngày mua hôm nay là $todayIso. Hãy đọc hóa đơn này, lọc bỏ đồ phi thực phẩm, dịch tên viết tắt sang tiếng Việt có dấu và ước tính hạn sử dụng phù hợp."
                    )
                }
            )
        }

        val jsonText = response.text
        if (jsonText.isNullOrBlank()) {
            throw IllegalStateException("PantryFlow chưa đọc được món hàng nào. Vui lòng chụp lại ảnh hóa đơn rõ nét hơn.")
        }

        return parseReceiptJson(jsonText, todayIso)
    }

    private fun parseReceiptJson(jsonString: String, todayIso: String): List<ReceiptDraftItem> {
        val root = JSONObject(jsonString)
        val array = root.optJSONArray("items") ?: return emptyList()
        val parsed = mutableListOf<ReceiptDraftItem>()

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val name = obj.optString("name", "").trim()
            if (name.isEmpty()) continue

            val rawLabel = obj.optString("rawLabel", "").trim()
            val quantity = obj.optDouble("quantity", 1.0).takeIf { !it.isNaN() && it > 0.0 } ?: 1.0
            val unit = obj.optString("unit", "g").trim().ifEmpty { "g" }
            val locationLabel = obj.optString("storageLocation", "Ngăn mát").trim()
            val storageLocation = StorageLocation.entries.firstOrNull {
                it.label.equals(locationLabel, ignoreCase = true)
            } ?: DateUtils.suggestStorageLocation(name)

            val rawExpiry = obj.optString("expiryDate", "").trim()
            val expiryDate = if (rawExpiry.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
                rawExpiry
            } else {
                DateUtils.addDaysToIso(
                    todayIso,
                    DateUtils.estimateShelfLifeDays(name, storageLocation)
                )
            }
            val isEstimated = obj.optBoolean("isExpiryEstimated", true)

            parsed.add(
                ReceiptDraftItem(
                    rawLabel = rawLabel,
                    name = name,
                    quantityText = DateUtils.formatQuantity(quantity),
                    unit = unit,
                    storageLocation = storageLocation,
                    expiryDate = expiryDate,
                    isExpiryEstimated = isEstimated
                )
            )
        }
        return parsed
    }

    companion object {
        suspend fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? =
            withContext(Dispatchers.IO) {
                try {
                    val boundsOptions = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, boundsOptions)
                    }
                    val maxDim = max(boundsOptions.outWidth, boundsOptions.outHeight)
                    var sampleSize = 1
                    while (maxDim / sampleSize > 1800) {
                        sampleSize *= 2
                    }
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                    }
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, decodeOptions)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to decode receipt image URI", e)
                    null
                }
            }

        private fun downscaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
            val width = bitmap.width
            val height = bitmap.height
            val longest = max(width, height)
            if (longest <= maxDimension) return bitmap
            val ratio = maxDimension.toFloat() / longest.toFloat()
            val targetWidth = (width * ratio).toInt().coerceAtLeast(1)
            val targetHeight = (height * ratio).toInt().coerceAtLeast(1)
            return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        }

        /**
         * Realistic 5-item Vietnamese supermarket receipt sample for instant testing without taking a photo.
         */
        fun createSampleReceiptDrafts(): List<ReceiptDraftItem> {
            return listOf(
                ReceiptDraftItem(
                    rawLabel = "UC GA PHI LE 500G",
                    name = "Ức gà phi lê",
                    quantityText = "500",
                    unit = "g",
                    storageLocation = StorageLocation.FRIDGE,
                    expiryDate = DateUtils.plusDaysIso(2),
                    isExpiryEstimated = true
                ),
                ReceiptDraftItem(
                    rawLabel = "TRUNG GA CP V10",
                    name = "Trứng gà",
                    quantityText = "10",
                    unit = "quả",
                    storageLocation = StorageLocation.FRIDGE,
                    expiryDate = DateUtils.plusDaysIso(14),
                    isExpiryEstimated = true
                ),
                ReceiptDraftItem(
                    rawLabel = "CAI NGOT VIETGAP 300G",
                    name = "Rau cải ngọt",
                    quantityText = "300",
                    unit = "g",
                    storageLocation = StorageLocation.FRIDGE,
                    expiryDate = DateUtils.plusDaysIso(3),
                    isExpiryEstimated = true
                ),
                ReceiptDraftItem(
                    rawLabel = "SUA TUOI K/D TH 1L",
                    name = "Sữa tươi không đường",
                    quantityText = "1",
                    unit = "hộp",
                    storageLocation = StorageLocation.FRIDGE,
                    expiryDate = DateUtils.plusDaysIso(7),
                    isExpiryEstimated = false
                ),
                ReceiptDraftItem(
                    rawLabel = "TOM SU DONG LANH 400G",
                    name = "Tôm sú đông lạnh",
                    quantityText = "400",
                    unit = "g",
                    storageLocation = StorageLocation.FREEZER,
                    expiryDate = DateUtils.plusDaysIso(30),
                    isExpiryEstimated = true
                )
            )
        }
    }
}
