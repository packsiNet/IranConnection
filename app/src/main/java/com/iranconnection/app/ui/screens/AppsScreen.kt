package com.iranconnection.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iranconnection.app.ui.theme.AppColors

data class BankApp(
    val id: String,
    val name: String,
    val sub: String,
    val letter: String,
    val c1: Color,
    val c2: Color,
)

private val freeApps = listOf(
    BankApp("bluebank", "BluBank", "بلوبانک", "BL", Color(0xFF0D47A1), Color(0xFF1976D2)),
)
private val premiumApps = listOf(
    BankApp("saman", "Saman Bank", "بانک سامان", "SM", Color(0xFF01579B), Color(0xFF0288D1)),
    BankApp("parsian", "Bank Parsian", "بانک پارسیان", "PR", Color(0xFF4A148C), Color(0xFF9C27B0)),
)

@Composable
fun AppsScreen(
    enabled: Set<String>,
    onToggle: (String) -> Unit,
    onClose: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()
    fun filter(list: List<BankApp>) = list.filter { q.isEmpty() || it.name.lowercase().contains(q) }
    val free = filter(freeApps)
    val prem = filter(premiumApps)
    val hasResults = free.isNotEmpty() || prem.isNotEmpty()

    Column(Modifier.fillMaxSize().background(AppColors.ScreenBg)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Iran Bank Apps", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, letterSpacing = (-0.5).sp)
            CloseButton(onClose)
        }

        SearchBar(query, { query = it }, "Search bank apps...", Modifier.padding(horizontal = 16.dp, vertical = 10.dp))

        // Active count badge
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier.background(AppColors.Teal, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text("${enabled.size} Active", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            Text("apps connected via VPN", fontSize = 11.sp, color = AppColors.TextMuted)
        }

        if (!hasResults) {
            NoResults("No apps found")
            return@Column
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 12.dp),
        ) {
            if (free.isNotEmpty()) {
                item { SectionLabel("Free Apps", premium = false) }
                item {
                    AppCard {
                        free.forEachIndexed { i, a -> AppRow(a, enabled.contains(a.id), i == free.lastIndex, onToggle) }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            if (prem.isNotEmpty()) {
                item { SectionLabel("Premium Apps", premium = true) }
                item {
                    // Premium list is independently scrollable, capped at 280dp (matches design).
                    Box(Modifier.heightIn(max = 280.dp)) {
                        LazyColumn(
                            Modifier
                                .shadow(4.dp, RoundedCornerShape(13.dp))
                                .background(AppColors.CardBg, RoundedCornerShape(13.dp)),
                        ) {
                            itemsIndexedPremium(prem, enabled, onToggle)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexedPremium(
    apps: List<BankApp>,
    enabled: Set<String>,
    onToggle: (String) -> Unit,
) {
    apps.forEachIndexed { i, a ->
        item(key = a.id) { AppRow(a, enabled.contains(a.id), i == apps.lastIndex, onToggle) }
    }
}

@Composable
private fun SectionLabel(text: String, premium: Boolean) {
    Row(
        Modifier.padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AppColors.TextMuted)
        if (premium) Text("👑", fontSize = 12.sp)
    }
}

@Composable
private fun AppCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(13.dp))
            .background(AppColors.CardBg, RoundedCornerShape(13.dp)),
    ) { content() }
}

@Composable
private fun AppRow(app: BankApp, isOn: Boolean, isLast: Boolean, onToggle: (String) -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onToggle(app.id) }
                .padding(horizontal = 13.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BankIcon(app)
            Column(Modifier.weight(1f)) {
                Text(app.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                Text(app.sub, fontSize = 10.sp, color = AppColors.TextMuted, modifier = Modifier.padding(top = 1.dp))
            }
            SlideToggle(isOn) { onToggle(app.id) }
        }
        if (!isLast) Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.Divider))
    }
}

@Composable
private fun BankIcon(app: BankApp) {
    Box(
        Modifier
            .size(32.dp)
            .shadow(2.dp, RoundedCornerShape(8.dp), spotColor = app.c1)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(listOf(app.c1, app.c2))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            app.letter,
            color = Color.White,
            fontSize = if (app.letter.length > 2) 8.sp else if (app.letter.length > 1) 10.sp else 14.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
        )
    }
}

@Composable
private fun SlideToggle(isOn: Boolean, onToggle: () -> Unit) {
    val bg by animateColorAsState(if (isOn) AppColors.Teal else AppColors.ToggleOff, label = "toggleBg")
    val knobOffset by animateDpAsState(if (isOn) 19.dp else 3.dp, label = "knob")
    Box(
        Modifier
            .size(36.dp, 20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onToggle() },
    ) {
        Box(
            Modifier
                .padding(top = 3.dp)
                .offset(x = knobOffset)
                .size(14.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}
