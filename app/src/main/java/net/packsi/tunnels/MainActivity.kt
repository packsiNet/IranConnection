package net.packsi.tunnels

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import net.packsi.tunnels.ads.AdManager
import net.packsi.tunnels.data.VpnStatus
import net.packsi.tunnels.data.VpnViewModel
import net.packsi.tunnels.data.auth.AuthViewModel
import net.packsi.tunnels.data.deviceauth.AppStartState
import net.packsi.tunnels.data.deviceauth.DeviceAuthViewModel
import net.packsi.tunnels.data.UpdateInfo
import net.packsi.tunnels.data.UpdateManager
import net.packsi.tunnels.ui.components.AppBottomNav
import net.packsi.tunnels.ui.components.NavTab
import net.packsi.tunnels.ui.components.UpdateDialog
import net.packsi.tunnels.ui.screens.AppsScreen
import net.packsi.tunnels.ui.screens.HomeScreen
import net.packsi.tunnels.ui.screens.LogScreen
import net.packsi.tunnels.ui.screens.ProfileScreen
import net.packsi.tunnels.ui.screens.SplashScreen
import net.packsi.tunnels.ui.theme.AppColors
import net.packsi.tunnels.ui.theme.SafeTunnelsTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        setContent {
            SafeTunnelsTheme {
                val authViewModel: DeviceAuthViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                            DeviceAuthViewModel(applicationContext) as T
                    }
                )
                val authState by authViewModel.state.collectAsState()

                when (authState.appStartState) {
                    AppStartState.CHECKING,
                    AppStartState.LOADING_CONFIG ->
                        SplashScreen(
                            completedSteps = authState.loadingCompletedSteps,
                            failedStep = authState.loadingFailedStep,
                        )
                    AppStartState.ERROR ->
                        SplashScreen(
                            message = authState.errorMessage ?: "Failed to connect. Please check your connection.",
                            isError = true,
                            onRetry = { authViewModel.retryLoadConfig() },
                            onSkip  = { authViewModel.forceReady() },
                            completedSteps = authState.loadingCompletedSteps,
                            failedStep = authState.loadingFailedStep,
                        )
                    AppStartState.READY ->
                        AppRoot(onSignOut = { authViewModel.signOut() })
                }
            }
        }
    }
}

@Composable
private fun AppRoot(
    onSignOut: () -> Unit = {},
    vm: VpnViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    var tab by remember { mutableStateOf(NavTab.HOME) }
    val context = LocalContext.current

    // Auth state — used to determine whether to show ads before connecting.
    val userAuthVm: AuthViewModel = viewModel()
    val userAuthState by userAuthVm.state.collectAsState()
    // Ads are shown only when the admin has enabled the ads system AND the user hasn't paid to remove them.
    val shouldShowAds = userAuthState.adsEnabled && (userAuthState.subscription?.showAds ?: true)

    // Preload the first interstitial ad as soon as the screen is ready.
    LaunchedEffect(Unit) {
        AdManager.preload(context)
    }

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

    // Whether the next VPN permission grant should start an ad-gated session.
    var pendingAdConnect by remember { mutableStateOf(false) }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (pendingAdConnect) {
                pendingAdConnect = false
                AdManager.showAd(context as Activity) { vm.startTunnelInAdMode() }
            } else {
                vm.startTunnel()
            }
        }
        pendingAdConnect = false
    }

    var showLogPanel by remember { mutableStateOf(false) }
    var openPayment by remember { mutableStateOf(false) }
    var openPaymentCurrency by remember { mutableStateOf("tmn") }
    var openNoAdsPayment by remember { mutableStateOf(false) }

    val buttonEnabled = state.status != VpnStatus.DISCONNECTING
    val onToggle: () -> Unit = {
        when (state.status) {
            VpnStatus.CONNECTED -> vm.stopTunnel()
            VpnStatus.CONNECTING -> vm.cancelConnecting()
            VpnStatus.DISCONNECTING -> {}
            else -> {
                // DISCONNECTED or FAILED
                val vpnIntent = VpnService.prepare(context)
                if (vpnIntent != null) {
                    // VPN permission not yet granted — request it first, then handle ad flow in launcher.
                    if (shouldShowAds) pendingAdConnect = true
                    vpnPermissionLauncher.launch(vpnIntent)
                } else if (shouldShowAds) {
                    // VPN permission already granted — show ad, then connect in ad-gated mode.
                    AdManager.showAd(context as Activity) { vm.startTunnelInAdMode() }
                } else {
                    vm.startTunnel()
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
                        onServerCardClick = {},
                        onShowLogs = { showLogPanel = true },
                        onGoToLogin = { tab = NavTab.PROFILE },
                        onGoToPayment = { currency -> tab = NavTab.PROFILE; openPayment = true; openPaymentCurrency = currency },
                        onGoToNoAdsPayment = { tab = NavTab.PROFILE; openNoAdsPayment = true },
                        configStatus = ConfigFetchStatus.Success,
                        buttonEnabled = buttonEnabled,
                        errorMessage = state.errorMessage,
                        browserVpnEnabled = state.browserVpnEnabled,
                        onBrowserVpnChange = { vm.setBrowserVpn(it) },
                        adSessionRemaining = state.adSessionRemaining,
                    )
                    NavTab.APPS -> AppsScreen(onClose = { tab = NavTab.HOME })
                    NavTab.PROFILE -> ProfileScreen(
                        onSignOut = onSignOut,
                        openPaymentOnLoad = openPayment,
                        onPaymentOpened = { openPayment = false },
                        initialPaymentCurrency = openPaymentCurrency,
                        openNoAdsPaymentOnLoad = openNoAdsPayment,
                        onNoAdsPaymentOpened = { openNoAdsPayment = false },
                    )
                }
            }
            AppBottomNav(
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.navigationBarsPadding(),
            )
        }

        if (showLogPanel) {
            LogScreen(onClose = { showLogPanel = false })
        }
    }
}

sealed class ConfigFetchStatus {
    object Loading : ConfigFetchStatus()
    object Success : ConfigFetchStatus()
    object Error : ConfigFetchStatus()
}
