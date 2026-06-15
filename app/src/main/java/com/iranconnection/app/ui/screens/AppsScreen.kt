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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iranconnection.app.data.IranianApp
import com.iranconnection.app.data.IranianAppList
import com.iranconnection.app.ui.theme.AppColors

/** Flat list entry types so the whole screen renders through real LazyColumn items (lazy, virtualized). */
private sealed interface AppEntry {
    data class Section(val text: String, val premium: Boolean) : AppEntry
    data class Category(val label: String, val divider: Boolean, val cardTop: Boolean) : AppEntry
    data class AppItem(
        val app: IranianApp,
        val divider: Boolean,
        val cardTop: Boolean,
        val cardBottom: Boolean,
    ) : AppEntry
    data class Gap(val id: String) : AppEntry
}

@Composable
fun AppsScreen(
    appToggles: Map<String, Boolean>,
    onSetEnabled: (String, Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    // Resolve every icon resource id ONCE (getIdentifier reflection is slow); reused across recompositions.
    val iconRes = remember {
        IranianAppList.apps.associate { app ->
            app.packageName to context.resources.getIdentifier(
                "appicon_${app.packageName.replace('.', '_')}", "drawable", context.packageName,
            )
        }
    }

    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()

    fun filter(list: List<IranianApp>) = list.filter { app ->
        q.isEmpty() ||
            app.nameFa.contains(q) ||
            app.nameEn.lowercase().contains(q) ||
            app.packageName.lowercase().contains(q)
    }

    val freeApps    = filter(IranianAppList.apps.filter { it.packageName in IranianAppList.FREE_PACKAGES })
    val premiumApps = filter(IranianAppList.apps.filter { it.packageName !in IranianAppList.FREE_PACKAGES })
    val activeCount = appToggles.values.count { it }
    val hasResults  = freeApps.isNotEmpty() || premiumApps.isNotEmpty()

    val entries = buildList {
        if (freeApps.isNotEmpty()) {
            add(AppEntry.Section("Free Apps", premium = false))
            freeApps.forEachIndexed { i, app ->
                add(AppEntry.AppItem(app, divider = i != freeApps.lastIndex, cardTop = i == 0, cardBottom = i == freeApps.lastIndex))
            }
            add(AppEntry.Gap("free"))
        }
        if (premiumApps.isNotEmpty()) {
            add(AppEntry.Section("Premium Apps", premium = true))
            val grouped = premiumApps.groupBy { it.category }
            val order = IranianAppList.CATEGORY_ORDER.filter { grouped.containsKey(it) }
            order.forEachIndexed { gi, cat ->
                val catApps = grouped[cat] ?: return@forEachIndexed
                add(AppEntry.Category(cat, divider = gi > 0, cardTop = gi == 0))
                catApps.forEachIndexed { i, app ->
                    val lastInCard = gi == order.lastIndex && i == catApps.lastIndex
                    add(AppEntry.AppItem(app, divider = !lastInCard, cardTop = false, cardBottom = lastInCard))
                }
            }
            add(AppEntry.Gap("premium"))
        }
    }

    Column(Modifier.fillMaxSize().background(AppColors.ScreenBg)) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Iranian Apps",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                letterSpacing = (-0.5).sp,
            )
            CloseButton(onClose)
        }

        SearchBar(query, { query = it }, "Search...", Modifier.padding(horizontal = 16.dp, vertical = 10.dp))

        // Active count badge
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier.background(AppColors.Teal, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text("$activeCount Active", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            Text("of ${IranianAppList.apps.size} apps", fontSize = 11.sp, color = AppColors.TextMuted)
        }

        if (!hasResults) {
            NoResults("No apps found")
            return@Column
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp,
            ),
        ) {
            items(entries, key = { entry ->
                when (entry) {
                    is AppEntry.Section  -> "sec_${entry.text}"
                    is AppEntry.Category -> "cat_${entry.label}"
                    is AppEntry.AppItem  -> "app_${entry.app.packageName}"
                    is AppEntry.Gap      -> "gap_${entry.id}"
                }
            }) { entry ->
                when (entry) {
                    is AppEntry.Section  -> SectionLabel(entry.text, entry.premium)
                    is AppEntry.Category -> CategoryBand(entry.label, entry.divider, entry.cardTop)
                    is AppEntry.AppItem  -> AppRow(
                        app = entry.app,
                        iconRes = iconRes[entry.app.packageName] ?: 0,
                        isOn = appToggles[entry.app.packageName] ?: true,
                        showDivider = entry.divider,
                        cardTop = entry.cardTop,
                        cardBottom = entry.cardBottom,
                        onSetEnabled = onSetEnabled,
                    )
                    is AppEntry.Gap      -> Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

/** Card-surface background shape: rounds only the corners that sit at the top/bottom of a section card. */
private fun cardShape(top: Boolean, bottom: Boolean) = RoundedCornerShape(
    topStart    = if (top) 13.dp else 0.dp,
    topEnd      = if (top) 13.dp else 0.dp,
    bottomStart = if (bottom) 13.dp else 0.dp,
    bottomEnd   = if (bottom) 13.dp else 0.dp,
)

private val CATEGORY_EMOJI = mapOf(
    "Banks"     to "🏦",
    "Finance"   to "💳",
    "Telecom"   to "📱",
    "Shopping"  to "🛒",
    "Insurance" to "🛡",
    "Social"    to "💬",
    "Services"  to "⚙️",
)

@Composable
private fun CategoryBand(category: String, divider: Boolean, cardTop: Boolean) {
    val emoji = CATEGORY_EMOJI[category] ?: "•"
    Column(Modifier.fillMaxWidth().background(AppColors.CardBg, cardShape(cardTop, false))) {
        if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.Divider))
        Row(
            Modifier
                .fillMaxWidth()
                .background(AppColors.ScreenBg.copy(alpha = 0.6f))
                .padding(horizontal = 13.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(emoji, fontSize = 11.sp)
            Text(
                category,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextMuted,
                letterSpacing = 0.3.sp,
            )
        }
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
private fun AppRow(
    app: IranianApp,
    iconRes: Int,
    isOn: Boolean,
    showDivider: Boolean,
    cardTop: Boolean,
    cardBottom: Boolean,
    onSetEnabled: (String, Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(AppColors.CardBg, cardShape(cardTop, cardBottom))) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onSetEnabled(app.packageName, !isOn) }
                .padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppIcon(app, iconRes)
            Column(Modifier.weight(1f)) {
                Text(app.nameEn, fontSize = 15.sp, fontWeight = FontWeight.Bold,  color = AppColors.TextPrimary)
                Text(app.nameFa, fontSize = 12.sp,                               color = AppColors.TextMuted, modifier = Modifier.padding(top = 1.dp))
            }
            SlideToggle(isOn) { onSetEnabled(app.packageName, !isOn) }
        }
        if (showDivider) Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.Divider))
    }
}

