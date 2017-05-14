package io.github.gmkbenjamin.gitrepo.beta.service;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import org.apache.mina.util.Base64;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.ForwardingFilter;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.SshdSocketAddress;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.shell.ProcessShellFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.sql.SQLException;

import io.github.gmkbenjamin.gitrepo.beta.R;
import io.github.gmkbenjamin.gitrepo.beta.db.DBHelper;
import io.github.gmkbenjamin.gitrepo.beta.db.entity.User;
import io.github.gmkbenjamin.gitrepo.beta.ssh.GitrepoCommandFactory;
import io.github.gmkbenjamin.gitrepo.beta.ssh.GitrepoHostKeyProvider;
import io.github.gmkbenjamin.gitrepo.beta.ssh.NoShell;
import io.github.gmkbenjamin.gitrepo.beta.ui.util.C;
import io.github.gmkbenjamin.gitrepo.beta.ui.util.GitrepoCommons;
import io.github.gmkbenjamin.gitrepo.beta.ui.util.PrefsConstants;
import io.github.gmkbenjamin.gitrepo.beta.ui.widget.ToggleAppWidgetProvider;

public class SSHDaemonService extends Service implements PasswordAuthenticator, PublickeyAuthenticator {
    private final static String TAG = SSHDaemonService.class.getSimpleName();

    private SshServer sshServer;
    private DBHelper dbHelper;
    private byte[] bytes;
    private int pos;

