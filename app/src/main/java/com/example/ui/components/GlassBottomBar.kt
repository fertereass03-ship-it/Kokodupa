package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.settings.AppSettingsManager
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.PrimaryYellow
import com.example.util.AppStrings

enum class NavTab(
    val ruTitle: String,
    val ukTitle: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    HOME("Главное", "Головна", Icons.Filled.Home, Icons.Outlined.Home, "tab_home"),
    CATALOG("Каталог", "Каталог", Icons.Filled.GridView, Icons.Outlined.GridView, "tab_catalog"),
    FAVORITES("Избранное", "Обране", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, "tab_favorites"),
    FRIENDS("Друзья", "Друзі", Icons.Filled.People, Icons.Outlined.PeopleOutline, "tab_friends"),
    PROFILE("Профиль", "Профіль", Icons.Filled.Person, Icons.Outlined.PersonOutline, "tab_profile");

    val title: String
        get() = if (AppSettingsManager.isUkrainian()) ukTitle else ruTitle
}

@Composable
fun GlassBottomBar(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val appSettings by AppSettingsManager.settingsState.collectAsState()
    val colors = LocalAppColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = if (colors.isLight) Color(0x33000000) else Color(0xAA000000)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(colors.glassBackground)
                .border(
                    width = 1.dp,
                    color = colors.glassBorder,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTab.values().forEach { tab ->
                    val isSelected = tab == currentTab
                    val targetColor = if (isSelected) colors.primary else colors.textMuted
                    val animatedColor by animateColorAsState(
                        targetValue = targetColor,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tabColor"
                    )

                    val tabTitle = if (appSettings.language == com.example.data.settings.AppLanguage.UKRAINIAN) tab.ukTitle else tab.ruTitle

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(tab.tag)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(tab) }
                            .padding(vertical = 4.dp, horizontal = 1.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .then(
                                    if (isSelected) {
                                        Modifier.background(
                                            brush = Brush.radialGradient(
                                                listOf(
                                                    colors.secondary.copy(alpha = 0.25f),
                                                    Color.Transparent
                                                )
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    } else Modifier
                                )
                        ) {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tabTitle,
                                tint = animatedColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = tabTitle,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = animatedColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
