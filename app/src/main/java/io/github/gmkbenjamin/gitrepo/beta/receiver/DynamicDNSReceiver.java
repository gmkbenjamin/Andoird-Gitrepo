package io.github.gmkbenjamin.gitrepo.beta.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import io.github.gmkbenjamin.gitrepo.beta.app.GitrepoApplication;
import io.github.gmkbenjamin.gitrepo.beta.dns.DynamicDNSManager;
import io.github.gmkbenjamin.gitrepo.beta.ui.util.GitrepoCommons;

public class DynamicDNSReceiver extends BroadcastReceiver {

    public final static int STANDART_REQUEST = 0;
    public final static int SCHEDULED_REQUEST = 1;
    private final static String TAG = DynamicDNSReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        GitrepoApplication application = (GitrepoApplication) context.getApplicationContext();

        long lastDynDnsUpdateTime = application.getUpdateDynDnsTime();
        boolean scheduled = intent.getBooleanExtra("scheduled", false);
        if (scheduled || ((System.currentTimeMillis() - lastDynDnsUpdateTime > GitrepoApplication.UPDATE_DYNDNS_INTERVAL)
                && GitrepoCommons.isWifiConnected(context))) {

            Log.i(TAG, "DynamicDNSReceiver ready to update!");

            DynamicDNSManager dynDnsManager = new DynamicDNSManager(context);
            dynDnsManager.update();
            application.setUpdateDynDnsTime(System.currentTimeMillis());
        }

    }

}
