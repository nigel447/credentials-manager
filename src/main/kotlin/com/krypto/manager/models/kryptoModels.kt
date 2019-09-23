package com.krypto.manager.models

data class KeyStoreWrapper(var symKePsswd: String, var keyStorePasswd: String, var keyStore:String )

data class CipherWithParams(val iv: ByteArray, val data: ByteArray)