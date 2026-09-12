package io.github.gmkbenjamin.gitrepo.beta.dns;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import io.github.gmkbenjamin.gitrepo.beta.ui.util.C;

public class DynDnsStrategy extends DynamicDNS {

    private final static String URL_TEMPLATE = "https://members.dyndns.org/nic/update?hostname=%s&myip=%s";

    private final static String RETURN_CODE_GOOD = "good";
    private final static String RETURN_CODE_NOCHG = "nochg";
    private final static String RETURN_CODE_NOHOST = "nohost";
    private final static String RETURN_CODE_BADAUTH = "badauth";
    private final static String RETURN_CODE_BADAGENT = "badagent";
    private final static String RETURN_CODE_DONATOR = "!donator";
    private final static String RETURN_CODE_ABUSE = "abuse";
    private final static String RETURN_CODE_911 = "911";
    private final static String RETURN_CODE_DNSERR = "dnserr";
    private final static String RETURN_CODE_NOTFQDN = "notfqdn";

    private final static String TAG = DynDnsStrategy.class.getSimpleName();

    public DynDnsStrategy(Context context) {
        super(context);
    }

    public void update(String hostname, String address, String username, String password) {
        HttpURLConnection connection = null;
        try {
            String url = String.format(URL_TEMPLATE, hostname, address);
            Log.i(TAG, "executing request " + url);

            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(3000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Gidder - Android - 1.0");
            String credentials = username + ":" + password;
            String basicAuth = "Basic " + Base64.encodeToString(
                    credentials.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
            connection.setRequestProperty("Authorization", basicAuth);

            int status = connection.getResponseCode();
            Log.i(TAG, "HTTP status: " + status);

            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (stream == null) {
                Log.w(TAG, "Response entity is empty!");
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            StringBuilder contentBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line);
            }
            reader.close();

            String content = contentBuilder.toString().trim();
            if (content.isEmpty()) {
                Log.w(TAG, "Content is empty!");
                return;
            }

            Log.i(TAG, "Content: " + content);

            if (content.startsWith(RETURN_CODE_GOOD) || content.startsWith(RETURN_CODE_NOCHG)) {
                makeToast("Dynamic DNS was successfully updated.");
            } else if (content.startsWith(RETURN_CODE_NOHOST) || content.startsWith(RETURN_CODE_NOTFQDN)) {
                makeToast("Dynamic DNS hostname is incorrect.");
            } else if (content.startsWith(RETURN_CODE_BADAUTH)) {
                makeToast("Dynamic DNS authentication failed.");
            } else if (content.startsWith(RETURN_CODE_BADAGENT)) {
                Log.e(TAG, "Agent information is not correct!");
            } else if (content.startsWith(RETURN_CODE_DONATOR)) {
                Log.w(TAG, "Update request include feature that is not available for the user!");
            } else if (content.startsWith(RETURN_CODE_ABUSE)) {
                makeToast("Dynamic DNS username abuse problem.");
            } else if (content.startsWith(RETURN_CODE_911) || content.startsWith(RETURN_CODE_DNSERR)) {
                makeToast("Dynamic DNS provider has fatal problem.");
            }
        } catch (SocketTimeoutException e) {
            Log.w(TAG, "WiFi is not yet connected! Try again in a minute.", e);
            scheduleRetry();
        } catch (Exception e) {
            Log.e(TAG, "Problem updating dynamic DNS.", e);
            makeToast("Problem updating dynamic DNS.");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void scheduleRetry() {
        Intent broadcastIntent = new Intent(C.action.UPDATE_DYNAMIC_DNS_ADDRESS);
        broadcastIntent.putExtra("scheduled", true);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, broadcastIntent, flags);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60L * 1000L, pendingIntent);
    }
}