    public SSHDaemonService() {
        Log.i(TAG, "Construct SSHDaemonService!");
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "SSHd onCreate!");

        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "Service BIND!");

        return null;
    }

    @Override
    public void onDestroy() {
        try {
            if (sshServer != null) {
                sshServer.stop(true);
                sendBroadcast(new Intent(C.action.SSHD_STOPPED));

                toggleWidgetState(false);
                GitrepoCommons.stopStatusBarNotification(this);
            }
            Log.i(TAG, "SSHd stopped!");
        } catch (InterruptedException e) {
            Log.e(TAG, "Problem when stopping SSHd.", e);
        } finally {
            if (dbHelper != null) {
                dbHelper.close();
            }
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        dbHelper = new DBHelper(this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SSHDaemonService.this);
        String sshPort = prefs.getString(PrefsConstants.SSH_PORT.getKey(), PrefsConstants.SSH_PORT.getDefaultValue());
        boolean sshShell = prefs.getBoolean(PrefsConstants.SSH_SHELL.getKey(),
                "true".equals(PrefsConstants.SSH_SHELL.getDefaultValue()) ? true : false);
        boolean scp = prefs.getBoolean(PrefsConstants.SCP.getKey(), "true".equals(PrefsConstants.SCP.getDefaultValue()) ? true : false);
        sshServer = SshServer.setUpDefaultServer();
        sshServer.setPort(Integer.parseInt(sshPort));
        sshServer.setKeyPairProvider(new GitrepoHostKeyProvider(this));
        if (sshShell) {
            sshServer.setShellFactory(new ProcessShellFactory(new String[]{"/system/bin/sh", "-i", "-l"})); // use ssh -T option on the client
            sshServer.setTcpipForwardingFilter(new ForwardingFilter() {
                @Override
                public boolean canForwardAgent(Session session) {
                    return true;
                }

                @Override
                public boolean canForwardX11(Session session) {
                    return true;
                }

                @Override
                public boolean canListen(SshdSocketAddress address, Session session) {
                    return true;
                }

                @Override
                public boolean canConnect(SshdSocketAddress address, Session session) {
                    return true;
                }
            });
        } else {
            sshServer.setShellFactory(new NoShell());
        }

        if (scp)
            sshServer.setCommandFactory(new ScpCommandFactory(new GitrepoCommandFactory(this)));
        else
            sshServer.setCommandFactory(new GitrepoCommandFactory(this));
        sshServer.setPasswordAuthenticator(this);
        sshServer.setPublickeyAuthenticator(this);

        try {
            sshServer.start();
            sendBroadcast(new Intent(C.action.SSHD_STARTED));

            toggleWidgetState(true);

            boolean isStatusBarNotificationEnabled = prefs.getBoolean(PrefsConstants.STATUSBAR_NOTIFICATION.getKey(),
                    "true".equals(PrefsConstants.STATUSBAR_NOTIFICATION.getDefaultValue()) ? true : false);

            if (isStatusBarNotificationEnabled) {
                GitrepoCommons.makeStatusBarNotification(this);
            }

            Log.i(TAG, "SSHd started!");
        } catch (IOException e) {
            Log.e(TAG, "Problem when starting SSHd.", e);
        }

        return START_STICKY;
    }

    private void toggleWidgetState(boolean runningState) {
        RemoteViews widgetViews = new RemoteViews(getPackageName(), R.layout.toggle_widget);
        widgetViews.setImageViewResource(R.id.toggleWidgetButton, runningState ? R.drawable.ic_widget_active : R.drawable.ic_widget_inactive);

        ComponentName widget = new ComponentName(this, ToggleAppWidgetProvider.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetManager.updateAppWidget(widget, widgetViews);
    }

    @Override
    public boolean authenticate(String username, String password, ServerSession session) {
        if (password == null || "".equals(password.trim())) {
            return false;
        }

        // Query for user by username
        try {
            User user = dbHelper.getUserDao().queryForUsernameAndActive(username);

            if (user == null) {
                return false;
            }

            String passwordSha256 = GitrepoCommons.generateSha256(password);
            Log.i(TAG, "Password SHA256: " + passwordSha256);

            if (passwordSha256.equals(user.getPassword())) {
                return true;
            }
        } catch (SQLException e) {
            Log.e(TAG, "Problem while retrieving user from database.", e);
            return false;
        }

        return false;
    }

    @Override
    public boolean authenticate(String username, PublicKey key, ServerSession session) {
        if (key == null) {
            return false;
        }

        if (key instanceof RSAPublicKey) {
            try {
                User user = dbHelper.getUserDao().queryForUsernameAndActive(username);
                if (user == null) {
                    return false;
                }

                if (user.getPublickey() == null || "".equals(user.getPublickey().trim())) {
                    return false;
                }

                PublicKey knownkey = decodePublicKey(user.getPublickey());
                return ((RSAPublicKey) knownkey).getModulus().equals(((RSAPublicKey) key).getModulus());

            } catch (SQLException e) {
                Log.e(TAG, "Problem while retrieving user form database.", e);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Problem while decoding the public key.", e);
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, "Problem while decoding the public key.", e);
            } catch (InvalidKeySpecException e) {
                Log.e(TAG, "Problem while decoding the public key.", e);
            }
        }

        return false;
    }

    private PublicKey decodePublicKey(String keystring) throws IllegalArgumentException, NoSuchAlgorithmException, InvalidKeySpecException {
        bytes = null;
        pos = 0;

        for (String part : keystring.split(" ")) {
            if (part.startsWith("AAAA")) {
                bytes = Base64.decodeBase64(part.getBytes());
                break;
            }
        }

        if (bytes == null) {
            throw new IllegalArgumentException("No Base64 part to decode.");
        }

        String type = decodeType();
        if (type.equals("ssh-rsa")) {
            BigInteger e = decodeBigInt();
            BigInteger m = decodeBigInt();
            RSAPublicKeySpec spec = new RSAPublicKeySpec(m, e);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } else {
            throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    private String decodeType() {
        int len = decodeInt();
        String type = new String(bytes, pos, len);
        pos += len;
        return type;
    }

    private int decodeInt() {
        return ((bytes[pos++] & 0xFF) << 24) | ((bytes[pos++] & 0xFF) << 16) | ((bytes[pos++] & 0xFF) << 8) | (bytes[pos++] & 0xFF);
    }

    private BigInteger decodeBigInt() {
        int len = decodeInt();
        byte[] bigIntBytes = new byte[len];
        System.arraycopy(bytes, pos, bigIntBytes, 0, len);
        pos += len;
        return new BigInteger(bigIntBytes);
    }

}
