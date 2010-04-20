package com.mendhak.gpslogger.helpers;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.widget.EditText;

import com.mendhak.gpslogger.GpsMainActivity;
import com.mendhak.gpslogger.Utilities;
import com.mendhak.gpslogger.interfaces.IGpsLoggerSaxHandler;
import com.mendhak.gpslogger.model.GpxPoint;

/**
 * Class for SeeMyMap.com functionality
 * 
 * @author mendhak
 * 
 */
public class SeeMyMapHelper implements ISeeMyMapHelper
{

	static GpsMainActivity mainActivity;
	ProgressDialog annotatedPointsProgressDialog;
	ProgressDialog singlePointProgressDialog;
	ProgressDialog clearMapProgressDialog;

	/**
	 * This constructor requires the GpsMainActivity form passed in
	 * 
	 * @param activity
	 */
	public SeeMyMapHelper(GpsMainActivity activity)
	{
		mainActivity = activity;

	}

	/**
	 * Prompts the user for input, sends the text along with location to the
	 * server, adds the point to the current log file.
	 */
	public void SendAnnotatedPoint()
	{

		mainActivity.GetPreferences();

		if (mainActivity.seeMyMapUrl == null || mainActivity.seeMyMapUrl.length() == 0
				|| mainActivity.seeMyMapGuid == null || mainActivity.seeMyMapGuid.length() == 0)
		{
			mainActivity.startActivity(new Intent("com.mendhak.gpslogger.SEEMYMAP_SETUP"));
		}
		else
		{

			if (mainActivity.currentLatitude != 0 && mainActivity.currentLongitude != 0)
			{

				AlertDialog.Builder alert = new AlertDialog.Builder(mainActivity);

				alert.setTitle("Add a description");
				alert.setMessage("Use only letters and numbers");

				// Set an EditText view to get user input
				final EditText input = new EditText(mainActivity);
				alert.setView(input);

				singlePointProgressDialog = ProgressDialog.show(mainActivity, "Sending...",
						"Sending to server", true, true);

				alert.setPositiveButton("Ok", new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int whichButton)
					{

						Thread t = new Thread(new SingleAnnotatedPointHandler(
								input.getText().toString(), mainActivity.currentLatitude,
								mainActivity.currentLongitude, mainActivity.seeMyMapGuid,
								SeeMyMapHelper.this));
						t.start();

						mainActivity.AddNoteToLastPoint(input.getText().toString());

					}
				});
				alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int whichButton)
					{
						singlePointProgressDialog.dismiss();
					}
				});

				alert.show();

			}

			else
			{
				Utilities.MsgBox("Not yet", "Nothing to send yet", mainActivity);

			}
		}

	}

	/**
	 * Prompts user to select a date. Based on the date, reads all GPX files and
	 * parses them. It then collects the points and sends them, one by one, to
	 * the server.
	 * 
	 * @param year
	 * @param monthOfYear
	 * @param dayOfMonth
	 */
	public void SendAnnotatedPointsSince(int year, int monthOfYear, int dayOfMonth)
	{

		mainActivity.GetPreferences();
		

		if (mainActivity.seeMyMapUrl == null || mainActivity.seeMyMapUrl.length() == 0
				|| mainActivity.seeMyMapGuid == null || mainActivity.seeMyMapGuid.length() == 0)
		{
			mainActivity.startActivity(new Intent("com.mendhak.gpslogger.SEEMYMAP_SETUP"));
		}
		else
		{

			final ArrayList<GpxPoint> points = new ArrayList<GpxPoint>();

			final Date chosenDate = new Date(
					(new GregorianCalendar(year, monthOfYear, dayOfMonth)).getTimeInMillis());

			Date today = new Date();

			final long difference = today.getTime() - chosenDate.getTime();

			if (difference > 0)
			{

				annotatedPointsProgressDialog = ProgressDialog.show(
						mainActivity,
						"Sending...",
						"Reading and sending points, if there are a lot of files, this could take a few minutes.",
						true, true);

				Thread t = new Thread(new MultipleAnnotatedPointsHandler(difference, chosenDate,
						mainActivity.seeMyMapGuid, points, this));
				t.start();

			}
			else
			{
				Utilities.MsgBox("Marty McFly?",
						"This application does not support futuristic time travel.", mainActivity);
			}	
		}
		
	}

	/**
	 * Removes all points from the map.
	 */
	public void ClearMap()
	{

		mainActivity.GetPreferences();

		if (mainActivity.seeMyMapUrl == null || mainActivity.seeMyMapUrl.length() == 0
				|| mainActivity.seeMyMapGuid == null || mainActivity.seeMyMapGuid.length() == 0)
		{
			mainActivity.startActivity(new Intent("com.mendhak.gpslogger.SEEMYMAP_SETUP"));
		}
		else
		{

			clearMapProgressDialog = ProgressDialog.show(mainActivity, "Clearing...", "Clearing Map",
					true, true);

			Thread t = new Thread(new ClearMapHandler(mainActivity.seeMyMapGuid, this));
			t.start();
		}
	}

	public void OnMultipleAnnotatedPointsCompleted(boolean success)
	{

		if (success)
		{
			mainActivity.handler.post(mainActivity.updateResultsSentPoints);
		}
		else
		{
			mainActivity.handler.post(mainActivity.updateResultsConnectionError);
		}

		annotatedPointsProgressDialog.dismiss();
	}

	public void OnSinglePointSent(boolean success)
	{
		if (success)
		{
			mainActivity.handler.post(mainActivity.updateResults);
		}
		else
		{
			mainActivity.handler.post(mainActivity.updateResultsConnectionError);
		}

		singlePointProgressDialog.dismiss();
	}

	public void OnClearMapCompleted(boolean success)
	{

		if (success)
		{
			mainActivity.handler.post(mainActivity.updateResultsClearMap);
		}
		else
		{
			mainActivity.handler.post(mainActivity.updateResultsConnectionError);
		}

		clearMapProgressDialog.dismiss();
	}

}

