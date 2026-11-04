package scheduler.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import scheduler.model.ClassRequest;
import scheduler.model.Classroom;
import scheduler.model.Course;
import scheduler.model.Section;

/**
 * Filename: ReportPrinter.java
 * Part of Project: Assignment 3
 *
 * File Purpose:
 * This file holds the code that prints the three reports and the scheduling results.
 *
 * Class Purpose:
 * This class takes the data that was loaded from the database and prints it out in lined up
 * columns so it is easy to read. The three reports are the schedule by day/time, the schedule by
 * faculty member with their total credit hours, and the schedule by class with the total student
 * capacity. It also prints a line for each class as it gets scheduled so the user can see where
 * everything went.
 */
public class ReportPrinter
{
    // The day letters used in the schedule and the full name of each day for the report headings.
    private static final char[] DAY_CODES = { 'M', 'T', 'W', 'R', 'F' };
    private static final String[] DAY_NAMES = { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday" };

    // The column layouts for each report. The numbers are how wide each column is and the '-'
    // lines the text up on the left. Keeping them up here means the headings and the rows always
    // line up since they use the exact same layout.
    private static final String DAY_ROW = "  %-11s  %-4s  %-10s  %-36s  %-9s  %s%n";
    private static final String PROFESSOR_ROW = "  %-10s  %-36s  %-11s  %-11s  %-4s  %7s%n";
    private static final String CLASS_ROW = "  %-10s  %-9s  %-11s  %-11s  %-4s  %8s%n";
    private static final String UNSCHEDULED_ROW = "  %-10s  %-36s  %s%n";
    private static final String RESULT_ROW = "  %-10s  %-9s  requested %-14s  ->  %s%n";

    // Text shown in place of the days for any class that couldn't be scheduled.
    private static final String UNSCHEDULED = "UNSCHEDULED";

    // Variable to help with formatting the width of the reports.
    private static final int REPORT_WIDTH = 96;

    /**
     * Prints one line showing what happened to a request. It shows what was asked for and where it
     * ended up, and marks it as moved if it didn't get the time it asked for.
     *
     * @param request The request that was scheduled
     * @param section The section it turned into
     */
    public void printSchedulingResult(ClassRequest request, Section section)
    {
        String outcome;

        // Nowhere was open so it couldn't be scheduled.
        if (!section.isScheduled())
        {
            outcome = "UNABLE TO SCHEDULE";
        }
        // It got exactly what it asked for.
        else if (section.getTimeSlot().equals(request.getRequestedSlot()))
        {
            outcome = section.getTimeSlot() + "  Room " + section.getClassroom();
        }
        // It got scheduled but at a different time or day than requested.
        else
        {
            outcome = section.getTimeSlot() + "  Room " + section.getClassroom() + "  (moved)";
        }

        System.out.printf(RESULT_ROW, section.getSectionName(), section.getProfessorName(),
                request.getRequestedSlot(), outcome);
    }

    /**
     * Prints the schedule by day/time report. Each day of the week gets its own section with the
     * classes sorted by start time and then by classroom. A MW class shows up under both Monday
     * and Wednesday since it meets on both days. Any classes that couldn't be scheduled are listed
     * at the bottom.
     *
     * @param sections Every section in the schedule
     * @param courses Every course, used to look up the titles
     */
    public void printScheduleByDayTime(List<Section> sections, Map<String, Course> courses)
    {
        printTitle("SCHEDULE BY DAY/TIME");

        // Pulling out only the sections that were scheduled since the others don't have a time.
        List<Section> scheduled = new ArrayList<>();
        for (Section section : sections)
        {
            if (section.isScheduled())
            {
                scheduled.add(section);
            }
        }

        // Sorting by start time first, and if two classes start at the same time then by classroom.
        scheduled.sort(Comparator.comparingInt((Section section) -> section.getTimeSlot().getStartMinutes())
                .thenComparing(Section::getClassroom));

        // Going through each day of the week.
        for (int dayIndex = 0; dayIndex < DAY_CODES.length; dayIndex++)
        {
            // Printing the day and the column headings.
            System.out.println();
            System.out.println(DAY_NAMES[dayIndex]);
            System.out.printf(DAY_ROW, "Time", "Room", "Section", "Title", "Professor", "Days");
            printDivider();

            // Counting the classes so I know if the day was empty.
            int classCount = 0;

            // Printing every class that meets on this day. Since the list is already sorted, they
            // come out in time order.
            for (Section section : scheduled)
            {
                if (section.getTimeSlot().meetsOn(DAY_CODES[dayIndex]))
                {
                    System.out.printf(DAY_ROW, section.getTimeSlot().getTimeRange(), section.getClassroom(),
                            section.getSectionName(), courses.get(section.getCourseId()).getTitle(),
                            section.getProfessorName(), section.getTimeSlot().getDays());
                    classCount++;
                }
            }

            // Printing a message so the day isn't just blank.
            if (classCount == 0)
            {
                System.out.println("  No classes scheduled");
            }
        }

        // Adding the classes that couldn't be scheduled at the end.
        printUnscheduledSections(sections, courses);
    }

    /**
     * Prints the schedule by faculty member report. Each professor gets their own section with
     * every class they were assigned and the total credit hours they are teaching. Classes that
     * couldn't be scheduled still show up but don't count toward the total since they aren't
     * actually being taught.
     *
     * @param professorNames Every professor in alphabetical order
     * @param sections Every section in the schedule
     * @param courses Every course, used to look up the titles and credit hours
     */
    public void printScheduleByProfessor(List<String> professorNames, List<Section> sections, Map<String, Course> courses)
    {
        printTitle("SCHEDULE BY FACULTY MEMBER");

        // Going through each professor.
        for (String professorName : professorNames)
        {
            // Printing the professor and the column headings.
            System.out.println();
            System.out.println(professorName);
            System.out.printf(PROFESSOR_ROW, "Section", "Title", "Days", "Time", "Room", "Credits");
            printDivider();

            // Variables to add up the credit hours and count the classes.
            int totalCreditHours = 0;
            int sectionCount = 0;

            // Finding every section this professor teaches.
            for (Section section : sections)
            {
                if (section.getProfessorName().equals(professorName))
                {
                    Course course = courses.get(section.getCourseId());
                    System.out.printf(PROFESSOR_ROW, section.getSectionName(), course.getTitle(), daysOf(section),
                            timeOf(section), roomOf(section), course.getCreditHours());

                    // Only scheduled classes count toward the total.
                    if (section.isScheduled())
                    {
                        totalCreditHours += course.getCreditHours();
                    }
                    sectionCount++;
                }
            }

            // Printing a message if the professor has nothing so the section isn't blank.
            if (sectionCount == 0)
            {
                System.out.println("  No classes assigned");
            }
            System.out.printf("  Total Credit Hours Taught: %d%n", totalCreditHours);
        }
    }

    /**
     * Prints the schedule by class report. Every course in the catalog gets its own section with
     * all of its sections listed and the total number of students that can take it. The total
     * capacity is found by adding up the size of each classroom the sections are in.
     *
     * @param courses Every course in the catalog
     * @param classrooms Every classroom, used to look up the capacities
     * @param sections Every section in the schedule
     */
    public void printScheduleByClass(Map<String, Course> courses, List<Classroom> classrooms, List<Section> sections)
    {
        printTitle("SCHEDULE BY CLASS");

        // Building a map of classroom letter to capacity so I can look up a classroom's size quickly
        // instead of searching through the list every time.
        Map<String, Integer> capacities = new HashMap<>();
        for (Classroom classroom : classrooms)
        {
            capacities.put(classroom.getDesignation(), classroom.getCapacity());
        }

        // Going through each course in the catalog.
        for (Course course : courses.values())
        {
            // Printing the course and the column headings.
            System.out.println();
            System.out.printf("%s  %s  (Credit Hours: %d)%n", course.getCourseId(), course.getTitle(), course.getCreditHours());
            System.out.printf(CLASS_ROW, "Section", "Professor", "Days", "Time", "Room", "Capacity");
            printDivider();

            // Variables to add up the capacity and count the sections.
            int totalCapacity = 0;
            int sectionCount = 0;

            // Finding every section of this course.
            for (Section section : sections)
            {
                if (section.getCourseId().equals(course.getCourseId()))
                {
                    // An unscheduled section doesn't have a classroom so no students can take it.
                    int capacity = section.isScheduled() ? capacities.get(section.getClassroom()) : 0;
                    System.out.printf(CLASS_ROW, section.getSectionName(), section.getProfessorName(),
                            daysOf(section), timeOf(section), roomOf(section), capacity);
                    totalCapacity += capacity;
                    sectionCount++;
                }
            }

            // Printing a message if the course isn't being offered so the section isn't blank.
            if (sectionCount == 0)
            {
                System.out.println("  No sections offered");
            }
            System.out.printf("  Sections: %d    Total Student Capacity: %d%n", sectionCount, totalCapacity);
        }
    }

    /**
     * Prints a list of every class that couldn't be scheduled so the administration knows what
     * they need to take care of.
     *
     * @param sections Every section in the schedule
     * @param courses Every course, used to look up the titles
     */
    private void printUnscheduledSections(List<Section> sections, Map<String, Course> courses)
    {
        // Printing the heading.
        System.out.println();
        System.out.println("Unable to Schedule");
        System.out.printf(UNSCHEDULED_ROW, "Section", "Title", "Professor");
        printDivider();

        int unscheduledCount = 0;

        // Printing every section that doesn't have a classroom.
        for (Section section : sections)
        {
            if (!section.isScheduled())
            {
                System.out.printf(UNSCHEDULED_ROW, section.getSectionName(),
                        courses.get(section.getCourseId()).getTitle(), section.getProfessorName());
                unscheduledCount++;
            }
        }

        // Everything got scheduled.
        if (unscheduledCount == 0)
        {
            System.out.println("  None");
        }
    }

    /**
     * Gets the days for the report, or UNSCHEDULED if the section doesn't have any.
     *
     * @param section The section being printed
     * @return The text for the days column
     */
    private String daysOf(Section section)
    {
        return section.isScheduled() ? section.getTimeSlot().getDays() : UNSCHEDULED;
    }

    /**
     * Gets the time range for the report, or blank if the section doesn't have one.
     *
     * @param section The section being printed
     * @return The text for the time column
     */
    private String timeOf(Section section)
    {
        return section.isScheduled() ? section.getTimeSlot().getTimeRange() : "";
    }

    /**
     * Gets the classroom for the report, or blank if the section doesn't have one.
     *
     * @param section The section being printed
     * @return The text for the room column
     */
    private String roomOf(Section section)
    {
        return section.isScheduled() ? section.getClassroom() : "";
    }

    /**
     * Prints the big title at the top of each report with the title centered between two lines.
     *
     * @param title The title of the report
     */
    private void printTitle(String title)
    {
        System.out.println();
        System.out.println("=".repeat(REPORT_WIDTH));

        // Adding spaces in front of the title to push it to the middle.
        System.out.println(" ".repeat((REPORT_WIDTH - title.length()) / 2) + title);
        System.out.println("=".repeat(REPORT_WIDTH));
    }

    /**
     * Prints a line of dashes under the column headings.
     */
    private void printDivider()
    {
        System.out.println("  " + "-".repeat(REPORT_WIDTH - 2));
    }
}
