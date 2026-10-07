package com.halil.ozel.huaweipushkitapp

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.text.DateFormat
import java.util.Date
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.common.ApiException
import com.huawei.hms.push.HmsMessaging

class MainActivity : AppCompatActivity() {
    private lateinit var store: PushStore
    private lateinit var tokenValue: TextView
    private lateinit var statusText: TextView
    private lateinit var messageLog: TextView

    private val storeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == PushStore.KEY_TOKEN || key == PushStore.KEY_MESSAGES) {
            runOnUiThread { showStoredState() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        store = PushStore(this)
        tokenValue = findViewById(R.id.tokenValue)
        statusText = findViewById(R.id.statusText)
        messageLog = findViewById(R.id.messageLog)
        findViewById<Button>(R.id.refreshToken).setOnClickListener { refreshToken() }
        findViewById<Button>(R.id.deleteToken).setOnClickListener { deleteToken() }
        findViewById<Button>(R.id.subscribeTopic).setOnClickListener { changeTopic(subscribe = true) }
        findViewById<Button>(R.id.unsubscribeTopic).setOnClickListener { changeTopic(subscribe = false) }
        findViewById<Button>(R.id.turnOnPush).setOnClickListener { setPushEnabled(true) }
        findViewById<Button>(R.id.turnOffPush).setOnClickListener { setPushEnabled(false) }
        findViewById<Button>(R.id.clearMessages).setOnClickListener {
            store.clearMessages()
            statusText.setText(R.string.status_log_cleared)
        }
        showStoredState()
        requestNotificationPermission()
    }

    override fun onStart() {
        super.onStart()
        store.listen(storeListener)
        showStoredState()
    }

    override fun onStop() {
        store.unlisten(storeListener)
        super.onStop()
    }

    private fun showStoredState() {
        showToken()
        showMessages()
    }

    private fun showToken() {
        val token = store.token()
        tokenValue.text = if (token.isEmpty()) getString(R.string.token_empty) else token
    }

    private fun showMessages() {
        val messages = store.messages()
        if (messages.isEmpty()) {
            messageLog.setText(R.string.messages_empty)
            return
        }
        val format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
        messageLog.text = messages.joinToString(separator = "\n\n") { message ->
            val whenText = format.format(Date(message.atMillis))
            val title = message.title.ifEmpty { getString(R.string.message_untitled) }
            val body = message.body.ifEmpty { getString(R.string.message_no_body) }
            getString(
                R.string.message_entry,
                whenText,
                message.kind,
                message.channelId,
                title,
                body,
            )
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) {
            return
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_NOTIFICATIONS,
        )
    }

    private fun refreshToken() {
        statusText.setText(R.string.status_idle)
        object : Thread() {
            override fun run() {
                try {
                    val appId = AGConnectOptionsBuilder().build(this@MainActivity)
                        .getString(APP_ID)
                    val token = HmsInstanceId.getInstance(this@MainActivity).getToken(appId, HCM)
                    store.saveToken(token)
                    runOnUiThread { statusText.setText(R.string.status_token_ready) }
                } catch (error: ApiException) {
                    runOnUiThread {
                        statusText.text = getString(R.string.status_token_failed, error.message)
                    }
                }
            }
        }.start()
    }

    private fun deleteToken() {
        object : Thread() {
            override fun run() {
                try {
                    val appId = AGConnectOptionsBuilder().build(this@MainActivity)
                        .getString(APP_ID)
                    HmsInstanceId.getInstance(this@MainActivity).deleteToken(appId, HCM)
                    store.clearToken()
                    runOnUiThread { statusText.setText(R.string.status_token_deleted) }
                } catch (error: ApiException) {
                    runOnUiThread {
                        statusText.text = getString(R.string.status_token_failed, error.message)
                    }
                }
            }
        }.start()
    }

    private fun changeTopic(subscribe: Boolean) {
        val topic = findViewById<EditText>(R.id.topicInput).text.toString().trim()
        if (!TOPIC.matches(topic)) {
            statusText.setText(R.string.status_topic_invalid)
            return
        }
        val task = if (subscribe) {
            HmsMessaging.getInstance(this).subscribe(topic)
        } else {
            HmsMessaging.getInstance(this).unsubscribe(topic)
        }
        task.addOnCompleteListener { completed ->
            if (completed.isSuccessful) {
                statusText.text = getString(
                    if (subscribe) R.string.status_subscribed else R.string.status_unsubscribed,
                    topic,
                )
            } else {
                statusText.text = getString(
                    R.string.status_topic_failed,
                    completed.exception?.message ?: "",
                )
            }
        }
    }

    private fun setPushEnabled(enabled: Boolean) {
        val task = if (enabled) {
            HmsMessaging.getInstance(this).turnOnPush()
        } else {
            HmsMessaging.getInstance(this).turnOffPush()
        }
        task.addOnCompleteListener { completed ->
            if (completed.isSuccessful) {
                statusText.setText(if (enabled) R.string.status_push_on else R.string.status_push_off)
            } else {
                statusText.text = getString(
                    R.string.status_push_failed,
                    completed.exception?.message ?: "",
                )
            }
        }
    }

    companion object {
        private const val HCM = "HCM"
        private const val APP_ID = "client/app_id"
        private const val REQUEST_NOTIFICATIONS = 100
        private val TOPIC = Regex("[a-zA-Z0-9\\-_.~%]{1,900}")
    }
}
