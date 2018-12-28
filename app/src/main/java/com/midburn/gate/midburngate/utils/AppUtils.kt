package com.midburn.gate.midburngate.utils

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.support.annotation.RawRes
import android.support.v7.app.AlertDialog
import android.util.Log
import com.midburn.gate.midburngate.OperationFinishedListener
import com.midburn.gate.midburngate.R
import com.midburn.gate.midburngate.consts.AppConsts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object SoundEffect {

    suspend fun ok(context: Context) {
        MediaPlayerStore.get(context, R.raw.ok).start()
    }

    suspend fun error(context: Context) {
        MediaPlayerStore.get(context, R.raw.error).start()
    }

    private object MediaPlayerStore {
        private val _players: MutableMap<Pair<Context, Int>, MediaPlayer> = ConcurrentHashMap()
        suspend fun get(context: Context, @RawRes rawRes: Int): MediaPlayer {
            return withContext(Dispatchers.IO) {
                _players.getOrPut(Pair(context, rawRes)) {
                    MediaPlayer.create(context, rawRes)
                }
            }
        }
    }

}

object AppUtils {

    fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        return netInfo != null && netInfo.isConnected
    }

    fun getErrorMessage(context: Context, error: String?): String {
        Log.d(AppConsts.TAG, "error to message: $error")
        return when (error) {
            AppConsts.QUOTA_REACHED_ERROR -> context.getString(R.string.quota_reached_error_message)
            AppConsts.USER_OUTSIDE_EVENT_ERROR -> context.getString(R.string.user_outside_event_error_message)
            AppConsts.GATE_CODE_MISSING_ERROR -> context.getString(R.string.gate_code_missing_error_message)
            AppConsts.TICKET_NOT_FOUND_ERROR -> context.getString(R.string.ticket_not_found_error_message)
            AppConsts.BAD_SEARCH_PARAMETERS_ERROR -> context.getString(R.string.bad_search_params_error_message)
            AppConsts.ALREADY_INSIDE_ERROR -> context.getString(R.string.already_inside_error_message)
            AppConsts.TICKET_NOT_IN_GROUP_ERROR -> context.getString(R.string.ticker_not_in_group_error_message)
            AppConsts.INTERNAL_ERROR -> context.getString(R.string.internal_error_message)
            else -> error ?: "no error message was specified"
        }
    }

    fun showProgressDialog(progressDialog: ProgressDialog) {
        progressDialog.setMessage("נא להמתין")
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog.isIndeterminate = true
        progressDialog.show()
    }

    fun showEventsDialog(context: Context, events: List<String>, stringOperationFinishedListener: OperationFinishedListener<String>) {
        // show event selection dialog
        val eventsArray = events.toTypedArray<CharSequence>()
        val builder = AlertDialog.Builder(context)
        builder.setTitle("בחר אירוע")
        builder.setItems(eventsArray) { dialog, which ->
            val eventId = eventsArray[which].toString()
            Log.d(AppConsts.TAG, "$eventId was clicked.")
            persistEventId(context, eventId)
            stringOperationFinishedListener.onFinish(eventId)

        }
        builder.show()
    }

    fun persistEventId(context: Context, eventId: String) {
        val sharedPref = (context as Activity).getPreferences(Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString(context.getString(R.string.gate_code_key), eventId)
        editor.apply()
    }

    fun getEventId(context: Context): String {
        val sharedPref = (context as Activity).getPreferences(Context.MODE_PRIVATE)
        return sharedPref.getString(context.getString(R.string.gate_code_key), "")
    }
}
