package org.nahoft.Nahoft.utils

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build

@SuppressLint("UnspecifiedRegisterReceiverFlag")
fun Context.registerReceiverCompat(
    receiver: BroadcastReceiver,
    filter: IntentFilter,
    exported: Boolean = false
)
{
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
    {
        registerReceiver(
            receiver,
            filter,
            if (exported) Context.RECEIVER_EXPORTED else Context.RECEIVER_NOT_EXPORTED
        )
    }
    else
    {
        registerReceiver(receiver, filter)
    }
}