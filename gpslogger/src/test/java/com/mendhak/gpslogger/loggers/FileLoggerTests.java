package com.mendhak.gpslogger.loggers;

import android.location.Location;
import android.test.AndroidTestCase;

public class FileLoggerTests extends AndroidTestCase {

    public void setUp() {

    }

    public void testWaypointXml_BasicInfo(){
        Gpx10AnnotateHandler annotateHandler = new Gpx10AnnotateHandler(null, null, null, null);

        Location loc = new Location("MOCK");
        loc.setLatitude(12.193);
        loc.setLongitude(19.111);

        String actual =  annotateHandler.GetWaypointXml(loc, "2011-09-17T18:45:33Z", "This is the annotation");
        String expected = "\n<wpt lat=\"12.193\" lon=\"19.111\"><time>2011-09-17T18:45:33Z</time><name>This is the annotation</name><src>MOCK</src></wpt>\n";

        assertEquals("Basic waypoint XML", expected, actual);
    }

    public void testWaypointXml_WithAltitude(){
        Gpx10AnnotateHandler annotateHandler = new Gpx10AnnotateHandler(null, null, null, null);

        Location loc = new Location("MOCK");
        loc.setLatitude(12.193);
        loc.setLongitude(19.111);
        loc.setAltitude(9001);

        String actual =  annotateHandler.GetWaypointXml(loc, "2011-09-17T18:45:33Z", "This is the annotation");
        String expected = "\n<wpt lat=\"12.193\" lon=\"19.111\"><ele>9001.0</ele><time>2011-09-17T18:45:33Z</time><name>This is the annotation</name><src>MOCK</src></wpt>\n";

        assertEquals("Basic waypoint XML", expected, actual);
    }

    public void testTrackPointXml_LatLongOnly(){

        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(null, null, null, false, 41);

        Location loc = new Location("MOCK");
        loc.setLatitude(12.193);
        loc.setLongitude(19.111);


        String actual = writeHandler.GetTrackPointXml(loc, "2011-09-17T18:45:33Z");
        String expected = "<trkpt lat=\"12.193\" lon=\"19.111\"><time>2011-09-17T18:45:33Z</time><src>MOCK</src><sat>41</sat></trkpt>\n</trkseg></trk></gpx>";

        assertEquals("Basic trackpoint XML", expected, actual);
    }


    public void testTrackPointXml_ExtraInfo(){

        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(null, null, null, false, 41);

        Location loc = new Location("MOCK");
        loc.setLatitude(12.193);
        loc.setLongitude(19.111);
        loc.setAltitude(9001);
        loc.setBearing(91.88f);
        loc.setSpeed(188.44f);
        loc.setAccuracy(55);

        String actual = writeHandler.GetTrackPointXml(loc, "2011-09-17T18:45:33Z");
        String expected = "<trkpt lat=\"12.193\" lon=\"19.111\"><ele>9001.0</ele><time>2011-09-17T18:45:33Z</time>" +
                "<course>91.88</course><speed>188.44</speed><src>MOCK</src><sat>41</sat><hdop>11.0</hdop></trkpt>\n</trkseg></trk></gpx>";

        assertEquals("Trackpoint XML with all info", expected, actual);
    }


    public void testTrackPointXml_ExtraInfoWithoutSatellites(){

        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(null, null, null, false, 0);

        Location loc = new Location("MOCK");
        loc.setLatitude(12.193);
        loc.setLongitude(19.111);
        loc.setAltitude(9001);
        loc.setBearing(91.88f);
        loc.setSpeed(188.44f);
        loc.setAccuracy(55);

        String actual = writeHandler.GetTrackPointXml(loc, "2011-09-17T18:45:33Z");
        String expected = "<trkpt lat=\"12.193\" lon=\"19.111\"><ele>9001.0</ele><time>2011-09-17T18:45:33Z</time>" +
                "<course>91.88</course><speed>188.44</speed><src>MOCK</src><hdop>11.0</hdop></trkpt>\n</trkseg></trk></gpx>";

        assertEquals("Trackpoint XML without satellites", expected, actual);
    }

    public void testTrackPointXml_NewTrackSegment(){

        Gpx10WriteHandler writeHandler = new Gpx10WriteHandler(null, null, null, true, 0);

        Location loc = new Location("MOCK");
        loc.setLatitude(12.193);
        loc.setLongitude(19.111);
        loc.setAltitude(9001);
        loc.setBearing(91.88f);
        loc.setSpeed(188.44f);


        String actual = writeHandler.GetTrackPointXml(loc, "2011-09-17T18:45:33Z");
        String expected = "<trkseg><trkpt lat=\"12.193\" lon=\"19.111\"><ele>9001.0</ele><time>2011-09-17T18:45:33Z</time>" +
                "<course>91.88</course><speed>188.44</speed><src>MOCK</src></trkpt>\n</trkseg></trk></gpx>";

        assertEquals("Trackpoint XML with a new segment", expected, actual);
    }

    public void testPlacemarkXml_BasicInfo() {

        Kml22AnnotateHandler kmlHandler = new Kml22AnnotateHandler(null, null, null);
        Location loc = new Location("MOCK");
        loc.setLatitude(12.193);
        loc.setLongitude(19.111);
        loc.setAltitude(9001);
        loc.setBearing(91.88f);
        loc.setSpeed(188.44f);

        String actual = kmlHandler.GetPlacemarkXml("This is the annotation",loc);
        String expected = "<Placemark><name>This is the annotation</name><Point><coordinates>19.111,12.193,9001.0</coordinates></Point></Placemark>\n";

        assertEquals("Basic Placemark XML", expected, actual);
    }


}
