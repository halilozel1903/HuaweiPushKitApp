package com.halil.ozel.huaweipushkitapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.common.ApiException

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getToken()
    }

    private fun getToken() {
        object : Thread() {
            override fun run() {
                try {
                    val appId = AGConnectOptionsBuilder().build(this@MainActivity)
                        .getString("client/app_id")
                    val token = HmsInstanceId.getInstance(this@MainActivity)
                        .getToken(appId, "HCM")
                    Log.i("PUSH", "getToken() token: $token")
                } catch (e: ApiException) {
                    Log.e("PUSH", "getToken() failure: ${e.message}")
                }
            }
        }.start()
    }
}