package io.github.gmkbenjamin.gitrepo.beta.ui.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.backup.BackupManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;
import java.io.IOException;

import javax.crypto.BadPaddingException;

import io.github.gmkbenjamin.gitrepo.beta.R;
import io.github.gmkbenjamin.gitrepo.beta.service.SSHDaemonService;
import io.github.gmkbenjamin.gitrepo.beta.ui.util.C;
import io.github.gmkbenjamin.gitrepo.beta.ui.util.GitrepoBackupAgent;
import io.github.gmkbenjamin.gitrepo.beta.ui.util.GitrepoCommons;
import io.github.gmkbenjamin.gitrepo.beta.ui.util.PrefsConstants;

public class HomeActivity extends BaseActivity {
//	private final static String TAG = HomeActivity.class.getSimpleName();

    private Button startStopButton;
    private ImageView wirelessImageView;
    private TextView wifiStatusTextView;
    private TextView wifiSSIDTextView;
    private TextView homeServerInfoTextView;
    private SharedPreferences prefs;

    private void stopAll(View v){
        boolean isSshServiceRunning = GitrepoCommons.isSshServiceRunning(HomeActivity.this);

        Intent intent = new Intent(v.getContext(), SSHDaemonService.class);
        if (isSshServiceRunning) {
            stopService(intent);
        }
    }
    private BroadcastReceiver connectivityChangeBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                showWifiStatus(context);
            }
        }
    };

    private BroadcastReceiver sshdBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(C.action.SSHD_STARTED)) {
                Animation flyInAnimation = new TranslateAnimation(
                        Animation.RELATIVE_TO_SELF, -1.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f);
                flyInAnimation.setDuration(500);

                Animation fadeInAnimation = new AlphaAnimation(0.0f, 1.0f);
                fadeInAnimation.setDuration(1000);

                AnimationSet animationSet = new AnimationSet(true);
                animationSet.addAnimation(flyInAnimation);
                animationSet.addAnimation(fadeInAnimation);
                animationSet.setInterpolator(AnimationUtils.loadInterpolator(HomeActivity.this, android.R.anim.overshoot_interpolator));

                homeServerInfoTextView.setText(GitrepoCommons.getCurrentServerAddress(HomeActivity.this, prefs));

                homeServerInfoTextView.startAnimation(animationSet);
                homeServerInfoTextView.setVisibility(View.VISIBLE);
                startStopButton.setText("Stop");
            } else if (action.equals(C.action.SSHD_STOPPED)) {
                Animation flyOutAnimation = new TranslateAnimation(
                        Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 1.1f,
                        Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f);
                flyOutAnimation.setDuration(500);

                Animation fadeOutAnimation = new AlphaAnimation(1.0f, 0.0f);
                fadeOutAnimation.setDuration(500);

                AnimationSet animationSet = new AnimationSet(true);
                animationSet.addAnimation(fadeOutAnimation);
                animationSet.addAnimation(flyOutAnimation);
                animationSet.setInterpolator(AnimationUtils.loadInterpolator(HomeActivity.this, android.R.anim.anticipate_interpolator));

                homeServerInfoTextView.startAnimation(animationSet);
                homeServerInfoTextView.setVisibility(View.INVISIBLE);
                startStopButton.setText("Start");
            }
        }
    };

    @Override
    protected void setup() {
        setContentView(R.layout.home);
    }

    @Override
    protected void initComponents(Bundle savedInstanceState) {


        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String password = getSharedPreferences("secret", MODE_PRIVATE).getString("password", "");
        final File backup = new File(getDatabasePath("gitrepo.db").getParentFile().getPath() + "/gitrepo.zip_enc");
        if (backup.exists() && password.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
            builder.setTitle("Enter encryption password to restore repo: ");

            // Set up the input
            final EditText passwordinput = new EditText(HomeActivity.this);
            passwordinput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordinput.setHint("Type in password");

            passwordinput.setId(R.id.passwordinput);
            LinearLayout layout = new LinearLayout(HomeActivity.this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.addView(passwordinput);
            builder.setView(layout);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                //somehow set checkbox to false
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    backup.delete();
                    SharedPreferences pref = getSharedPreferences("secret", MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("password", "");
                    editor.commit();
                    dialog.cancel();
                    AlertDialog alertDialog = new AlertDialog.Builder(HomeActivity.this).create();
                    alertDialog.setTitle("Are you sure?");
                    alertDialog.setMessage("You will lose all your backups if you click YES");
                    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "NO",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    Intent intent = getIntent();
                                    finish();
                                    startActivity(intent);
                                }
                            });
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "YES",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }
            });
            builder.setCancelable(false);
            final AlertDialog dialog = builder.create();
            dialog.show();
            Button ok = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            passwordinput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (!passwordinput.getText().toString().isEmpty())
                        ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE)
                                .setEnabled(true);
                    else
                        ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE)
                                .setEnabled(false);
                }
            });
            ok.setOnClickListener(new myListener(dialog));
            ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE)
                    .setEnabled(false);


            SharedPreferences.Editor prefsEditor = prefs.edit();
            prefsEditor.putBoolean("firstRun", false);
            prefsEditor.commit();
            new BackupManager(this).dataChanged();

        }

        boolean isSshServiceRunning = GitrepoCommons.isSshServiceRunning(this);

        startStopButton = (Button) findViewById(R.id.homeBtnStartStop);
        startStopButton.setOnClickListener(this);
        if (isSshServiceRunning) {
            startStopButton.setText("Stop");
        } else {
            startStopButton.setText("Start");
        }

        wirelessImageView = (ImageView) findViewById(R.id.homeWirelessImage);
        wifiStatusTextView = (TextView) findViewById(R.id.homeWifiStatus);
        wifiSSIDTextView = (TextView) findViewById(R.id.homeWifiSSID);

        showWifiStatus(this);

        homeServerInfoTextView = (TextView) findViewById(R.id.homeServerInfoTextView);

        if (isSshServiceRunning) {
            homeServerInfoTextView.setVisibility(View.VISIBLE);
            homeServerInfoTextView.setText(GitrepoCommons.getCurrentWifiIpAddress(this) + ":" +
                    prefs.getString(PrefsConstants.SSH_PORT.getKey(), PrefsConstants.SSH_PORT.getDefaultValue()));
        } else {
            homeServerInfoTextView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem settingsMenuItem = menu.add("Settings").setIcon(R.drawable.ic_actionbar_settings);
        settingsMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        settingsMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(C.action.START_PREFERENCE_ACTIVITY);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            }

        });

        MenuItem setupMenuItem = menu.add("Setup");
        setupMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        setupMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(C.action.START_SETUP_ACTIVITY);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            }

        });

        MenuItem dynamicDnsMenuItem = menu.add("Dynamic DNS").setIcon(R.drawable.ic_actionbar_dns);
        dynamicDnsMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        dynamicDnsMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(C.action.START_DYNAMIC_DNS_ACTIVITY);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            }

        });

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(connectivityChangeBroadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        IntentFilter sshdIntentFilter = new IntentFilter();
        sshdIntentFilter.addAction(C.action.SSHD_STARTED);
        sshdIntentFilter.addAction(C.action.SSHD_STOPPED);

        registerReceiver(sshdBroadcastReceiver, sshdIntentFilter);


    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(connectivityChangeBroadcastReceiver);
        unregisterReceiver(sshdBroadcastReceiver);


    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.homeBtnStartStop) {
            boolean isSshServiceRunning = GitrepoCommons.isSshServiceRunning(HomeActivity.this);

            Intent intent = new Intent(v.getContext(), SSHDaemonService.class);
            if (!isSshServiceRunning) {
                if (!GitrepoCommons.isNetworkReady(HomeActivity.this)) {
                    return;
                }

                startService(intent);
            } else {
                stopService(intent);
            }
        }
    }

    private void showWifiStatus(Context context) {
        if (GitrepoCommons.isWifiReady(context)) {
            wifiStatusTextView.setText("WiFi connected to");
            wifiSSIDTextView.setText(GitrepoCommons.getWifiSSID(context));
            wirelessImageView.setImageResource(R.drawable.ic_wireless_enabled);
            startStopButton.setBackgroundResource(R.drawable.blue_btn_selector);
        } else if (GitrepoCommons.isEthernetConnected(context)) {
            wifiStatusTextView.setText("Ethernet connected");
            wifiSSIDTextView.setText("");
            wirelessImageView.setImageResource(R.drawable.ic_wireless_enabled);
            startStopButton.setBackgroundResource(R.drawable.blue_btn_selector);
        } else {
            wifiStatusTextView.setText("WiFi is NOT connected");
            wifiSSIDTextView.setText("");
            wirelessImageView.setImageResource(R.drawable.ic_wireless_disabled);
            startStopButton.setBackgroundResource(R.drawable.white_btn_selector);
            stopAll(this.homeServerInfoTextView);
        }
    }

    private class myListener implements View.OnClickListener {
        private final Dialog dialog;

        public myListener(AlertDialog dialog) {
            this.dialog = dialog;
        }

        @Override
        public void onClick(View v) {
            final EditText passwordinput = (EditText) dialog.findViewById(R.id.passwordinput);
            final String password = passwordinput.getText().toString();
            boolean decrypted = true;
            new AsyncTask<Void, Void, Boolean>() {
                ProgressDialog progressDialog;

                protected void onPreExecute() {
                    progressDialog = ProgressDialog.show(HomeActivity.this, "Standby for decryption", "Decrypting repositories", true);
                }

                protected Boolean doInBackground(Void... unused) {

                    try {
                        prefs = getSharedPreferences("secret", MODE_PRIVATE);
                        return GitrepoBackupAgent.restore(HomeActivity.this, password, prefs, dialog, passwordinput);
                    } catch (BadPaddingException | IOException e) {
                        Log.e("in catch", "");
                        e.printStackTrace();
                    }
                    return false;
                }

                protected void onPostExecute(Boolean decrypted) {
                    if (!decrypted) {
                        Toast.makeText(HomeActivity.this, "Please try again", Toast.LENGTH_SHORT).show();
                    }
                    progressDialog.dismiss();
                }
            }.execute();

            passwordinput.setText("");
        }
    }
}
