package com.iranconnection.app

import android.app.Activity
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iranconnection.app.data.ConfigFetcher
import com.iranconnection.app.data.VpnStatus
import com.iranconnection.app.data.VpnViewModel
import com.iranconnection.app.data.WireGuardConfig
import com.iranconnection.app.ui.components.AppBottomNav
import com.iranconnection.app.ui.components.NavTab
import com.iranconnection.app.ui.screens.AppsScreen
import com.iranconnection.app.ui.screens.HomeScreen
import com.iranconnection.app.ui.screens.ServersScreen
import com.iranconnection.app.ui.theme.AppColors
import com.iranconnection.app.ui.theme.IranConnectionTheme
import kotlinx.coroutines.launch

private const val CONFIG_URL = "https://gist.githubusercontent.com/packsiNet/4358f6d56dcb7cceefb38f6e3a7573ba/raw/7844e4b2e41765585cba6bb1a2745a61eb42c392/config.json"

class MainActivity : ComponentActivity() {

    private var configStatus: ConfigFetchStatus by mutableStateOf(ConfigFetchStatus.Loading)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val config = ConfigFetcher.fetch(CONFIG_URL)
            if (config != null) {
                saveConfig(config)
                configStatus = ConfigFetchStatus.Success
            } else {
                configStatus = ConfigFetchStatus.Error
            }
        }

        setContent {
            IranConnectionTheme {
                AppRoot(configStatus = configStatus)
            }
        }
    }

    private fun saveConfig(config: WireGuardConfig) {
        getSharedPreferences("wireguard", MODE_PRIVATE).edit().apply {
            putString("endpoint", config.serverEndpoint)
            putString("server_pub_key", config.serverPublicKey)
            putString("client_priv_key", config.clientPrivateKey)
            putString("address", config.clientAddress)
            putString("dns", config.dns)
            apply()
        }
    }
}

@Composable
private fun AppRoot(configStatus: ConfigFetchStatus, vm: VpnViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var tab by remember { mutableStateOf(NavTab.HOME) }
    val context = LocalContext.current

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            vm.startTunnel()
        }
    }

    val onToggle: () -> Unit = {
        if (state.status == VpnStatus.CONNECTED || state.status == VpnStatus.CONNECTING) {
            vm.stopTunnel()
        } else {
            val intent = VpnService.prepare(context)
            if (intent == null) {
                vm.startTunnel()
            } else {
                vpnPermissionLauncher.launch(intent)
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppColors.ScreenBg)
            .statusBarsPadding(),
    ) {
        ConfigStatusBanner(configStatus)
        Column(Modifier.weight(1f)) {
            when (tab) {
                NavTab.HOME -> HomeScreen(
                    connected = state.connected,
                    statusLabel = state.statusLabel,
                    seconds = state.seconds,
                    onToggle = onToggle,
                    onServerCardClick = { tab = NavTab.SERVERS },
                )
                NavTab.SERVERS -> ServersScreen(
                    connectedId = state.selectedServerId,
                    onSelect = vm::selectServer,
                    onClose = { tab = NavTab.HOME },
                )
                NavTab.APPS -> AppsScreen(
                    enabled = state.enabledApps,
                    onToggle = vm::toggleApp,
                    onClose = { tab = NavTab.HOME },
                )
            }
        }
        AppBottomNav(
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}

@Composable
private fun ConfigStatusBanner(status: ConfigFetchStatus) {
    val entry = when (status) {
        ConfigFetchStatus.Loading -> "Fetching config…" to Color(0xFF888888)
        ConfigFetchStatus.Success -> "Config loaded" to Color(0xFF4BBDB8)
        ConfigFetchStatus.Error -> "Config unavailable" to Color(0xFFEF4444)
    }
    Text(
        text = entry.first,
        color = Color.White,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(entry.second)
            .padding(vertical = 4.dp),
    )
}

sealed class ConfigFetchStatus {
    object Loading : ConfigFetchStatus()
    object Success : ConfigFetchStatus()
    object Error : ConfigFetchStatus()
}
