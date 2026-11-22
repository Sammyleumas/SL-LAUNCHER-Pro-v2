package com.sllauncher.pro.assistant

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import com.sllauncher.pro.commands.ActionResult
import com.sllauncher.pro.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lets the SL assistant app run launcher actions. Protected by the
 * signature permission `com.sllauncher.pro.permission.EXECUTE_LAUNCHER_ACTIONS`:
 * only apps signed with the same key as SL Launcher can bind.
 *
 * Protocol (Messenger): send a Message with what = [MSG_EXECUTE], data =
 * Bundle { "action": String, "params": Bundle of String values } and
 * replyTo set. The reply has what = [MSG_RESULT] and data = Bundle {
 * "status": "success" | "needs_confirmation" | "needs_permission" | "failure",
 * "message": String }.
 */
@AndroidEntryPoint
class AssistantBridgeService : Service() {

    @Inject lateinit var bridge: LauncherActionBridge
    @Inject @field:ApplicationScope lateinit var scope: CoroutineScope

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what != MSG_EXECUTE) return
            val replyTo = msg.replyTo ?: return
            val data = msg.data
            val action = data.getString("action").orEmpty()
            val paramsBundle = data.getBundle("params") ?: Bundle()
            val params = paramsBundle.keySet().associateWith { paramsBundle.getString(it).orEmpty() }
            scope.launch {
                val result = bridge.execute(action, params)
                val reply = Message.obtain(null, MSG_RESULT).apply {
                    this.data = Bundle().apply {
                        putString("status", statusOf(result))
                        putString("message", result.message)
                    }
                }
                try {
                    replyTo.send(reply)
                } catch (e: RemoteException) {
                    Log.w("AssistantBridge", "SL app went away before the reply", e)
                }
            }
        }
    }

    private val messenger = Messenger(handler)

    override fun onBind(intent: Intent?): IBinder = messenger.binder

    companion object {
        const val MSG_EXECUTE = 1
        const val MSG_RESULT = 2

        fun statusOf(result: ActionResult) = when (result) {
            is ActionResult.Success -> "success"
            is ActionResult.NeedsConfirmation -> "needs_confirmation"
            is ActionResult.NeedsPermission -> "needs_permission"
            is ActionResult.Failure -> "failure"
        }
    }
}
