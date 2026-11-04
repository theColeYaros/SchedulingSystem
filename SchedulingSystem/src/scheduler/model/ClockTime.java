package scheduler.model;

/**
 * Filename: ClockTime.java
 * Part of Project: Assignment 3
 *
 * File Purpose:
 * This file holds the helper methods for converting times between text and numbers.
 *
 * Class Purpose:
 * Every time in this program is stored as the number of minutes after midnight because it makes
 * comparing and adding times simple math. This class converts the times from the input file into
 * minutes, and converts minutes back into text for the reports and the database.
 */
public final class ClockTime
{
    // The input file doesn't use AM or PM, so any hour before 8 is treated as an afternoon time
    // since no class at JIT starts before 8:30 in the morning.
    private static final int FIRST_MORNING_HOUR = 8;

    // Constant to avoid a magic number in the math.
    private static final int MINUTES_PER_HOUR = 60;

    /**
     * Private constructor so nobody can make an object of this class. Everything in here is static.
     */
    private ClockTime()
    {
    }

    /**
     * A method to turn an hour and minute into minutes after midnight.
     *
     * @param hour The hour in 24 hour time
     * @param minute The minutes past the hour
     * @return The total minutes after midnight
     */
    public static int of(int hour, int minute)
    {
        return hour * MINUTES_PER_HOUR + minute;
    }

    /**
     * This method takes a time from the input file such as "8:30" or "2:30" and converts it into
     * minutes after midnight.
     *
     * @param text The time as it appears in the input file
     * @return The total minutes after midnight
     */
    public static int parse(String text)
    {
        // Splitting the time into the hour and the minutes.
        String[] parts = text.trim().split(":");

        // If there aren't exactly two pieces then the time isn't formatted right.
        if (parts.length != 2)
        {
            throw new IllegalArgumentException("Invalid time \"" + text.trim() + "\"");
        }

        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        // Moving afternoon times to 24 hour time so 2:30 becomes 14:30.
        if (hour < FIRST_MORNING_HOUR)
        {
            hour += 12;
        }
        return of(hour, minute);
    }

    /**
     * Converts minutes after midnight into a 12 hour time for the reports, like "2:30".
     * I left off AM and PM so the reports match how the times look in the input file.
     *
     * @param minutes The total minutes after midnight
     * @return The time formatted for display
     */
    public static String toDisplay(int minutes)
    {
        // Getting the hour on a 12 hour clock.
        int hour = minutes / MINUTES_PER_HOUR % 12;

        // Noon comes out as 0 so it gets swapped to 12.
        return String.format("%d:%02d", hour == 0 ? 12 : hour, minutes % MINUTES_PER_HOUR);
    }

    /**
     * Converts minutes after midnight into 24 hour time with a leading zero, like "08:30", for the
     * database. Storing it this way keeps the raw SQLite file easy to read and the times still sort
     * in the correct order.
     *
     * @param minutes The total minutes after midnight
     * @return The time formatted for the database
     */
    public static String toStorage(int minutes)
    {
        return String.format("%02d:%02d", minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR);
    }

    /**
     * Converts a time from the database back into minutes after midnight.
     *
     * @param text The time as it is stored in the database
     * @return The total minutes after midnight
     */
    public static int fromStorage(String text)
    {
        String[] parts = text.split(":");
        return of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
}
