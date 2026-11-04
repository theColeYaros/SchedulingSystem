package scheduler.logic;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import scheduler.model.ClockTime;
import scheduler.model.TimeSlot;

/**
 * Filename: CandidateSlots.java
 * Part of Project: Assignment 3
 *
 * File Purpose:
 * This file holds all of the JIT scheduling rules for which days and times a class is allowed to use.
 *
 * Class Purpose:
 * Given a class's credit hours and the time slot the professor asked for, this class builds the
 * full list of time slots to try, in the exact order the scheduling rules say to try them. The
 * Scheduler then just walks down the list until it finds an open classroom. Keeping the rules in
 * here means the Scheduler never has to worry about what order to search in.
 */
public final class CandidateSlots
{
    // The day patterns that classes can be scheduled on. 4 and 3 credit classes are split across two
    // days, while 2 and 1 credit classes meet on a single day.
    private static final String[] SPLIT_DAY_PATTERNS = { "MW", "TR" };
    private static final String[] SINGLE_DAYS = { "M", "T", "W", "R" };
    private static final String[] FRIDAY = { "F" };

    // The allowed start times for each kind of class, built straight from the times in the
    // assignment. For example, 4 credit classes start every two hours from 8:30 to 2:30.
    private static final int[] FOUR_CREDIT_STARTS = hourly(ClockTime.of(8, 30), ClockTime.of(14, 30), 120);
    private static final int[] THREE_CREDIT_STARTS = hourly(ClockTime.of(9, 0), ClockTime.of(15, 0), 60);
    private static final int[] TWO_CREDIT_STARTS = hourly(ClockTime.of(9, 0), ClockTime.of(14, 0), 60);
    private static final int[] ONE_CREDIT_STARTS = hourly(ClockTime.of(9, 0), ClockTime.of(15, 0), 60);
    private static final int[] FOUR_CREDIT_FRIDAY_STARTS = hourly(ClockTime.of(8, 30), ClockTime.of(12, 30), 60);
    private static final int[] THREE_CREDIT_FRIDAY_STARTS = hourly(ClockTime.of(9, 0), ClockTime.of(13, 0), 60);

    // How long each kind of class lasts in minutes. Friday classes get the whole week's time in one
    // long block since nobody wants to work on Fridays.
    private static final int FOUR_CREDIT_LENGTH = 120;
    private static final int THREE_CREDIT_LENGTH = 90;
    private static final int TWO_CREDIT_LENGTH = 120;
    private static final int ONE_CREDIT_LENGTH = 60;
    private static final int FOUR_CREDIT_FRIDAY_LENGTH = 240;
    private static final int THREE_CREDIT_FRIDAY_LENGTH = 180;

    /**
     * Private constructor so nobody can make an object of this class. Everything in here is static.
     */
    private CandidateSlots()
    {
    }

    /**
     * This is the method the Scheduler calls. It picks the right set of rules based on the credit
     * hours and hands back every time slot to try in order.
     *
     * @param creditHours The credit hours of the course being scheduled
     * @param requested The time slot the professor asked for
     * @return Every time slot to try, starting with the most preferred
     */
    public static List<TimeSlot> forRequest(int creditHours, TimeSlot requested)
    {
        // 4 and 3 credit classes follow the split day rules, while 2 and 1 credit classes follow the
        // simplified single day rules.
        return switch (creditHours)
        {
            case 4 -> splitDayCandidates(requested, FOUR_CREDIT_STARTS, FOUR_CREDIT_LENGTH,
                    FOUR_CREDIT_FRIDAY_STARTS, FOUR_CREDIT_FRIDAY_LENGTH);
            case 3 -> splitDayCandidates(requested, THREE_CREDIT_STARTS, THREE_CREDIT_LENGTH,
                    THREE_CREDIT_FRIDAY_STARTS, THREE_CREDIT_FRIDAY_LENGTH);
            case 2 -> singleDayCandidates(requested, TWO_CREDIT_STARTS, TWO_CREDIT_LENGTH);
            case 1 -> singleDayCandidates(requested, ONE_CREDIT_STARTS, ONE_CREDIT_LENGTH);

            // Should never happen since the database only allows 1 through 4 credit hours.
            default -> throw new IllegalArgumentException("Unsupported credit hours: " + creditHours);
        };
    }

