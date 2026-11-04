package scheduler.model;

/**
 * Filename: ClassRequest.java
 * Part of Project: Assignment 3
 *
 * File Purpose:
 * This file holds the class that represents one line from an input file.
 *
 * Class Purpose:
 * Each line of the input file is a professor asking for a class to be scheduled at a certain day and
 * time. This class holds that request exactly as it was asked for, before the scheduler decides
 * where it actually goes.
 */
public class ClassRequest
{
    // The line number is kept so any error messages can tell the user exactly which line was bad.
    private final int lineNumber;

    // The information read from the line.
    private final String courseId;
    private final String professorName;
    private final TimeSlot requestedSlot;

    /**
     * The main constructor for the class.
     *
     * @param lineNumber The line in the file this request came from
     * @param courseId The course being requested
     * @param professorName The professor who will teach it
     * @param requestedSlot The days and time the professor asked for
     */
    public ClassRequest(int lineNumber, String courseId, String professorName, TimeSlot requestedSlot)
    {
        this.lineNumber = lineNumber;
        this.courseId = courseId;
        this.professorName = professorName;
        this.requestedSlot = requestedSlot;
    }

    /**
     * @return The line in the file this request came from
     */
    public int getLineNumber()
    {
        return lineNumber;
    }

    /**
     * @return The course being requested
     */
    public String getCourseId()
    {
        return courseId;
    }

    /**
     * @return The professor who will teach it
     */
    public String getProfessorName()
    {
        return professorName;
    }

    /**
     * @return The days and time the professor asked for
     */
    public TimeSlot getRequestedSlot()
    {
        return requestedSlot;
    }
}
