package io.tchepannou.enigma.oms.support;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateHelper {
    public static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ssZ";
    public static final String TIMEZONE_ID = "UTC";

    public static DateFormat createDateFormat(){
        final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        dateFormat.setTimeZone(getTimeZone());
        return dateFormat;
    }

    public static TimeZone getTimeZone(){
        return TimeZone.getTimeZone(TIMEZONE_ID);
    }

    public static Date now(){
        return getCalendar().getTime();
    }

    public static Calendar getCalendar(){
        final Calendar cal = Calendar.getInstance();
        cal.setTimeZone(getTimeZone());
        return cal;
    }

    public static Calendar toCalendar(final Date date){
        final Calendar cal = getCalendar();
        cal.setTime(date);
        return cal;
    }
}
