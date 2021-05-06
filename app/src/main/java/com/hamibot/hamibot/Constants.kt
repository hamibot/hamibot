package com.hamibot.hamibot

import io.socket.client.Socket

object Constants {
    const val LOG_TAG = "[h4m1]"

    // Socket
    @kotlin.jvm.JvmField
    var socket: Socket? = null
    const val WS_SERVER = ""
    var WS_TOKEN: String? = ""
}