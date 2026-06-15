package com.iranconnection.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iranconnection.app.data.VpnViewModel
import com.iranconnection.app.ui.components.AppBottomNav
import com.iranconnection.app.ui.components.NavTab
import com.iranconnection.app.ui.screens.AppsScreen
import com.iranconnection.app.ui.screens.HomeScreen
import com.iranconnection.app.ui.screens.ServersScreen
import com.iranconnection.app.ui.theme.AppColors
import com.iranconnection.app.ui.theme.IranConnectionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            IranConnectionTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot(vm: VpnViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var tab by remember { mutableStateOf(NavTab.HOME) }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppColors.ScreenBg)
            .statusBarsPadding(),
    ) {
        Column(Modifier.weight(1f)) {
            when (tab) {
                NavTab.HOME -> HomeScreen(
                    connected = state.connected,
                    seconds = state.seconds,
                    onToggle = vm::toggleConnection,
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
