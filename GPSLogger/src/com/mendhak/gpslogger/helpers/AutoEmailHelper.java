package com.mendhak.gpslogger.helpers;

import java.io.File;
import java.util.Date;

import android.app.ProgressDialog;
import android.os.Environment;

import com.mendhak.gpslogger.GpsLoggingService;
import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.Utilities;
import com.mendhak.gpslogger.model.AppSettings;
import com.mendhak.gpslogger.model.Session;

public class AutoEmailHelper implements IAutoSendHelper
{

	ProgressDialog pd;
	GpsLoggingService mainActivity;
	boolean forcedSend = false;

	public AutoEmailHelper(GpsLoggingService activity)
	{
		this.mainActivity = activity;
	}

	public void SendLogFile(String currentFileName, String personId, boolean forcedSend)
	{
		this.forcedSend = forcedSend;

		try
		{
			if (mainActivity.IsMainFormVisible())
			{
				pd = new ProgressDialog(mainActivity, ProgressDialog.STYLE_HORIZONTAL);
				pd.setMax(100);
				pd.setIndeterminate(true);

				pd = ProgressDialog.show((GpsMainActivity) GpsLoggingService.mainServiceClient,
						mainActivity.getString(R.string.autoemail_sending),
						mainActivity.getString(R.string.please_wait), true, true);

			}	
		}
		catch(Exception ex)
		{
			//	Swallow exception	
		}
		
		Thread t = new Thread(new AutoSendHandler(currentFileName, personId, this));
		t.start();

	}
	
	

	public void OnRelay(boolean connectionSuccess, String errorMessage)
	{
		try
		{
			if (pd != null)
			{
				pd.dismiss();
			}	
		}
		catch(Exception ex)
		{
			//swallow exception
		}
		
		if (!connectionSuccess)
		{
				mainActivity.handler.post(mainActivity.updateResultsEmailSendError);
		}
		else
		{
			//This was a success
			Utilities.LogInfo("Email sent");
			
			if(!forcedSend)
			{
				Utilities.LogDebug("setEmailReadyToBeSent = false");
				Session.setEmailReadyToBeSent(false);
				Session.setAutoEmailTimeStamp(System.currentTimeMillis());
			}
		}

	}
}

interface IAutoSendHelper
{
	public void OnRelay(boolean connectionSuccess, String errorMessage);
}

class AutoSendHandler implements Runnable
{

	String currentFileName;
	IAutoSendHelper helper;

	public AutoSendHandler(String currentFileName, String personId, IAutoSendHelper helper)
	{
		this.currentFileName = currentFileName;
		this.helper = helper;
	}

	public void run()
	{
		File gpxFolder = new File(Environment.getExternalStorageDirectory(), "GPSLogger");

		if (!gpxFolder.exists())
		{
			helper.OnRelay(true, null);
			return;
		}

		File gpxFile = new File(gpxFolder.getPath(), currentFileName + ".gpx");
		File kmlFile = new File(gpxFolder.getPath(), currentFileName + ".kml");

		File foundFile = null;

		if (kmlFile.exists())
		{
			foundFile = kmlFile;
		}
		if (gpxFile.exists())
		{
			foundFile = gpxFile;
		}

		if (foundFile == null)
		{
			helper.OnRelay(true, null);
			return;
		}

		String[] files = new String[] { foundFile.getAbsolutePath() };
		File zipFile = new File(gpxFolder.getPath(), currentFileName + ".zip");

		try
		{

			Utilities.LogInfo("Zipping file");
			ZipHelper zh = new ZipHelper(files, zipFile.getAbsolutePath());
			zh.Zip();

			 Mail m = new Mail(AppSettings.getSmtpUsername(), AppSettings.getSmtpPassword());

		     String[] toArr = {AppSettings.getAutoEmailTarget()};
		     m.setTo(toArr);
		     m.setFrom(AppSettings.getSmtpUsername());
		     m.setSubject("GPS Log file generated at " + Utilities.GetReadableDateTime(new Date()) + " - " + zipFile.getName());
		     m.setBody(zipFile.getName());
		     
		     m.setPort(AppSettings.getSmtpPort());
		     m.setSecurePort(AppSettings.getSmtpPort());
		     m.setSmtpHost(AppSettings.getSmtpServer());
		     m.setSsl(AppSettings.isSmtpSsl());
	         m.addAttachment(zipFile.getAbsolutePath());
	        
	        Utilities.LogInfo("Sending email...");
	        
	        if(m.send()) {
	        	helper.OnRelay(true, "Email was sent successfully.");
	        } 
	        else 
	        {
	        	helper.OnRelay(false, "Email was not sent.");
	        }
		}
		catch (Exception e)
		{
			helper.OnRelay(false, e.getMessage());
			Utilities.LogError("AutoSendHandler.run", e);
		}

	}

}
