package com.midburn.gate.midburngate.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import com.midburn.gate.midburngate.R
import com.midburn.gate.midburngate.consts.AppConsts
import com.midburn.gate.midburngate.network.NetworkApi
import com.midburn.gate.midburngate.utils.AppUtils
import java.util.*

const val EVENTS_LIST = "EVENTS_LIST"

class SplashActivity : AppCompatActivity() {
    private var failureCounter: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val gateCode = AppUtils.getEventId(this)

        if (TextUtils.isEmpty(gateCode)) {
            val hasInternetConnection = AppUtils.isConnected(this)
            if (!hasInternetConnection) {
                AlertDialog.Builder(this).setTitle(getString(R.string.no_network_dialog_title))
                        .setMessage(getString(R.string.no_network_dialog_message))
                        .setPositiveButton(getString(R.string.ok), null)
                        .setNegativeButton(null, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show()
                return
            }
            NetworkApi.getEvents(this, object : NetworkApi.Callback<List<String>> {
                override fun onSuccess(response: List<String>) {
                    if (response.isEmpty()) {
                        val errorMessage = "events list is empty"
                        onFailure(Exception(errorMessage))
                        return
                    }
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    intent.putStringArrayListExtra(EVENTS_LIST, response as ArrayList<String>)
                    startActivity(intent)
                }

                override fun onFailure(throwable: Throwable) {
                    failureCounter++
                    Log.e(AppConsts.TAG, throwable.message + " failureCounter: " + failureCounter)
                    AlertDialog.Builder(this@SplashActivity).setTitle("שגיאה")
                            .setMessage(AppUtils.getErrorMessage(this@SplashActivity, throwable.message))
                            .setPositiveButton(getString(R.string.ok), null)
                            .setNegativeButton(null, null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show()
                    finish()
                }
            })
        }
    }
}
