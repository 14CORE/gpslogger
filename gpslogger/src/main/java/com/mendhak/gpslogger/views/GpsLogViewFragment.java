package com.mendhak.gpslogger.views;


import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.events.ServiceEvents;
import com.mendhak.gpslogger.common.slf4j.SessionLogcatAppender;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GpsLogViewFragment extends GenericViewFragment {

    private View rootView;
    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(GpsLogViewFragment.class.getSimpleName());
    long startTime = 0;
    TextView logTextView;

    Handler timerHandler = new Handler();

    public static final GpsLogViewFragment newInstance() {
        GpsLogViewFragment fragment = new GpsLogViewFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_log_view, container, false);
        logTextView = (TextView) rootView.findViewById(R.id.logview_txtstatus);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
    }

    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }


    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            //logTextView.setText(logTextView.getText() + "\r\n" + String.format("%d:%02d", minutes, seconds));
            ShowStatusMessages();
            timerHandler.postDelayed(this, 1500);
        }
    };


    private void ShowStatusMessages(){


        StringBuilder sb = new StringBuilder();
        for(ServiceEvents.StatusMessage message : SessionLogcatAppender.Statuses){

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            sb.append(sdf.format(new Date(message.timestamp)));
            sb.append(" ");
            if(!message.success) {
                sb.append("<font color='red'>" + message.status + "</font>");
            }
            else {
                sb.append(message.status);
            }
            sb.append("<br />");
        }
        logTextView.setText(Html.fromHtml(sb.toString()));
    }



}
