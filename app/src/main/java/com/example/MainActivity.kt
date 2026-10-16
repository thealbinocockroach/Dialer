package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
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
        try {
            enableEdgeToEdge()

            // Allow app to wake screen and show when locked or on top of other apps
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                try {
                    setShowWhenLocked(true)
                    setTurnScreenOn(true)
                } catch (e: Throwable) {
                    android.util.Log.w("MainActivity", "Lock screen display setup: ${e.message}")
                }
            }
            @Suppress("DEPRECATION")
            try {
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
            } catch (e: Throwable) {
                android.util.Log.w("MainActivity", "Window flags setup: ${e.message}")
            }

            // Handle incoming dial intent
            handleDialIntent(intent)
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Error in onCreate", e)
        }

        setContent {
            BrutalDialTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            viewModel.setAppInForeground(true)
            viewModel.updatePermissionStates()
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Error in onResume", e)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            viewModel.setAppInForeground(false)
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Error in onPause", e)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        try {
            setIntent(intent)
            handleDialIntent(intent)
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Error in onNewIntent", e)
        }
    }

    private fun handleDialIntent(intent: Intent?) {
        try {
            val uri = intent?.data
            if (uri != null && uri.scheme == "tel") {
                val schemeSpecific = uri.schemeSpecificPart
                if (!schemeSpecific.isNullOrBlank()) {
                    viewModel.setDialedInput(schemeSpecific)
                    viewModel.selectTab(DialerTab.KEYPAD)
                }
            }

            if (intent?.getBooleanExtra("EXTRA_INCOMING_CALL", false) == true) {
                val incomingNum = intent.getStringExtra("EXTRA_INCOMING_NUMBER") ?: ""
                if (incomingNum.isNotBlank()) {
                    viewModel.onIncomingCallDetected(incomingNum)
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Error in handleDialIntent", e)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        try {
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
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Error in onKeyDown", e)
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
    val isDefaultDialer by viewModel.isDefaultDialer.collectAsStateWithLifecycle()
    val canDrawOverlays by viewModel.canDrawOverlays.collectAsStateWithLifecycle()
    val hasContactsPermission by viewModel.hasContactsPermission.collectAsStateWithLifecycle()
    val hasCallLogPermission by viewModel.hasCallLogPermission.collectAsStateWithLifecycle()

    val permissionsToRequest = remember {
        val list = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            list.add(Manifest.permission.ANSWER_PHONE_CALLS)
        }
        list.toTypedArray()
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        viewModel.onPermissionsBatchResult(allGranted)
    }

    val defaultDialerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.updatePermissionStates()
    }

    // Every permission required from a phone app is asked on start up
    LaunchedEffect(Unit) {
        try {
            val anyMissing = permissionsToRequest.any { perm ->
                ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
            }
            if (anyMissing) {
                permissionsLauncher.launch(permissionsToRequest)
            }
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Permission launch error: ${e.message}")
        }
    }

    // Listen to toasts
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { msg ->
            try {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            } catch (_: Throwable) {}
        }
    }

    // Handle back button when Settings is open
    BackHandler(enabled = isSettingsOpen) {
        viewModel.closeSettings()
    }

    // Handle back button when Keypad is open (returns to Recents)
    BackHandler(enabled = !isSettingsOpen && currentTab == DialerTab.KEYPAD) {
        viewModel.selectTab(DialerTab.RECENTS)
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
                // There shouldn't be a keypad bar: only Recents and Contacts tabs when not on Keypad
                if (!isFullScreenCall && currentTab != DialerTab.KEYPAD) {
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
                Column(modifier = Modifier.fillMaxSize()) {
                    // Default phone app & permissions prompt banner if action required
                    if (!isFullScreenCall && (!isDefaultDialer || !hasContactsPermission || !hasCallLogPermission || !canDrawOverlays)) {
                        DefaultPhoneAppBanner(
                            isDefaultDialer = isDefaultDialer,
                            hasMissingPermissions = !hasContactsPermission || !hasCallLogPermission,
                            canDrawOverlays = canDrawOverlays,
                            onSetDefault = {
                                try {
                                    val intent = viewModel.getDefaultDialerIntent()
                                    if (intent != null) {
                                        defaultDialerLauncher.launch(intent)
                                    } else {
                                        viewModel.updatePermissionStates()
                                    }
                                } catch (e: Throwable) {
                                    android.util.Log.e("MainActivity", "Failed to launch default dialer intent: ${e.message}")
                                }
                            },
                            onGrantPermissions = {
                                try {
                                    permissionsLauncher.launch(permissionsToRequest)
                                } catch (e: Throwable) {
                                    android.util.Log.e("MainActivity", "Failed to launch permissions request: ${e.message}")
                                }
                            },
                            onEnableOverlay = {
                                try {
                                    val intent = viewModel.getOverlaySettingsIntent()
                                    context.startActivity(intent)
                                } catch (e: Throwable) {
                                    android.util.Log.e("MainActivity", "Failed to launch overlay settings: ${e.message}")
                                    try {
                                        val fallback = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(fallback)
                                    } catch (_: Throwable) {}
                                }
                            }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (currentTab) {
                            DialerTab.RECENTS -> RecentsScreen(viewModel = viewModel)
                            DialerTab.KEYPAD -> KeypadScreen(viewModel = viewModel)
                            DialerTab.CONTACTS -> ContactsScreen(viewModel = viewModel)
                        }
                    }
                }

                // Floating Keypad Button that opens the Keypad (when on Recents or Contacts)
                if (!isFullScreenCall && currentTab != DialerTab.KEYPAD) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 20.dp, bottom = 20.dp)
                    ) {
                        NeobrutalFloatingKeypadButton(
                            onClick = { viewModel.selectTab(DialerTab.KEYPAD) }
                        )
                    }
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
fun DefaultPhoneAppBanner(
    isDefaultDialer: Boolean,
    hasMissingPermissions: Boolean,
    canDrawOverlays: Boolean,
    onSetDefault: () -> Unit,
    onGrantPermissions: () -> Unit,
    onEnableOverlay: () -> Unit
) {
    NeobrutalCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        containerColor = NeobrutalYellow,
        shadowOffset = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhoneInTalk,
                        contentDescription = null,
                        tint = NeobrutalBlack,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            !isDefaultDialer -> "DEFAULT PHONE APP SETUP"
                            !canDrawOverlays -> "ENABLE ON OTHER APPS"
                            else -> "PERMISSIONS REQUIRED"
                        },
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = NeobrutalBlack
                        )
                    )
                }

                NeobrutalBadge(
                    text = when {
                        !isDefaultDialer -> "DEFAULT DIALER"
                        !canDrawOverlays -> "OVERLAY"
                        else -> "PERMISSIONS"
                    },
                    color = NeobrutalPink
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = when {
                    !isDefaultDialer -> "Set Brutal Dial as your default phone app to handle all calls, audio recording, and spam protection."
                    !canDrawOverlays -> "Allow 'Display over other apps' so Brutal Dial immediately opens and shows who's calling while you are using any other app."
                    else -> "Grant phone, call log, audio, and contacts permissions for full dialer features."
                },
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = NeobrutalBlack
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isDefaultDialer) {
                    NeobrutalButton(
                        onClick = onSetDefault,
                        modifier = Modifier.weight(1f).height(38.dp),
                        text = "SET AS DEFAULT",
                        containerColor = NeobrutalCyan,
                        shadowOffset = 2.dp,
                        testTag = "set_as_default_dialer_btn"
                    )
                }
                if (!canDrawOverlays) {
                    NeobrutalButton(
                        onClick = onEnableOverlay,
                        modifier = Modifier.weight(1f).height(38.dp),
                        text = "ALLOW ON OTHER APPS",
                        containerColor = NeobrutalPink,
                        shadowOffset = 2.dp,
                        testTag = "enable_overlay_btn"
                    )
                }
                if (hasMissingPermissions) {
                    NeobrutalButton(
                        onClick = onGrantPermissions,
                        modifier = Modifier.weight(1f).height(38.dp),
                        text = "PERMISSIONS",
                        containerColor = NeobrutalGreen,
                        shadowOffset = 2.dp,
                        testTag = "grant_all_permissions_btn"
                    )
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
                .padding(horizontal = 16.dp, vertical = 6.dp),
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
