package io.github.gmkbenjamin.gitrepo.beta.ui.util;

import android.content.Context;
import androidx.preference.PreferenceManager;
import java.io.File;

/** Removes obsolete plaintext backup credentials; automatic backups are disabled. */
public final class BackupPrivacy {
    private BackupPrivacy() { }

    public static void clearLegacyCredentials(Context context) {
        boolean secretCleared = context.getSharedPreferences("secret", Context.MODE_PRIVATE)
                .edit().remove("password").commit();
        boolean preferencesCleared = PreferenceManager.getDefaultSharedPreferences(context)
                .edit().remove("password").putBoolean("repo_backup", false).commit();
        if (!secretCleared || !preferencesCleared) {
            throw new IllegalStateException("Cannot remove legacy backup credentials");
        }
        deletePlaintext(new File(context.getDatabasePath("gitrepo.db").getParentFile(), "gitrepo.zip"));
        deletePlaintext(new File(context.getFilesDir(), "gitrepo.zip"));
    }

    private static void deletePlaintext(File file) {
        if (file.exists() && !file.delete()) {
            throw new IllegalStateException("Cannot remove obsolete plaintext backup");
        }
    }
}
