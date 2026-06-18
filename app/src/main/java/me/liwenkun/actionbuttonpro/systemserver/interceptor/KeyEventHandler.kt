package me.liwenkun.actionbuttonpro.systemserver.interceptor

import android.view.KeyEvent

interface KeyEventHandler {
    fun onKeyEvent(keyEvent: KeyEvent): Boolean
}