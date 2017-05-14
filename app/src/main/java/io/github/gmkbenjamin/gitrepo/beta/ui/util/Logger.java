package io.github.gmkbenjamin.gitrepo.beta.ui.util;

import android.app.Activity;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;


public class Logger extends Activity {
    private final String logFile = getApplicationInfo().dataDir + "/log";

    public static String getIntInfo() {
        String intfaceInfo = "";
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intface : interfaces) {
                intfaceInfo += "Display name: " + intface.getDisplayName() + "\n";
                intfaceInfo += "Name: " + intface.getName() + "\n";
                intfaceInfo += "IPs: ";
                for (InterfaceAddress ip : intface.getInterfaceAddresses()) {
                    intfaceInfo += ip.getAddress().toString() + "\n";
                }

            }
        } catch (SocketException e) {
            e.printStackTrace();
            intfaceInfo += e.getMessage();
        }

        return intfaceInfo;
    }

    public String getTime() {
        String time = "";
        Calendar calendar = Calendar.getInstance();
        time += calendar.get(Calendar.DATE) + " ";
        time += calendar.get(Calendar.HOUR) + ":";
        time += calendar.get(Calendar.MINUTE) + ":";
        time += calendar.get(Calendar.SECOND);
        return time;
    }

    public boolean log(String message) {
        try {
            OutputStreamWriter out = new OutputStreamWriter(openFileOutput(logFile, MODE_APPEND));
            out.write(getTime() + "\n");
            out.write(message);
            out.write("\n\n\n\n");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
