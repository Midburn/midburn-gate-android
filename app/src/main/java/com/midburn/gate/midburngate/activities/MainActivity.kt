@file:Suppress("DEPRECATION")

//Only for ProgressDialog, this is temporary
//TODO: stop using ProgressDialog

package com.midburn.gate.midburngate.activities

import android.app.Activity
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.midburn.gate.midburngate.R
import com.midburn.gate.midburngate.consts.AppConsts
import com.midburn.gate.midburngate.consts.IntentExtras.EVENTS_LIST
import com.midburn.gate.midburngate.dialogs.CarsDialog
import com.midburn.gate.midburngate.network.NetworkApi
import com.midburn.gate.midburngate.network.TicketNew
import com.midburn.gate.midburngate.utils.AppUtils
import com.midburn.gate.midburngate.utils.SoundEffect
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.hockeyapp.android.CrashManager
import net.hockeyapp.android.UpdateManager

class MainActivity : AppCompatActivity() {

    private lateinit var mCarsDialog: CarsDialog
    private var mProgressDialog: ProgressDialog? = null

    private var mNeedToDownloadScannerAppClickListener: DialogInterface.OnClickListener? = null
    private var mBackPressedClickListener: DialogInterface.OnClickListener? = null

    private var mGateCode: String? = null

    private val uiScope = CoroutineScope(Dispatchers.Main)

    private val onEventSelected: (eventId: String) -> Unit = {
        mProgressDialog!!.dismiss()
        mGateCode = it
        onEventIdChanged()
    }

    private val mEventsCallback = object : NetworkApi.Callback<List<String>> {
        override fun onSuccess(response: List<String>) {
            mProgressDialog!!.dismiss()
            if (response.isNotEmpty()) {
                AppUtils.showEventsDialog(this@MainActivity, response, onEventSelected)
            }
        }

        override fun onFailure(throwable: Throwable) {
            mProgressDialog!!.dismiss()
            //TODO show error dialog
        }
    }

