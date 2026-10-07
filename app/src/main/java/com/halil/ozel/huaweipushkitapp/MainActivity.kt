package com.halil.ozel.huaweipushkitapp

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.common.ApiException
import com.huawei.hms.push.HmsMessaging

class MainActivity : AppCompatActivity() {
    private lateinit var store: PushStore
    private lateinit var tokenValue: TextView
    private lateinit var statusText: TextView

    private val tokenListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == PushStore.KEY_TOKEN) {
            runOnUiThread { showToken() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        store = PushStore(this)
        tokenValue = findViewById(R.id.tokenValue)
        statusText = findViewById(R.id.statusText)
        findViewById<Button>(R.id.refreshToken).setOnClickListener { refreshToken() }
        findViewById<Button>(R.id.deleteToken).setOnClickListener { deleteToken() }
        findViewById<Button>(R.id.subscribeTopic).setOnClickListener { changeTopic(subscribe = true) }
        findViewById<Button>(R.id.unsubscribeTopic).setOnClickListener { changeTopic(subscribe = false) }
        findViewById<Button>(R.id.turnOnPush).setOnClickListener { setPushEnabled(true) }
        findViewById<Button>(R.id.turnOffPush).setOnClickListener { setPushEnabled(false) }
        showToken()
    }

    override fun onStart() {
        super.onStart()
        store.listen(tokenListener)
        showToken()
    }

    override fun onStop() {
        store.unlisten(tokenListener)
        super.onStop()
    }

    private fun showToken() {
        val token = store.token()
        tokenValue.text = if (token.isEmpty()) getString(R.string.token_empty) else token
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
        private val TOPIC = Regex("[a-zA-Z0-9\\-_.~%]{1,900}")
    }
}
