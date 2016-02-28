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

package com.mendhak.gpslogger.senders.googledrive;

import android.support.annotation.Nullable;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.senders.IFileSender;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.TagConstraint;
import de.greenrobot.event.EventBus;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;


public class GoogleDriveManager implements IFileSender {

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(GoogleDriveManager.class.getSimpleName());
    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    /*
    To revoke permissions:
    (new Android)
    ./adb -e shell 'sqlite3 /data/system/users/0/accounts.db "delete from grants;"'
    or
    (old Android)
   ./adb -e shell 'sqlite3 /data/system/accounts.db "delete from grants;"'
     */

    public GoogleDriveManager() {
    }

    public String getOauth2Scope() {
        return "oauth2:https://www.googleapis.com/auth/drive.file";
    }



    /**
     * Returns whether the app is authorized to perform Google API operations
     *
     */
    protected boolean isLinked() {
        return !Utilities.IsNullOrEmpty(AppSettings.getGoogleDriveAccountName()) && !Utilities.IsNullOrEmpty(AppSettings.getGoogleDriveAuthToken());
    }

    @Override
    public void uploadFile(List<File> files) {
        for (File f : files) {
            uploadFile(f.getName(), null);
        }
    }


    @Override
    public boolean isAvailable() {
        return AppSettings.isGDocsAutoSendEnabled() && isLinked();
    }

    public void uploadTestFile(File file, String googleDriveFolderName){

        uploadFile(file.getName(), googleDriveFolderName);
    }

    public void uploadFile(final String fileName, @Nullable String googleDriveFolderName) {
        if (!isLinked()) {
            EventBus.getDefault().post(new UploadEvents.GDocs(false));
            return;
        }

        try {
            File gpsDir = new File(preferenceHelper.getGpsLoggerFolder());
            File gpxFile = new File(gpsDir, fileName);

            tracer.debug("Submitting Google Docs job");

            String uploadFolderName = googleDriveFolderName;

            if(Utilities.IsNullOrEmpty(googleDriveFolderName)){
                uploadFolderName = AppSettings.getGoogleDriveFolderName();
            }

            if(Utilities.IsNullOrEmpty(uploadFolderName)){
                uploadFolderName = "GPSLogger for Android";
            }

            JobManager jobManager = AppSettings.GetJobManager();
            jobManager.cancelJobsInBackground(null, TagConstraint.ANY, GoogleDriveJob.getJobTag(gpxFile));
            jobManager.addJobInBackground(new GoogleDriveJob(gpxFile, uploadFolderName));

        } catch (Exception e) {
            EventBus.getDefault().post(new UploadEvents.GDocs(false));
            tracer.error("GoogleDriveManager.uploadFile", e);
        }
    }

    @Override
    public boolean accept(File dir, String name) {
        return true;
    }

}
