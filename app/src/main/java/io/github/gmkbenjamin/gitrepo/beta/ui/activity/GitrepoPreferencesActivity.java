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

            SharedPreferences pref = requireContext().getSharedPreferences("secret", Context.MODE_PRIVATE);
            boolean backupEnabled = !pref.getString("password", "").isEmpty();
            if (!backupEnabled && repoBackup != null) {
                pref = PreferenceManager.getDefaultSharedPreferences(requireContext());
                SharedPreferences.Editor editor = pref.edit();
                editor.putBoolean("repo_backup", false);
                editor.commit();
                repoBackup.setChecked(false);
                new BackupManager(requireContext()).dataChanged();
            }

            if (repoBackup != null) {
                repoBackup.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (!repoBackup.isChecked()) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                            builder.setTitle("Set repo encryption password");

                            final EditText passwordInput = new EditText(requireContext());
                            final EditText confirm = new EditText(requireContext());
                            passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            confirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            passwordInput.setHint("Type in password");
                            confirm.setHint("Confirm password");
                            LinearLayout layout = new LinearLayout(requireContext());
                            layout.setOrientation(LinearLayout.VERTICAL);
                            layout.addView(passwordInput);
                            layout.addView(confirm);
                            builder.setView(layout);
                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    password = passwordInput.getText().toString();
                                    if (password != null && !password.isEmpty()
                                            && password.equals(confirm.getText().toString())) {
                                        SharedPreferences secret = requireContext()
                                                .getSharedPreferences("secret", Context.MODE_PRIVATE);
                                        SharedPreferences.Editor editor = secret.edit();
                                        editor.putString("password", password);
                                        editor.commit();
                                        Toast.makeText(requireContext(), "Encryption password set.",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    SharedPreferences secret = requireContext()
                                            .getSharedPreferences("secret", Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = secret.edit();
                                    editor.putString("password", "");
                                    editor.commit();
                                    repoBackup.setChecked(false);
                                    dialog.cancel();
                                }
                            });
                            builder.setCancelable(false);
                            final AlertDialog dialog = builder.create();
                            dialog.show();
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                            passwordInput.addTextChangedListener(new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                }

                                @Override
                                public void onTextChanged(CharSequence s, int start, int before, int count) {
                                }

                                @Override
                                public void afterTextChanged(Editable s) {
                                    if (TextUtils.isEmpty(s)) {
                                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                    }
                                    if (s.toString().equals(confirm.getText().toString())) {
                                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                    }
                                }
                            });
                            confirm.addTextChangedListener(new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                }

                                @Override
                                public void onTextChanged(CharSequence s, int start, int before, int count) {
                                }

                                @Override
                                public void afterTextChanged(Editable s) {
                                    if (TextUtils.isEmpty(s)) {
                                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                    }
                                    if (s.toString().equals(passwordInput.getText().toString())) {
                                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                    }
                                }
                            });
                        } else {
                            Toast.makeText(requireContext(), "Backup disabled, password reset.",
                                    Toast.LENGTH_SHORT).show();
                            SharedPreferences secret = requireContext()
                                    .getSharedPreferences("secret", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = secret.edit();
                            editor.putString("password", "");
                            editor.commit();
                        }
                        return true;
                    }
                });
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
