package io.github.gmkbenjamin.gitrepo.beta.app;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import io.github.gmkbenjamin.gitrepo.beta.ui.util.C;

public class GitrepoApplication extends Application {
    public final static long UPDATE_DYNDNS_INTERVAL = 10L * 60L * 1000L;
    private final static String TAG = GitrepoApplication.class.getSimpleName();
    private static GitrepoApplication instance;

    private long updateDynDnsTime = 0;

    public GitrepoApplication() {
        GitrepoApplication.instance = this;
    }

    public synchronized static GitrepoApplication getInstance() {
        if (instance == null) {
            throw new IllegalStateException("There is no Application initialized!");
        }
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "[App] Started!");

        Intent intent = new Intent(C.action.UPDATE_DYNAMIC_DNS_ADDRESS);
        sendBroadcast(intent);
    }

    public long getUpdateDynDnsTime() {
        return updateDynDnsTime;
    }

    public void setUpdateDynDnsTime(long updateDynDnsTime) {
        this.updateDynDnsTime = updateDynDnsTime;
    }

}
