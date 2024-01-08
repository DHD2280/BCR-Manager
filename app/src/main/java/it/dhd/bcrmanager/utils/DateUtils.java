package it.dhd.bcrmanager.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DateUtils {

    /**
     * Check if two dates are the same day
     * @param date1 The first date
     * @param date2 The second date
     * @return true if the two dates are the same day, false otherwise
     */
    public static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Check if the second date is yesternday
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

    public static boolean isValidDate(String dateString) {
        // 20231229
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        sdf.setLenient(false);
        try {
            // true if date is not null
            return sdf.parse(dateString) != null;
        } catch (ParseException e) {
            // false if parsing fails
            e.printStackTrace();
            return false;
        }
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
     * Format the timestamp
     * @param timestamp The timestamp
     * @return The formatted timestamp in format yyyyMMddHHmmss (e.g. 20231229113232)
     */
    public static String formatTimestamp(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        return dateFormat.format(date);
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
