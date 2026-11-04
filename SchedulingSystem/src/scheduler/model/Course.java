package scheduler.model;

/**
 * Filename: Course.java
 * Part of Project: Assignment 3
 *
 * File Purpose:
 * This file holds the class that represents a course from the JIT course catalog.
 *
 * Class Purpose:
 * A simple class that holds one row of the COURSE_TABLE. The credit hours are the most important
 * piece since they decide which scheduling rules a class follows.
 */
public class Course
{
    // The course information that gets pulled from the database.
    private final String courseId;
    private final String title;
    private final int creditHours;

    /**
     * The main constructor for the class.
     *
     * @param courseId The course ID, such as "CSC 116"
     * @param title The title of the course
     * @param creditHours How many credit hours the course is worth
     */
    public Course(String courseId, String title, int creditHours)
    {
        this.courseId = courseId;
        this.title = title;
        this.creditHours = creditHours;
    }

    /**
     * @return The course ID
     */
    public String getCourseId()
    {
        return courseId;
    }

    /**
     * @return The title of the course
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * @return How many credit hours the course is worth
     */
    public int getCreditHours()
    {
        return creditHours;
    }
}
