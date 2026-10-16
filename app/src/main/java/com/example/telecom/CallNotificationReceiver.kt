package com.example.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.MainActivity

class CallNotificationReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_ANSWER = "com.example.telecom.ACTION_ANSWER_CALL"
        const val ACTION_DECLINE = "com.example.telecom.ACTION_DECLINE_CALL"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val receivedAction = intent?.action
        Log.d("CallNotificationReceiver", "Notification action received: $receivedAction")
        if (receivedAction == null) {
            Log.w("CallNotificationReceiver", "Received null action intent")
            return
        }
        when (receivedAction) {
            ACTION_ANSWER -> {
                Log.d("CallNotificationReceiver", "Handling ANSWER action")
                BrutalInCallService.answerCurrentCall()
                try {
                    val launchIntent = Intent(context, MainActivity::class.java).apply {
                        this.action = Intent.ACTION_MAIN
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    Log.d("CallNotificationReceiver", "Launching MainActivity")
                    context.startActivity(launchIntent)
                } catch (e: Exception) {
                    Log.e("CallNotificationReceiver", "Failed to launch MainActivity on answer: ${e.message}", e)
                }
            }
            ACTION_DECLINE -> {
                Log.d("CallNotificationReceiver", "Handling DECLINE action")
                BrutalInCallService.disconnectCurrentCall()
            }
            else -> {
                Log.w("CallNotificationReceiver", "Unknown action received: $receivedAction")
            }
        }
    }
}
