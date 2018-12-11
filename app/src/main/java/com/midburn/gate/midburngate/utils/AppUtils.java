package com.midburn.gate.midburngate.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.midburn.gate.midburngate.HttpRequestListener;
import com.midburn.gate.midburngate.OperationFinishedListener;
import com.midburn.gate.midburngate.R;
import com.midburn.gate.midburngate.application.MainApplication;
import com.midburn.gate.midburngate.consts.AppConsts;

import java.io.IOException;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AppUtils {

	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public static void playMusic(Context context, int which) {
		switch (which) {
			case AppConsts.OK_MUSIC:
				MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.ok);
				mediaPlayer.start();
				break;
			case AppConsts.ERROR_MUSIC:
				mediaPlayer = MediaPlayer.create(context, R.raw.error);
				mediaPlayer.start();
				break;
		}
	}

	public static boolean isConnected(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		return netInfo != null && netInfo.isConnected();
	}

	public static void doPOSTHttpRequest(final HttpUrl url, final String requestBodyJson, final HttpRequestListener httpRequestListener) {
		Log.d(AppConsts.TAG, "url: " + url);
		Log.d(AppConsts.TAG, "requestBody: " + requestBodyJson);
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				RequestBody body = RequestBody.create(JSON, requestBodyJson);
				Request request = new Request.Builder().url(url)
				                                       .post(body)
				                                       .build();
				try {
					Response response = MainApplication.getHttpClient()
					                                   .newCall(request)
					                                   .execute();

					httpRequestListener.onResponse(response);
				} catch (IOException e) {
					Log.e(AppConsts.TAG, e.getMessage());
					httpRequestListener.onResponse(null);
				}
			}
		});
		thread.start();
	}

    public static String getErrorMessage(Context context, String error) {
		Log.d(AppConsts.TAG, "error to message: " + error);
		switch (error) {
			case AppConsts.QUOTA_REACHED_ERROR:
				return context.getString(R.string.quota_reached_error_message);
			case AppConsts.USER_OUTSIDE_EVENT_ERROR:
				return context.getString(R.string.user_outside_event_error_message);
			case AppConsts.GATE_CODE_MISSING_ERROR:
				return context.getString(R.string.gate_code_missing_error_message);
			case AppConsts.TICKET_NOT_FOUND_ERROR:
				return context.getString(R.string.ticket_not_found_error_message);
			case AppConsts.BAD_SEARCH_PARAMETERS_ERROR:
				return context.getString(R.string.bad_search_params_error_message);
			case AppConsts.ALREADY_INSIDE_ERROR:
				return context.getString(R.string.already_inside_error_message);
			case AppConsts.TICKET_NOT_IN_GROUP_ERROR:
				return context.getString(R.string.ticker_not_in_group_error_message);
			case AppConsts.INTERNAL_ERROR:
				return context.getString(R.string.internal_error_message);
			default:
				return error;

		}
	}

	public static void showProgressDialog(ProgressDialog progressDialog) {
		progressDialog.setMessage("נא להמתין");
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setIndeterminate(true);
		progressDialog.show();
	}

	public static void showEventsDialog(Context context, List<String> events, OperationFinishedListener<String> stringOperationFinishedListener) {
		// show event selection dialog
		CharSequence eventsArray[] = events.toArray(new CharSequence[events.size()]);
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("בחר אירוע");
		builder.setItems(eventsArray, (dialog, which) -> {
			String eventId = eventsArray[which].toString();
			Log.d(AppConsts.TAG, eventId + " was clicked.");
			persistEventId(context, eventId);
			stringOperationFinishedListener.onFinish(eventId);

		});
		builder.show();
	}

	public static void persistEventId(Context context, String eventId) {
		SharedPreferences sharedPref = ((Activity) context).getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(context.getString(R.string.gate_code_key), eventId);
		editor.commit();
	}

	public static String getEventId(Context context) {
		SharedPreferences sharedPref = ((Activity) context).getPreferences(Context.MODE_PRIVATE);
		return sharedPref.getString(context.getString(R.string.gate_code_key), "");
	}
}
