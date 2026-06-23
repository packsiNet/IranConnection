package net.packsi.tunnels.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import net.packsi.tunnels.data.AppsViewModel
import net.packsi.tunnels.data.IranianAppInfo
import net.packsi.tunnels.ui.theme.AppColors

/** Flat list entry types so the whole screen renders through real LazyColumn items (lazy, virtualized). */
private sealed interface AppEntry {
    data class Section(val text: String, val premium: Boolean) : AppEntry
    data class AppItem(
        val app: IranianAppInfo,
        val divider: Boolean,
        val cardTop: Boolean,
        val cardBottom: Boolean,
        val locked: Boolean,
    ) : AppEntry
    data class Gap(val id: String) : AppEntry
}

@Composable
fun AppsScreen(
    onClose: () -> Unit,
    vm: AppsViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    val isPremiumUser = state.isPremium

    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()

    fun filter(list: List<IranianAppInfo>) = list.filter { app ->
        q.isEmpty() ||
            app.appName.lowercase().contains(q) ||
            app.packageName.lowercase().contains(q)
    }

    val freeApps = filter(state.freeApps)
    val premiumApps = filter(state.premiumApps)
    val hasResults = freeApps.isNotEmpty() || premiumApps.isNotEmpty()

    val entries = buildList {
        if (freeApps.isNotEmpty()) {
            add(AppEntry.Section("Free", premium = false))
            freeApps.forEachIndexed { i, app ->
                add(AppEntry.AppItem(app, divider = i != freeApps.lastIndex, cardTop = i == 0, cardBottom = i == freeApps.lastIndex, locked = false))
            }
            add(AppEntry.Gap("free"))
        }
        if (premiumApps.isNotEmpty()) {
            add(AppEntry.Section("Premium", premium = true))
            premiumApps.forEachIndexed { i, app ->
                add(AppEntry.AppItem(app, divider = i != premiumApps.lastIndex, cardTop = i == 0, cardBottom = i == premiumApps.lastIndex, locked = !isPremiumUser))
            }
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

        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            RefreshButton(isLoading = state.isLoading, onClick = vm::loadApps)
        }

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppColors.Teal)
            }
            !hasResults -> NoResults("No Iranian apps found.")
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            ) {
                items(entries, key = { entry ->
                    when (entry) {
                        is AppEntry.Section -> "sec_${entry.text}"
                        is AppEntry.AppItem -> "app_${entry.app.packageName}"
                        is AppEntry.Gap     -> "gap_${entry.id}"
                    }
                }) { entry ->
                    when (entry) {
                        is AppEntry.Section -> SectionLabel(entry.text, entry.premium)
                        is AppEntry.AppItem -> AppRow(
                            entry = entry,
                            isOn = entry.app.packageName in state.enabledPackages,
                            onSetEnabled = { vm.toggleApp(entry.app.packageName) },
                        )
                        is AppEntry.Gap -> Box(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RefreshButton(isLoading: Boolean, onClick: () -> Unit) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isLoading) {
        if (isLoading) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = infiniteRepeatable(animation = tween(800, easing = LinearEasing)),
            )
        } else {
            rotation.snapTo(0f)
        }
    }
    Text(
        "⟳",
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF7A82A0),
        modifier = Modifier
            .graphicsLayer { rotationZ = rotation.value }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !isLoading,
                onClick = onClick,
            ),
    )
}

/** Card-surface background shape: rounds only the corners that sit at the top/bottom of a section card. */
private fun cardShape(top: Boolean, bottom: Boolean) = RoundedCornerShape(
    topStart    = if (top) 13.dp else 0.dp,
    topEnd      = if (top) 13.dp else 0.dp,
    bottomStart = if (bottom) 13.dp else 0.dp,
    bottomEnd   = if (bottom) 13.dp else 0.dp,
)

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
    entry: AppEntry.AppItem,
    isOn: Boolean,
    onSetEnabled: () -> Unit,
) {
    val app = entry.app
    Column(Modifier.fillMaxWidth().background(AppColors.CardBg, cardShape(entry.cardTop, entry.cardBottom))) {
        Row(
            Modifier
                .fillMaxWidth()
                .let { mod ->
                    if (entry.locked) mod
                    else mod.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSetEnabled() }
                }
                .padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppIcon(app)
            Column(Modifier.weight(1f)) {
                Text(app.appName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(app.packageName, fontSize = 11.sp, color = AppColors.TextFaint,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 1.dp))
            }
            if (entry.locked) {
                Text("🔒", fontSize = 16.sp)
            } else {
                SlideToggle(isOn) { onSetEnabled() }
            }
        }
        if (entry.divider) Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.Divider))
    }
}

@Composable
private fun AppIcon(app: IranianAppInfo) {
    val bitmap = remember(app.packageName) { app.icon.toBitmap().asImageBitmap() }
    androidx.compose.foundation.Image(
        bitmap = bitmap,
        contentDescription = app.appName,
        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)),
    )
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