interface ISeeMyMapHelper
{

	/**
	 * Event raised when the sending of multiple points to SeeMyMap is done.
	 * 
	 * @param success
	 *            indicates whether all of the points were sent successfully.
	 */
	public void OnMultipleAnnotatedPointsCompleted(boolean success);

	/**
	 * Event raised when a single point has been sent to SeeMyMap.
	 * 
	 * @param success
	 *            indicates whether the point was sent successfully.
	 */
	public void OnSinglePointSent(boolean success);

	/**
	 * Event raised when the SeeMyMap map has been cleared.
	 * 
	 * @param success
	 *            indicates whether the map was cleared successfully.
	 */
	public void OnClearMapCompleted(boolean success);

}

class ClearMapHandler implements Runnable
{

	String seeMyMapGuid;
	ISeeMyMapHelper helper;

	public ClearMapHandler(String seeMyMapGuid, ISeeMyMapHelper helper)
	{
		this.seeMyMapGuid = seeMyMapGuid;
		this.helper = helper;
	}

	public void run()
	{
		boolean success = true;
		// Send to server
		try
		{
			Utilities.GetUrl(Utilities.GetSeeMyMapClearMapUrl(seeMyMapGuid));

		}
		catch (Exception e)
		{
			success = false;

		}

		helper.OnClearMapCompleted(success);
	}
}

class SingleAnnotatedPointHandler implements Runnable
{

	String input;
	double currentLatitude;
	double currentLongitude;
	String seeMyMapGuid;
	ISeeMyMapHelper helper;

	public SingleAnnotatedPointHandler(String input, double currentLatitude, double currentLongitude,
			String seeMyMapGuid, ISeeMyMapHelper helper)
	{
		this.input = input;
		this.currentLatitude = currentLatitude;
		this.currentLongitude = currentLongitude;
		this.seeMyMapGuid = seeMyMapGuid;
		this.helper = helper;
	}

	public void run()
	{

		boolean success = true;

		// Send to server
		try
		{

			input = Utilities.CleanString(input);

			String whereUrl = Utilities.GetSeeMyMapAddLocationUrl(seeMyMapGuid, currentLatitude,
					currentLongitude, input);

			try
			{
				Utilities.GetUrl(whereUrl);
			}
			catch (Exception e)
			{
				success = false;
			}

			helper.OnSinglePointSent(success);

		}
		catch (Exception e)
		{

		}
		// Also add to the file being logged

	}
}

class MultipleAnnotatedPointsHandler implements Runnable
{

	long difference;
	Date chosenDate;
	ArrayList<GpxPoint> points;
	ISeeMyMapHelper helper;
	String seeMyMapGuid;

	public MultipleAnnotatedPointsHandler(long difference, Date chosenDate, String seeMyMapGuid,
			ArrayList<GpxPoint> points, ISeeMyMapHelper helper)
	{

		this.difference = difference;
		this.chosenDate = chosenDate;
		this.points = points;
		this.helper = helper;
		this.seeMyMapGuid = seeMyMapGuid;
	}

	public void run()
	{

		boolean success = true;

		XMLReader parser;

		try
		{

			File gpxFolder = new File(Environment.getExternalStorageDirectory(), "GPSLogger");

			// Read all files in GPSLogger folder
			// get first 8 characters
			// convert to date
			// compare with the set date
			// if greater than, parse.

			if (!gpxFolder.exists())
			{
				return;
			}

			File[] logFiles = gpxFolder.listFiles();
			for (File file : logFiles)
			{
				String fileName = file.getName();
				int year = Integer.valueOf(fileName.substring(0, 4));
				int month = Integer.valueOf(fileName.substring(4, 6));
				int day = Integer.valueOf(fileName.substring(6, 8));
				Date fileDate = new Date(year-1900, month-1, day);

				long fileDifference = fileDate.getTime() - chosenDate.getTime();

				if (fileDifference >= 0)
				{
					// The file is on or after the set date, process it.
					// Get extension
					String extension = fileName.substring(fileName.indexOf('.'));
					IGpsLoggerSaxHandler ch = null;

					if (extension.equalsIgnoreCase(".GPX"))
					{
						ch = new GpxSaxHandler();
					}
					else if (extension.equalsIgnoreCase(".KML"))
					{
						ch = new KmlSaxHandler();
					}

					if (ch != null)
					{
						System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");
						parser = XMLReaderFactory.createXMLReader();
						parser.setContentHandler(ch);
						parser.parse(file.getPath());
						points.addAll(ch.GetPoints());
					}

				}

			}
		}
		catch (SAXException e)
		{

			e.printStackTrace();
		}
		catch (IOException e)
		{

			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		for (GpxPoint p : points)
		{

			String addLocationUrl = Utilities.GetSeeMyMapAddLocationWithDateUrl(seeMyMapGuid,
					Double.valueOf(p.getLatitude()), Double.valueOf(p.getLongitude()),
					p.getDescription(), p.getDateTime());

			try
			{
				Utilities.GetUrl(addLocationUrl);
			}
			catch (Exception e)
			{
				success = false;
				break;
			}
		}

		helper.OnMultipleAnnotatedPointsCompleted(success);

	}
}
