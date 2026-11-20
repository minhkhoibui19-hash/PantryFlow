package com.example.ai

import com.example.data.model.ItemStatus
import com.example.data.model.PantryItem
import com.example.data.model.StorageLocation
import com.example.data.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RecipeSuggestionServiceTest {

    private fun samplePantry(): List<PantryItem> = listOf(
        PantryItem(
            id = "item_chicken",
            name = "Ức gà phi lê",
            quantity = 0.5,
            unit = "kg",
            storageLocation = StorageLocation.FRIDGE,
            purchaseDate = DateUtils.todayIso(),
            expiryDate = DateUtils.plusDaysIso(1),
            isExpiryEstimated = true,
            status = ItemStatus.ACTIVE
        ),
        PantryItem(
            id = "item_greens",
            name = "Rau cải ngọt",
            quantity = 1.0,
            unit = "bó",
            storageLocation = StorageLocation.FRIDGE,
            purchaseDate = DateUtils.todayIso(),
            expiryDate = DateUtils.plusDaysIso(2),
            isExpiryEstimated = true,
            status = ItemStatus.ACTIVE
        ),
        PantryItem(
            id = "item_rice",
            name = "Gạo ST25",
            quantity = 0.2,
            unit = "kg",
            storageLocation = StorageLocation.PANTRY,
            purchaseDate = DateUtils.todayIso(),
            expiryDate = DateUtils.plusDaysIso(30),
            isExpiryEstimated = true,
            status = ItemStatus.ACTIVE
        )
    )

    @Test
    fun `parseRecipesJson converts units and clamps to pantry quantity`() {
        val pantry = samplePantry()
        val json = """
            {
              "recipes": [
                {
                  "title": "Ức gà xào cải ngọt",
                  "description": "Món xào nhanh gọn giải cứu ức gà.",
                  "cookTimeMinutes": 15,
                  "difficulty": "Dễ",
                  "mealType": "Tối",
                  "availableIngredients": [
                    {
                      "pantryItemId": "item_chicken",
                      "name": "Ức gà phi lê",
                      "amountUsed": 250,
                      "unit": "g",
                      "isExpiringSoon": true
                    },
                    {
                      "pantryItemId": "item_greens",
                      "name": "Rau cải ngọt",
                      "amountUsed": 3,
                      "unit": "bó",
                      "isExpiringSoon": true
                    }
                  ],
                  "missingIngredients": ["Tỏi băm", "Dầu hào"],
                  "steps": ["Thái lát ức gà", "Xào nhanh với cải ngọt"]
                }
              ]
            }
        """.trimIndent()

        val recipes = RecipeSuggestionService.parseRecipesJson(json, pantry, MealTypeFilter.DINNER)
        assertEquals(1, recipes.size)
        val recipe = recipes.first()
        assertEquals("Ức gà xào cải ngọt", recipe.title)
        assertEquals(2, recipe.availableIngredients.size)

        val chickenIng = recipe.availableIngredients[0]
        assertEquals("item_chicken", chickenIng.pantryItemId)
        assertEquals("kg", chickenIng.unit)
        assertEquals(0.25, chickenIng.amountUsed, 0.001)
        assertTrue(chickenIng.isExpiringSoon)

        val greensIng = recipe.availableIngredients[1]
        assertEquals("item_greens", greensIng.pantryItemId)
        assertEquals(1.0, greensIng.amountUsed, 0.001)
    }

    @Test
    fun `parseRecipesJson handles markdown code fences, string numbers, and top-level array`() {
        val pantry = samplePantry()
        val raw = """
            ```json
            [
              {
                "title": "Cơm gà áp chảo",
                "cookTimeMinutes": "20 phút",
                "availableIngredients": [
                  {
                    "name": "Ức gà phi lê",
                    "amountUsed": "300g",
                    "unit": "g"
                  }
                ],
                "steps": []
              }
            ]
            ```
        """.trimIndent()

        val recipes = RecipeSuggestionService.parseRecipesJson(raw, pantry, MealTypeFilter.LUNCH)
        assertEquals(1, recipes.size)
        val recipe = recipes.first()
        assertEquals(20, recipe.cookTimeMinutes)
        assertEquals("Trưa", recipe.mealType)
        assertEquals(0.3, recipe.availableIngredients.first().amountUsed, 0.001)
        assertTrue(recipe.steps.isNotEmpty())
    }

    @Test
    fun `createSampleSuggestions never exceeds small pantry quantities and converts units`() {
        val pantry = samplePantry()
        val samples = RecipeSuggestionService.createSampleSuggestions(pantry, MealTypeFilter.DINNER)
        assertEquals(4, samples.size)

        val friedRice = samples.first { it.id == "sample_recipe_4" }
        val riceIng = friedRice.availableIngredients.first { it.pantryItemId == "item_rice" }
        assertEquals("kg", riceIng.unit)
        assertTrue("Expected <= 0.2 but got ${riceIng.amountUsed}", riceIng.amountUsed <= 0.2)

        val chickenIng = friedRice.availableIngredients.first { it.pantryItemId == "item_chicken" }
        assertEquals("kg", chickenIng.unit)
        assertEquals(0.2, chickenIng.amountUsed, 0.001)
    }
}
