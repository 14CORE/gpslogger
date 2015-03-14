package com.mendhak.gpslogger.common.events;


public class CommandEvents {
    /**
     * Requests starting or stopping the logging service.
     * Called from the fragment button click events
     */
    public static class RequestToggle {
    }

    /**
     * Requests starting the logging service
     */
    public static class RequestStartStop {
        public boolean start;
        public RequestStartStop(boolean start){
            this.start = start;
        }
    }

    /**
     * Requests auto sending to targets
     */
    public static class AutoSend {
        public boolean force;
        public AutoSend(boolean force){
            this.force = force;
        }
    }

    /**
     * Set a description for the next point
     */
    public static class Annotate {
        public String annotation;
        public Annotate(String annotation) {
            this.annotation = annotation;
        }
    }

    /**
     * Log once and stop
     */
    public static class LogOnce {
    }
}
