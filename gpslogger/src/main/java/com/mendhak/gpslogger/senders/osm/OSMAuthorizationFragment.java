/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 2 of the License, or
*    (at your option) any later version.
*
*    GPSLogger for Android is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.mendhak.gpslogger.senders.osm;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.Preference;
import com.canelmas.let.AskPermission;
import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.views.PermissionedPreferenceFragment;
import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import org.slf4j.LoggerFactory;

public class OSMAuthorizationFragment extends PermissionedPreferenceFragment implements Preference.OnPreferenceClickListener {

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(OSMAuthorizationFragment.class.getSimpleName());
    private OAuthProvider provider;
    private OAuthConsumer consumer;
    OpenStreetMapManager manager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.osmsettings);

        manager = new OpenStreetMapManager();

        final Intent intent = getActivity().getIntent();
        final Uri myURI = intent.getData();

        if (myURI != null && myURI.getQuery() != null
                && myURI.getQuery().length() > 0) {
            //User has returned! Read the verifier info from querystring
            String oAuthVerifier = myURI.getQueryParameter("oauth_verifier");

            try {
                if (provider == null) {
                    provider = manager.GetOSMAuthProvider();
                }

                if (consumer == null) {
                    //In case consumer is null, re-initialize from stored values.
                    consumer = manager.GetOSMAuthConsumer();
                }

                //Ask OpenStreetMap for the access token. This is the main event.
                provider.retrieveAccessToken(consumer, oAuthVerifier);

                String osmAccessToken = consumer.getToken();
                String osmAccessTokenSecret = consumer.getTokenSecret();

                //Save for use later.
                AppSettings.setOSMAccessToken(osmAccessToken);
                AppSettings.setOSMAccessTokenSecret(osmAccessTokenSecret);

            } catch (Exception e) {
                tracer.error("OSM authorization error", e);
                Utilities.MsgBox(getString(R.string.sorry), getString(R.string.osm_auth_error), getActivity());
            }
        }


        Preference visibilityPref = findPreference("osm_visibility");
        Preference descriptionPref = findPreference("osm_description");
        Preference tagsPref = findPreference("osm_tags");
        Preference resetPref = findPreference("osm_resetauth");

        if (!manager.IsOsmAuthorized()) {
            resetPref.setTitle(R.string.osm_lbl_authorize);
            resetPref.setSummary(R.string.osm_lbl_authorize_description);
            visibilityPref.setEnabled(false);
            descriptionPref.setEnabled(false);
            tagsPref.setEnabled(false);
        } else {
            visibilityPref.setEnabled(true);
            descriptionPref.setEnabled(true);
            tagsPref.setEnabled(true);

        }

        resetPref.setOnPreferenceClickListener(this);

    }


    @Override
    @AskPermission({Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE})
    public boolean onPreferenceClick(Preference preference) {
        if (manager.IsOsmAuthorized()) {
            AppSettings.setOSMAccessToken("");
            AppSettings.setOSMAccessTokenSecret("");
            AppSettings.setOSMRequestToken("");
            AppSettings.setOSMRequestTokenSecret("");

            startActivity(new Intent(getActivity(), GpsMainActivity.class));
            getActivity().finish();

        } else {
            try {
                StrictMode.enableDefaults();

                //User clicks. Set the consumer and provider up.
                consumer = manager.GetOSMAuthConsumer();
                provider = manager.GetOSMAuthProvider();

                String authUrl;

                //Get the request token and request token secret
                authUrl = provider.retrieveRequestToken(consumer, OAuth.OUT_OF_BAND);

                //Save for later
                AppSettings.setOSMRequestToken(consumer.getToken());
                AppSettings.setOSMRequestTokenSecret(consumer.getTokenSecret());


                //Open browser, send user to OpenStreetMap.org
                Uri uri = Uri.parse(authUrl);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);

            } catch (Exception e) {
                tracer.error("onClick", e);
                Utilities.MsgBox(getString(R.string.sorry), getString(R.string.osm_auth_error),
                        getActivity());
            }
        }

        return true;
    }
}
