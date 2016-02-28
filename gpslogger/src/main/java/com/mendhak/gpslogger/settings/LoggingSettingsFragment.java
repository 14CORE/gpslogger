/*******************************************************************************
 * This file is part of GPSLogger for Android.
 *
 * GPSLogger for Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * GPSLogger for Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.mendhak.gpslogger.settings;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.text.Html;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.prefs.MaterialListPreference;
import com.mendhak.gpslogger.MainPreferenceActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.views.component.CustomSwitchPreference;
import net.rdrei.android.dirchooser.DirectoryChooserConfig;
import net.rdrei.android.dirchooser.DirectoryChooserFragment;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.MessageFormat;


public class LoggingSettingsFragment extends PreferenceFragment
        implements
        Preference.OnPreferenceClickListener,
        Preference.OnPreferenceChangeListener,
        DirectoryChooserFragment.OnFragmentInteractionListener
{

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(LoggingSettingsFragment.class.getSimpleName());
    private DirectoryChooserFragment folderDialog;
    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_logging);

        Preference gpsloggerFolder = findPreference("gpslogger_folder");
        gpsloggerFolder.setOnPreferenceClickListener(this);
        String gpsLoggerFolderPath = preferenceHelper.getGpsLoggerFolder();
        gpsloggerFolder.setSummary(gpsLoggerFolderPath);
        if(!(new File(gpsLoggerFolderPath)).canWrite()){
            gpsloggerFolder.setSummary(Html.fromHtml("<font color='red'>" + gpsLoggerFolderPath + "</font>"));
        }

        /**
         * Logging Details - New file creation
         */
        MaterialListPreference newFilePref = (MaterialListPreference) findPreference("new_file_creation");
        newFilePref.setOnPreferenceChangeListener(this);
        /* Trigger artificially the listener and perform validations. */
        newFilePref.getOnPreferenceChangeListener()
                .onPreferenceChange(newFilePref, newFilePref.getValue());

        CustomSwitchPreference chkfile_prefix_serial = (CustomSwitchPreference) findPreference("new_file_prefix_serial");
        if (Utilities.IsNullOrEmpty(Utilities.GetBuildSerial())) {
            chkfile_prefix_serial.setEnabled(false);
            chkfile_prefix_serial.setSummary("This option not available on older phones or if a serial id is not present");
        } else {
            chkfile_prefix_serial.setSummary(chkfile_prefix_serial.getSummary().toString() + "(" + Utilities.GetBuildSerial() + ")");
        }


        Preference prefNewFileCustomName = findPreference("new_file_custom_name");
        prefNewFileCustomName.setOnPreferenceClickListener(this);


        CustomSwitchPreference prefCustomUrl = (CustomSwitchPreference)findPreference("log_customurl_enabled");
        prefCustomUrl.setOnPreferenceChangeListener(this);

        CustomSwitchPreference chkLog_opengts = (CustomSwitchPreference) findPreference("log_opengts");
        chkLog_opengts.setOnPreferenceChangeListener(this);
    }


    @Override
    public void onResume() {
        super.onResume();

        setPreferencesEnabledDisabled();
    }



    @Override
    public boolean onPreferenceClick(Preference preference) {

        if (preference.getKey().equals("gpslogger_folder")) {

            final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                    .initialDirectory(preferenceHelper.getGpsLoggerFolder())
                    .newDirectoryName("GPSLogger")
                    .allowReadOnlyDirectory(false)
                    .allowNewDirectoryNameModification(true)
                    .build();

            folderDialog = DirectoryChooserFragment.newInstance(config);
            folderDialog.setTargetFragment(this, 0);
            folderDialog.show(getActivity().getFragmentManager(), null);

            return true;
        }


        if(preference.getKey().equalsIgnoreCase("new_file_custom_name")){

            MaterialDialog alertDialog = new MaterialDialog.Builder(getActivity())
                    .title(R.string.new_file_custom_title)
                    .customView(R.layout.alertview, true)
                    .positiveText(R.string.ok)
                    .negativeText(R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                            EditText userInput = (EditText) materialDialog.getCustomView().findViewById(R.id.alert_user_input);
                            preferenceHelper.setCustomFileName(userInput.getText().toString());
                        }
                    })
                   .build();

            EditText userInput = (EditText) alertDialog.getCustomView().findViewById(R.id.alert_user_input);
            userInput.setText(preferenceHelper.getCustomFileName());
            TextView tvMessage = (TextView)alertDialog.getCustomView().findViewById(R.id.alert_user_message);
            tvMessage.setText(R.string.new_file_custom_message);
            alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            alertDialog.show();
        }

        return false;
    }


    @Override
    public boolean onPreferenceChange(final Preference preference, Object newValue) {

        if (preference.getKey().equalsIgnoreCase("log_opengts")) {

            if(!((CustomSwitchPreference) preference).isChecked() && (Boolean)newValue  ) {

                Intent targetActivity = new Intent(getActivity(), MainPreferenceActivity.class);
                targetActivity.putExtra("preference_fragment", MainPreferenceActivity.PREFERENCE_FRAGMENTS.OPENGTS);
                startActivity(targetActivity);
            }

            return true;
        }

        if(preference.getKey().equalsIgnoreCase("log_customurl_enabled") ){

            // Bug in SwitchPreference: http://stackoverflow.com/questions/19503931/switchpreferences-calls-multiple-times-the-onpreferencechange-method
            // Check if isChecked == false && newValue == true
            if(!((CustomSwitchPreference) preference).isChecked() && (Boolean)newValue  ) {
                MaterialDialog alertDialog = new MaterialDialog.Builder(getActivity())
                        .title(R.string.log_customurl_title)
                        .customView(R.layout.alertview, true)
                        .positiveText(R.string.ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                EditText userInput = (EditText) materialDialog.getCustomView().findViewById(R.id.alert_user_input);
                                preferenceHelper.setCustomLoggingUrl(userInput.getText().toString());
                            }
                        })
                        .keyListener(new DialogInterface.OnKeyListener() {
                            @Override
                            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                                if (keyCode == KeyEvent.KEYCODE_BACK) {
                                    ((CustomSwitchPreference) preference).setChecked(false);
                                    return false;
                                }
                                return false;
                            }
                        })
                        .build();

                EditText userInput = (EditText) alertDialog.getCustomView().findViewById(R.id.alert_user_input);
                userInput.setText(preferenceHelper.getCustomLoggingUrl());
                userInput.setSingleLine(true);
                userInput.setLines(4);
                userInput.setHorizontallyScrolling(false);
                TextView tvMessage = (TextView)alertDialog.getCustomView().findViewById(R.id.alert_user_message);

                String legend = MessageFormat.format("{0} %LAT\n{1} %LON\n{2} %DESC\n{3} %SAT\n{4} %ALT\n{5} %SPD\n{6} %ACC\n{7} %DIR\n{8} %PROV\n{9} %TIME\n{10} %BATT\n{11} %AID\n{12} %SER",
                        getString(R.string.txt_latitude), getString(R.string.txt_longitude), getString(R.string.txt_annotation),
                        getString(R.string.txt_satellites), getString(R.string.txt_altitude), getString(R.string.txt_speed),
                        getString(R.string.txt_accuracy), getString(R.string.txt_direction), getString(R.string.txt_provider),
                        getString(R.string.txt_time_isoformat), "Battery:", "Android ID:", "Serial:");

                tvMessage.setText(legend);
                alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                alertDialog.show();
            }


            return true;
        }

        if (preference.getKey().equals("new_file_creation")) {

            Preference prefFileStaticName = findPreference("new_file_custom_name");
            Preference prefAskEachTime = findPreference("new_file_custom_each_time");
            Preference prefSerialPrefix = findPreference("new_file_prefix_serial");
            prefAskEachTime.setEnabled(newValue.equals("custom"));
            prefFileStaticName.setEnabled(newValue.equals("custom"));
            prefSerialPrefix.setEnabled(!newValue.equals("custom"));


            return true;
        }
        return false;
    }

    private void setPreferencesEnabledDisabled() {

        Preference prefFileStaticName = findPreference("new_file_custom_name");
        Preference prefAskEachTime = findPreference("new_file_custom_each_time");
        Preference prefSerialPrefix = findPreference("new_file_prefix_serial");

        prefFileStaticName.setEnabled(preferenceHelper.shouldCreateCustomFile());
        prefAskEachTime.setEnabled(preferenceHelper.shouldCreateCustomFile());
        prefSerialPrefix.setEnabled(!preferenceHelper.shouldCreateCustomFile());
    }

    @Override
    public void onSelectDirectory(@NonNull String folderPath) {
        folderDialog.dismiss();

        tracer.debug(folderPath);

        File folder = new File(folderPath);

        if(!folder.isDirectory()) {
            folderPath = folder.getParent();
        }

        if(!folder.canWrite()){
            Utilities.MsgBox(getString(R.string.sorry), getString(R.string.pref_logging_file_no_permissions), getActivity());
            return;
        }

        preferenceHelper.setGpsLoggerFolder(folderPath);
        Preference gpsloggerFolder = findPreference("gpslogger_folder");
        gpsloggerFolder.setSummary(folderPath);
    }

    @Override
    public void onCancelChooser() {
        folderDialog.dismiss();
    }
}
