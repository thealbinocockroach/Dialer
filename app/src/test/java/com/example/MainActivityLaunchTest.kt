package com.example

import androidx.test.core.app.ActivityScenario
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.assertNotNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityLaunchTest {

    @Test
    fun testMainActivityLaunch() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
            }
        }
    }

    @Test
    fun testRecentsWithMultipleCallLogs() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<BrutalDialApplication>()
        kotlinx.coroutines.runBlocking {
            app.database.callLogDao().insert(
                com.example.data.model.CallLogEntity(
                    number = "1234567890",
                    cachedName = "Test 1",
                    callType = com.example.data.model.CallType.INCOMING,
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = 10
                )
            )
            app.database.callLogDao().insert(
                com.example.data.model.CallLogEntity(
                    number = "0987654321",
                    cachedName = "Test 2",
                    callType = com.example.data.model.CallType.INCOMING,
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = 20
                )
            )
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
            }
        }
    }
}
