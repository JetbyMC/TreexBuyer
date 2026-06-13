package org.jetby.treexBuyer.tools;

public final class TimerFormat {

    public static String get(int time) {
        int hours = time / 3600;
        int minutes = (time % 3600) / 60;
        int seconds = time % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

}
