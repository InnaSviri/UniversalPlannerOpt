package ru.programpark.entity.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Date: 27.07.15
 */
public class Time {
    Date today;

    public Time(Long time) {
        this.today = new Date(time * 1000);
    }

    public int getSeconds() {
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("ss");
        int sec = Integer.valueOf(DATE_FORMAT.format(today));

        return sec;
    }

    public int getMinutes() {
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("mm");
        int minutes = Integer.valueOf(DATE_FORMAT.format(today));

        return minutes;
    }

    public int getHour() {
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH");
        int hour = Integer.valueOf(DATE_FORMAT.format(today));

        return hour;
    }

    public int getDay() {
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd");
        int day = Integer.valueOf(DATE_FORMAT.format(today));

        return day;
    }

    public int getMonth() {
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("m");
        int m = Integer.valueOf(DATE_FORMAT.format(today));

        return m;
    }

    public int getYear() {
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yy");
        int year = Integer.valueOf(DATE_FORMAT.format(today));

        return year;
    }

    public Long getTimeOneDayBack(){
        Calendar cal = Calendar.getInstance();
        cal.setTime(today);
        cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) - 1);
        cal.set(Calendar.HOUR_OF_DAY, 18);

        return cal.getTimeInMillis() / 1000L;
    }

    public String getTimeStamp(){
        SimpleDateFormat format = new SimpleDateFormat("dd-M-yyyy HH:mm:ss");
        String str = format.format(today);

        return str;
    }
}
