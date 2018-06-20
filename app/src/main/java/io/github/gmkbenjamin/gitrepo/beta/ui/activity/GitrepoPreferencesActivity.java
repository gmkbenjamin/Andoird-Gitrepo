package io.github.gmkbenjamin.gitrepo.beta.ui.activity;

import android.app.AlertDialog;
import android.app.backup.BackupManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.support.v7.app.AppCompatDelegate;

import io.github.gmkbenjamin.gitrepo.beta.R;
import io.github.gmkbenjamin.gitrepo.beta.ui.util.C;
import io.github.gmkbenjamin.gitrepo.beta.ui.util.Logger;
import io.github.gmkbenjamin.gitrepo.beta.ui.util.PrefsConstants;

public class GitrepoPreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    private String password = "";
    private EditTextPreference sshPortPreferences;
    private AppCompatDelegate mDelegate;

    public void emailLog(String[] addresses, String subject, String body, Uri attachment) {
        try {
            FileInputStream file = new FileInputStream(new File(getApplicationInfo().dataDir + "/log"));
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(file));
            String line = null;
            while ((line = reader.readLine()) != null)
                sb.append(line).append("\n");
            reader.close();
            body += "\n\n\n" + sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        //intent.setData(Uri.parse("mailto:")); // only email apps should handle this //This doesn't work on Pixel????
        intent.putExtra(Intent.EXTRA_EMAIL, addresses);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);
        intent.putExtra(Intent.EXTRA_STREAM, attachment);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(Intent.createChooser(intent, "Send email..."));
            finish();
        } else {
            Toast.makeText(this, getResources().getString(R.string.no_email_client), Toast.LENGTH_SHORT).show();
        }

    }

    public ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_AppCompat);


        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        addPreferencesFromResource(R.xml.preferences);

        sshPortPreferences = (EditTextPreference) getPreferenceScreen().findPreference(PrefsConstants.SSH_PORT.getKey());
        final CheckBoxPreference repo_backup = (CheckBoxPreference) getPreferenceManager().findPreference("repo_backup");

        SharedPreferences pref = getSharedPreferences("secret", MODE_PRIVATE);
        boolean backup_enabled = !pref.getString("password", "").isEmpty() ? true : false;
        if (!backup_enabled) {
            pref = PreferenceManager.getDefaultSharedPreferences(GitrepoPreferencesActivity.this);
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("repo_backup", false);
            editor.commit();
            repo_backup.setChecked(false);
            new BackupManager(GitrepoPreferencesActivity.this).dataChanged();
        }
        repo_backup.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {


            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                if (!repo_backup.isChecked()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(GitrepoPreferencesActivity.this);
                    builder.setTitle("Set repo encryption password");

                    // Set up the input
                    final EditText passwordinput = new EditText(GitrepoPreferencesActivity.this);
                    final EditText confirm = new EditText(GitrepoPreferencesActivity.this);
                    // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                    passwordinput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    confirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    passwordinput.setHint("Type in password");
                    confirm.setHint("Confirm password");
                    LinearLayout layout = new LinearLayout(GitrepoPreferencesActivity.this);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    layout.addView(passwordinput);
                    layout.addView(confirm);
                    builder.setView(layout);
                    // Set up the buttons
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        //somehow set checkbox to false
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            password = passwordinput.getText().toString();
                            if (password != null && !password.isEmpty() && password.equals(confirm.getText().toString())) {
                                SharedPreferences pref = getSharedPreferences("secret", MODE_PRIVATE);
                                SharedPreferences.Editor editor = pref.edit();
                                editor.putString("password", password);
                                editor.commit();
                                Toast.makeText(GitrepoPreferencesActivity.this, "Encryption password set.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences pref = getSharedPreferences("secret", MODE_PRIVATE);
                            SharedPreferences.Editor editor = pref.edit();
                            editor.putString("password", "");
                            editor.commit();
                            repo_backup.setChecked(false);
                            dialog.cancel();
                        }
                    });
                    builder.setCancelable(false);
                    final AlertDialog dialog = builder.create();
                    dialog.show();
                    ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE)
                            .setEnabled(false);
                    passwordinput.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            if (TextUtils.isEmpty(s))
                                ((AlertDialog) dialog).getButton(
                                        AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                            if (s.toString().equals(confirm.getText().toString()))
                                ((AlertDialog) dialog).getButton(
                                        AlertDialog.BUTTON_POSITIVE).setEnabled(true);
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
                            if (TextUtils.isEmpty(s))
                                ((AlertDialog) dialog).getButton(
                                        AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                            if (s.toString().equals(passwordinput.getText().toString()))
                                ((AlertDialog) dialog).getButton(
                                        AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        }
                    });
                } else {
                    Toast.makeText(GitrepoPreferencesActivity.this, "Backup disabled, password reset.", Toast.LENGTH_SHORT).show();
                    SharedPreferences pref = getSharedPreferences("secret", MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("password", "");
                    editor.commit();
                }
                return true;
            }
        });

        Preference emaillog = findPreference(getString(R.string.email_log));
        emaillog.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Resources res = getResources();
                emailLog(new String[]{res.getString(R.string.email)}, res.getString(R.string.debug_log), Logger.getIntInfo(), null);

                return true;
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(C.action.START_HOME_ACTIVITY);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            finish();
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {

        super.onResume();

        SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();

        sshPortPreferences.setSummary("SSH server port: " + prefs.getString(PrefsConstants.SSH_PORT.getKey(), PrefsConstants.SSH_PORT.getDefaultValue()));

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {

        super.onPause();

        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(PrefsConstants.SSH_PORT)) {
            sshPortPreferences.setSummary("SSH server port: " + sharedPreferences.getString(
                    PrefsConstants.SSH_PORT.getKey(), PrefsConstants.SSH_PORT.getDefaultValue()));
        }
    }

    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }
}
