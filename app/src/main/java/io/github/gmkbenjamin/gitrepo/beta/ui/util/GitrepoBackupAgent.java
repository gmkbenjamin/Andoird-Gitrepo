package io.github.gmkbenjamin.gitrepo.beta.ui.util;

import android.app.Dialog;
import android.app.backup.BackupAgentHelper;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.EditText;

import org.bouncycastle.crypto.CryptoException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.BadPaddingException;


public class GitrepoBackupAgent extends BackupAgentHelper {

    // The name of the SharedPreferences file
    static final String PREFS = "io.github.gmkbenjamin.gitrepo.beta_preferences";
    static final String DB_NAME = "gitrepo.db";

    // A key to uniquely identify the set of backup data
    static final String PREFS_BACKUP_KEY = "prefs";

    public static boolean restore(Context ctx, String password, SharedPreferences prefs, Dialog dialog, EditText passwordinput) throws BadPaddingException, IOException {
        try {

            Crypto.decrypt(password, new File(ctx.getDatabasePath("gitrepo.db").getParent() + "/gitrepo.zip_enc"), new File(ctx.getDatabasePath("gitrepo.db").getParent() + "/gitrepo.zip"));
            GitrepoBackupAgent.unzip(new File(ctx.getDatabasePath("gitrepo.db").getParent() + "/gitrepo.zip"), new File(Environment.getExternalStorageDirectory().getPath()));
            GitrepoBackupAgent.populateDirectory();
            new File(ctx.getDatabasePath("gitrepo.db").getParent() + "/gitrepo.zip_enc").delete();
            new File(ctx.getDatabasePath("gitrepo.db").getParent() + "/gitrepo.zip").delete();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("password", password);
            editor.commit();
            dialog.dismiss();
            return true;
        } catch (CryptoException e) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("password", "");
            editor.commit();
            e.printStackTrace();
            return false;
        }
    }

    public static void populateDirectory() {
        File repo = new File(Environment.getExternalStorageDirectory().getPath() + "/gitrepo/repositories");
        File[] repos = repo.listFiles();
        for (File file : repos) {
            if (file.isDirectory() && file.getPath().contains(".git")) {
                File directory = new File(file.getPath() + "/branches");
                if (!directory.exists())
                    directory.mkdir();
                directory = new File(file.getPath() + "/hooks");
                if (!directory.exists())
                    directory.mkdir();
                directory = new File(file.getPath() + "/logs");
                if (!directory.exists()) {
                    directory.mkdir();
                    directory = new File(file.getPath() + "/logs/refs");
                    if (!directory.exists()) {
                        directory.mkdir();
                        directory = new File(file.getPath() + "/logs/refs/heads");
                        if (!directory.exists())
                            directory.mkdir();
                    }
                }
                directory = new File(file.getPath() + "/objects");
                if (!directory.exists()) {
                    directory.mkdir();
                    directory = new File(file.getPath() + "/objects/info");
                    if (!directory.exists())
                        directory.mkdir();
                    directory = new File(file.getPath() + "/objects/pack");
                    if (!directory.exists())
                        directory.mkdir();
                }
                directory = new File(file.getPath() + "/refs");
                if (!directory.exists()) {
                    directory.mkdir();
                    directory = new File(file.getPath() + "/refs/heads");
                    if (!directory.exists())
                        directory.mkdir();
                    directory = new File(file.getPath() + "/refs/tags");
                    if (!directory.exists())
                        directory.mkdir();
                }
            }
        }
    }

    public static boolean zipFileAtPath(String sourcePath, String toLocation) {
        final int BUFFER = 2048;

        File sourceFile = new File(sourcePath);
        try {
            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(toLocation);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                    dest));
            if (sourceFile.isDirectory()) {
                zipSubFolder(out, sourceFile, sourceFile.getParent().length());
            } else {
                byte data[] = new byte[BUFFER];
                FileInputStream fi = new FileInputStream(sourcePath);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(getLastPathComponent(sourcePath));
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static void zipSubFolder(ZipOutputStream out, File folder,
                                     int basePathLength) throws IOException {

        final int BUFFER = 2048;

        File[] fileList = folder.listFiles();
        BufferedInputStream origin = null;
        for (File file : fileList) {
            if (file.isDirectory()) {
                zipSubFolder(out, file, basePathLength);
            } else {
                byte data[] = new byte[BUFFER];
                String unmodifiedFilePath = file.getPath();
                String relativePath = unmodifiedFilePath
                        .substring(basePathLength);
                FileInputStream fi = new FileInputStream(unmodifiedFilePath);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(relativePath);
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }
        }
    }


    /*
 *
 * Zips a file at a location and places the resulting zip file at the toLocation
 * Example: zipFileAtPath("downloads/myfolder", "downloads/myFolder.zip");
 */

    /*
     * gets the last path component
     *
     * Example: getLastPathComponent("downloads/example/fileToZip");
     * Result: "fileToZip"
     */
    public static String getLastPathComponent(String filePath) {
        String[] segments = filePath.split("/");
        if (segments.length == 0)
            return "";
        String lastPathComponent = segments[segments.length - 1];
        return lastPathComponent;
    }

/*
 *
 * Zips a subfolder
 *
 */

    public static void unzip(File zipFile, File targetDirectory) throws IOException {
        ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipFile)));
        try {
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(targetDirectory, ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs())
                    throw new FileNotFoundException("Failed to ensure directory: " +
                            dir.getAbsolutePath());
                if (ze.isDirectory())
                    continue;
                FileOutputStream fout = new FileOutputStream(file);
                try {
                    while ((count = zis.read(buffer)) != -1)
                        fout.write(buffer, 0, count);
                } finally {
                    fout.close();
                }
            /* if time should be restored as well
            long time = ze.getTime();
            if (time > 0)
                file.setLastModified(time);
            */
            }
        } finally {
            zis.close();
        }
    }

    // Allocate a helper and add it to the backup agent
    @Override
    public void onCreate() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean backup = (pref.getBoolean(PrefsConstants.REPO_BACKUP.getKey(), Boolean.parseBoolean(PrefsConstants.REPO_BACKUP.getDefaultValue())));
        pref = getSharedPreferences("secret", MODE_PRIVATE);
        String password = (pref.getString("password", ""));
        if (backup && !password.isEmpty() && zipFileAtPath(Environment.getExternalStorageDirectory().getPath() + "/gitrepo", getDatabasePath(DB_NAME).getParent() + "/gitrepo.zip")) {
            try {
                Crypto.encrypt(password, new File(getDatabasePath(DB_NAME).getParent() + "/gitrepo.zip"), new File(getDatabasePath(DB_NAME).getParent() + "/gitrepo.zip_enc"));
                new File(getDatabasePath(DB_NAME).getParent() + "/gitrepo.zip").delete();
            } catch (CryptoException e) {
                e.printStackTrace();
            }
            FileBackupHelper repo = new FileBackupHelper(this, "gitrepo.zip");
            addHelper("repo", repo);
        }
        if (true) {
            FileBackupHelper dbs = new FileBackupHelper(this, DB_NAME);
            addHelper("dbs", dbs);
            SharedPreferencesBackupHelper helper =
                    new SharedPreferencesBackupHelper(this, PREFS);
            addHelper(PREFS_BACKUP_KEY, helper);
        }
    }

    @Override
    public File getFilesDir() {
        File path = getDatabasePath(DB_NAME);
        return path.getParentFile();

    }
}
