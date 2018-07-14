package com.ermacaz.imagebackup;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.ermacaz.imagebackup.data.model.ImageUpload;
import com.ermacaz.imagebackup.data.model.UserAuthentication;
import com.ermacaz.imagebackup.data.model.UserAuthenticationResponse;
import com.ermacaz.imagebackup.data.remote.APIService;
import com.ermacaz.imagebackup.data.remote.ApiUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.ContentValues.TAG;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    private SharedPreferences mPreferences;

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                if (preference.getKey().equals("backup_server_password")) {
                    EditText edit = ((EditTextPreference) preference).getEditText();
                    String pref = edit.getTransformationMethod().getTransformation(value.toString(), edit).toString();
                    preference.setSummary(pref);
                } else {
                    preference.setSummary(stringValue);
                }
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || DataSyncPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        private SharedPreferences mPreferences;
        private APIService mAPIService;
        private SwitchPreference example_swtich;
        private EditTextPreference mServerDomain;
        private EditTextPreference mServerUsername;
        private EditTextPreference mServerPassword;
        private Preference mAuthenticateButton;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("backup_server_domain"));
            bindPreferenceSummaryToValue(findPreference("backup_server_username"));
            bindPreferenceSummaryToValue(findPreference("backup_server_password"));
            mAPIService = ApiUtils.getAPIService();
            example_swtich = (SwitchPreference) findPreference("example_switch");
            mServerDomain = (EditTextPreference) findPreference("backup_server_domain");
            mServerUsername = (EditTextPreference) findPreference("backup_server_username");
            mServerPassword = (EditTextPreference) findPreference("backup_server_password");
            mAuthenticateButton = (Preference) findPreference("authenticate_button");
            mAuthenticateButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    UserAuthentication userParams = new UserAuthentication();
                    userParams.setUsername(mServerUsername.getText());
                    userParams.setPassword(mServerPassword.getText());
                    mAPIService.authenticate(userParams).enqueue(new Callback<UserAuthenticationResponse>() {
                        @Override
                        public void onResponse(Call<UserAuthenticationResponse> call, Response<UserAuthenticationResponse> response) {

                            if(response.isSuccessful()) {
                                UserAuthenticationResponse u = response.body();
                                mPreferences = getActivity().getSharedPreferences("CurrentUser", MODE_PRIVATE);
                                SharedPreferences.Editor editor = mPreferences.edit();
                                editor.putString("apiKey", response.body().getApiKey());
                                editor.apply();
                                Toast.makeText(getContext(), mPreferences.getString("apiKey", "-1"), Toast.LENGTH_LONG).show();
                                Log.i("RESTTEST", "post submitted to API." + response.body().toString());
                            } else {
                                Toast.makeText(getContext(), response.message(), Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<UserAuthenticationResponse> call, Throwable t) {
                            Log.e("RESTTEST", "Unable to submit post to API.");
                        }
                    });
                    return true;
                }
            });

            example_swtich.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    ImageUpload mImage= new ImageUpload();
                    mImage.setDocName("androidTest.png");
                    mImage.setDocContents("data:image/png;base64,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");
                    mAPIService.saveDocument(mImage).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                            if(response.isSuccessful()) {
                                Log.i("RESTTEST", "post submitted to API." + response.body().toString());
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Log.e("RESTTEST", "Unable to submit post to API.");
                        }
                    });
                    return true;
                }
            });

        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("sync_frequency"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
