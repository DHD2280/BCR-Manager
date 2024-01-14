package it.dhd.bcrmanager.utils;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateUtils {

    /** Date and time formats are specified by date and time pattern strings.
     * <a href="https://developer.android.com/reference/java/time/format/DateTimeFormatterBuilder#appendPattern(java.lang.String)">...</a>
     *   Symbol  Meaning                     Presentation      Examples
     *   ------  -------                     ------------      -------
     *    G       era                         text              AD; Anno Domini; A
     *    u       year                        year              2004; 04
     *    y       year-of-era                 year              2004; 04
     *    D       day-of-year                 number            189
     *    M/L     month-of-year               number/text       7; 07; Jul; July; J
     *    d       day-of-month                number            10
     *    g       modified-julian-day         number            2451334
     *
     *    Q/q     quarter-of-year             number/text       3; 03; Q3; 3rd quarter
     *    Y       week-based-year             year              1996; 96
     *    w       week-of-week-based-year     number            27
     *    W       week-of-month               number            4
     *    E       day-of-week                 text              Tue; Tuesday; T
     *    e/c     localized day-of-week       number/text       2; 02; Tue; Tuesday; T
     *    F       day-of-week-in-month        number            3
     *
     *    a       am-pm-of-day                text              PM
     *    h       clock-hour-of-am-pm (1-12)  number            12
     *    K       hour-of-am-pm (0-11)        number            0
     *    k       clock-hour-of-day (1-24)    number            24
     *
     *    H       hour-of-day (0-23)          number            0
     *    m       minute-of-hour              number            30
     *    s       second-of-minute            number            55
     *    S       fraction-of-second          fraction          978
     *    A       milli-of-day                number            1234
     *    n       nano-of-second              number            987654321
     *    N       nano-of-day                 number            1234000000
     *
     *    V       time-zone ID                zone-id           America/Los_Angeles; Z; -08:30
     *    v       generic time-zone name      zone-name         PT, Pacific Time
     *    z       time-zone name              zone-name         Pacific Standard Time; PST
     *    O       localized zone-offset       offset-O          GMT+8; GMT+08:00; UTC-08:00;
     *    X       zone-offset 'Z' for zero    offset-X          Z; -08; -0830; -08:30; -083015; -08:30:15
     *    x       zone-offset                 offset-x          +0000; -08; -0830; -08:30; -083015; -08:30:15
     *    Z       zone-offset                 offset-Z          +0000; -0800; -08:00
     */


    private static final Map<String, String> DATE_REGEX = new HashMap<>() {{
        // Numbers
        put("([0-9]{4}[^a-zA-Z0-9][0-9]{2}[^a-zA-Z0-9][0-9]{2})", "yyyy*MM*dd*");
        put("([0-9]{1,2}-[0-9]{1,2}-[0-9]{4})", "dd-MM-yyyy");
        // Letters
    }};


    public record DatePattern(String regexPattern, String dateTimeFormat, boolean shouldReplace) {
        public DatePattern(String regexPattern, String dateTimeFormat) {
            this(regexPattern, dateTimeFormat, false);
        }

    }

    private static final List<DatePattern> DATE_TIME_PATTERNS = new ArrayList<>() {{
        add(new DatePattern("[0-9]{1,2}-[0-9]{1,2}-[0-9]{4}-[0-9]{1,2}-[0-9]{2}-[0-9]{2}", "dd-MM-yyyy-HH-mmss"));
        add(new DatePattern("[0-9]{1,2}-[0-9]{1,2}-[0-9]{4}\\s[0-9]{1,2}:[0-9]{2}:[0-9]{2}", "dd-MM-yyyy HH:mm:ss"));
        add(new DatePattern("[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}\\s[0-9]{1,2}:[0-9]{2}:[0-9]{2}", "yyyy-MM-dd HH:mm:ss"));
        add(new DatePattern("[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}-[0-9]{1,2}-[0-9]{2}-[0-9]{2}", "yyyy-MM-dd-HH-mm-ss"));
        add(new DatePattern("[0-9]{1,2}/[0-9]{1,2}/[0-9]{4}\\s[0-9]{1,2}:[0-9]{2}:[0-9]{2}", "MM/dd/yyyy HH:mm:ss"));
        add(new DatePattern("[0-9]{4}/[0-9]{1,2}/[0-9]{1,2}\\s[0-9]{1,2}:[0-9]{2}:[0-9]{2}", "yyyy/MM/dd HH:mm:ss"));
        add(new DatePattern("[0-9]{1,2}\\s[a-z]{3}\\s[0-9]{4}\\s[0-9]{1,2}:[0-9]{2}:[0-9]{2}", "dd MMM yyyy HH:mm:ss"));
        add(new DatePattern("[0-9]{1,2}\\s[a-z]{4,}\\s[0-9]{4}\\s[0-9]{1,2}:[0-9]{2}:[0-9]{2}", "dd MMMM yyyy HH:mm:ss"));
        add(new DatePattern("[0-9]{14}", "yyyyMMddHHmmss"));
        add(new DatePattern("[0-9]{8}\\s[0-9]{6}", "yyyyMMdd HHmmss"));
        add(new DatePattern("[0-9]{8}_[0-9]{6}", "yyyyMMdd_HHmmss"));

        // Full Date + Partial Time
        add(new DatePattern("[0-9]{1,2}-[0-9]{1,2}-[0-9]{4}\\s[0-9]{1,2}:[0-9]{2}", "dd-MM-yyyy HH:mm"));
        add(new DatePattern("[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}\\s[0-9]{1,2}:[0-9]{2}", "yyyy-MM-dd HH:mm"));
        add(new DatePattern("[0-9]{1,2}/[0-9]{1,2}/[0-9]{4}\\s[0-9]{1,2}:[0-9]{2}", "MM/dd/yyyy HH:mm"));
        add(new DatePattern("[0-9]{4}/[0-9]{1,2}/[0-9]{1,2}\\s[0-9]{1,2}:[0-9]{2}", "yyyy/MM/dd HH:mm"));
        add(new DatePattern("[0-9]{1,2}\\s[a-z]{3}\\s[0-9]{4}\\s[0-9]{1,2}:[0-9]{2}", "dd MMM yyyy HH:mm"));
        add(new DatePattern("[0-9]{1,2}\\s[a-z]{4,}\\s[0-9]{4}\\s[0-9]{1,2}:[0-9]{2}", "dd MMMM yyyy HH:mm"));

    }};

    /**
     * The date formats
     */
    private static final Map<String, String> DATE_FORMAT_REGEXPS = new HashMap<>() {{
        put("[0-9]{1,2}-[0-9]{1,2}-[0-9]{4}-[0-9]{1,2}-[0-9]{2}-[0-9]{2}", "dd-MM-yyyy-HH-mmss");
        put("[0-9]{1,2}-[0-9]{1,2}-[0-9]{4}\\s[0-9]{1,2}:[0-9]{2}:[0-9]{2}", "dd-MM-yyyy HH:mm:ss");
        put("[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}\\s[0-9]{1,2}:[0-9]{2}:[0-9]{2}", "yyyy-MM-dd HH:mm:ss");
        put("[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}-[0-9]{1,2}-[0-9]{2}-[0-9]{2}", "yyyy-MM-dd-HH-mm-ss");
        put("[0-9]{1,2}/[0-9]{1,2}/[0-9]{4}\\s[0-9]{1,2}:[0-9]{2}:[0-9]{2}", "MM/dd/yyyy HH:mm:ss");
        put("[0-9]{4}/[0-9]{1,2}/[0-9]{1,2}\\s[0-9]{1,2}:[0-9]{2}:[0-9]{2}", "yyyy/MM/dd HH:mm:ss");
        put("[0-9]{1,2}\\s[a-z]{3}\\s[0-9]{4}\\s[0-9]{1,2}:[0-9]{2}:[0-9]{2}", "dd MMM yyyy HH:mm:ss");
        put("[0-9]{1,2}\\s[a-z]{4,}\\s[0-9]{4}\\s[0-9]{1,2}:[0-9]{2}:[0-9]{2}", "dd MMMM yyyy HH:mm:ss");
        put("[0-9]{14}", "yyyyMMddHHmmss");
        put("[0-9]{8}\\s[0-9]{6}", "yyyyMMdd HHmmss");
        put("[0-9]{8}_[0-9]{6}", "yyyyMMdd_HHmmss");

        // Full Date + Partial Time
        put("[0-9]{1,2}-[0-9]{1,2}-[0-9]{4}\\s[0-9]{1,2}:[0-9]{2}", "dd-MM-yyyy HH:mm");
        put("[0-9]{4}-[0-9]{1,2}-[0-9]{1,2}\\s[0-9]{1,2}:[0-9]{2}", "yyyy-MM-dd HH:mm");
        put("[0-9]{1,2}/[0-9]{1,2}/[0-9]{4}\\s[0-9]{1,2}:[0-9]{2}", "MM/dd/yyyy HH:mm");
        put("[0-9]{4}/[0-9]{1,2}/[0-9]{1,2}\\s[0-9]{1,2}:[0-9]{2}", "yyyy/MM/dd HH:mm");
        put("[0-9]{1,2}\\s[a-z]{3}\\s[0-9]{4}\\s[0-9]{1,2}:[0-9]{2}", "dd MMM yyyy HH:mm");
        put("[0-9]{1,2}\\s[a-z]{4,}\\s[0-9]{4}\\s[0-9]{1,2}:[0-9]{2}", "dd MMMM yyyy HH:mm");

    }};

    /**
     * Check if the date is today
     * @param itemDate The date to check
     * @return true if the date is today, false otherwise
     */
    public static boolean isToday(Date itemDate) {
        if (itemDate == null) {
            return false;
        } else {
            return android.text.format.DateUtils.isToday(itemDate.getTime());
        }
    }

    /**
     * Check if two dates are the same day
     * @param date1 The first date
     * @param date2 The second date
     * @return true if the two dates are the same day, false otherwise
     */
    public static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return false;
        } else {
            Calendar cal1 = Calendar.getInstance();
            cal1.setTime(date1);

            Calendar cal2 = Calendar.getInstance();
            cal2.setTime(date2);

            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                    cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
        }
    }

    /**
     * Check if the second date is yesterday
     * @param currentDate The current date
     * @param itemDate The date to check
     * @return true if the second date is yesterday, false otherwise
     */
    public static boolean isYesterday(Date currentDate, Date itemDate) {
        if (currentDate == null || itemDate == null) {
            return false;
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(currentDate);
        cal1.add(Calendar.DAY_OF_MONTH, -1);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(itemDate);

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Check if the date is in the last week
     * @param currentDate The current date
     * @param itemDate The date to check
     * @return true if the date is in the last week, false otherwise
     */
    public static boolean isLastWeek(Date currentDate, Date itemDate) {
        if (currentDate == null || itemDate == null) {
            return false;
        }

        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(currentDate);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(itemDate);

        // Set hours, minutes, seconds, and milliseconds to 0 for both dates
        resetTime(cal1);
        resetTime(cal2);

        // Calculate the difference in days
        long daysDifference = TimeUnit.MILLISECONDS.toDays(cal1.getTimeInMillis() - cal2.getTimeInMillis());

        // Check if the itemDate is within the last week
        return daysDifference >= 0 && daysDifference <= 7 && cal2.before(cal1);
    }

    /**
     * Capitalize the first char
     * @param input The string to capitalize
     * @return The capitalized string
     */
    public static String capitalizeFirstChar(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }

    /**
     * Check if the date is in the last month
     * @param currentDate The current date
     * @param itemDate The date to check
     * @return true if the date is in the last month, false otherwise
     */
    public static boolean isLastMonth(Date currentDate, Date itemDate) {
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTime(currentDate);

        Calendar itemCalendar = Calendar.getInstance();
        itemCalendar.setTime(itemDate);

        // Calculate the date 7 days before today
        Calendar lastWeekCalendar = Calendar.getInstance();
        lastWeekCalendar.add(Calendar.DAY_OF_MONTH, -6);

        // Check if the item's date is in the last month
        return (currentCalendar.get(Calendar.MONTH) == itemCalendar.get(Calendar.MONTH)
                || (currentCalendar.get(Calendar.DAY_OF_MONTH) < itemCalendar.get(Calendar.DAY_OF_MONTH)
                && lastWeekCalendar.getTime().before(itemDate)))
                && currentCalendar.get(Calendar.YEAR) == itemCalendar.get(Calendar.YEAR);
    }

    /**
     * Get the day of the week
     * @param date The date
     * @return The day of the week in format EEEE (e.g. Monday)
     */
    public static String getDayOfWeek(Date date) {
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        return capitalizeFirstChar(dayFormat.format(date));
    }

    /**
     * Get the month
     * @param date The date
     * @return The month in format MMMM (e.g. January)
     */
    public static String getMonth(Date date) {
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.getDefault());
        return capitalizeFirstChar(monthFormat.format(date));
    }

    /**
     * Reset the current calendar, use to compare two dates, since we only need the date, not the time
     * @param calendar The calendar to reset
     */
    private static void resetTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    /**
     * Format the duration of the audio
     * @param duration The duration in double
     * @return The formatted duration in format mm:ss (e.g. 01:30)
     */
    public static String formatDuration(double duration) {
        long minutes = (long) (duration / 60);
        long seconds = (long) (duration % 60);
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    /**
     * Parse the given date string to date object and return a date instance based on the given
     * date string. This makes use of the {@link DateUtils#determineDateFormat(String)} to determine
     * the SimpleDateFormat pattern to be used for parsing.
     * @param dateString The date string to be parsed to date object.
     * @return The parsed date object.
     * @throws ParseException If the date format pattern of the given date string is unknown, or if
     * the given date string or its actual date is invalid based on the date format pattern.
     */
    public static Date parse(String dateString) throws ParseException {
        String dateFormat = determineDateFormat(dateString);
        if (dateFormat == null) {
            throw new ParseException("Unknown date format.", 0);
        }
        return parse(dateString, dateFormat);
    }

    /**
     * Validate the actual date of the given date string based on the given date format pattern and
     * return a date instance based on the given date string.
     * @param dateString The date string.
     * @param dateFormat The date format pattern which should respect the SimpleDateFormat rules.
     * @return The parsed date object.
     * @throws ParseException If the given date string or its actual date is invalid based on the
     * given date format pattern.
     * @see SimpleDateFormat
     */
    public static Date parse(String dateString, String dateFormat) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.getDefault());
        simpleDateFormat.setLenient(false); // Don't automatically convert invalid date.
        return simpleDateFormat.parse(dateString);
    }

    /**
     * Checks whether the actual date of the given date string is valid. This makes use of the
     * {@link DateUtils#determineDateFormat(String)} to determine the SimpleDateFormat pattern to be
     * used for parsing.
     * @param dateString The date string.
     * @return True if the actual date of the given date string is valid.
     */
    public static boolean isValidDate(String dateString) {
        try {
            parse(dateString);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Determine SimpleDateFormat pattern matching with the given date string. Returns null if
     * format is unknown. You can simply extend DateUtil with more formats if needed.
     * @param dateString The date string to determine the SimpleDateFormat pattern for.
     * @return The matching SimpleDateFormat pattern, or null if format is unknown.
     * @see SimpleDateFormat
     */
    public static String determineDateFormat(String dateString) {
        for (DatePattern pat : DATE_TIME_PATTERNS) {
            Pattern pattern = Pattern.compile(pat.regexPattern);
            Matcher matcher = pattern.matcher(dateString);

            // Se trovi un match, estrai la parte interessante
            if (matcher.find()) {
                Log.d("DateUtils", "dateString : " + dateString + " determineDateFormat: " + pat.dateTimeFormat);
                return pat.dateTimeFormat;
            }
        }
        return null; // Unknown format.
    }


    public static Date isValidTime(String timeString) {
        // 113232
        SimpleDateFormat sdf = new SimpleDateFormat("HHmmss", Locale.getDefault());
        sdf.setLenient(false);

        try {
            // true if date is not null
            return sdf.parse(timeString);
        } catch (ParseException e) {
            // false if parsing fails
            e.printStackTrace();
            return null;
        }
    }

    public static Date parseDateTime(String dateTimeString) {
        try {
            SimpleDateFormat combinedFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
            return combinedFormat.parse(dateTimeString);
        } catch (ParseException e) {
            e.printStackTrace();
            // Handle parsing exception as needed
            return null;
        }
    }

    /**
     * Format the time
     * @param time The time string (e.g. 113232.448+0100)
     * @return The formatted time in format HHmmss (e.g. 113232)
     */
    public static String parseTime(String time) {
        // 113232
        SimpleDateFormat sdf = new SimpleDateFormat("HHmmss", Locale.getDefault());
        sdf.setLenient(false);

        try {
            Date date = sdf.parse(time);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmss", Locale.getDefault());
            if (date != null) {
                return timeFormat.format(date);
            } else return null;
        } catch (ParseException e) {
            e.printStackTrace();
            // false if parsing fails
            return null;
        }
    }


}
