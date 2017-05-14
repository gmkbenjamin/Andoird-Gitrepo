package io.github.gmkbenjamin.gitrepo.beta.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.util.Log;

import io.github.gmkbenjamin.gitrepo.beta.app.GitrepoApplication;
import io.github.gmkbenjamin.gitrepo.beta.dns.DynamicDNSManager;
import io.github.gmkbenjamin.gitrepo.beta.service.SSHDaemonService;
import io.github.gmkbenjamin.gitrepo.beta.ui.util.GitrepoCommons;
import io.github.gmkbenjamin.gitrepo.beta.ui.util.PrefsConstants;

public class ConnectivityChangeBroadcastReceiver extends BroadcastReceiver {
    private final static String TAG = ConnectivityChangeBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(final Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean autostartOnWifiOn = prefs.getBoolean(PrefsConstants.AUTOSTART_ON_WIFI_ON.getKey(), false);
            boolean autostopOnWifiOff = prefs.getBoolean(PrefsConstants.AUTOSTOP_ON_WIFI_OFF.getKey(), false);

            if (GitrepoCommons.isWifiReady(context)) {
                Log.i(TAG, "[" + GitrepoCommons.getWifiSSID(context) + "] WiFi is active!");

                final GitrepoApplication application = (GitrepoApplication) context.getApplicationContext();
                long lastDynDnsUpdateTime = application.getUpdateDynDnsTime();
                if ((System.currentTimeMillis() - lastDynDnsUpdateTime > GitrepoApplication.UPDATE_DYNDNS_INTERVAL)) {
                    new DynamicDNSManager(context).update();
                    application.setUpdateDynDnsTime(System.currentTimeMillis());
                }

                if (autostartOnWifiOn && !GitrepoCommons.isSshServiceRunning(context)) {
                    context.startService(new Intent(context, SSHDaemonService.class));
                    GitrepoCommons.makeStatusBarNotification(context);
                }
            } else {
                Log.i(TAG, "WiFi is NOT active!");
                Intent startServiceIntent = new Intent(context, SSHDaemonService.class);
                if (autostopOnWifiOff && GitrepoCommons.isSshServiceRunning(context)) {
                    context.stopService(startServiceIntent);
                    GitrepoCommons.stopStatusBarNotification(context);
                }
            }
        }

    }

}
