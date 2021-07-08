package com.meekworth.lwdronecam;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "LWDroneCam/SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    // "SettingsFragment must be a public static class to be properly recreated from
    // instance state."
    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            Preference pref;

            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            if ((pref = findPreference(getString(R.string.settings_key_cam_ip))) != null) {
                setOnChangeListener(pref, new IPPreferenceValidator(), R.string.invalid_ip);
                setEditTextSummaryProvider((EditTextPreference)pref);
            }

            if ((pref = findPreference(getString(R.string.settings_key_cam_stream_port))) != null) {
                ((EditTextPreference)pref).setOnBindEditTextListener(
                        editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER)
                );
                setOnChangeListener(pref, new PortPreferenceValidator(), R.string.invalid_port);
                setEditTextSummaryProvider((EditTextPreference)pref);
            }

            if ((pref = findPreference(getString(R.string.settings_key_cam_cmd_port))) != null) {
                ((EditTextPreference)pref).setOnBindEditTextListener(
                        editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER)
                );
                setOnChangeListener(pref, new PortPreferenceValidator(), R.string.invalid_port);
                setEditTextSummaryProvider((EditTextPreference)pref);
            }

            if ((pref = findPreference(getString(R.string.settings_key_about))) != null) {
                pref.setOnPreferenceClickListener(preference -> {
                    Context context = getContext();
                    if (context != null) {
                        AlertDialog dialog = new AlertDialog.Builder(context)
                                .setTitle(R.string.about_title)
                                .setView(R.layout.dialog_about)
                                .create();
                        dialog.show();
                    }
                    return true;
                });
            }

            if ((pref = findPreference(getString(R.string.settings_key_help))) != null) {
                pref.setOnPreferenceClickListener(preference -> {
                    Context context = getContext();
                    if (context != null) {
                        View helpView = getLayoutInflater().inflate(
                                R.layout.dialog_help, null);
                        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                                context,
                                R.array.help_content,
                                R.layout.listview_help_item);
                        ListView listView = helpView.findViewById(R.id.help_content_list);
                        listView.setAdapter(adapter);
                        AlertDialog dialog = new AlertDialog.Builder(context)
                                .setTitle(R.string.help_title)
                                .setView(helpView)
                                .create();
                        dialog.show();
                    }
                    return true;
                });
            }
        }

        private void setEditTextSummaryProvider(EditTextPreference pref) {
            pref.setSummaryProvider(preference -> ((EditTextPreference)preference).getText());
        }

        private void setOnChangeListener(Preference pref, final PreferenceValidator validator,
                                         final int errMsgResId) {
            pref.setOnPreferenceChangeListener((preference, newValue) -> {
                if (!validator.validate(newValue)) {
                    StatusHandler.showMessage(getString(errMsgResId));
                    return false;
                }
                return true;
            });
        }
    }

    private interface PreferenceValidator {
        boolean validate(Object o);
    }

    private static class IPPreferenceValidator implements PreferenceValidator {
        @Override
        public boolean validate(Object o) {
            return Patterns.IP_ADDRESS.matcher((String)o).matches();
        }
    }

    private static class PortPreferenceValidator implements PreferenceValidator {
        @Override
        public boolean validate(Object o) {
            try {
                int port = Integer.parseInt((String)o);
                if (port < 0 || port > 65535) {
                    return false;
                }
            }
            catch (NumberFormatException e) {
                return false;
            }
            return true;
        }
    }
}