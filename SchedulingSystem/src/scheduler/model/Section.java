package scheduler.model;

/**
 * Filename: Section.java
 * Part of Project: Assignment 3
 *
 * File Purpose:
 * This file holds the class that represents a scheduled section of a course.
 *
 * Class Purpose:
 * A Section is the final result of scheduling a request and matches one row of the SCHEDULE_TABLE.
 * If the section could not be placed anywhere, the classroom and time slot are left null but it
 * still keeps its section number so the administration can deal with it.
 */
public class Section
{
    // The information that identifies the section, such as CSC 116-02.
    private final String courseId;
    private final int sectionNumber;
    private final String professorName;

    // Where and when the section ended up. Both of these are null if it couldn't be scheduled.
    private final String classroom;
    private final TimeSlot timeSlot;

    /**
     * The main constructor for the class.
     *
     * @param courseId The course this section belongs to
     * @param sectionNumber The section number of the course
     * @param professorName The professor teaching the section
     * @param classroom The classroom letter, or null if unscheduled
     * @param timeSlot The days and time it meets, or null if unscheduled
     */
    public Section(String courseId, int sectionNumber, String professorName, String classroom, TimeSlot timeSlot)
    {
        this.courseId = courseId;
        this.sectionNumber = sectionNumber;
        this.professorName = professorName;
        this.classroom = classroom;
        this.timeSlot = timeSlot;
    }

    /**
     * A shortcut for making a section that couldn't be scheduled so I don't have to pass in nulls
     * everywhere.
     *
     * @param courseId The course this section belongs to
     * @param sectionNumber The section number of the course
     * @param professorName The professor teaching the section
     * @return A section with no classroom or time slot
     */
    public static Section unscheduled(String courseId, int sectionNumber, String professorName)
    {
        return new Section(courseId, sectionNumber, professorName, null, null);
    }

    /**
     * @return The course this section belongs to
     */
    public String getCourseId()
    {
        return courseId;
    }

    /**
     * @return The section number of the course
     */
    public int getSectionNumber()
    {
        return sectionNumber;
    }

    /**
     * @return The professor teaching the section
     */
    public String getProfessorName()
    {
        return professorName;
    }

    /**
     * @return The classroom letter, or null if unscheduled
     */
    public String getClassroom()
    {
        return classroom;
    }

    /**
     * @return The days and time it meets, or null if unscheduled
     */
    public TimeSlot getTimeSlot()
    {
        return timeSlot;
    }

    /**
     * A section is only scheduled if it was given a classroom.
     *
     * @return True if the section has a classroom, false if it couldn't be scheduled
     */
    public boolean isScheduled()
    {
        return classroom != null;
    }

    /**
     * Builds the full section name the way the assignment shows it, with a two digit section number.
     *
     * @return The section name, such as "CSC 116-01"
     */
    public String getSectionName()
    {
        return String.format("%s-%02d", courseId, sectionNumber);
    }
}
