package com.example

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.NeobrutalAppBar
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DialerTab
import com.example.ui.viewmodel.DialerViewModel
import com.example.ui.viewmodel.DialerViewModelFactory
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: DialerViewModel by viewModels {
        DialerViewModelFactory(application as BrutalDialApplication)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming dial intent
        handleDialIntent(intent)

        setContent {
            BrutalDialTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDialIntent(intent)
    }

    private fun handleDialIntent(intent: Intent?) {
        val uri = intent?.data
        if (uri != null && uri.scheme == "tel") {
            val schemeSpecific = uri.schemeSpecificPart
            if (!schemeSpecific.isNullOrBlank()) {
                viewModel.setDialedInput(schemeSpecific)
                viewModel.selectTab(DialerTab.KEYPAD)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Hardware button callback handlers:
        // Volume Up to answer incoming call
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            val active = viewModel.activeCall.value
            if (active != null && active.callState == com.example.data.model.CallState.RINGING) {
                viewModel.onHardwareVolumeUp()
                return true
            }
        }
        // Power/Headset hook to end call
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
            if (viewModel.activeCall.value != null) {
                viewModel.onHardwarePowerOrEnd()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun MainAppContent(viewModel: DialerViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activeCall by viewModel.activeCall.collectAsStateWithLifecycle()

    // Listen to toasts
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(NeobrutalBgLight),
        topBar = {
            NeobrutalAppBar(
                title = "BRUTAL DIAL",
                backgroundColor = NeobrutalYellow,
                actions = {
                    Box(
                        modifier = Modifier
                            .background(NeobrutalBlack)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "NEOBRUTAL",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                color = NeobrutalYellow
                            )
                        )
                    }
                }
            )
        },
        bottomBar = {
            NeobrutalBottomNav(
                selectedTab = currentTab,
                onSelectTab = { viewModel.selectTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main tab view
            when (currentTab) {
                DialerTab.SPEED_DIAL -> SpeedDialScreen(viewModel = viewModel)
                DialerTab.RECENTS -> RecentsScreen(viewModel = viewModel)
                DialerTab.KEYPAD -> KeypadScreen(viewModel = viewModel)
                DialerTab.CONTACTS -> ContactsScreen(viewModel = viewModel)
                DialerTab.RECORDINGS -> RecordingsScreen(viewModel = viewModel)
                DialerTab.SETTINGS -> SettingsBlocklistScreen(viewModel = viewModel)
            }

            // In-Call HUD Overlay (covers or floats above UI depending on mode)
            if (activeCall != null) {
                ActiveCallContainer(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun NeobrutalBottomNav(
    selectedTab: DialerTab,
    onSelectTab: (DialerTab) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Heavy 3.dp top border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(NeobrutalBlack)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NeobrutalWhite)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NavTabItem(
                title = "SPEED",
                icon = Icons.Default.Bolt,
                isSelected = selectedTab == DialerTab.SPEED_DIAL,
                accentColor = NeobrutalYellow,
                onClick = { onSelectTab(DialerTab.SPEED_DIAL) },
                testTag = "nav_speed_dial"
            )
            NavTabItem(
                title = "RECENTS",
                icon = Icons.Default.History,
                isSelected = selectedTab == DialerTab.RECENTS,
                accentColor = NeobrutalCyan,
                onClick = { onSelectTab(DialerTab.RECENTS) },
                testTag = "nav_recents"
            )
            NavTabItem(
                title = "KEYPAD",
                icon = Icons.Default.Dialpad,
                isSelected = selectedTab == DialerTab.KEYPAD,
                accentColor = NeobrutalGreen,
                onClick = { onSelectTab(DialerTab.KEYPAD) },
                testTag = "nav_keypad"
            )
            NavTabItem(
                title = "CONTACTS",
                icon = Icons.Default.People,
                isSelected = selectedTab == DialerTab.CONTACTS,
                accentColor = NeobrutalPink,
                onClick = { onSelectTab(DialerTab.CONTACTS) },
                testTag = "nav_contacts"
            )
            NavTabItem(
                title = "AUDIO",
                icon = Icons.Default.Mic,
                isSelected = selectedTab == DialerTab.RECORDINGS,
                accentColor = NeobrutalPurple,
                onClick = { onSelectTab(DialerTab.RECORDINGS) },
                testTag = "nav_recordings"
            )
            NavTabItem(
                title = "BLOCK",
                icon = Icons.Default.Shield,
                isSelected = selectedTab == DialerTab.SETTINGS,
                accentColor = NeobrutalOrange,
                onClick = { onSelectTab(DialerTab.SETTINGS) },
                testTag = "nav_settings"
            )
        }
    }
}

@Composable
private fun NavTabItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    val bgColor = if (isSelected) accentColor else Color.Transparent
    val shadowOffset = if (isSelected) 2.dp else 0.dp

    Box(
        modifier = Modifier
            .padding(end = shadowOffset, bottom = shadowOffset)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 2.dp, y = 2.dp)
                    .background(NeobrutalBlack)
            )
        }
        Column(
            modifier = Modifier
                .background(bgColor)
                .border(
                    BorderStroke(
                        if (isSelected) 2.dp else 0.dp,
                        if (isSelected) NeobrutalBlack else Color.Transparent
                    )
                )
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = NeobrutalBlack,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp,
                    color = NeobrutalBlack
                )
            )
        }
    }
}
