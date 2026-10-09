package com.example

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import com.example.data.model.OverlayMode
import com.example.ui.components.*
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

    override fun onResume() {
        super.onResume()
        // Check permissions dynamically on resume
        viewModel.updatePermissionStates()
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
        // Hardware volume key handler:
        // Volume Up to answer incoming call
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            val active = viewModel.activeCall.value
            if (active != null && active.callState == com.example.data.model.CallState.RINGING) {
                viewModel.onHardwareVolumeUp()
                return true
            }
        }
        // Headset hook to end active call
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
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activeCall by viewModel.activeCall.collectAsStateWithLifecycle()

    // Listen to toasts
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Handle back button when Settings is open
    BackHandler(enabled = isSettingsOpen) {
        viewModel.closeSettings()
    }

    if (isSettingsOpen) {
        SettingsScreen(
            viewModel = viewModel,
            onBack = { viewModel.closeSettings() }
        )
    } else {
        val ussdSession by viewModel.ussdSession.collectAsState()
        val isFullScreenCall = activeCall != null && activeCall?.overlayMode == OverlayMode.FULL_SCREEN

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(NeobrutalBgLight)
                .statusBarsPadding(),
            containerColor = NeobrutalBgLight,
            bottomBar = {
                if (!isFullScreenCall) {
                    // 3 Clean Tabs: Recents, Keypad, Contacts
                    CleanBottomNav(
                        selectedTab = currentTab,
                        onSelectTab = { viewModel.selectTab(it) }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isFullScreenCall) PaddingValues(0.dp) else innerPadding)
            ) {
                when (currentTab) {
                    DialerTab.RECENTS -> RecentsScreen(viewModel = viewModel)
                    DialerTab.KEYPAD -> KeypadScreen(viewModel = viewModel)
                    DialerTab.CONTACTS -> ContactsScreen(viewModel = viewModel)
                }

                // Clean Floating Settings Button at Top Right without any black row (only when not in full screen call)
                if (!isFullScreenCall) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 14.dp)
                    ) {
                        NeobrutalIconButton(
                            onClick = { viewModel.openSettings() },
                            containerColor = NeobrutalYellow,
                            size = 38.dp,
                            shadowOffset = 2.dp,
                            testTag = "open_settings_button",
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = NeobrutalBlack,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }

                // In-Call HUD Overlay
                if (activeCall != null) {
                    ActiveCallContainer(viewModel = viewModel)
                }

                // Global USSD Carrier Themed Dialog
                if (ussdSession != null) {
                    val session = ussdSession!!
                    NeobrutalDialog(
                        onDismissRequest = { viewModel.dismissUssd() },
                        title = "USSD CARRIER CODE",
                        titleColor = NeobrutalCyan
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "CODE: ${session.code}",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = NeobrutalBlack
                                )
                            )

                            if (session.isRunning) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = NeobrutalBlack,
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "SENDING MMI CODE TO CARRIER...",
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = NeobrutalBlack
                                        )
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(NeobrutalBgLight)
                                        .border(BorderStroke(2.dp, NeobrutalBlack))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = session.responseText ?: "Carrier service request completed.",
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = NeobrutalBlack
                                        )
                                    )
                                }
                            }

                            NeobrutalButton(
                                onClick = { viewModel.dismissUssd() },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                text = "DISMISS",
                                containerColor = NeobrutalYellow,
                                shadowOffset = 4.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CleanBottomNav(
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
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
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
                accentColor = NeobrutalYellow,
                onClick = { onSelectTab(DialerTab.CONTACTS) },
                testTag = "nav_contacts"
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
        Row(
            modifier = Modifier
                .background(bgColor)
                .border(
                    BorderStroke(
                        if (isSelected) 2.dp else 0.dp,
                        if (isSelected) NeobrutalBlack else Color.Transparent
                    )
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = NeobrutalBlack,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = NeobrutalBlack
                )
            )
        }
    }
}