    private fun manuallyInput() {
        val invitationNumber = invitationNumberEditText.text.toString()
        val ticketNumber = ticketNumberEditText.text.toString()
        if (TextUtils.isEmpty(invitationNumber) || TextUtils.isEmpty(ticketNumber)) {
            uiScope.launch { SoundEffect.error(this@MainActivity) }
            AlertDialog.Builder(this).setTitle(getString(R.string.manually_validate_dialog_title))
                    .setMessage(getString(R.string.manually_validate_dialog_message))
                    .setPositiveButton(getString(R.string.ok), null)
                    .setNegativeButton(null, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show()
            return
        }
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
        AppUtils.showProgressDialog(mProgressDialog!!)

        showTicketDetailsActivity(TicketIdentification(invitationNumber, ticketNumber))
    }


    inner class TicketIdentification {
        val barcode: String?
        val invitationNumber: String?
        val ticketNumber: String?

        constructor(invitationNumber: String, ticketNumber: String) {
            this.invitationNumber = invitationNumber
            this.ticketNumber = ticketNumber
            this.barcode = null
        }

        constructor(barcode: String) {
            this.barcode = barcode
            this.invitationNumber = null
            this.ticketNumber = null
        }

    }

    private fun showTicketDetailsActivity(ticketIdentification: TicketIdentification) {
        val callback = object : NetworkApi.Callback<TicketNew> {
            override fun onSuccess(response: TicketNew) {
                mProgressDialog!!.dismiss()
                Log.d(AppConsts.TAG, "onResponse called")
                uiScope.launch { SoundEffect.ok(this@MainActivity) }
                val intent = Intent(this@MainActivity, ShowActivity::class.java)
                intent.putExtra("event_id", mGateCode)
                intent.putExtra("ticketDetails", response)
                startActivity(intent)

            }

            override fun onFailure(throwable: Throwable) {
                throwable.printStackTrace()
                uiScope.launch { SoundEffect.error(this@MainActivity) }
                AlertDialog.Builder(this@MainActivity).setTitle("שגיאה")
                        .setMessage(AppUtils.getErrorMessage(this@MainActivity, throwable.message))
                        .setPositiveButton(getString(R.string.ok), null)
                        .setNegativeButton(null, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show()

            }
        }

        if (ticketIdentification.ticketNumber != null && ticketIdentification.invitationNumber != null) {
            NetworkApi.getTicketManually(this, mGateCode!!, ticketIdentification.ticketNumber, ticketIdentification.invitationNumber, callback)
        } else if (ticketIdentification.barcode != null) {
            NetworkApi.getTicket(this, mGateCode!!, ticketIdentification.barcode, callback)
        }
    }

    private fun showCarDialog() {
        mCarsDialog = CarsDialog(this, View.OnClickListener {
            Log.d(AppConsts.TAG, "carEnter")

            AppUtils.showProgressDialog(mProgressDialog!!)
            mCarsDialog.dismiss()
            NetworkApi.enterCar(this, mGateCode!!, object : NetworkApi.Callback<Unit> {
                override fun onSuccess(response: Unit) {
                    mProgressDialog!!.dismiss()
                    uiScope.launch { SoundEffect.ok(this@MainActivity) }
                }

                override fun onFailure(throwable: Throwable) {
                    Log.w(AppConsts.TAG, throwable.message)
                    mProgressDialog!!.dismiss()
                    uiScope.launch { SoundEffect.error(this@MainActivity) }
                }
            })

        }, View.OnClickListener {
            Log.d(AppConsts.TAG, "carExit")
            AppUtils.showProgressDialog(mProgressDialog!!)
            mCarsDialog.dismiss()
            NetworkApi.exitCar(this@MainActivity, mGateCode!!, object : NetworkApi.Callback<Unit> {
                override fun onSuccess(response: Unit) {
                    mProgressDialog!!.dismiss()
                    uiScope.launch { SoundEffect.ok(this@MainActivity) }
                }

                override fun onFailure(throwable: Throwable) {
                    mProgressDialog!!.dismiss()
                    uiScope.launch { SoundEffect.error(this@MainActivity) }
                }
            })
        })
        mCarsDialog.show()
    }

    private fun scanQR() {
        try {
            //start the scanning activity from the com.google.zxing.client.android.SCAN intent
            val intent = Intent(AppConsts.ACTION_SCAN)
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE")
            intent.setPackage("com.google.zxing.client.android")
            startActivityForResult(intent, 0)
        } catch (anfe: ActivityNotFoundException) {
            //on catch, show the download dialog
            uiScope.launch { SoundEffect.error(this@MainActivity) }
            AlertDialog.Builder(this).setTitle("סורק לא נמצא")
                    .setMessage("להוריד אפליקציית סורק?")
                    .setPositiveButton("כן", mNeedToDownloadScannerAppClickListener)
                    .setNegativeButton("לא", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show()
        }

    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                val barcode = intent!!.getStringExtra("SCAN_RESULT")
                val format = intent.getStringExtra("SCAN_RESULT_FORMAT")
                Log.d(AppConsts.TAG, "barcode: $barcode | format: $format")

                AppUtils.showProgressDialog(mProgressDialog!!)

                showTicketDetailsActivity(TicketIdentification(barcode))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scanQRCode.setOnClickListener { scanQR() }
        getTicketDetailsButton.setOnClickListener { manuallyInput() }
        carCounterImageButton.setOnClickListener { showCarDialog() }
        mProgressDialog = ProgressDialog(this)
        mNeedToDownloadScannerAppClickListener = DialogInterface.OnClickListener { _, _ ->
            val uri = Uri.parse("market://search?q=pname:" + "com.google.zxing.client.android")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.e(AppConsts.TAG, e.message)
            }
        }
        mBackPressedClickListener = DialogInterface.OnClickListener { _, which ->
            //exit app
            finishAffinity()
        }
        UpdateManager.register(this)

        //fetch gate code from shared prefs
        mGateCode = AppUtils.getEventId(this)
        if (TextUtils.isEmpty(mGateCode)) {
            val events = intent.getStringArrayListExtra(EVENTS_LIST)
            if (events == null || events.size <= 0) {
                AppUtils.showProgressDialog(mProgressDialog!!)
                NetworkApi.getEvents(this, mEventsCallback)
            } else {
                AppUtils.showEventsDialog(this, events, onEventSelected)
            }
        }
        if (TextUtils.isEmpty(mGateCode)) {
            Log.e(AppConsts.TAG, "Gate code is empty!")
        }
        onEventIdChanged()
    }

    private fun onEventIdChanged() {
        if (TextUtils.isEmpty(mGateCode)) {
            eventIdTextView.text = "חסר קוד אירוע"
        } else {
            eventIdTextView.text = mGateCode
        }
    }

    override fun onResume() {
        super.onResume()
        CrashManager.register(this)
    }

    public override fun onPause() {
        super.onPause()
        UpdateManager.unregister()
    }

    public override fun onDestroy() {
        super.onDestroy()
        UpdateManager.unregister()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_layout, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_menu -> {
                AlertDialog.Builder(this).setTitle("הכנס קוד אירוע חדש?")
                        .setMessage("פעולה זו תמחק את קוד האירוע הישן")
                        .setPositiveButton("כן") { dialog, which ->
                            AppUtils.persistEventId(this@MainActivity, "")
                            NetworkApi.getEvents(this@MainActivity, mEventsCallback)
                        }
                        .setNegativeButton("לא", null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show()
                return true
            }
            else ->
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this).setTitle("האם ברצונך לצאת?")
                .setMessage("")
                .setPositiveButton("כן", mBackPressedClickListener)
                .setNegativeButton("לא", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
    }

}
