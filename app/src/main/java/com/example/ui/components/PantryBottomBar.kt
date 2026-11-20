package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.viewmodel.PantryTab

@Composable
fun PantryBottomBar(
    selectedTab: PantryTab,
    urgentCount: Int,
    onSelectTab: (PantryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTabItem(
                    label = stringResource(R.string.nav_pantry),
                    selectedIcon = Icons.Filled.Kitchen,
                    unselectedIcon = Icons.Outlined.Kitchen,
                    selected = selectedTab == PantryTab.PANTRY,
                    badgeCount = urgentCount,
                    testTag = "nav_pantry",
                    onClick = { onSelectTab(PantryTab.PANTRY) },
                    modifier = Modifier.weight(1f)
                )

                ProminentAddNavItem(
                    label = stringResource(R.string.nav_add),
                    selected = selectedTab == PantryTab.ADD,
                    testTag = "nav_add",
                    onClick = { onSelectTab(PantryTab.ADD) },
                    modifier = Modifier.weight(1.1f)
                )

                NavTabItem(
                    label = stringResource(R.string.nav_recipes),
                    selectedIcon = Icons.Filled.RestaurantMenu,
                    unselectedIcon = Icons.Outlined.RestaurantMenu,
                    selected = selectedTab == PantryTab.RECIPES,
                    badgeCount = 0,
                    testTag = "nav_recipes",
                    onClick = { onSelectTab(PantryTab.RECIPES) },
                    modifier = Modifier.weight(1f)
                )

                NavTabItem(
                    label = stringResource(R.string.nav_settings),
                    selectedIcon = Icons.Filled.Settings,
                    unselectedIcon = Icons.Outlined.Settings,
                    selected = selectedTab == PantryTab.SETTINGS,
                    badgeCount = 0,
                    testTag = "nav_settings",
                    onClick = { onSelectTab(PantryTab.SETTINGS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NavTabItem(
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    selected: Boolean,
    badgeCount: Int,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "nav_content_color"
    )
    val pillColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0f)
        },
        label = "nav_pill_color"
    )

    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                role = Role.Tab,
                onClick = onClick
            )
            .testTag(testTag)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(pillColor)
                .padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (badgeCount > 0) {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ) {
                            Text(
                                text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (selected) selectedIcon else unselectedIcon,
                        contentDescription = label,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = if (selected) selectedIcon else unselectedIcon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProminentAddNavItem(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val elevation by animateDpAsState(
        targetValue = if (selected) 8.dp else 5.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "add_btn_elevation"
    )
    val liftOffset by animateDpAsState(
        targetValue = if (selected) (-4).dp else (-2).dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "add_btn_offset"
    )

    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .offset(y = liftOffset)
                .size(46.dp)
                .shadow(elevation, CircleShape, clip = false)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        )
                    )
                )
                .border(
                    width = if (selected) 2.5.dp else 1.5.dp,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PantryNavigationRail(
    selectedTab: PantryTab,
    urgentCount: Int,
    onSelectTab: (PantryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        header = {
            FloatingActionButton(
                onClick = { onSelectTab(PantryTab.ADD) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 12.dp)
                    .testTag("nav_add")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.nav_add)
                )
            }
        }
    ) {
        NavigationRailItem(
            selected = selectedTab == PantryTab.PANTRY,
            onClick = { onSelectTab(PantryTab.PANTRY) },
            icon = {
                if (urgentCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge { Text(urgentCount.toString()) }
                        }
                    ) {
                        Icon(
                            imageVector = if (selectedTab == PantryTab.PANTRY) Icons.Filled.Kitchen else Icons.Outlined.Kitchen,
                            contentDescription = stringResource(R.string.nav_pantry)
                        )
                    }
                } else {
                    Icon(
                        imageVector = if (selectedTab == PantryTab.PANTRY) Icons.Filled.Kitchen else Icons.Outlined.Kitchen,
                        contentDescription = stringResource(R.string.nav_pantry)
                    )
                }
            },
            label = { Text(stringResource(R.string.nav_pantry)) },
            modifier = Modifier.testTag("nav_pantry")
        )
        NavigationRailItem(
            selected = selectedTab == PantryTab.RECIPES,
            onClick = { onSelectTab(PantryTab.RECIPES) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == PantryTab.RECIPES) Icons.Filled.RestaurantMenu else Icons.Outlined.RestaurantMenu,
                    contentDescription = stringResource(R.string.nav_recipes)
                )
            },
            label = { Text(stringResource(R.string.nav_recipes)) },
            modifier = Modifier.testTag("nav_recipes")
        )
        NavigationRailItem(
            selected = selectedTab == PantryTab.SETTINGS,
            onClick = { onSelectTab(PantryTab.SETTINGS) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == PantryTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.nav_settings)
                )
            },
            label = { Text(stringResource(R.string.nav_settings)) },
            modifier = Modifier.testTag("nav_settings")
        )
    }
}
