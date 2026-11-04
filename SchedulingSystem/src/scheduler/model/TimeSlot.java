package scheduler.model;

import java.util.Objects;

/**
 * Filename: TimeSlot.java
 * Part of Project: Assignment 3
 *
 * File Purpose:
 * This file holds the class that represents when a class meets.
 *
 * Class Purpose:
 * A TimeSlot is the days a class meets along with its start and end time. It is used for both what
 * a professor requested in the input file and where the class actually ended up. The most important
 * job of this class is figuring out if two time slots overlap, because that is how the scheduler
 * knows if a classroom is already taken.
 */
public class TimeSlot
{
    // The days the class meets, such as "MW", "TR", "M" or "F".
    private final String days;

    // The start and end times stored as minutes after midnight.
    private final int startMinutes;
    private final int endMinutes;

    /**
     * The main constructor for the class.
     *
     * @param days The days the class meets
     * @param startMinutes The start time in minutes after midnight
     * @param endMinutes The end time in minutes after midnight
     */
    public TimeSlot(String days, int startMinutes, int endMinutes)
    {
        this.days = days;
        this.startMinutes = startMinutes;
        this.endMinutes = endMinutes;
    }

    /**
     * @return The days the class meets
     */
    public String getDays()
    {
        return days;
    }

    /**
     * @return The start time in minutes after midnight
     */
    public int getStartMinutes()
    {
        return startMinutes;
    }

    /**
     * @return The end time in minutes after midnight
     */
    public int getEndMinutes()
    {
        return endMinutes;
    }

    /**
     * A quick check to see if this time slot meets on a certain day.
     *
     * @param day The day letter being checked, such as 'M' or 'R'
     * @return True if the class meets on that day, false if it doesn't
     */
    public boolean meetsOn(char day)
    {
        return days.indexOf(day) >= 0;
    }

    /**
     * This method checks if two time slots run into each other. To overlap they need to share at
     * least one day and their times have to cross. Classes that end right when another one starts,
     * like 8:30-10:30 and 10:30-12:30, do not count as overlapping.
     *
     * @param other The time slot being compared against
     * @return True if the two time slots overlap, false if they don't
     */
    public boolean overlaps(TimeSlot other)
    {
        // Checking the times first. If one ends before the other starts, they can't overlap no
        // matter what days they are on.
        if (startMinutes >= other.endMinutes || other.startMinutes >= endMinutes)
        {
            return false;
        }

        // The times cross, so now checking if they share any days.
        for (char day : days.toCharArray())
        {
            if (other.meetsOn(day))
            {
                return true;
            }
        }

        // Same times but different days, so no conflict.
        return false;
    }

    /**
     * Builds the time range for the reports, like "8:30-10:30".
     *
     * @return The start and end time as text
     */
    public String getTimeRange()
    {
        return ClockTime.toDisplay(startMinutes) + "-" + ClockTime.toDisplay(endMinutes);
    }

    /**
     * Two time slots are equal if the days and times all match. This is needed so the scheduler can
     * tell if a class got the time it asked for and so duplicate time slots can be removed.
     *
     * @param other The object being compared
     * @return True if both time slots are the same, false if they aren't
     */
    @Override
    public boolean equals(Object other)
    {
        // Same object in memory, so it's obviously equal.
        if (this == other)
        {
            return true;
        }

        // Not even a TimeSlot so it can't be equal.
        if (!(other instanceof TimeSlot))
        {
            return false;
        }

        // Comparing every field.
        TimeSlot slot = (TimeSlot) other;
        return startMinutes == slot.startMinutes
                && endMinutes == slot.endMinutes
                && days.equals(slot.days);
    }

    /**
     * Java requires hashCode to be changed any time equals is changed so the class works correctly
     * in sets and maps.
     *
     * @return A hash built from the same fields used in equals
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(days, startMinutes, endMinutes);
    }

    /**
     * @return The time slot as text, like "MW 8:30-10:30"
     */
    @Override
    public String toString()
    {
        return days + " " + getTimeRange();
    }
}