    /**
     * Builds the search order for 4 and 3 credit classes. The order is:
     *     1. The requested time on the requested days.
     *     2. The same time on the opposite days, so MW becomes TR and TR becomes MW.
     *     3. Every time after the requested one, moving through the week and looping back around.
     *     4. The long Friday blocks, earliest first.
     *
     * @param requested The time slot the professor asked for
     * @param starts The allowed start times Monday through Thursday
     * @param length How long the class lasts Monday through Thursday
     * @param fridayStarts The allowed start times on Friday
     * @param fridayLength How long the class lasts on Friday
     * @return Every time slot to try in order
     */
    private static List<TimeSlot> splitDayCandidates(TimeSlot requested, int[] starts, int length,
            int[] fridayStarts, int fridayLength)
    {
        // Building every MW and TR time slot in order so the whole week is in one list.
        List<TimeSlot> week = buildSlots(SPLIT_DAY_PATTERNS, starts, length);

        // Finding where the requested time is in that list.
        int startingIndex = findStartingIndex(week, requested);
        TimeSlot first = week.get(startingIndex);

        // I am using a LinkedHashSet because it keeps everything in the order it was added but
        // ignores anything that's already in it. That way when the loop around reaches a time that
        // was already tried, like the opposite days, it doesn't get added a second time.
        Set<TimeSlot> candidates = new LinkedHashSet<>();

        // Rule 1, the requested time.
        candidates.add(first);

        // Rule 2, the same time on the opposite days.
        candidates.add(new TimeSlot(oppositeDays(first.getDays()), first.getStartMinutes(), first.getEndMinutes()));

        // Rule 3, every other time in the week starting from the requested one and looping around.
        candidates.addAll(rotate(week, startingIndex));

        // Rule 4, the Friday blocks as the last resort.
        candidates.addAll(buildSlots(FRIDAY, fridayStarts, fridayLength));

        // Turning it back into a list for the Scheduler.
        return new ArrayList<>(candidates);
    }

    /**
     * Builds the search order for 2 and 1 credit classes. These are simpler because they don't
     * check the opposite days. They just move forward through the week from the requested time,
     * loop back around, and then try Friday.
     *
     * @param requested The time slot the professor asked for
     * @param starts The allowed start times
     * @param length How long the class lasts
     * @return Every time slot to try in order
     */
    private static List<TimeSlot> singleDayCandidates(TimeSlot requested, int[] starts, int length)
    {
        // Building every Monday through Thursday time slot in order.
        List<TimeSlot> week = buildSlots(SINGLE_DAYS, starts, length);

        // Starting from the requested time and looping around the rest of the week.
        Set<TimeSlot> candidates = new LinkedHashSet<>(rotate(week, findStartingIndex(week, requested)));

        // 1 and 2 credit Friday classes use the same start times as the rest of the week.
        candidates.addAll(buildSlots(FRIDAY, starts, length));
        return new ArrayList<>(candidates);
    }

    /**
     * A helper method that builds a time slot for every combination of days and start times.
     *
     * @param dayPatterns The days to build slots for
     * @param starts The start times to build slots for
     * @param length How long each slot lasts
     * @return Every time slot in day order, then time order
     */
    private static List<TimeSlot> buildSlots(String[] dayPatterns, int[] starts, int length)
    {
        List<TimeSlot> slots = new ArrayList<>();

        // Going through each day and adding every start time for that day before moving on.
        for (String days : dayPatterns)
        {
            for (int start : starts)
            {
                slots.add(new TimeSlot(days, start, start + length));
            }
        }
        return slots;
    }

    /**
     * Finds where the requested time slot is in the list for the week. The input files are
     * supposed to be good data so it should always find an exact match, but if it doesn't, it
     * falls back to the next time after the requested one on the same days.
     *
     * @param week Every time slot for the week in order
     * @param requested The time slot the professor asked for
     * @return The position in the list to start searching from
     */
    private static int findStartingIndex(List<TimeSlot> week, TimeSlot requested)
    {
        // Walking through the week until finding the right days at or after the requested time.
        for (int index = 0; index < week.size(); index++)
        {
            TimeSlot slot = week.get(index);
            if (slot.getDays().equals(requested.getDays()) && slot.getStartMinutes() >= requested.getStartMinutes())
            {
                return index;
            }
        }

        // Nothing matched so just start at the beginning of the week.
        return 0;
    }

    /**
     * This method is what does the "loop around". It takes the list and starts it at a different
     * spot, with everything that came before that spot moved to the end.
     * For example, rotating [A, B, C, D] starting at C gives [C, D, A, B].
     *
     * @param slots The list being rotated
     * @param startingIndex The position that should come first
     * @return A new list starting at the given position
     */
    private static List<TimeSlot> rotate(List<TimeSlot> slots, int startingIndex)
    {
        List<TimeSlot> rotated = new ArrayList<>();

        // The % makes the position wrap back to 0 once it goes past the end of the list.
        for (int offset = 0; offset < slots.size(); offset++)
        {
            rotated.add(slots.get((startingIndex + offset) % slots.size()));
        }
        return rotated;
    }

    /**
     * Swaps MW for TR and TR for MW.
     *
     * @param days The days being swapped
     * @return The opposite days
     */
    private static String oppositeDays(String days)
    {
        return days.equals("MW") ? "TR" : "MW";
    }

    /**
     * A helper method to build a list of start times that are evenly spaced apart. I used this
     * so I didn't have to type out every single start time by hand.
     *
     * @param first The first start time
     * @param last The last start time
     * @param step How many minutes apart each start time is
     * @return Every start time from first to last
     */
    private static int[] hourly(int first, int last, int step)
    {
        // Figuring out how many start times fit between the first and the last.
        int[] starts = new int[(last - first) / step + 1];

        // Filling in each start time.
        for (int index = 0; index < starts.length; index++)
        {
            starts[index] = first + index * step;
        }
        return starts;
    }
}
