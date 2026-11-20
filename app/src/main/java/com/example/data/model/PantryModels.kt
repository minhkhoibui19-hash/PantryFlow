package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class StorageLocation(val label: String) {
    FRIDGE("Ngăn mát"),
    FREEZER("Ngăn đông"),
    PANTRY("Tủ bếp"),
    OTHER("Khác")
}

enum class ItemStatus(val value: String) {
    ACTIVE("active"),
    USED("used"),
    DISCARDED("discarded")
}

enum class ExpiryUrgency {
    URGENT, // Expired or <= 1 day (Red)
    SOON,   // 2 - 3 days (Amber/Yellow)
    FRESH   // > 3 days (Green)
}

@Entity(tableName = "pantry_items")
data class PantryItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val quantity: Double,
    val unit: String,
    val storageLocation: StorageLocation,
    val purchaseDate: String,      // YYYY-MM-DD
    val expiryDate: String,        // YYYY-MM-DD
    val isExpiryEstimated: Boolean,
    val status: ItemStatus = ItemStatus.ACTIVE
)

enum class FoodVisualCategory {
    POULTRY_MEAT,
    VEGETABLE,
    DAIRY,
    EGGS,
    SEAFOOD,
    GRAIN_PANTRY,
    FRUIT,
    OTHER
}

fun classifyFoodCategory(name: String, location: StorageLocation): FoodVisualCategory {
    val n = name.lowercase()
    return when {
        listOf("gà", "thịt", "heo", "bò", "sườn", "vịt", "xúc xích").any { n.contains(it) } -> FoodVisualCategory.POULTRY_MEAT
        listOf("tôm", "cá", "mực", "cua", "nghêu", "sò", "hải sản").any { n.contains(it) } -> FoodVisualCategory.SEAFOOD
        listOf("trứng").any { n.contains(it) } -> FoodVisualCategory.EGGS
        listOf("sữa", "phô mai", "bơ", "yaourt", "kem").any { n.contains(it) } -> FoodVisualCategory.DAIRY
        listOf("rau", "cải", "xà lách", "cà chua", "cà rốt", "dưa", "nấm", "hành", "bông cải", "khoai").any { n.contains(it) } -> FoodVisualCategory.VEGETABLE
        listOf("táo", "cam", "chuối", "nho", "xoài", "ổi", "chanh", "trái cây", "dâu").any { n.contains(it) } -> FoodVisualCategory.FRUIT
        listOf("gạo", "mì", "bún", "bánh", "đậu", "yến mạch", "bột", "nước mắm", "dầu").any { n.contains(it) } || location == StorageLocation.PANTRY -> FoodVisualCategory.GRAIN_PANTRY
        else -> FoodVisualCategory.OTHER
    }
}

data class QuickPresetItem(
    val name: String,
    val defaultQuantity: Double,
    val defaultUnit: String,
    val defaultLocation: StorageLocation
)

val CommonVietnamesePresets = listOf(
    QuickPresetItem("Thịt ba rọi", 300.0, "g", StorageLocation.FRIDGE),
    QuickPresetItem("Rau muống", 1.0, "bó", StorageLocation.FRIDGE),
    QuickPresetItem("Cà chua bi", 250.0, "g", StorageLocation.FRIDGE),
    QuickPresetItem("Đậu hũ non", 2.0, "hộp", StorageLocation.FRIDGE),
    QuickPresetItem("Cá hồi phi lê", 300.0, "g", StorageLocation.FREEZER),
    QuickPresetItem("Sữa chua ít đường", 4.0, "hộp", StorageLocation.FRIDGE),
    QuickPresetItem("Bánh mì gối", 1.0, "bịch", StorageLocation.PANTRY),
    QuickPresetItem("Chuối chín", 1.0, "nải", StorageLocation.OTHER)
)

val CommonUnits = listOf(
    "g", "kg", "quả", "hộp", "bó", "bịch", "ml", "lít", "cái", "vỉ", "gói"
)

data class RecipeDefinition(
    val id: String,
    val title: String,
    val subtitle: String,
    val prepMinutes: Int,
    val difficulty: String,
    val requiredKeywords: List<Pair<String, List<String>>>, // Display ingredient -> matching keywords
    val pantryStaples: List<String>,
    val steps: List<String>
)

data class MatchedRecipe(
    val recipe: RecipeDefinition,
    val matchedIngredients: List<String>,
    val missingIngredients: List<String>,
    val rescuedUrgentItems: List<String>,
    val matchPercent: Int
)

