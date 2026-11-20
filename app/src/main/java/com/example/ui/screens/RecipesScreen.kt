package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ai.AiRecipeSuggestion
import com.example.ai.CookTimeFilter
import com.example.ai.MealTypeFilter
import com.example.data.model.PantryItem
import com.example.data.util.DateUtils
import com.example.data.util.UnitConverter
import com.example.ui.components.RecipeThinkingSkeletonSection
import com.example.ui.theme.LocalPantryColors
import com.example.viewmodel.CookedIngredientDeduction
import com.example.viewmodel.PantryUiState
import com.example.viewmodel.RecipeGenerationState
import com.example.viewmodel.RecipeTabUiState
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipesScreen(
    uiState: PantryUiState,
    recipeTabState: RecipeTabUiState,
    onToggleIngredient: (String) -> Unit,
    onSelectOnlyExpiring: () -> Unit,
    onSelectAllIngredients: () -> Unit,
    onSetCookTimeFilter: (CookTimeFilter) -> Unit,
    onSetMealTypeFilter: (MealTypeFilter) -> Unit,
    onGenerateAiRecipes: () -> Unit,
    onLoadSampleRecipes: () -> Unit,
    onConfirmCookedDeductions: (String, List<CookedIngredientDeduction>) -> Unit,
    modifier: Modifier = Modifier
) {
    val pantryColors = LocalPantryColors.current
    var selectedRecipeForSheet by remember { mutableStateOf<AiRecipeSuggestion?>(null) }
    val listState = rememberLazyListState()

    val expiringItems = remember(uiState.allActiveItems) {
        uiState.allActiveItems.filter { DateUtils.daysUntil(it.expiryDate) <= 3 }
    }
    val freshItems = remember(uiState.allActiveItems) {
        uiState.allActiveItems.filter { DateUtils.daysUntil(it.expiryDate) > 3 }
    }

    LaunchedEffect(recipeTabState.generationState) {
        when (recipeTabState.generationState) {
            is RecipeGenerationState.Loading,
            is RecipeGenerationState.Success,
            is RecipeGenerationState.Error -> {
                listState.animateScrollToItem(index = 4)
            }
            is RecipeGenerationState.Idle -> Unit
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item(key = "recipes_header") {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.recipes_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Chọn nguyên liệu đang có để PantryFlow gợi ý món ngon & trừ kho nhanh",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Editorial Hero Banner
            item(key = "recipes_hero") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(118.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_recipe_banner),
                            contentDescription = "Gợi ý nấu ăn thông minh cùng PantryFlow",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF143822).copy(alpha = 0.90f),
                                            Color(0xFF157F3D).copy(alpha = 0.62f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Giải cứu thực phẩm cùng PantryFlow",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = if (expiringItems.isNotEmpty()) {
                                    "Đã tự động tick sẵn ${expiringItems.size} món sắp hết hạn (≤ 3 ngày) để ưu tiên nấu trước."
                                } else {
                                    "Tick chọn nguyên liệu trong tủ để PantryFlow lên thực đơn cơm nhà Việt phù hợp."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }
                }
            }

            // 1. Smart Ingredient Selection Card
            item(key = "ingredient_picker") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "1. Chọn nguyên liệu (${recipeTabState.selectedIngredientIds.size}/${uiState.allActiveItems.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (expiringItems.isNotEmpty()) {
                                    TextButton(
                                        onClick = onSelectOnlyExpiring,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("select_only_expiring_btn")
                                    ) {
                                        Text("Chỉ sắp hết hạn", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                TextButton(
                                    onClick = onSelectAllIngredients,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("select_all_ingredients_btn")
                                ) {
                                    Text("Chọn tất cả", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        if (uiState.allActiveItems.isEmpty()) {
                            Text(
                                text = "Tủ đồ hiện chưa có thực phẩm nào. Hãy thêm thực phẩm ở tab Tủ đồ trước nhé.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            // Expiring soon items (auto-selected with "Ưu tiên giải cứu")
                            if (expiringItems.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = pantryColors.urgentFg,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Ưu tiên giải cứu (sắp hết hạn 1–3 ngày)",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = pantryColors.urgentFg
                                        )
                                    }

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        expiringItems.forEach { item ->
                                            SelectableIngredientCard(
                                                item = item,
                                                isExpiringSoon = true,
                                                selected = item.id in recipeTabState.selectedIngredientIds,
                                                onToggle = { onToggleIngredient(item.id) }
                                            )
                                        }
                                    }
                                }
                            }

                            // Still fresh items (optional to cook together)
                            if (freshItems.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Thực phẩm còn hạn khác (tick thêm để nấu chung)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        freshItems.forEach { item ->
                                            SelectableIngredientCard(
                                                item = item,
                                                isExpiringSoon = false,
                                                selected = item.id in recipeTabState.selectedIngredientIds,
                                                onToggle = { onToggleIngredient(item.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Quick Filters (Cook Time + Meal Type) & CTA Buttons
            item(key = "filters_and_generate") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Thời gian nấu",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CookTimeFilter.entries.forEach { filter ->
                                    val selected = recipeTabState.cookTimeFilter == filter
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onSetCookTimeFilter(filter) },
                                        label = { Text(filter.label, style = MaterialTheme.typography.labelMedium) },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.testTag("cook_time_${filter.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Bữa ăn",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MealTypeFilter.entries.forEach { meal ->
                                    val selected = recipeTabState.mealTypeFilter == meal
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onSetMealTypeFilter(meal) },
                                        label = { Text(meal.label, style = MaterialTheme.typography.labelMedium) },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.testTag("meal_type_${meal.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Primary Generate Button + Sample Button
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onGenerateAiRecipes,
                                enabled = recipeTabState.generationState !is RecipeGenerationState.Loading &&
                                    recipeTabState.selectedIngredientIds.isNotEmpty(),
                                shape = RoundedCornerShape(15.dp),
                                contentPadding = PaddingValues(vertical = 14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("generate_ai_recipes_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Gợi ý món ăn ngay (${recipeTabState.selectedIngredientIds.size} nguyên liệu)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            OutlinedButton(
                                onClick = onLoadSampleRecipes,
                                enabled = recipeTabState.generationState !is RecipeGenerationState.Loading,
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(vertical = 11.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("view_sample_recipes_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Science,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Xem gợi ý mẫu",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }

            // 3. Generation Results Area
            when (val genState = recipeTabState.generationState) {
                is RecipeGenerationState.Idle -> {
                    item(key = "recipes_idle_hint") {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = "Bấm \"Gợi ý món ăn ngay\" để PantryFlow thiết kế 3–4 món Việt phù hợp nhất, hoặc bấm \"Xem gợi ý mẫu\" để xem nhanh giao diện.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            )
                        }
                    }
                }

                is RecipeGenerationState.Loading -> {
                    item(key = "recipes_loading") {
                        RecipeThinkingSkeletonSection(
                            cookTimeLabel = recipeTabState.cookTimeFilter.label
                        )
                    }
                }

                is RecipeGenerationState.Error -> {
                    item(key = "recipes_error") {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = pantryColors.urgentBg,
                            border = BorderStroke(1.dp, pantryColors.urgentAccent.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ErrorOutline,
                                        contentDescription = null,
                                        tint = pantryColors.urgentFg
                                    )
                                    Text(
                                        text = "Chưa thể tải gợi ý món ăn",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = pantryColors.urgentFg
                                    )
                                }
                                Text(
                                    text = genState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = pantryColors.urgentFg
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = onGenerateAiRecipes) {
                                        Text("Thử lại")
                                    }
                                    TextButton(onClick = onLoadSampleRecipes) {
                                        Text("Xem gợi ý mẫu")
                                    }
                                }
                            }
                        }
                    }
                }

                is RecipeGenerationState.Success -> {
                    item(key = "results_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Thực đơn gợi ý (${genState.recipes.size} món)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = if (genState.isSampleData) "Gợi ý mẫu" else "PantryFlow gợi ý",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    items(
                        items = genState.recipes,
                        key = { it.id }
                    ) { recipe ->
                        AiRecipeResultCard(
                            recipe = recipe,
                            onClick = { selectedRecipeForSheet = recipe }
                        )
                    }
                }
            }
        }
    }

    // Recipe Detail & "Tôi đã nấu món này" Stock Deduction Bottom Sheet
    selectedRecipeForSheet?.let { activeRecipe ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedRecipeForSheet = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            RecipeDetailAndDeductionSheet(
                recipe = activeRecipe,
                activePantryItems = uiState.allActiveItems,
                onConfirmDeductions = { deductions ->
                    onConfirmCookedDeductions(activeRecipe.title, deductions)
                    selectedRecipeForSheet = null
                },
                onClose = { selectedRecipeForSheet = null }
            )
        }
    }
}

@Composable
private fun SelectableIngredientCard(
    item: PantryItem,
    isExpiringSoon: Boolean,
    selected: Boolean,
    onToggle: () -> Unit
) {
    val pantryColors = LocalPantryColors.current
    val days = DateUtils.daysUntil(item.expiryDate)

    val containerColor = when {
        selected && isExpiringSoon -> pantryColors.urgentBg
        selected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val borderColor = when {
        selected && isExpiringSoon -> pantryColors.urgentAccent
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .testTag("recipe_ing_select_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (selected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                contentDescription = null,
                tint = when {
                    selected && isExpiringSoon -> pantryColors.urgentFg
                    selected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(18.dp)
            )

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isExpiringSoon) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = pantryColors.urgentAccent
                        ) {
                            Text(
                                text = "Ưu tiên giải cứu",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${UnitConverter.formatPreciseQuantity(item.quantity)} ${item.unit} • ${DateUtils.formatDaysRemainingLabel(days)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isExpiringSoon) pantryColors.urgentFg else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiRecipeResultCard(
    recipe: AiRecipeSuggestion,
    onClick: () -> Unit
) {
    val pantryColors = LocalPantryColors.current
    val rescuedCount = recipe.availableIngredients.count { it.isExpiringSoon }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("ai_recipe_card_${recipe.id}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (rescuedCount > 0) 1.3.dp else 1.dp,
            color = if (rescuedCount > 0) {
                pantryColors.warningAccent.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Title + Cook Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (rescuedCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = pantryColors.urgentBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = pantryColors.urgentFg,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Giải cứu $rescuedCount món sắp hết hạn",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = pantryColors.urgentFg
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Text(
                        text = recipe.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${recipe.cookTimeMinutes} phút",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (recipe.description.isNotBlank()) {
                Text(
                    text = recipe.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Available Ingredients (expiring highlighted in red/amber, fresh in green)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "Nguyên liệu đang có:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    recipe.availableIngredients.forEach { ing ->
                        val bg = if (ing.isExpiringSoon) pantryColors.urgentBg else pantryColors.freshBg
                        val fg = if (ing.isExpiringSoon) pantryColors.urgentFg else pantryColors.freshFg
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = bg,
                            border = if (ing.isExpiringSoon) {
                                BorderStroke(1.dp, pantryColors.urgentAccent.copy(alpha = 0.4f))
                            } else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (ing.isExpiringSoon) Icons.Filled.LocalFireDepartment else Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = fg,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "${ing.name} (${UnitConverter.formatPreciseQuantity(ing.amountUsed)} ${ing.unit})",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (ing.isExpiringSoon) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = fg
                                )
                            }
                        }
                    }
                }
            }

            // Missing / Basic Ingredients
            if (recipe.missingIngredients.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ShoppingBag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Cần thêm / gia vị: ${recipe.missingIngredients.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${recipe.mealType} • ${recipe.difficulty}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Xem cách nấu & Trừ kho →",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private data class DeductionRowDraft(
    val pantryItem: PantryItem,
    val checked: Boolean,
    val amountText: String,
    val selectedUnit: String,
    val markFullyUsed: Boolean
)

@Composable
private fun RecipeDetailAndDeductionSheet(
    recipe: AiRecipeSuggestion,
    activePantryItems: List<PantryItem>,
    onConfirmDeductions: (List<CookedIngredientDeduction>) -> Unit,
    onClose: () -> Unit
) {
    val pantryColors = LocalPantryColors.current
    var showDeductionStep by remember(recipe.id) { mutableStateOf(false) }

    // Match recipe availableIngredients to real active pantry items for direct deduction
    var deductionRows by remember(recipe.id, activePantryItems) {
        val matched = recipe.availableIngredients.mapNotNull { ing ->
            val pantryItem = ing.pantryItemId?.let { id ->
                activePantryItems.firstOrNull { it.id == id }
            } ?: activePantryItems.firstOrNull { item ->
                item.name.equals(ing.name, ignoreCase = true) ||
                    item.name.lowercase().contains(ing.name.lowercase()) ||
                    ing.name.lowercase().contains(item.name.lowercase())
            }
            pantryItem?.let { item ->
                val convertedAmt = UnitConverter.convert(ing.amountUsed, ing.unit, item.unit)
                val defaultAmt = if (item.quantity > 0.0) {
                    convertedAmt.coerceIn(minOf(0.1, item.quantity), item.quantity)
                } else {
                    0.0
                }
                val full = item.quantity > 0.0 && defaultAmt >= item.quantity
                DeductionRowDraft(
                    pantryItem = item,
                    checked = true,
                    amountText = UnitConverter.formatPreciseQuantity(defaultAmt),
                    selectedUnit = item.unit,
                    markFullyUsed = full
                )
            }
        }.distinctBy { it.pantryItem.id }
        mutableStateOf(matched)
    }

    fun updateDeductionRow(itemId: String, transform: (DeductionRowDraft) -> DeductionRowDraft) {
        deductionRows = deductionRows.map { row ->
            if (row.pantryItem.id == itemId) transform(row) else row
        }
    }

    AnimatedContent(
        targetState = showDeductionStep,
        label = "recipe_sheet_step"
    ) { inDeductionStep ->
        if (!inDeductionStep) {
            // View 1: Concise Recipe Steps + "Tôi đã nấu món này" button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = recipe.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${recipe.cookTimeMinutes} phút • ${recipe.difficulty} • Bữa ${recipe.mealType}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (recipe.description.isNotBlank()) {
                        Text(
                            text = recipe.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Ingredients summary
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Nguyên liệu sử dụng",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    recipe.availableIngredients.forEach { ing ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (ing.isExpiringSoon) Icons.Filled.LocalFireDepartment else Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = if (ing.isExpiringSoon) pantryColors.urgentFg else pantryColors.freshFg,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${ing.name}: ${UnitConverter.formatPreciseQuantity(ing.amountUsed)} ${ing.unit}" +
                                    if (ing.isExpiringSoon) " (Giải cứu)" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (ing.isExpiringSoon) pantryColors.urgentFg else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    if (recipe.missingIngredients.isNotEmpty()) {
                        Text(
                            text = "Gia vị / mua thêm: ${recipe.missingIngredients.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Numbered concise steps
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Các bước nấu ăn",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    recipe.steps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Prominent "Tôi đã nấu món này" CTA
                Button(
                    onClick = { showDeductionStep = true },
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 15.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cooked_this_recipe_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tôi đã nấu món này",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        } else {
            // View 2: Quick Stock Deduction Confirmation Table
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { showDeductionStep = false },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại công thức",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Công thức")
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Trừ nguyên liệu vừa dùng",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Kiểm tra số lượng vừa nấu cho món \"${recipe.title}\" để cập nhật Tủ đồ ngay.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (deductionRows.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Text(
                            text = "Không có nguyên liệu nào của món này còn tồn trong tủ đồ để trừ.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                    FilledTonalButton(
                        onClick = onClose,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Đóng")
                    }
                } else {
                    deductionRows.forEach { row ->
                        val compatUnits = UnitConverter.compatibleUnitsFor(row.pantryItem.unit)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(
                                1.dp,
                                if (row.checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = row.checked,
                                            onCheckedChange = { isChecked ->
                                                updateDeductionRow(row.pantryItem.id) {
                                                    it.copy(checked = isChecked)
                                                }
                                            }
                                        )
                                        Column {
                                            Text(
                                                text = row.pantryItem.name,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Hiện còn: ${UnitConverter.formatPreciseQuantity(row.pantryItem.quantity)} ${row.pantryItem.unit}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    FilterChip(
                                        selected = row.markFullyUsed,
                                        onClick = {
                                            val nextFull = !row.markFullyUsed
                                            updateDeductionRow(row.pantryItem.id) {
                                                it.copy(
                                                    checked = true,
                                                    markFullyUsed = nextFull,
                                                    selectedUnit = if (nextFull) row.pantryItem.unit else it.selectedUnit,
                                                    amountText = if (nextFull) {
                                                        UnitConverter.formatPreciseQuantity(row.pantryItem.quantity)
                                                    } else it.amountText
                                                )
                                            }
                                        },
                                        label = { Text("Dùng hết", style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = pantryColors.freshBg,
                                            selectedLabelColor = pantryColors.freshFg
                                        ),
                                        modifier = Modifier.testTag("deduct_full_${row.pantryItem.id}")
                                    )
                                }

                                if (row.checked && !row.markFullyUsed) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier
                                                .size(44.dp)
                                                .minimumInteractiveComponentSize()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    val cur = row.amountText.replace(",", ".").toDoubleOrNull() ?: 1.0
                                                    val step = when {
                                                        row.selectedUnit == "g" || row.selectedUnit == "ml" -> 50.0
                                                        cur <= 1.0 -> 0.1
                                                        else -> 0.5
                                                    }
                                                    val minVal = if (row.selectedUnit == "g" || row.selectedUnit == "ml") 10.0 else 0.1
                                                    val next = round((cur - step).coerceAtLeast(minVal) * 100.0) / 100.0
                                                    updateDeductionRow(row.pantryItem.id) {
                                                        it.copy(amountText = UnitConverter.formatPreciseQuantity(next))
                                                    }
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Filled.Remove, contentDescription = "Giảm", modifier = Modifier.size(18.dp))
                                            }
                                        }

                                        OutlinedTextField(
                                            value = row.amountText,
                                            onValueChange = { input ->
                                                val cleaned = input.replace(",", ".")
                                                if (cleaned.isEmpty() || cleaned.matches(Regex("^\\d*\\.?\\d*$"))) {
                                                    updateDeductionRow(row.pantryItem.id) {
                                                        it.copy(amountText = cleaned)
                                                    }
                                                }
                                            },
                                            singleLine = true,
                                            suffix = { Text(row.selectedUnit) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .minimumInteractiveComponentSize()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    val cur = row.amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                                                    val step = when {
                                                        row.selectedUnit == "g" || row.selectedUnit == "ml" -> 50.0
                                                        cur < 1.0 -> 0.1
                                                        else -> 0.5
                                                    }
                                                    val next = round((cur + step) * 100.0) / 100.0
                                                    updateDeductionRow(row.pantryItem.id) {
                                                        it.copy(amountText = UnitConverter.formatPreciseQuantity(next))
                                                    }
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Filled.Add,
                                                    contentDescription = "Tăng",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        if (compatUnits.size > 1) {
                                            val otherUnit = compatUnits.first { it != row.selectedUnit }
                                            TextButton(
                                                onClick = {
                                                    val cur = row.amountText.replace(",", ".").toDoubleOrNull()
                                                    val convertedText = if (cur != null && cur > 0.0) {
                                                        val converted = UnitConverter.convert(cur, row.selectedUnit, otherUnit)
                                                        UnitConverter.formatPreciseQuantity(converted)
                                                    } else {
                                                        row.amountText
                                                    }
                                                    updateDeductionRow(row.pantryItem.id) {
                                                        it.copy(
                                                            selectedUnit = otherUnit,
                                                            amountText = convertedText
                                                        )
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 6.dp)
                                            ) {
                                                Text(otherUnit, style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val validCheckedCount = deductionRows.count { r ->
                        r.checked && (r.markFullyUsed || (r.amountText.replace(",", ".").toDoubleOrNull() ?: 0.0) > 0.0)
                    }
                    Button(
                        onClick = {
                            val toApply = deductionRows
                                .filter { it.checked }
                                .map { r ->
                                    val amt = r.amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                                    CookedIngredientDeduction(
                                        pantryItem = r.pantryItem,
                                        usedAmount = amt,
                                        usedUnit = r.selectedUnit,
                                        markFullyUsed = r.markFullyUsed
                                    )
                                }
                            onConfirmDeductions(toApply)
                        },
                        enabled = validCheckedCount > 0,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 15.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_cooked_deductions_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Xác nhận trừ kho ($validCheckedCount nguyên liệu)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
