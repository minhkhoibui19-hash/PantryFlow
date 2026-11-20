package com.example.ai

import android.util.Log
import com.example.data.model.PantryItem
import com.example.data.util.DateUtils
import com.example.data.util.UnitConverter
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.UnknownHostException
import java.util.Locale
import java.util.UUID
import kotlin.math.round

private const val TAG = "RecipeSuggestionService"

enum class CookTimeFilter(val label: String) {
    UNDER_15("< 15 phút"),
    UNDER_30("< 30 phút"),
    FLEXIBLE("Thoải mái")
}

enum class MealTypeFilter(val label: String) {
    BREAKFAST("Sáng"),
    LUNCH("Trưa"),
    DINNER("Tối"),
    SNACK("Ăn vặt")
}

data class AiRecipeIngredient(
    val pantryItemId: String?,
    val name: String,
    val amountUsed: Double,
    val unit: String,
    val isExpiringSoon: Boolean
)

data class AiRecipeSuggestion(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val cookTimeMinutes: Int,
    val difficulty: String,
    val mealType: String,
    val availableIngredients: List<AiRecipeIngredient>,
    val missingIngredients: List<String>,
    val steps: List<String>
)

class RecipeSuggestionService {

    /**
     * Clean, flat-friendly JSON schema without Unicode Schema.enumeration constraints,
     * keeping constrained decoding fast and reliable across all Gemini 3.x models.
     */
    private val recipesResponseSchema = Schema.obj(
        properties = mapOf(
            "recipes" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "title" to Schema.string(
                            description = "Ten mon an tieng Viet hap dan, gan gui, de nau tai nha"
                        ),
                        "description" to Schema.string(
                            description = "Mo ta ngan 1 cau ve huong vi va ly do mon nay phu hop"
                        ),
                        "cookTimeMinutes" to Schema.integer(
                            description = "Thoi gian che bien uoc tinh tinh bang phut"
                        ),
                        "difficulty" to Schema.string(
                            description = "Do kho: 'Rất dễ', 'Dễ', hoac 'Trung bình'"
                        ),
                        "mealType" to Schema.string(
                            description = "Bua an: 'Sáng', 'Trưa', 'Tối', hoac 'Ăn vặt'"
                        ),
                        "availableIngredients" to Schema.array(
                            items = Schema.obj(
                                properties = mapOf(
                                    "pantryItemId" to Schema.string(
                                        description = "ID chinh xac cua nguyen lieu tu danh sach tu do"
                                    ),
                                    "name" to Schema.string(
                                        description = "Ten nguyen lieu dang co trong tu do"
                                    ),
                                    "amountUsed" to Schema.double(
                                        description = "So luong uoc tinh can dung, khong vuot qua luong hien co"
                                    ),
                                    "unit" to Schema.string(
                                        description = "Don vi trung voi nguyen lieu trong tu"
                                    ),
                                    "isExpiringSoon" to Schema.boolean(
                                        description = "true neu nguyen lieu duoc danh dau sap het han"
                                    )
                                ),
                                optionalProperties = listOf("pantryItemId", "unit", "isExpiringSoon")
                            ),
                            description = "Danh sach nguyen lieu lay tu tu do dang co"
                        ),
                        "missingIngredients" to Schema.array(
                            items = Schema.string(),
                            description = "Gia vi co ban hoac toi da 1-2 nguyen lieu nho can them"
                        ),
                        "steps" to Schema.array(
                            items = Schema.string(),
                            description = "3 den 4 buoc nau an ngan gon, ro rang, de lam theo"
                        )
                    ),
                    optionalProperties = listOf(
                        "description",
                        "difficulty",
                        "mealType",
                        "missingIngredients"
                    )
                ),
                description = "Danh sach 3 den 4 mon an goi y phu hop nhat"
            )
        )
    )

    private val schemaModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = recipesResponseSchema
                temperature = 0.4f
                maxOutputTokens = 4096
            }
        )
    }

    private val jsonModeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                temperature = 0.4f
                maxOutputTokens = 4096
            }
        )
    }

    private val fastLiteFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                temperature = 0.4f
                maxOutputTokens = 4096
            }
        )
    }

    suspend fun suggestRecipes(
        selectedItems: List<PantryItem>,
        cookTime: CookTimeFilter,
        mealType: MealTypeFilter
    ): List<AiRecipeSuggestion> {
        if (selectedItems.isEmpty()) return emptyList()

        val ingredientsSummary = selectedItems.joinToString("\n") { item ->
            val days = DateUtils.daysUntil(item.expiryDate)
            val urgencyTag = if (days <= 3) {
                "[SẮP HẾT HẠN - ${DateUtils.formatDaysRemainingLabel(days)}]"
            } else {
                "[Còn tươi - ${DateUtils.formatDaysRemainingLabel(days)}]"
            }
            "- id=\"${item.id}\" | tên=\"${item.name}\" | hiện có=${UnitConverter.formatPreciseQuantity(item.quantity)} ${item.unit} | nơi=${item.storageLocation.label} | $urgencyTag"
        }

        val corePrompt = """
            Bạn là đầu bếp gia đình Việt Nam của ứng dụng PantryFlow.
            Hãy gợi ý 3 món ăn Việt Nam hoặc món ăn hàng ngày dễ làm từ danh sách nguyên liệu người dùng đã chọn:
            - Thời gian nấu mong muốn: ${cookTime.label}
            - Bữa ăn: ${mealType.label}
            - Nguyên liệu đã chọn trong tủ đồ:
            $ingredientsSummary

            Quy tắc bắt buộc:
            1. ƯU TIÊN TỐI ĐA giải cứu các nguyên liệu có nhãn [SẮP HẾT HẠN].
            2. Thời gian nấu (cookTimeMinutes) phù hợp tiêu chí "${cookTime.label}" và bữa "${mealType.label}".
            3. missingIngredients thật ngắn (ưu tiên gia vị cơ bản như tỏi, hành, nước mắm, tiêu, dầu ăn).
            4. Trong availableIngredients, giữ đúng pantryItemId và unit của nguyên liệu từ danh sách trên, và ước tính amountUsed hợp lý không vượt quá lượng hiện có.
            5. Các bước nấu (steps) gồm 3 đến 4 bước ngắn gọn, rõ ràng bằng tiếng Việt có dấu.
        """.trimIndent()

        val jsonStructurePrompt = """
            $corePrompt

            Trả về DUY NHẤT một JSON object hợp lệ (không có markdown, không có chữ bên ngoài) theo đúng cấu trúc:
            {
              "recipes": [
                {
                  "title": "Tên món ăn tiếng Việt",
                  "description": "Mô tả ngắn 1 câu",
                  "cookTimeMinutes": 15,
                  "difficulty": "Dễ",
                  "mealType": "${mealType.label}",
                  "availableIngredients": [
                    {
                      "pantryItemId": "id chính xác từ danh sách",
                      "name": "Tên nguyên liệu",
                      "amountUsed": 200,
                      "unit": "g",
                      "isExpiringSoon": true
                    }
                  ],
                  "missingIngredients": ["Tỏi băm", "Nước mắm"],
                  "steps": ["Bước 1...", "Bước 2...", "Bước 3..."]
                }
              ]
            }
        """.trimIndent()

        var lastError: Exception? = null

        // Tier 1: Structured output with responseSchema on gemini-3.8-flash
        try {
            val response = withTimeout(45_000L) {
                schemaModel.generateContent(corePrompt)
            }
            val parsed = response.text?.let { parseRecipesJson(it, selectedItems, mealType) }.orEmpty()
            if (parsed.isNotEmpty()) {
                return parsed
            }
            Log.w(TAG, "Tier 1 returned empty recipe list; trying JSON mode fallback.")
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Tier 1 timed out; falling back to fast model.", e)
            lastError = e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (isOfflineNetworkError(e)) throw e
            Log.w(TAG, "Tier 1 structured schema request failed: ${e.javaClass.simpleName}: ${e.message}", e)
            lastError = e
        }

        // Tier 2: JSON mode without strict responseSchema on gemini-3.8-flash (skip if Tier 1 timed out)
        if (lastError !is TimeoutCancellationException) {
            try {
                val response = withTimeout(45_000L) {
                    jsonModeModel.generateContent(jsonStructurePrompt)
                }
                val parsed = response.text?.let { parseRecipesJson(it, selectedItems, mealType) }.orEmpty()
                if (parsed.isNotEmpty()) {
                    return parsed
                }
                Log.w(TAG, "Tier 2 returned empty recipe list; trying fast lite model.")
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Tier 2 timed out; trying fast lite model.", e)
                lastError = e
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isOfflineNetworkError(e)) throw e
                Log.w(TAG, "Tier 2 JSON mode request failed: ${e.javaClass.simpleName}: ${e.message}", e)
                lastError = e
            }
        }

        // Tier 3: Ultra-fast billing-free Gemini 3.5 Flash-Lite fallback
        try {
            val response = withTimeout(45_000L) {
                fastLiteFallbackModel.generateContent(jsonStructurePrompt)
            }
            val parsed = response.text?.let { parseRecipesJson(it, selectedItems, mealType) }.orEmpty()
            if (parsed.isNotEmpty()) {
                return parsed
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Tier 3 fast lite fallback timed out.", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Tier 3 fast lite fallback failed: ${e.javaClass.simpleName}: ${e.message}", e)
            throw lastError ?: e
        }

        throw lastError ?: IllegalStateException("Empty recipe response")
    }

    private fun isOfflineNetworkError(e: Throwable): Boolean {
        var cur: Throwable? = e
        while (cur != null) {
            if (cur is UnknownHostException || cur is ConnectException || cur is NoRouteToHostException) {
                return true
            }
            val msg = cur.message?.lowercase(Locale.ROOT).orEmpty()
            if (cur is IOException && (msg.contains("unable to resolve host") || msg.contains("network is unreachable"))) {
                return true
            }
            cur = cur.cause
        }
        return false
    }

    companion object {

        internal fun parseRecipesJson(
            rawText: String,
            selectedItems: List<PantryItem>,
            defaultMealType: MealTypeFilter = MealTypeFilter.DINNER
        ): List<AiRecipeSuggestion> {
            val cleaned = extractCleanJsonPayload(rawText)
            if (cleaned.isEmpty()) return emptyList()

            val array: JSONArray = when {
                cleaned.startsWith("[") -> JSONArray(cleaned)
                else -> {
                    val root = JSONObject(cleaned)
                    root.optJSONArray("recipes")
                        ?: root.optJSONArray("dishes")
                        ?: root.optJSONArray("suggestions")
                        ?: root.optJSONArray("data")
                        ?: return emptyList()
                }
            }

            val itemById = selectedItems.associateBy { it.id }
            val result = mutableListOf<AiRecipeSuggestion>()

            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val title = obj.optString("title", "")
                    .ifBlank { obj.optString("name", "") }
                    .ifBlank { obj.optString("recipeName", "") }
                    .trim()
                if (title.isEmpty()) continue

                val description = obj.optString("description", "").trim()
                val cookTime = optFlexibleInt(obj, "cookTimeMinutes", 20).coerceIn(5, 180)
                val difficulty = normalizeDifficulty(obj.optString("difficulty", "Dễ"))
                val meal = normalizeMealType(obj.optString("mealType", defaultMealType.label), defaultMealType)

                val availArray = obj.optJSONArray("availableIngredients")
                    ?: obj.optJSONArray("ingredients")
                val available = mutableListOf<AiRecipeIngredient>()
                val seenPantryIds = mutableSetOf<String>()

                if (availArray != null) {
                    for (j in 0 until availArray.length()) {
                        val ingObj = availArray.optJSONObject(j)
                        if (ingObj != null) {
                            val ingName = ingObj.optString("name", "").trim()
                            if (ingName.isEmpty()) continue

                            val rawId = ingObj.optString("pantryItemId", "")
                                .trim()
                                .takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
                            val matchedPantry = rawId?.let { itemById[it] }
                                ?: findBestMatchingPantryItem(ingName, selectedItems)

                            if (matchedPantry != null && !seenPantryIds.add(matchedPantry.id)) {
                                continue
                            }

                            val rawUnit = ingObj.optString("unit", "")
                                .trim()
                                .takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
                                ?: matchedPantry?.unit
                                ?: "g"

                            val rawAmount = optFlexibleDouble(
                                ingObj,
                                "amountUsed",
                                matchedPantry?.quantity ?: 1.0
                            )

                            val finalUnit = matchedPantry?.unit ?: rawUnit
                            val finalAmount = if (matchedPantry != null) {
                                val converted = UnitConverter.convert(rawAmount, rawUnit, matchedPantry.unit)
                                val safe = if (converted.isNaN() || converted <= 0.0) {
                                    matchedPantry.quantity
                                } else {
                                    converted
                                }
                                val clamped = safe.coerceIn(minOf(0.1, matchedPantry.quantity), matchedPantry.quantity)
                                round(clamped * 100.0) / 100.0
                            } else {
                                round(rawAmount.coerceAtLeast(0.1) * 100.0) / 100.0
                            }

                            val isExpiring = if (matchedPantry != null) {
                                DateUtils.daysUntil(matchedPantry.expiryDate) <= 3
                            } else {
                                ingObj.optBoolean("isExpiringSoon", false)
                            }

                            available.add(
                                AiRecipeIngredient(
                                    pantryItemId = matchedPantry?.id,
                                    name = matchedPantry?.name ?: ingName,
                                    amountUsed = finalAmount,
                                    unit = finalUnit,
                                    isExpiringSoon = isExpiring
                                )
                            )
                        } else {
                            // Tolerate plain string entries in fallback mode
                            val ingText = availArray.optString(j, "").trim()
                            if (ingText.isEmpty()) continue
                            val matchedPantry = findBestMatchingPantryItem(ingText, selectedItems)
                            if (matchedPantry != null && !seenPantryIds.add(matchedPantry.id)) {
                                continue
                            }
                            available.add(
                                AiRecipeIngredient(
                                    pantryItemId = matchedPantry?.id,
                                    name = matchedPantry?.name ?: ingText,
                                    amountUsed = matchedPantry?.quantity ?: 1.0,
                                    unit = matchedPantry?.unit ?: "phần",
                                    isExpiringSoon = matchedPantry?.let { DateUtils.daysUntil(it.expiryDate) <= 3 } ?: false
                                )
                            )
                        }
                    }
                }

                // If the model returned no mapped ingredients, link the most urgent selected item
                if (available.isEmpty() && selectedItems.isNotEmpty()) {
                    val primary = selectedItems.minByOrNull { DateUtils.daysUntil(it.expiryDate) } ?: selectedItems.first()
                    available.add(
                        AiRecipeIngredient(
                            pantryItemId = primary.id,
                            name = primary.name,
                            amountUsed = primary.quantity,
                            unit = primary.unit,
                            isExpiringSoon = DateUtils.daysUntil(primary.expiryDate) <= 3
                        )
                    )
                }

                val missingArray = obj.optJSONArray("missingIngredients")
                val missing = mutableListOf<String>()
                if (missingArray != null) {
                    for (j in 0 until missingArray.length()) {
                        val m = missingArray.optString(j, "").trim()
                        if (m.isNotEmpty() && !m.equals("null", ignoreCase = true)) {
                            missing.add(m)
                        }
                    }
                }

                val stepsArray = obj.optJSONArray("steps")
                val steps = mutableListOf<String>()
                if (stepsArray != null) {
                    for (j in 0 until stepsArray.length()) {
                        val s = stepsArray.optString(j, "").trim()
                        if (s.isNotEmpty() && !s.equals("null", ignoreCase = true)) {
                            steps.add(s)
                        }
                    }
                }
                if (steps.isEmpty()) {
                    steps.add("Sơ chế sạch các nguyên liệu đã chuẩn bị và để ráo nước.")
                    steps.add("Chế biến trên lửa vừa cùng gia vị cơ bản cho đến khi chín đều, nêm nếm vừa miệng rồi thưởng thức nóng.")
                }

                result.add(
                    AiRecipeSuggestion(
                        title = title,
                        description = description,
                        cookTimeMinutes = cookTime,
                        difficulty = difficulty,
                        mealType = meal,
                        availableIngredients = available,
                        missingIngredients = missing,
                        steps = steps
                    )
                )
            }
            return result
        }

        private fun extractCleanJsonPayload(raw: String): String {
            val trimmed = raw.trim()
                .removePrefix("\uFEFF")
                .removePrefix("```json")
                .removePrefix("```JSON")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            if (trimmed.isEmpty()) return ""
            if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]"))
            ) {
                return trimmed
            }

            val firstBrace = trimmed.indexOf('{')
            val lastBrace = trimmed.lastIndexOf('}')
            val firstBracket = trimmed.indexOf('[')
            val lastBracket = trimmed.lastIndexOf(']')

            return when {
                firstBrace >= 0 && lastBrace > firstBrace && (firstBracket < 0 || firstBrace < firstBracket) ->
                    trimmed.substring(firstBrace, lastBrace + 1)
                firstBracket >= 0 && lastBracket > firstBracket ->
                    trimmed.substring(firstBracket, lastBracket + 1)
                else -> trimmed
            }
        }

        private fun findBestMatchingPantryItem(name: String, items: List<PantryItem>): PantryItem? {
            val lower = name.trim().lowercase(Locale.ROOT)
            if (lower.isEmpty()) return null
            return items.firstOrNull { it.name.trim().equals(name.trim(), ignoreCase = true) }
                ?: items.firstOrNull {
                    val itemLower = it.name.trim().lowercase(Locale.ROOT)
                    itemLower.contains(lower) || lower.contains(itemLower)
                }
        }

        private fun optFlexibleDouble(obj: JSONObject, key: String, fallback: Double): Double {
            if (!obj.has(key) || obj.isNull(key)) return fallback
            val raw = obj.opt(key)
            return when (raw) {
                is Number -> raw.toDouble().takeIf { !it.isNaN() && !it.isInfinite() && it > 0.0 } ?: fallback
                is String -> {
                    val cleaned = raw.trim().replace(",", ".")
                    val numberMatch = Regex("""\d+(\.\d+)?""").find(cleaned)?.value
                    numberMatch?.toDoubleOrNull()?.takeIf { it > 0.0 } ?: fallback
                }
                else -> fallback
            }
        }

        private fun optFlexibleInt(obj: JSONObject, key: String, fallback: Int): Int {
            if (!obj.has(key) || obj.isNull(key)) return fallback
            val raw = obj.opt(key)
            return when (raw) {
                is Number -> raw.toInt().takeIf { it > 0 } ?: fallback
                is String -> Regex("""\d+""").find(raw)?.value?.toIntOrNull()?.takeIf { it > 0 } ?: fallback
                else -> fallback
            }
        }

        private fun normalizeDifficulty(raw: String): String {
            val lower = raw.trim().lowercase(Locale.ROOT)
            return when {
                lower.contains("rất dễ") || lower.contains("rat de") || lower.contains("very easy") -> "Rất dễ"
                lower.contains("trung") || lower.contains("medium") -> "Trung bình"
                else -> "Dễ"
            }
        }

        private fun normalizeMealType(raw: String, fallback: MealTypeFilter): String {
            val lower = raw.trim().lowercase(Locale.ROOT)
            return when {
                lower.contains("sáng") || lower.contains("sang") || lower.contains("breakfast") -> MealTypeFilter.BREAKFAST.label
                lower.contains("trưa") || lower.contains("trua") || lower.contains("lunch") -> MealTypeFilter.LUNCH.label
                lower.contains("tối") || lower.contains("toi") || lower.contains("dinner") -> MealTypeFilter.DINNER.label
                lower.contains("vặt") || lower.contains("vat") || lower.contains("snack") -> MealTypeFilter.SNACK.label
                else -> fallback.label
            }
        }

        /**
         * Builds realistic 4-recipe sample suggestions dynamically linked to the user's real pantry items
         * so the user can preview the full UI & test the "Tôi đã nấu món này" stock deduction immediately.
         */
        fun createSampleSuggestions(
            allActiveItems: List<PantryItem>,
            mealType: MealTypeFilter
        ): List<AiRecipeSuggestion> {
            fun findItem(vararg keywords: String): PantryItem? =
                allActiveItems.firstOrNull { item ->
                    keywords.any { kw -> item.name.lowercase(Locale.ROOT).contains(kw) }
                }

            fun toIngredient(
                item: PantryItem?,
                fallbackName: String,
                suggestedAmount: Double,
                fallbackUnit: String
            ): AiRecipeIngredient {
                return if (item != null) {
                    val converted = UnitConverter.convert(suggestedAmount, fallbackUnit, item.unit)
                    val safeAmt = if (item.quantity <= 0.0) {
                        suggestedAmount
                    } else {
                        val clamped = converted.coerceIn(minOf(0.1, item.quantity), item.quantity)
                        round(clamped * 100.0) / 100.0
                    }
                    AiRecipeIngredient(
                        pantryItemId = item.id,
                        name = item.name,
                        amountUsed = safeAmt,
                        unit = item.unit,
                        isExpiringSoon = DateUtils.daysUntil(item.expiryDate) <= 3
                    )
                } else {
                    AiRecipeIngredient(
                        pantryItemId = null,
                        name = fallbackName,
                        amountUsed = suggestedAmount,
                        unit = fallbackUnit,
                        isExpiringSoon = true
                    )
                }
            }

            val chicken = findItem("ức gà", "gà")
            val greens = findItem("cải ngọt", "cải", "rau")
            val milk = findItem("sữa tươi", "sữa")
            val eggs = findItem("trứng")
            val shrimp = findItem("tôm")
            val rice = findItem("gạo")

            return listOf(
                AiRecipeSuggestion(
                    id = "sample_recipe_1",
                    title = "Ức gà áp chảo sốt tiêu & Cải ngọt xào tỏi",
                    description = "Giải cứu ngay ức gà và rau cải ngọt trong 15 phút, đậm vị và lành mạnh.",
                    cookTimeMinutes = 15,
                    difficulty = "Dễ",
                    mealType = mealType.label,
                    availableIngredients = listOf(
                        toIngredient(chicken, "Ức gà phi lê", 250.0, "g"),
                        toIngredient(greens, "Rau cải ngọt", 1.0, "bó")
                    ),
                    missingIngredients = listOf("Tỏi băm", "Nước tương", "Tiêu đen"),
                    steps = listOf(
                        "Rửa sạch ức gà, thấm khô, cắt lát vừa ăn rồi ướp với 1 thìa nước tương, chút tiêu và tỏi băm trong 5 phút.",
                        "Rửa rau cải ngọt, cắt khúc khoảng 4–5 cm và để ráo nước.",
                        "Áp chảo ức gà với chút dầu trên lửa vừa khoảng 4 phút mỗi mặt đến khi vàng xém thơm lừng.",
                        "Dùng chảo nóng phi thơm tỏi, xào nhanh cải ngọt 2 phút trên lửa lớn cho xanh giòn rồi bày ra đĩa cùng ức gà."
                    )
                ),
                AiRecipeSuggestion(
                    id = "sample_recipe_2",
                    title = "Trứng đúc sữa tươi mềm béo",
                    description = "Món ăn siêu nhanh dùng hết sữa tươi sắp hết hạn và trứng gà ta đang có sẵn.",
                    cookTimeMinutes = 10,
                    difficulty = "Rất dễ",
                    mealType = "Sáng",
                    availableIngredients = listOf(
                        toIngredient(milk, "Sữa tươi không đường", 0.5, "hộp"),
                        toIngredient(eggs, "Trứng gà ta", 3.0, "quả")
                    ),
                    missingIngredients = listOf("Chút muối", "Hành lá hoặc tiêu"),
                    steps = listOf(
                        "Đánh tan 3 quả trứng gà với 100ml sữa tươi không đường và 1/4 thìa cà phê muối.",
                        "Làm nóng chảo chống dính với lửa nhỏ, phết lớp dầu hoặc bơ mỏng.",
                        "Đổ hỗn hợp trứng sữa vào, đậy nắp nấu lửa nhỏ 4–5 phút đến khi mặt trứng se mềm, béo mịn.",
                        "Rắc thêm chút tiêu xay, hành lá và thưởng thức nóng."
                    )
                ),
                AiRecipeSuggestion(
                    id = "sample_recipe_3",
                    title = "Canh rau cải ngọt nấu tôm tươi",
                    description = "Tô canh ngọt thanh giải nhiệt, tận dụng trọn bó cải ngọt và tôm trong tủ.",
                    cookTimeMinutes = 15,
                    difficulty = "Rất dễ",
                    mealType = "Trưa",
                    availableIngredients = listOf(
                        toIngredient(greens, "Rau cải ngọt", 1.0, "bó"),
                        toIngredient(shrimp, "Tôm sú đông lạnh", 150.0, "g")
                    ),
                    missingIngredients = listOf("Gừng vài lát", "Hành tím", "Nước mắm"),
                    steps = listOf(
                        "Rã đông tôm, bóc vỏ, rút chỉ lưng và băm sơ với chút tiêu, nước mắm.",
                        "Phi thơm hành tím, xào săn tôm rồi đổ khoảng 700ml nước sôi cùng vài lát gừng đập dập.",
                        "Khi nước sôi bùng, thả rau cải ngọt vào nấu thêm 2 phút rồi nêm vừa ăn và tắt bếp."
                    )
                ),
                AiRecipeSuggestion(
                    id = "sample_recipe_4",
                    title = "Cơm chiên ức gà trứng vàng ươm",
                    description = "Bữa tối no bụng chuẩn vị quán, kết hợp ức gà, trứng và gạo ST25.",
                    cookTimeMinutes = 25,
                    difficulty = "Dễ",
                    mealType = "Tối",
                    availableIngredients = listOf(
                        toIngredient(chicken, "Ức gà phi lê", 200.0, "g"),
                        toIngredient(eggs, "Trứng gà ta", 2.0, "quả"),
                        toIngredient(rice, "Gạo ST25", 0.3, "kg")
                    ),
                    missingIngredients = listOf("Hành tím", "Nước mắm", "Hành lá"),
                    steps = listOf(
                        "Thái hạt lựu ức gà, ướp nhẹ với chút nước mắm và tiêu rồi xào chín tới.",
                        "Trộn cơm nguội (nấu từ gạo ST25) với 1 lòng đỏ trứng cho hạt cơm tơi vàng đẹp mắt.",
                        "Phi thơm hành tím, đổ cơm vào đảo lửa lớn đến khi hạt cơm săn nhẹ, thêm phần trứng còn lại và ức gà xào.",
                        "Nêm nếm vừa miệng, đảo thêm 1 phút với hành lá cắt nhỏ rồi múc ra đĩa."
                    )
                )
            )
        }
    }
}
