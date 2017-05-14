package io.github.gmkbenjamin.gitrepo.beta.dns;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

public abstract class DynamicDNS {

    protected Context context;
    private Handler toastHandler;

    public DynamicDNS(Context context) {
        this.context = context;
    }

    public abstract void update(String hostname, String address, String username, String password);

    public void setToastHandler(Handler toastHandler) {
        this.toastHandler = toastHandler;
    }

    public void makeToast(final String text) {
        toastHandler.post(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
            }

        });
    }

}
