package io.github.gmkbenjamin.gitrepo.beta.ui.util;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.util.encoders.Hex;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import io.github.gmkbenjamin.gitrepo.beta.R;
import io.github.gmkbenjamin.gitrepo.beta.service.SSHDaemonService;

public abstract class GitrepoCommons {
    private final static int SSH_STARTED_NOTIFICATION_ID = 1;
    private static boolean hotspot = false;

    public static int convertDpToPixels(WindowManager windowManager, float dp) {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        float logicalDensity = metrics.density;

        return (int) (dp * logicalDensity + 0.5);
    }

    public static boolean isWifiReady(Context context) {
        String ip = getCurrentWifiIpAddress(context);
        String ssid = getWifiSSID(context);

        if (ssid != null && !"".equals(ssid) && ip != null) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isNetworkReady(Context context) {
        return isWifiReady(context) || isEthernetConnected(context);
    }

    public static boolean isEthernetConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
        //NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (info.isConnected()) {
            return true;
        } else {
            if (hotspot)
                return true;
            return false;
        }
    }

    public static String getWifiSSID(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (!info.isConnected() && hotspot)
            return "Hotspot Mode";
        return wifiManager.getConnectionInfo().getSSID();
    }

    public static int convertInet4AddrToInt(byte[] addr) {
        int addrInt = 0;

        byte[] reversedAddr = reverse(addr);
        for (int i = 0; i < reversedAddr.length; i++) {
            addrInt = (addrInt << 8) | (reversedAddr[i] & 0xFF);
        }

        return addrInt;
    }

    public static byte[] convertIntToInet4Addr(int addrInt) {
        byte[] addr = new byte[4];

        for (int i = 0; i < 4; i++) {
            addr[i] = (byte) ((addrInt >> i * 8) & 0xFF);
        }

        return addr;
    }

    public static byte[] reverse(byte[] array) {
        int limit = array.length / 2;
        byte[] reversedArray = new byte[array.length];

        for (int i = 0; i < limit; i++) {
            reversedArray[i] = array[array.length - i - 1];
            reversedArray[reversedArray.length - i - 1] = array[i];
        }

        return reversedArray;
    }

    public static String getCurrentAddress(Context context) {
        String addr = getCurrentWifiIpAddress(context);
        if (addr == null) {
            return getFirstNonLoopbackAddress();
        }
        return addr;
    }

    public static String getFirstNonLoopbackAddress() {
        String result = "";
        try {

            for (Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements(); ) {
                NetworkInterface ni = interfaces.nextElement();
                for (Enumeration<InetAddress> iaddress = ni.getInetAddresses(); iaddress.hasMoreElements(); ) {
                    InetAddress ia = iaddress.nextElement();
                    byte[] addr = ia.getAddress();
                    Log.i("Commons", "interface address:" + ia.toString() + " host:" + ia.getHostAddress() + " loopback:" + ia.isLoopbackAddress() +
                            " addr:" + formatIpAddress(addr));
                    if (!ia.isLoopbackAddress() && addr != null && addr.length == 4) {
                        result = formatIpAddress(addr);
                    }
                }
            }

        } catch (Exception e) {
            Log.e("Commons", "getFirstNonLoopbackAddress: '" + e.getMessage() + "'", e);
        }
        return result;
    }


    public static String getCurrentWifiIpAddress(Context context) {
        WifiManager myWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        WifiInfo myWifiInfo = myWifiManager.getConnectionInfo();
        int ipAddress = myWifiInfo.getIpAddress();

        if (ipAddress != 0) {
            return formatIpAddress(ipAddress);
        } else {
            //Is it on hotspot mode?
            try {
                List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
                for (NetworkInterface intf : interfaces) {
                    if (intf.getName().contains("wlan") || intf.getDisplayName().contains("wlan")) {
                        if (intf.isUp() && intf.getInterfaceAddresses().size() > 0) {
                            hotspot = true;
                            return intf.getInterfaceAddresses().get(0).getAddress().toString().replace("/", "");
                        }

                    }
                    if (intf.isUp() && intf.getInterfaceAddresses().size() > 0 &&
                            intf.getInterfaceAddresses().get(0).getAddress().toString().contains("192.168.43.1")) {
                        hotspot = true;
                        return intf.getInterfaceAddresses().get(0).getAddress().toString().replace("/", "");
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private static String formatIpAddress(int ipAddress) {
        byte[] addr = GitrepoCommons.convertIntToInet4Addr(ipAddress);
        return formatIpAddress(addr);
    }

    private static String formatIpAddress(byte[] addr) {
        StringBuffer addressBuffer = new StringBuffer();
        for (byte b : addr) {
            if (!(addressBuffer.length() == 0)) {
                addressBuffer.append('.');
            }
            addressBuffer.append(String.valueOf(b & 0xff));
        }

        return addressBuffer.toString();
    }

    public static String getCurrentServerAddress(Context context, SharedPreferences prefs) {
        return getCurrentAddress(context) + ':' + prefs.getString(PrefsConstants.SSH_PORT.getKey(), PrefsConstants.SSH_PORT.getDefaultValue());
    }

    public static String generateSha256(String data) {
        byte[] dataBytes = data.getBytes();
        SHA256Digest sha256 = new SHA256Digest();
        sha256.reset();
        sha256.update(dataBytes, 0, dataBytes.length);

        int outputSize = sha256.getDigestSize();
        byte[] dataDigest = new byte[outputSize];

        sha256.doFinal(dataDigest, 0);

        String dataSha256 = new String(Hex.encode(dataDigest));

        return dataSha256;
    }

    public static String toCamelCase(String s) {
        String[] parts = s.split("_|\\s+");
        StringBuffer camelCaseString = new StringBuffer();

        boolean isFirst = true;
        for (String part : parts) {
            Log.i("Commons", "Camel case: '" + part + "'");
            camelCaseString.append(toProperCase(part, isFirst));
            isFirst = false;
        }

        return camelCaseString.toString();
    }

    private static String toProperCase(String s, boolean firstLetterSmall) {
        if (firstLetterSmall) {
            return s.toLowerCase();
        }

        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    public static void makeStatusBarNotification(Context context) {
        Intent notificationIntent = new Intent(C.action.START_HOME_ACTIVITY);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 1, notificationIntent, 0);

        String currentAddress = GitrepoCommons.getCurrentServerAddress(context, PreferenceManager.getDefaultSharedPreferences(context));

        Notification notification = new NotificationCompat.Builder(context)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setTicker("SSH server started!")
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentText(currentAddress)
                .setContentTitle("SSH server is running")
                .setOngoing(true)
                .setWhen(System.currentTimeMillis())
                .build();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(SSH_STARTED_NOTIFICATION_ID, notification);
    }

    public static boolean isSshServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SSHDaemonService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void stopStatusBarNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(SSH_STARTED_NOTIFICATION_ID);
    }
}
