package net.packsi.tunnels.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.packsi.tunnels.ui.theme.AppColors

enum class NavTab { HOME, APPS, BROWSER, PROFILE }

@Composable
fun AppBottomNav(
    selected: NavTab,
    onSelect: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.background(AppColors.CardBg).fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().drawBehind {
                drawLine(
                    AppColors.NavBorder,
                    Offset(0f, 0f),
                    Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            },
        ) {
            NavItem("Home", selected == NavTab.HOME, onClick = { onSelect(NavTab.HOME) }) { color ->
                HomeIcon(color, filled = selected == NavTab.HOME)
            }
            NavItem("Apps", selected == NavTab.APPS, onClick = { onSelect(NavTab.APPS) }) { color ->
                AppsIcon(color)
            }
            NavItem("browser", selected == NavTab.BROWSER, onClick = { onSelect(NavTab.BROWSER) }) { color ->
                GlobeIcon(color)
            }
            NavItem("Profile", selected == NavTab.PROFILE, onClick = { onSelect(NavTab.PROFILE) }) { color ->
                ProfileIcon(color)
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit,
) {
    val color = if (active) AppColors.Teal else AppColors.TextMuted
    Column(
        Modifier
            .weight(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() }
            .padding(top = 10.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        icon(color)
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            color = color,
        )
    }
}
