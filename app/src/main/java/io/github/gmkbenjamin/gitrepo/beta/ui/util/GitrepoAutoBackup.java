package io.github.gmkbenjamin.gitrepo.beta.ui.util;

import android.annotation.TargetApi;
import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;

import org.bouncycastle.crypto.CryptoException;

import java.io.File;
import java.io.IOException;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class GitrepoAutoBackup extends BackupAgent {
    static final String DB_NAME = "gitrepo.db";
    String password = "";

    @Override
    public void onCreate() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean backup = (pref.getBoolean(PrefsConstants.REPO_BACKUP.getKey(), Boolean.parseBoolean(PrefsConstants.REPO_BACKUP.getDefaultValue())));
        pref = getSharedPreferences("secret", MODE_PRIVATE);
        String password = (pref.getString("password", ""));
        if (backup && !password.isEmpty() && GitrepoBackupAgent.zipFileAtPath(Environment.getExternalStorageDirectory().getPath() + "/gitrepo", getDatabasePath(DB_NAME).getParent() + "/gitrepo.zip")) {
            try {
                Crypto.encrypt("Pa55w0rd", new File(getDatabasePath(DB_NAME).getParent() + "/gitrepo.zip"), new File(getDatabasePath(DB_NAME).getParent() + "/gitrepo.zip_enc"));
                new File(getDatabasePath(DB_NAME).getParent() + "/gitrepo.zip").delete();
            } catch (CryptoException e) {
                e.printStackTrace();
            }
        }
        //erase password before backup
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("password", "");
        editor.commit();
        super.onCreate();
    }

    @Override
    public void onBackup(ParcelFileDescriptor parcelFileDescriptor, BackupDataOutput backupDataOutput, ParcelFileDescriptor parcelFileDescriptor1) throws IOException {

    }

    @Override
    public void onRestore(BackupDataInput backupDataInput, int i, ParcelFileDescriptor parcelFileDescriptor) throws IOException {


    }

    @Override
    public void onDestroy() {
        //restore password after backup
        SharedPreferences pref = this.getSharedPreferences("secret", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("password", password);
        editor.commit();
        super.onDestroy();
    }
}
