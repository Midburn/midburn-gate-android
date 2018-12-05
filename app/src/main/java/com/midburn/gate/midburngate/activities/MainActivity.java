package com.midburn.gate.midburngate.activities;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.midburn.gate.midburngate.OperationFinishedListener;
import com.midburn.gate.midburngate.R;
import com.midburn.gate.midburngate.consts.AppConsts;
import com.midburn.gate.midburngate.dialogs.CarsDialog;
import com.midburn.gate.midburngate.network.NetworkApi;
import com.midburn.gate.midburngate.network.TicketNew;
import com.midburn.gate.midburngate.utils.AppUtils;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import kotlin.Unit;

import static com.midburn.gate.midburngate.activities.SplashActivity.EVENTS_LIST;

public class MainActivity
        extends AppCompatActivity {

    private EditText mInvitationNumberEditText;
    private EditText mTicketNumberEditText;
    private CarsDialog mCarsDialog;
    private ProgressDialog mProgressDialog;
    private TextView mEventIdTextView;

    private DialogInterface.OnClickListener mNeedToDownloadScannerAppClickListener;
    private DialogInterface.OnClickListener mBackPressedClickListener;

    private String mGateCode;


    private OperationFinishedListener<String> mEventIdFetchedListener = new OperationFinishedListener<String>() {
        @Override
        public void onFinish(String result) {
            mProgressDialog.dismiss();
            mGateCode = result;
            onEventIdChanged();
        }
    };

    private NetworkApi.Callback<List<String>> mEventsCallback = new NetworkApi.Callback<List<String>>() {
        @Override
        public void onSuccess(List<String> response) {
            mProgressDialog.dismiss();
            if (response != null && response.size() > 0) {
                AppUtils.showEventsDialog(MainActivity.this, response, mEventIdFetchedListener);
            }
        }

        @Override
        public void onFailure(@NotNull Throwable throwable) {
            mProgressDialog.dismiss();
            //TODO show error dialog
        }
    };

    public void manuallyInput(View view) {
        final String invitationNumber = mInvitationNumberEditText.getText().toString();
        final String ticketNumber = mTicketNumberEditText.getText().toString();
        if (TextUtils.isEmpty(invitationNumber) || TextUtils.isEmpty(ticketNumber)) {
            AppUtils.playMusic(this, AppConsts.ERROR_MUSIC);
            new AlertDialog.Builder(this).setTitle(getString(R.string.manually_validate_dialog_title))
                    .setMessage(getString(R.string.manually_validate_dialog_message))
                    .setPositiveButton(getString(R.string.ok), null)
                    .setNegativeButton(null, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return;
        }
        boolean hasInternetConnection = AppUtils.isConnected(this);
        if (!hasInternetConnection) {
            new AlertDialog.Builder(this).setTitle(getString(R.string.no_network_dialog_title))
                    .setMessage(getString(R.string.no_network_dialog_message))
                    .setPositiveButton(getString(R.string.ok), null)
                    .setNegativeButton(null, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return;
        }
        AppUtils.showProgressDialog(mProgressDialog);

        showTicketDetailsActivity(new TicketIdentification(invitationNumber, ticketNumber));
    }

    private class TicketIdentification {
        private String barcode;
        private String invitationNumber;
        private String ticketNumber;

        private TicketIdentification(@NonNull String invitationNumber, @NonNull String ticketNumber) {
            this.invitationNumber = invitationNumber;
            this.ticketNumber = ticketNumber;
        }

        private TicketIdentification(String barcode) {
            this.barcode = barcode;
        }

    }

    private void showTicketDetailsActivity(TicketIdentification ticketIdentification) {
        NetworkApi.Callback<TicketNew> callback = new NetworkApi.Callback<TicketNew>() {
            @Override
            public void onSuccess(final TicketNew ticketNew) {
                mProgressDialog.dismiss();
                Log.d(AppConsts.TAG, "onResponse called");
                if (ticketNew == null) {
                    Log.e(AppConsts.TAG, "ticketNew is null");
                    AppUtils.playMusic(MainActivity.this, AppConsts.ERROR_MUSIC);
                    new AlertDialog.Builder(MainActivity.this).setTitle("פעולה נכשלה")
                            .setMessage(null)
                            .setPositiveButton(getString(R.string.ok), null)
                            .setNegativeButton(null, null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    return;
                }

                AppUtils.playMusic(MainActivity.this, AppConsts.OK_MUSIC);

                Intent intent = new Intent(MainActivity.this, ShowActivity.class);
                intent.putExtra("event_id", mGateCode);
                intent.putExtra("ticketDetails", ticketNew);
                startActivity(intent);

            }

            @Override
            public void onFailure(@NotNull Throwable throwable) {
                throwable.printStackTrace();
                AppUtils.playMusic(MainActivity.this, AppConsts.ERROR_MUSIC);
                new AlertDialog.Builder(MainActivity.this).setTitle("שגיאה")
                        .setMessage(AppUtils.getErrorMessage(MainActivity.this, throwable.getMessage()))
                        .setPositiveButton(getString(R.string.ok), null)
                        .setNegativeButton(null, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();

            }
        };

        if (ticketIdentification.ticketNumber != null && ticketIdentification.invitationNumber != null) {
            NetworkApi.INSTANCE.getTicketManually(this, mGateCode, ticketIdentification.ticketNumber, ticketIdentification.invitationNumber, callback);
        } else {
            NetworkApi.INSTANCE.getTicket(this, mGateCode, ticketIdentification.barcode, callback);
        }
    }

    public void showCarDialog(View view) {
        mCarsDialog = new CarsDialog(this, v -> {
            Log.d(AppConsts.TAG, "carEnter");

            AppUtils.showProgressDialog(mProgressDialog);
            if (mCarsDialog != null) {
                mCarsDialog.dismiss();
            }
            NetworkApi.INSTANCE.enterCar(this, mGateCode, new NetworkApi.Callback<Unit>() {
                @Override
                public void onSuccess(Unit response) {
                    mProgressDialog.dismiss();
                    AppUtils.playMusic(MainActivity.this, AppConsts.OK_MUSIC);
                }

                @Override
                public void onFailure(@NotNull Throwable throwable) {
                    Log.w(AppConsts.TAG, throwable.getMessage());
                    mProgressDialog.dismiss();
                    AppUtils.playMusic(MainActivity.this, AppConsts.ERROR_MUSIC);
                }
            });

        }, v -> {
            Log.d(AppConsts.TAG, "carExit");
            AppUtils.showProgressDialog(mProgressDialog);
            if (mCarsDialog != null) {
                mCarsDialog.dismiss();
            }
            NetworkApi.INSTANCE.exitCar(MainActivity.this, mGateCode, new NetworkApi.Callback<Unit>() {
                @Override
                public void onSuccess(Unit response) {
                    mProgressDialog.dismiss();
                    AppUtils.playMusic(MainActivity.this, AppConsts.OK_MUSIC);
                }

                @Override
                public void onFailure(@NotNull Throwable throwable) {
                    mProgressDialog.dismiss();
                    AppUtils.playMusic(MainActivity.this, AppConsts.ERROR_MUSIC);
                }
            });
        });
        mCarsDialog.show();
    }

    public void scanQR(View view) {
        try {
            //start the scanning activity from the com.google.zxing.client.android.SCAN intent
            Intent intent = new Intent(AppConsts.ACTION_SCAN);
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            intent.setPackage("com.google.zxing.client.android");
            startActivityForResult(intent, 0);
        } catch (ActivityNotFoundException anfe) {
            //on catch, show the download dialog
            AppUtils.playMusic(this, AppConsts.ERROR_MUSIC);
            new AlertDialog.Builder(this).setTitle("סורק לא נמצא")
                    .setMessage("להוריד אפליקציית סורק?")
                    .setPositiveButton("כן", mNeedToDownloadScannerAppClickListener)
                    .setNegativeButton("לא", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                final String barcode = intent.getStringExtra("SCAN_RESULT");
                String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
                Log.d(AppConsts.TAG, "barcode: " + barcode + " | format: " + format);

                AppUtils.showProgressDialog(mProgressDialog);

                showTicketDetailsActivity(new TicketIdentification(barcode));
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindView();
        setListeners();
        checkForUpdates();

        //fetch gate code from shared prefs
        mGateCode = AppUtils.getEventId(this);
        if (TextUtils.isEmpty(mGateCode)) {
            List<String> events = getIntent().getStringArrayListExtra(EVENTS_LIST);
            if (events == null || events.size() <= 0) {
                AppUtils.showProgressDialog(mProgressDialog);
                NetworkApi.INSTANCE.getEvents(this, mEventsCallback);
            } else {
                AppUtils.showEventsDialog(this, events, mEventIdFetchedListener);
            }
        }
        if (TextUtils.isEmpty(mGateCode)) {
            Log.e(AppConsts.TAG, "Gate code is empty!");
        }
        onEventIdChanged();
    }

    private void onEventIdChanged() {
        if (TextUtils.isEmpty(mGateCode)) {
            mEventIdTextView.setText("חסר קוד אירוע");
        } else {
            mEventIdTextView.setText(mGateCode);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        CrashManager.register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        UpdateManager.unregister();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UpdateManager.unregister();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_layout, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_menu:
                new AlertDialog.Builder(this).setTitle("הכנס קוד אירוע חדש?")
                        .setMessage("פעולה זו תמחק את קוד האירוע הישן")
                        .setPositiveButton("כן", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                AppUtils.persistEventId(MainActivity.this, "");
                                NetworkApi.INSTANCE.getEvents(MainActivity.this, mEventsCallback);
                            }
                        })
                        .setNegativeButton("לא", null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private void setListeners() {
        mNeedToDownloadScannerAppClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Uri uri = Uri.parse("market://search?q=pname:" + "com.google.zxing.client.android");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    MainActivity.this.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.e(AppConsts.TAG, e.getMessage());
                }
            }
        };

        mBackPressedClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //exit app
                finishAffinity();
            }
        };

    }

    private void bindView() {
        mInvitationNumberEditText = findViewById(R.id.invitationNumberEditText_MainActivity);
        mTicketNumberEditText = findViewById(R.id.ticketNumberEditText_MainActivity);
        mProgressDialog = new ProgressDialog(this);
        mEventIdTextView = findViewById(R.id.eventId_TextView_MainActivity);
    }


    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this).setTitle("האם ברצונך לצאת?")
                .setMessage("")
                .setPositiveButton("כן", mBackPressedClickListener)
                .setNegativeButton("לא", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void checkForUpdates() {
        // Remove this for store builds!
        UpdateManager.register(this);
    }

}
