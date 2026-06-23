package net.packsi.tunnels.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.packsi.tunnels.ui.components.CountryFlag
import net.packsi.tunnels.ui.components.SignalBars
import net.packsi.tunnels.ui.theme.AppColors

data class Server(
    val id: String,
    val name: String,
    val ip: String,
    val ping: Int,
    val signal: Int,
    val type: String,
)

private val autoServers = listOf(
    Server("us", "United States", "237.147.180.65", 14, 3, "auto"),
)
private val freeServers = listOf(
    Server("ar", "Argentina", "224.72.248.103", 28, 2, "free"),
    Server("fi", "Finland", "224.72.248.103", 102, 2, "free"),
)
private val premServers = listOf(
    Server("au", "Australia", "77.32.214.194", 120, 1, "premium"),
    Server("nl", "Netherlands", "94.177.206.149", 143, 2, "premium"),
    Server("ca", "Canada", "237.147.180.65", 102, 3, "premium"),
    Server("uk", "UK", "77.32.214.194", 88, 2, "premium"),
)

@Composable
fun ServersScreen(
    connectedId: String?,
    onSelect: (String?) -> Unit,
    onClose: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()

    fun filter(list: List<Server>) = list.filter { q.isEmpty() || it.name.lowercase().contains(q) }
    val auto = filter(autoServers)
    val free = filter(freeServers)
    val prem = filter(premServers)
    val hasResults = auto.isNotEmpty() || free.isNotEmpty() || prem.isNotEmpty()

    Column(Modifier.fillMaxSize().background(AppColors.ScreenBg)) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("All Server", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, letterSpacing = (-0.5).sp)
            CloseButton(onClose)
        }

        SearchBar(query, { query = it }, "Search server here...", Modifier.padding(horizontal = 18.dp, vertical = 14.dp))

        if (!hasResults) {
            NoResults("No servers found", "Try a different search term")
            return@Column
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp),
        ) {
            if (auto.isNotEmpty()) section("Automatic server", auto, connectedId, onSelect)
            if (free.isNotEmpty()) section("Free server", free, connectedId, onSelect)
            if (prem.isNotEmpty()) section("Premium server", prem, connectedId, onSelect)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    label: String,
    servers: List<Server>,
    connectedId: String?,
    onSelect: (String?) -> Unit,
) {
    item {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AppColors.TextMuted, modifier = Modifier.padding(bottom = 6.dp))
    }
    item {
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(13.dp))
                .background(AppColors.CardBg, RoundedCornerShape(13.dp)),
        ) {
            servers.forEachIndexed { i, s ->
                ServerRow(s, connectedId == s.id, i == servers.lastIndex, onSelect)
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun ServerRow(server: Server, isConnected: Boolean, isLast: Boolean, onSelect: (String?) -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CountryFlag(server.id, size = 38.dp)
            Column(Modifier.weight(1f)) {
                Text(server.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                Row(
                    Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text("IP ${server.ip}", fontSize = 11.sp, color = AppColors.TextMuted)
                    Text("•", fontSize = 11.sp, color = AppColors.Chevron)
                    Text("${server.ping}ms", fontSize = 11.sp, color = AppColors.TextMuted)
                    SignalBars(server.signal, Modifier.size(12.dp, 9.dp))
                }
            }
            if (isConnected) {
                Box(
                    Modifier
                        .background(AppColors.RedPink, RoundedCornerShape(20.dp))
                        .clickable { onSelect(null) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("Disconnect", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            } else {
                Row(
                    Modifier
                        .border(1.5.dp, AppColors.Teal, RoundedCornerShape(20.dp))
                        .clickable { onSelect(server.id) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Teal)
                    if (server.type == "premium") Text("👑", fontSize = 13.sp)
                }
            }
        }
        if (!isLast) Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.Divider))
    }
}

@Composable
fun CloseButton(onClose: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .shadow(4.dp, CircleShape)
            .background(AppColors.CardBg, CircleShape)
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Text("×", fontSize = 20.sp, color = Color(0xFF7A82A0))
    }
}

@Composable
fun SearchBar(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(14.dp))
            .background(AppColors.CardBg, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        androidx.compose.foundation.Canvas(Modifier.size(18.dp)) {
            val u = size.width / 18f
            drawCircle(AppColors.TextMuted, radius = 5.5f * u, center = androidx.compose.ui.geometry.Offset(8 * u, 8 * u), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6f * u))
            drawLine(AppColors.TextMuted, androidx.compose.ui.geometry.Offset(12.5f * u, 12.5f * u), androidx.compose.ui.geometry.Offset(16 * u, 16 * u), strokeWidth = 1.6f * u, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, fontSize = 15.sp, color = AppColors.TextMuted)
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                textStyle = TextStyle(fontSize = 15.sp, color = AppColors.TextPrimary),
                cursorBrush = SolidColor(AppColors.Teal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun NoResults(title: String, subtitle: String? = null) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        androidx.compose.foundation.Canvas(Modifier.size(52.dp)) {
            val u = size.width / 52f
            drawCircle(AppColors.Chevron, radius = 14 * u, center = androidx.compose.ui.geometry.Offset(23 * u, 23 * u), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2 * u))
            drawLine(AppColors.Chevron, androidx.compose.ui.geometry.Offset(34 * u, 34 * u), androidx.compose.ui.geometry.Offset(45 * u, 45 * u), strokeWidth = 2 * u, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = AppColors.TextMuted)
        if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = AppColors.TextFaint)
    }
}
