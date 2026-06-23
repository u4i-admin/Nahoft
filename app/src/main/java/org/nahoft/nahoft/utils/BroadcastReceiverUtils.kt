package org.nahoft.nahoft.utils

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter

@SuppressLint("UnspecifiedRegisterReceiverFlag")
fun Context.registerReceiverCompat(
    receiver: BroadcastReceiver,
    filter: IntentFilter,
    exported: Boolean = false
)
{
    registerReceiver(
        receiver,
        filter,
        if (exported) Context.RECEIVER_EXPORTED else Context.RECEIVER_NOT_EXPORTED
    )
}