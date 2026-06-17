package com.iranconnection.app

import android.app.Activity
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.iranconnection.app.data.auth.ApiClient
import com.iranconnection.app.data.UpdateInfo
import com.iranconnection.app.data.UpdateManager
import com.iranconnection.app.data.VpnStatus
import com.iranconnection.app.data.VpnViewModel
import com.iranconnection.app.data.WireGuardConfig
import com.iranconnection.app.ui.components.AppBottomNav
import com.iranconnection.app.ui.components.NavTab
import com.iranconnection.app.ui.components.UpdateDialog
import com.iranconnection.app.ui.screens.AppsScreen
import com.iranconnection.app.ui.screens.BrowserScreen
import com.iranconnection.app.ui.screens.HomeScreen
import com.iranconnection.app.ui.screens.LogScreen
import com.iranconnection.app.ui.screens.ProfileScreen
import com.iranconnection.app.ui.screens.ServersScreen
import com.iranconnection.app.ui.theme.AppColors
import com.iranconnection.app.ui.theme.IranConnectionTheme
import kotlinx.coroutines.launch

private const val CONFIG_URL = "https://gist.githubusercontent.com/packsiNet/4358f6d56dcb7cceefb38f6e3a7573ba/raw/config.json"

class MainActivity : ComponentActivity() {

    private var configStatus: ConfigFetchStatus by mutableStateOf(ConfigFetchStatus.Loading)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Light system bars = dark status/nav icons, so they stay visible on the app's light background.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        // Build the HTTP/auth layer once before any screen/ViewModel uses it.
        ApiClient.init(applicationContext)

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
            val iranianApps = config.iranianApps
            if (iranianApps != null && iranianApps.isNotEmpty()) {
                putString("iranian_apps", iranianApps.joinToString(","))
            }
            apply()
        }
    }
}

@Composable
private fun AppRoot(configStatus: ConfigFetchStatus, vm: VpnViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var tab by remember { mutableStateOf(NavTab.HOME) }
    val context = LocalContext.current

    var updateInfo by remember { mutableStateOf<UpdateInfo>(UpdateInfo.UpToDate) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val currentVersion = context.packageManager
            .getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        val result = UpdateManager.checkForUpdate(currentVersion)
        if (result is UpdateInfo.UpdateAvailable) {
            updateInfo = result
            showUpdateDialog = true
        }
    }

    if (showUpdateDialog && updateInfo is UpdateInfo.UpdateAvailable) {
        val info = updateInfo as UpdateInfo.UpdateAvailable
        UpdateDialog(
            newVersion = info.newVersion,
            downloadUrl = info.downloadUrl,
            onDismiss = { showUpdateDialog = false },
        )
    }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            vm.startTunnel()
        }
    }

    var showLogPanel by remember { mutableStateOf(false) }

    // Button stays tappable while connecting so the user can cancel a connect attempt that's
    // taking too long; it's only disabled mid-disconnect, where there's nothing left to cancel.
    val buttonEnabled = state.status != VpnStatus.DISCONNECTING
    val onToggle: () -> Unit = {
        when (state.status) {
            VpnStatus.CONNECTED -> vm.stopTunnel()
            VpnStatus.CONNECTING -> vm.cancelConnecting()
            VpnStatus.DISCONNECTING -> { /* in transition — ignore */ }
            else -> {
                val intent = VpnService.prepare(context)
                if (intent == null) {
                    vm.startTunnel()
                } else {
                    vpnPermissionLauncher.launch(intent)
                }
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(AppColors.ScreenBg)
            .statusBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f)) {
            when (tab) {
                NavTab.HOME -> HomeScreen(
                    connected = state.connected,
                    statusLabel = state.statusLabel,
                    seconds = state.seconds,
                    serverIp = state.serverIp,
                    onToggle = onToggle,
                    onServerCardClick = { },
                    onShowLogs = { showLogPanel = true },
                    configStatus = configStatus,
                    buttonEnabled = buttonEnabled,
                )
                NavTab.APPS -> AppsScreen(
                    onClose = { tab = NavTab.HOME },
                )
                NavTab.BROWSER -> BrowserScreen()
                NavTab.PROFILE -> ProfileScreen()
            }
        }
        AppBottomNav(
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.navigationBarsPadding(),
        )
        } // end inner Column

        if (showLogPanel) {
            LogScreen(onClose = { showLogPanel = false })
        }
    } // end Box
}

sealed class ConfigFetchStatus {
    object Loading : ConfigFetchStatus()
    object Success : ConfigFetchStatus()
    object Error : ConfigFetchStatus()
}
