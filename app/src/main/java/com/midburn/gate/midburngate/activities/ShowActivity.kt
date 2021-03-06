package com.midburn.gate.midburngate.activities

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.midburn.gate.midburngate.R
import com.midburn.gate.midburngate.consts.AppConsts
import com.midburn.gate.midburngate.network.InnerTicket
import com.midburn.gate.midburngate.network.NetworkApi
import com.midburn.gate.midburngate.network.TicketNew
import com.midburn.gate.midburngate.utils.AppUtils
import com.midburn.gate.midburngate.utils.SoundEffect
import kotlinx.android.synthetic.main.activity_show.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShowActivity : AppCompatActivity() {

    private lateinit var mGateCode: String
    private lateinit var mTicket: TicketNew
    private var mSelectedGroup: com.midburn.gate.midburngate.network.Group? = null
    private val uiScope = CoroutineScope(Dispatchers.Main)

    fun exit() {
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

        progressBar_ShowActivity.visibility = View.VISIBLE

        val barcode = mTicket.ticket.barcode
        Log.d(AppConsts.TAG, "user barcode to exit: $barcode")

        NetworkApi.gateExit(this, mGateCode, barcode, object : NetworkApi.Callback<Unit> {
            override fun onSuccess(response: Unit) {
                runOnUiThread { progressBar_ShowActivity.visibility = View.GONE }
                uiScope.launch { SoundEffect.ok(this@ShowActivity) }
                val builder = AlertDialog.Builder(this@ShowActivity)
                builder.setMessage(mTicket.ticket.holder_name + " יצא/ה בהצלחה מהאירוע.")
                        .setTitle("אישור")
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            val intent = Intent(this@ShowActivity, MainActivity::class.java)
                            startActivity(intent)
                        }
                val dialog = builder.create()
                dialog.show()
                showUserEnteredSuccessfullyDialog()
            }

            override fun onFailure(throwable: Throwable) {
                runOnUiThread { progressBar_ShowActivity.visibility = View.GONE }
                throwable.printStackTrace()
                uiScope.launch { SoundEffect.error(this@ShowActivity) }
                AlertDialog.Builder(this@ShowActivity).setTitle("פעולה נכשלה")
                        .setMessage("")
                        .setPositiveButton(getString(R.string.ok), null)
                        .setNegativeButton("", null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show()
            }
        })
    }

    fun entrance() {
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
        when (mTicket.gate_status) {
            TicketNew.State.EARLY_ENTRANCE -> handleGroupTypes()
            TicketNew.State.MIDBURN -> {
                NetworkApi.gateEnter(this, mGateCode, mTicket.ticket.barcode, object : NetworkApi.Callback<Unit> {
                    override fun onSuccess(response: Unit) {
                        progressBar_ShowActivity.visibility = View.GONE
                        showUserEnteredSuccessfullyDialog()
                    }

                    override fun onFailure(throwable: Throwable) {
                        runOnUiThread { progressBar_ShowActivity.visibility = View.GONE }
                        throwable.printStackTrace()
                        uiScope.launch { SoundEffect.error(this@ShowActivity) }
                        AlertDialog.Builder(this@ShowActivity).setTitle("פעולה נכשלה")
                                .setMessage("")
                                .setPositiveButton(getString(R.string.ok), null)
                                .setNegativeButton("", null)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show()
                    }
                })
            }
        }
    }

    private fun showUserEnteredSuccessfullyDialog() {
        uiScope.launch { SoundEffect.ok(this@ShowActivity) }
        var message: String = mTicket.ticket.holder_name + " נכנס/ה בהצלחה לאירוע."
        if (mTicket.gate_status == TicketNew.State.EARLY_ENTRANCE) {
            message += "\n" + "הקצאה לכניסה מוקדמת - " + if (mSelectedGroup != null)
                mSelectedGroup!!
                        .name
            else
                "הפקה"
        }
        val builder = AlertDialog.Builder(this@ShowActivity)
        builder.setMessage(message)
                .setTitle("אישור")
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val intent = Intent(this@ShowActivity, MainActivity::class.java)
                    startActivity(intent)
                }
        val dialog = builder.create()
        dialog.show()
    }

    private fun handleGroupTypes() {
        val groups = mTicket.ticket.groups

        //check if group type is production. if so, select it immediately
        if (mTicket.ticket.production_early_arrival) {
            NetworkApi.gateEnter(this, mGateCode, mTicket.ticket.barcode, object : NetworkApi.Callback<Unit> {
                override fun onSuccess(response: Unit) {
                    progressBar_ShowActivity.visibility = View.GONE
                    showUserEnteredSuccessfullyDialog()
                }

                override fun onFailure(throwable: Throwable) {
                    runOnUiThread { progressBar_ShowActivity.visibility = View.GONE }
                    throwable.printStackTrace()
                    uiScope.launch { SoundEffect.error(this@ShowActivity) }
                    AlertDialog.Builder(this@ShowActivity).setTitle("פעולה נכשלה")
                            .setMessage("")
                            .setPositiveButton(getString(R.string.ok), null)
                            .setNegativeButton("", null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show()
                }
            }, PRODUCTION_EARLY_ARRIVAL_WORKAROUND)
        }

        // no groups alert
        if (groups.isEmpty()) {
            AlertDialog.Builder(this).setTitle("שגיאה")
                    .setMessage(getString(R.string.no_early_arrival_message))
                    .setPositiveButton(getString(R.string.ok), null)
                    .setNegativeButton(null, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show()
            return
        }

        // show group selection dialog
        val groupsArrayListSize = groups.size
        val groupsArray = arrayOfNulls<CharSequence>(groupsArrayListSize)
        for (i in 0 until groupsArrayListSize) {
            val group = groups[i]
            groupsArray[i] = getGroupType(group) + ": " + group.name
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle("בחר קבוצה")
        builder.setItems(groupsArray) { _, which ->
            val selectedGroup = groups[which]
            Log.d(AppConsts.TAG, selectedGroup.name + " was clicked. id: " + selectedGroup.id)
            progressBar_ShowActivity.visibility = View.VISIBLE
            mSelectedGroup = selectedGroup
            NetworkApi.gateEnter(this, mGateCode, mTicket.ticket.barcode, object : NetworkApi.Callback<Unit> {
                override fun onSuccess(response: Unit) {
                    progressBar_ShowActivity.visibility = View.GONE
                    showUserEnteredSuccessfullyDialog()
                }

                override fun onFailure(throwable: Throwable) {
                    runOnUiThread { progressBar_ShowActivity.visibility = View.GONE }
                    throwable.printStackTrace()
                    uiScope.launch { SoundEffect.error(this@ShowActivity) }
                    AlertDialog.Builder(this@ShowActivity).setTitle("פעולה נכשלה")
                            .setMessage("")
                            .setPositiveButton(getString(R.string.ok), null)
                            .setNegativeButton("", null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show()
                }
            }, selectedGroup.id)
        }
        builder.show()
    }

    private fun getGroupType(group: com.midburn.gate.midburngate.network.Group): String {
        if (group.type == AppConsts.GROUP_TYPE_ART) {
            return "מיצב"
        }

        if (group.type == AppConsts.GROUP_TYPE_CAMP) {
            return "מחנה"
        }

        return if (group.type == AppConsts.GROUP_TYPE_PRODUCTION) {
            "הפקה"
        } else ""
    }

    fun cancel() {
        val intent = Intent(this@ShowActivity, MainActivity::class.java)
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show)
        exitButton.setOnClickListener { exit() }
        entranceButton.setOnClickListener { entrance() }
        cancelButton.setOnClickListener { cancel() }
        supportActionBar?.title = getString(R.string.ticket_details)

        mGateCode = intent.getStringExtra("event_id") ?: AppUtils.getEventId(this) ?: throw NullPointerException("event_id must be non null")

        mTicket = intent.getSerializableExtra("ticketDetails") as TicketNew
        orderNumberTextView_ShowActivity.text = mTicket.ticket.order_id.toString()
        ticketNumberTextView_ShowActivity.text = mTicket.ticket.ticket_number.toString()
        ticketOwnerTextView_ShowActivity.text = mTicket.ticket.holder_name
        ticketTypeTextView_ShowActivity.text = mTicket.ticket.type
        ticketOwnerIdTextView_ShowActivity.text = mTicket.ticket.israeli_id

        if (mTicket.ticket.production_early_arrival) {
            findViewById<View>(R.id.earlyArrivalProductionTV).visibility = View.VISIBLE
        }

        if (mTicket.ticket.inside_event == InnerTicket.EventEntry.INSIDE) {
            entranceButton.visibility = View.GONE
            exitButton.visibility = View.VISIBLE
        } else {
            exitButton.visibility = View.GONE
            entranceButton.visibility = View.VISIBLE
        }


        //decide if disabled layout should be displayed
        if (mTicket.ticket.disabled_parking == 1) {
            //show disabled parking
            disabledLayout_ShowActivity.visibility = View.VISIBLE
        } else {
            disabledLayout_ShowActivity.visibility = View.GONE
        }

    }

    override fun onBackPressed() {
        val upIntent = Intent(this, MainActivity::class.java)
        NavUtils.navigateUpTo(this, upIntent)
    }

    companion object {

        private const val PRODUCTION_EARLY_ARRIVAL_WORKAROUND = -1
    }
}
