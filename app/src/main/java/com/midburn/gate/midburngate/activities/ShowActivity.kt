package com.midburn.gate.midburngate.activities

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.midburn.gate.midburngate.HttpRequestListener
import com.midburn.gate.midburngate.R
import com.midburn.gate.midburngate.application.MainApplication
import com.midburn.gate.midburngate.consts.AppConsts
import com.midburn.gate.midburngate.network.NetworkApi
import com.midburn.gate.midburngate.network.TicketNew
import com.midburn.gate.midburngate.utils.AppUtils
import kotlinx.android.synthetic.main.activity_show.*
import okhttp3.HttpUrl
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class ShowActivity : AppCompatActivity() {
    private var mState: State? = null
    private var mAction: Action? = null

    private var mHttpRequestListener: HttpRequestListener? = null

    private lateinit var mGateCode: String
    private var mTicket: TicketNew? = null
    private var mSelectedGroup: com.midburn.gate.midburngate.network.Group? = null

    private enum class State {
        ERALY_ENTRANCE,
        MIDBURN
    }

    private enum class Action {
        ENTER,
        EXIT
    }

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
        if (mTicket == null) {
            Log.e(AppConsts.TAG, "ticket is null")
            return
        }

        progressBar_ShowActivity.visibility = View.VISIBLE

        val barcode = mTicket!!.ticket.barcode
        Log.d(AppConsts.TAG, "user barcode to exit: $barcode")

        NetworkApi.gateExit(this, mGateCode, barcode, object : NetworkApi.Callback<Unit> {
            override fun onSuccess(response: Unit) {
                runOnUiThread { progressBar_ShowActivity.visibility = View.GONE }

                AppUtils.playMusic(this@ShowActivity, AppConsts.OK_MUSIC)
                showConfirmationAlert()
            }

            override fun onFailure(throwable: Throwable) {
                runOnUiThread { progressBar_ShowActivity.visibility = View.GONE }
                throwable.printStackTrace()
                AppUtils.playMusic(this@ShowActivity, AppConsts.ERROR_MUSIC)
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
        if (mTicket == null) {
            Log.e(AppConsts.TAG, "ticket is null")
            return
        }
        when (mState) {
            State.ERALY_ENTRANCE -> handleGroupTypes()
            State.MIDBURN -> sendEntranceRequestWithoutGroups()
            else -> Log.e(AppConsts.TAG, "unknown state. mState: " + mState!!)
        }
    }

    private fun handleGroupTypes() {
        val groups = mTicket!!.ticket.groups

        //check if group type is production. if so, select it immediately
        if (mTicket!!.ticket.production_early_arrival) {
            sendEntranceRequest(PRODUCTION_EARLY_ARRIVAL_WORKAROUND)
            return
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
            sendEntranceRequest(selectedGroup.id)
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

    private fun sendEntranceRequestWithoutGroups() {
        val barcode = mTicket!!.ticket.barcode
        Log.d(AppConsts.TAG, "user barcode to enter: $barcode")
        val jsonObject = JSONObject()
        try {
            jsonObject.put("barcode", barcode)
            jsonObject.put("event_id", mGateCode)
        } catch (e: JSONException) {
            Log.e(AppConsts.TAG, e.message)
        }

        val url = HttpUrl.Builder().scheme("https")
                .host(AppConsts.SERVER_URL)
                .addPathSegment("api")
                .addPathSegment("gate")
                .addPathSegment("gate-enter")
                .build()

        AppUtils.doPOSTHttpRequest(url, jsonObject.toString(), mHttpRequestListener)
    }

    private fun sendEntranceRequest(groupId: Int) {
        val barcode = mTicket!!.ticket.barcode
        Log.d(AppConsts.TAG, "user barcode to enter: $barcode")
        val jsonObject = JSONObject()
        try {
            jsonObject.put("barcode", barcode)
            jsonObject.put("event_id", mGateCode)
            // TODO(alex): 5/7/18 this is a workaround -1 means this is a production early arrival. server will validate that if no group is present
            if (groupId != PRODUCTION_EARLY_ARRIVAL_WORKAROUND) {
                jsonObject.put("group_id", groupId)
            }
        } catch (e: JSONException) {
            Log.e(AppConsts.TAG, e.message)
        }

        val url = HttpUrl.Builder().scheme("https")
                .host(AppConsts.SERVER_URL)
                .addPathSegment("api")
                .addPathSegment("gate")
                .addPathSegment("gate-enter")
                .build()

        AppUtils.doPOSTHttpRequest(url, jsonObject.toString(), mHttpRequestListener)
    }

    fun cancel() {
        val intent = Intent(this@ShowActivity, MainActivity::class.java)
        startActivity(intent)
    }

    private fun handleServerResponse(response: Response?) {
        MainApplication.getsMainThreadHandler()
                .post {
                    if (response == null) {
                        Log.e(AppConsts.TAG, "response is null")
                        AppUtils.playMusic(this@ShowActivity, AppConsts.ERROR_MUSIC)
                        AlertDialog.Builder(this).setTitle("פעולה נכשלה")
                                .setMessage(null)
                                .setPositiveButton(getString(R.string.ok), null)
                                .setNegativeButton(null, null)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show()
                        return@post
                    }
                    try {
                        val responseBodyString = response.body()!!
                                .string()
                        Log.d(AppConsts.TAG, "response.body():$responseBodyString")
                        if (response.code() == AppConsts.RESPONSE_OK) {
                            val jsonObject = JSONObject(responseBodyString)
                            AppUtils.playMusic(this@ShowActivity, AppConsts.OK_MUSIC)
                            val resultMessage = jsonObject.get("message") as String
                            Log.d(AppConsts.TAG, "resultMessage: $resultMessage")
                            showConfirmationAlert()
                        } else {
                            Log.e(AppConsts.TAG, "response code: " + response.code() + " | response body: " + responseBodyString)
                            AppUtils.playMusic(this@ShowActivity, AppConsts.ERROR_MUSIC)
                            val jsonObject = JSONObject(responseBodyString)
                            val errorMessage = jsonObject.get("error") as String
                            AlertDialog.Builder(this).setTitle("שגיאה")
                                    .setMessage(AppUtils.getErrorMessage(this, errorMessage))
                                    .setPositiveButton(getString(R.string.ok), null)
                                    .setNegativeButton(null, null)
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show()
                        }
                    } catch (e: IOException) {
                        Log.e(AppConsts.TAG, e.message)
                        AppUtils.playMusic(this@ShowActivity, AppConsts.ERROR_MUSIC)
                        AlertDialog.Builder(this).setTitle("שגיאה")
                                .setMessage(e.message)
                                .setPositiveButton(getString(R.string.ok), null)
                                .setNegativeButton(null, null)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show()
                    } catch (e: JSONException) {
                        Log.e(AppConsts.TAG, e.message)
                        AppUtils.playMusic(this@ShowActivity, AppConsts.ERROR_MUSIC)
                        AlertDialog.Builder(this).setTitle("שגיאה").setMessage(e.message).setPositiveButton(getString(R.string.ok), null).setNegativeButton(null, null).setIcon(android.R.drawable.ic_dialog_alert).show()
                    }
                }
    }

    private fun showConfirmationAlert() {
        var message: String

        if (mAction == Action.ENTER) {
            message = mTicket!!.ticket.holder_name + " נכנס/ה בהצלחה לאירוע."
            if (mState == State.ERALY_ENTRANCE) {
                message += "\n" + "הקצאה לכניסה מוקדמת - " + if (mSelectedGroup != null)
                    mSelectedGroup!!
                            .name
                else
                    "הפקה"
            }
        } else {
            message = mTicket!!.ticket.holder_name + " יצא/ה בהצלחה מהאירוע."
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show)
        exitButton.setOnClickListener { exit() }
        entranceButton.setOnClickListener { entrance() }
        cancelButton.setOnClickListener { cancel() }
        supportActionBar?.title = getString(R.string.ticket_details)

        mGateCode = intent.getStringExtra("event_id") ?: AppUtils.getEventId(this) ?: throw NullPointerException("event_id must be non null")

        mHttpRequestListener = HttpRequestListener { response ->
            runOnUiThread { progressBar_ShowActivity.visibility = View.GONE }
            handleServerResponse(response)
        }

        val ticket = intent.getSerializableExtra("ticketDetails") as TicketNew?
        if (ticket != null) {

            mTicket = ticket
            orderNumberTextView_ShowActivity.text = ticket.ticket.order_id.toString()
            ticketNumberTextView_ShowActivity.text = ticket.ticket.ticket_number.toString()
            ticketOwnerTextView_ShowActivity.text = ticket.ticket.holder_name
            ticketTypeTextView_ShowActivity.text = ticket.ticket.type
            ticketOwnerIdTextView_ShowActivity.text = ticket.ticket.israeli_id

            if (mTicket!!.ticket.production_early_arrival) {
                findViewById<View>(R.id.earlyArrivalProductionTV).visibility = View.VISIBLE
            }

            //decide which button to show (entrance/exit)
            when {
                ticket.ticket.inside_event == 0 -> //the user is outside the event
                    mAction = Action.ENTER
                ticket.ticket.inside_event == 1 -> //the user is inside the event
                    mAction = Action.EXIT
                else -> Log.e(AppConsts.TAG, "unknown isInsideEvent state. isInsideEvent: " + ticket.ticket.inside_event)
            }
            toggleButtonsState()


            //decide if disabled layout should be displayed
            if (ticket.ticket.disabled_parking == 1) {
                //show disabled parking
                disabledLayout_ShowActivity.visibility = View.VISIBLE
            } else {
                disabledLayout_ShowActivity.visibility = View.GONE
            }

            //early arrival mode
            mState = if (ticket.gate_status == "early_arrival") {
                State.ERALY_ENTRANCE
            } else { //otherwise, this is the real deal
                State.MIDBURN
            }
        }
    }

    private fun toggleButtonsState() {
        if (mAction == Action.EXIT) {
            entranceButton.visibility = View.GONE
            exitButton.visibility = View.VISIBLE
        } else {
            exitButton.visibility = View.GONE
            entranceButton.visibility = View.VISIBLE
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