object RecipeCatalog {
    val recipes = listOf(
        RecipeDefinition(
            id = "rec_ga_cai_ngot",
            title = "Ức gà áp chảo & Cải ngọt xào tỏi",
            subtitle = "Thanh đạm, giàu đạm, cực kỳ lý tưởng để dùng ngay ức gà và rau cải",
            prepMinutes = 20,
            difficulty = "Dễ",
            requiredKeywords = listOf(
                "Ức gà" to listOf("ức gà", "gà"),
                "Rau cải ngọt" to listOf("cải ngọt", "cải", "rau")
            ),
            pantryStaples = listOf("Tỏi băm", "Dầu ô liu hoặc dầu ăn", "Nước tương", "Tiêu đen"),
            steps = listOf(
                "Rửa sạch ức gà, thấm khô, ướp với 1 thìa nước tương, chút tiêu và tỏi băm trong 10 phút.",
                "Rửa sạch rau cải ngọt, cắt khúc vừa ăn khoảng 4–5 cm.",
                "Áp chảo ức gà với lửa vừa mỗi mặt 4–5 phút đến khi vàng đều và chín mọng.",
                "Dùng chảo khác phi thơm tỏi, xào nhanh rau cải ngọt trên lửa lớn 2 phút cho xanh giòn, nêm nhẹ rồi dọn cùng ức gà thái lát."
            )
        ),
        RecipeDefinition(
            id = "rec_canh_cai_tom",
            title = "Canh rau cải ngọt nấu tôm tươi",
            subtitle = "Món canh ngọt thanh chuẩn cơm nhà Việt, nấu nhanh trong 15 phút",
            prepMinutes = 15,
            difficulty = "Rất dễ",
            requiredKeywords = listOf(
                "Rau cải ngọt" to listOf("cải ngọt", "cải", "rau"),
                "Tôm sú" to listOf("tôm")
            ),
            pantryStaples = listOf("Gừng vài lát", "Hành tím", "Nước mắm", "Hạt nêm"),
            steps = listOf(
                "Rã đông tôm sú, bóc vỏ, rút chỉ lưng rồi băm sơ hoặc giã nhẹ với chút đầu hành.",
                "Phi thơm hành tím, xào săn tôm với nửa thìa nước mắm cho dậy mùi.",
                "Chế 700ml nước sôi vào nồi, thả thêm vài lát gừng đập dập.",
                "Nước sôi lại thì cho rau cải ngọt vào nấu thêm 2 phút, nêm nếm vừa miệng rồi tắt bếp."
            )
        ),
        RecipeDefinition(
            id = "rec_com_ga_trung",
            title = "Cơm ức gà xé sốt tương & Trứng ốp la",
            subtitle = "Bữa trưa đủ chất, tận dụng cơm nóng, ức gà và trứng gà ta",
            prepMinutes = 25,
            difficulty = "Dễ",
            requiredKeywords = listOf(
                "Ức gà" to listOf("ức gà", "gà"),
                "Trứng gà" to listOf("trứng"),
                "Gạo" to listOf("gạo", "cơm")
            ),
            pantryStaples = listOf("Nước tương", "Mật ong hoặc đường", "Hành lá"),
            steps = listOf(
                "Vo gạo ST25 và nấu chín dẻo thơm.",
                "Luộc hoặc áp chảo chín ức gà, để nguội bớt rồi xé sợi hoặc thái lát mỏng, đảo nhanh với sốt tương mật ong.",
                "Chiên trứng gà ta ốp la lòng đào viền giòn nhẹ.",
                "Múc cơm ra tô, xếp ức gà, trứng ốp la và rắc chút tiêu, hành lá lên trên."
            )
        ),
        RecipeDefinition(
            id = "rec_trung_hap_sua",
            title = "Trứng hấp sữa tươi mềm mịn",
            subtitle = "Bữa sáng béo nhẹ hoặc món tráng miệng bổ dưỡng chỉ với 2 nguyên liệu chính",
            prepMinutes = 15,
            difficulty = "Rất dễ",
            requiredKeywords = listOf(
                "Trứng gà" to listOf("trứng"),
                "Sữa tươi" to listOf("sữa tươi", "sữa")
            ),
            pantryStaples = listOf("Chút muối hoặc đường", "Vài giọt vani (nếu làm vị ngọt)"),
            steps = listOf(
                "Đánh nhẹ 2 quả trứng gà cùng 150ml sữa tươi không đường theo tỷ lệ 1:1.5, tránh tạo nhiều bọt.",
                "Lọc hỗn hợp qua rây mịn vào chén sứ chịu nhiệt.",
                "Bọc kín miệng chén bằng màng bọc chịu nhiệt hoặc đậy đĩa nhỏ để nước không rỏ xuống mặt trứng.",
                "Hấp lửa nhỏ 10–12 phút đến khi trứng đông mịn núng nính là dùng nóng ngay."
            )
        ),
        RecipeDefinition(
            id = "rec_tom_rim_com",
            title = "Tôm sú rim mặn ngọt ăn cơm nóng",
            subtitle = "Đậm đà đưa cơm cho bữa tối bận rộn cả gia đình",
            prepMinutes = 20,
            difficulty = "Dễ",
            requiredKeywords = listOf(
                "Tôm sú" to listOf("tôm"),
                "Gạo" to listOf("gạo", "cơm")
            ),
            pantryStaples = listOf("Nước mắm", "Đường", "Tỏi băm", "Tiêu"),
            steps = listOf(
                "Rã đông tôm, cắt râu và bỏ chỉ lưng, để thật ráo nước.",
                "Pha sốt gồm 1.5 thìa nước mắm, 1 thìa đường, 2 thìa nước lọc và chút tiêu.",
                "Phi thơm tỏi, cho tôm vào đảo lửa lớn đến khi vỏ chuyển đỏ cam.",
                "Đổ sốt vào rim lửa nhỏ đến khi nước sốt sánh bóng bám đều quanh con tôm, ăn kèm cơm nóng."
            )
        ),
        RecipeDefinition(
            id = "rec_trung_xao_cai",
            title = "Rau cải ngọt xào trứng nhanh gọn",
            subtitle = "Món xào 10 phút siêu tiết kiệm, giòn ngọt tự nhiên",
            prepMinutes = 10,
            difficulty = "Rất dễ",
            requiredKeywords = listOf(
                "Rau cải ngọt" to listOf("cải ngọt", "cải", "rau"),
                "Trứng gà" to listOf("trứng")
            ),
            pantryStaples = listOf("Tỏi", "Dầu ăn", "Dầu hào hoặc hạt nêm"),
            steps = listOf(
                "Đánh tan 2 quả trứng gà với chút hạt nêm và tiêu.",
                "Chiên sơ trứng thành miếng mềm rồi trút ra đĩa riêng.",
                "Phi thơm tỏi, xào rau cải ngọt lửa lớn cho vừa chín tới.",
                "Cho trứng vào đảo đều 30 giây cùng chút dầu hào rồi tắt bếp."
            )
        )
    )
}