@Composable
private fun AppIcon(app: IranianApp, resId: Int) {
    val shape = RoundedCornerShape(9.dp)
    if (resId != 0) {
        androidx.compose.foundation.Image(
            painter = painterResource(resId),
            contentDescription = app.nameEn,
            modifier = Modifier
                .size(36.dp)
                .clip(shape),
            contentScale = ContentScale.Crop,
        )
    } else {
        val (c1, c2) = iconColors(app.packageName)
        val letter = app.nameEn.filter { it.isUpperCase() }.take(2).ifEmpty {
            app.nameEn.take(2).uppercase()
        }
        Box(
            Modifier
                .size(36.dp)
                .clip(shape)
                .background(Brush.linearGradient(listOf(c1, c2))),
            contentAlignment = Alignment.Center,
        ) {
            Text(letter, color = Color.White, fontSize = if (letter.length > 1) 11.sp else 14.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun SlideToggle(isOn: Boolean, onToggle: () -> Unit) {
    val bg by animateColorAsState(if (isOn) AppColors.Teal else AppColors.ToggleOff, label = "tbg")
    val knob by animateDpAsState(if (isOn) 19.dp else 3.dp, label = "knob")
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
                .offset(x = knob)
                .size(14.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

private val PALETTE = arrayOf(
    Color(0xFF1A7A3C) to Color(0xFF2EBB5E),
    Color(0xFF01579B) to Color(0xFF0288D1),
    Color(0xFF4A148C) to Color(0xFF9C27B0),
    Color(0xFFB71C5E) to Color(0xFFE91E8C),
    Color(0xFF33691E) to Color(0xFF8BC34A),
    Color(0xFFBF360C) to Color(0xFFFF5722),
    Color(0xFF006064) to Color(0xFF00BCD4),
    Color(0xFF37474F) to Color(0xFF78909C),
    Color(0xFF880E4F) to Color(0xFFAD1457),
    Color(0xFF1B5E20) to Color(0xFF43A047),
)

private fun iconColors(pkg: String): Pair<Color, Color> =
    PALETTE[Math.abs(pkg.hashCode()) % PALETTE.size]
