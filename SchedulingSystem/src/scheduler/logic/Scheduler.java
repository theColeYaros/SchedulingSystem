package scheduler.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import scheduler.model.ClassRequest;
import scheduler.model.Classroom;
import scheduler.model.Course;
import scheduler.model.Section;
import scheduler.model.TimeSlot;

/**
 * Filename: Scheduler.java
 * Part of Project: Assignment 3
 *
 * File Purpose:
 * This file holds the class that decides where every requested class gets placed.
 *
 * Class Purpose:
 * The Scheduler takes requests one at a time, first come first serve, and finds the first open
 * classroom. It keeps every section that is already scheduled in memory so it can check for conflicts
 * without having to keep a database connection open the whole time. It also keeps track of the
 * section numbers so each course counts up 01, 02, 03 and so on, even across multiple input files.
 */
public class Scheduler
{
    // All of the courses in the catalog, looked up by course ID.
    private final Map<String, Course> courses;

    // The classrooms in the order they should be checked, A then B then C.
    private final List<Classroom> classrooms;

    // Every section scheduled so far, both from the database and from the current file.
    private final List<Section> sections;

    // The highest section number used so far for each course.
    private final Map<String, Integer> lastSectionNumbers = new HashMap<>();

    /**
     * The main constructor for the class. It takes everything already in the database so new
     * classes don't get scheduled on top of old ones.
     *
     * @param courses Every course in the catalog
     * @param classrooms Every classroom in the order to check them
     * @param existingSections Every section already in the database
     */
    public Scheduler(Map<String, Course> courses, List<Classroom> classrooms, List<Section> existingSections)
    {
        this.courses = courses;
        this.classrooms = classrooms;

        // Making my own copy of the list so adding new sections doesn't change the list that was
        // passed in.
        this.sections = new ArrayList<>(existingSections);

        // Finding the highest section number used for each course so new sections pick up where the
        // last file left off. Merge with Math::max keeps whichever number is bigger.
        for (Section section : existingSections)
        {
            lastSectionNumbers.merge(section.getCourseId(), section.getSectionNumber(), Math::max);
        }
    }

    /**
     * This is the main method of the class. It gives the request the next section number for its
     * course and tries to find it a spot.
     *
     * @param request The class being scheduled
     * @return The section, either scheduled or marked as unable to be scheduled
     */
    public Section schedule(ClassRequest request)
    {
        // Getting the course so I know how many credit hours it is.
        Course course = courses.get(request.getCourseId());

        // Adding 1 to the last section number for this course. If the course hasn't been seen yet,
        // merge just starts it at 1.
        int sectionNumber = lastSectionNumbers.merge(course.getCourseId(), 1, Integer::sum);

        // Finding a spot for the section.
        Section section = findPlacement(course, sectionNumber, request);

        // Adding the section to the list so the next request knows this spot is taken. Unscheduled
        // sections get added too, but they don't take up a classroom.
        sections.add(section);
        return section;
    }

    /**
     * This method goes through every time slot in the order the rules say to, and for each one
     * checks classroom A, then B, then C. The first open one it finds is where the class goes.
     *
     * @param course The course being scheduled
     * @param sectionNumber The section number it was given
     * @param request The class being scheduled
     * @return The scheduled section, or an unscheduled one if nothing was open
     */
    private Section findPlacement(Course course, int sectionNumber, ClassRequest request)
    {
        // Getting every time slot to try in order from the scheduling rules.
        for (TimeSlot slot : CandidateSlots.forRequest(course.getCreditHours(), request.getRequestedSlot()))
        {
            // Checking each classroom at this time.
            for (Classroom classroom : classrooms)
            {
                // Found an open spot, so the section goes here.
                if (isAvailable(classroom, slot))
                {
                    return new Section(course.getCourseId(), sectionNumber, request.getProfessorName(),
                            classroom.getDesignation(), slot);
                }
            }
        }

        // If the code makes it here, every spot including Friday was full so the class can't be
        // scheduled. It still keeps its section number so the administration can deal with it.
        return Section.unscheduled(course.getCourseId(), sectionNumber, request.getProfessorName());
    }

    /**
     * Checks if a classroom is free during a time slot by comparing it against every section that
     * is already scheduled.
     *
     * @param classroom The classroom being checked
     * @param slot The time slot being checked
     * @return True if the classroom is open, false if something is already there
     */
    private boolean isAvailable(Classroom classroom, TimeSlot slot)
    {
        for (Section section : sections)
        {
            // A conflict is when a scheduled section is in the same room and its time overlaps.
            if (section.isScheduled()
                    && section.getClassroom().equals(classroom.getDesignation())
                    && section.getTimeSlot().overlaps(slot))
            {
                return false;
            }
        }

        // No conflicts found so the classroom is open.
        return true;
    }
}
