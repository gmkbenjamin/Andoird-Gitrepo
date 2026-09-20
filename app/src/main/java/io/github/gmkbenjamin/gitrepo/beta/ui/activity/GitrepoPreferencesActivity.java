package io.github.gmkbenjamin.gitrepo.beta.ui.activity;

import android.app.AlertDialog;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import io.github.gmkbenjamin.gitrepo.beta.R;
import io.github.gmkbenjamin.gitrepo.beta.ui.util.C;
import io.github.gmkbenjamin.gitrepo.beta.ui.util.PrefsConstants;

public class GitrepoPreferencesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new GitrepoPreferenceFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(C.action.START_HOME_ACTIVITY);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            finish();
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class GitrepoPreferenceFragment extends PreferenceFragmentCompat
            implements OnSharedPreferenceChangeListener {

        private String password = "";
        private EditTextPreference sshPortPreferences;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            sshPortPreferences = findPreference(PrefsConstants.SSH_PORT.getKey());
            final CheckBoxPreference repoBackup = findPreference("repo_backup");

            if (repoBackup != null) {
                repoBackup.setChecked(false);
                repoBackup.setEnabled(false);
                repoBackup.setSummary("Automatic backup is disabled to protect credentials. "
                        + "Keep repository copies using Git; existing encrypted archives can still be restored.");
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            if (sshPortPreferences != null) {
                sshPortPreferences.setSummary("SSH server port: " + prefs.getString(
                        PrefsConstants.SSH_PORT.getKey(), PrefsConstants.SSH_PORT.getDefaultValue()));
            }
            prefs.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (PrefsConstants.SSH_PORT.getKey().equals(key) && sshPortPreferences != null) {
                sshPortPreferences.setSummary("SSH server port: " + sharedPreferences.getString(
                        PrefsConstants.SSH_PORT.getKey(), PrefsConstants.SSH_PORT.getDefaultValue()));
            }
        }
    }
}
