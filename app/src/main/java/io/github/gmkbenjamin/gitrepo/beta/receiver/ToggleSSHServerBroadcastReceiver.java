package io.github.gmkbenjamin.gitrepo.beta.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import io.github.gmkbenjamin.gitrepo.beta.service.SSHDaemonService;
import io.github.gmkbenjamin.gitrepo.beta.ui.util.C;
import io.github.gmkbenjamin.gitrepo.beta.ui.util.GitrepoCommons;

public class ToggleSSHServerBroadcastReceiver extends BroadcastReceiver {
    private final static String TAG = ToggleSSHServerBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(C.action.TOGGLE_SSH_SERVER)) {
            if (GitrepoCommons.isSshServiceRunning(context)) {
                context.stopService(new Intent(context, SSHDaemonService.class));
                Log.i(TAG, "Broadcast - stop service!");
            } else if (!GitrepoCommons.isNetworkReady(context)) {
                Toast.makeText(context, "Network is NOT ready!", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "Broadcast failed - network is NOT ready!");
            } else {
                Intent serviceIntent = new Intent(context, SSHDaemonService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
                Log.i(TAG, "Broadcast - start service!");
            }
        }
    }
}
