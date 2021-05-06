package com.hamibot.hamibot.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast

class Receiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.

        Toast.makeText(context, "Broadcast Receiver", Toast.LENGTH_SHORT).show()
        Log.d("hamibot", "Broadcast Receiver")

        val activityIntent = Intent(context, CommandService::class.java)
        activityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) context.startService(activityIntent)
    }
}