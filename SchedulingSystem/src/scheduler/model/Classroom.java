package scheduler.model;

/**
 * Filename: Classroom.java
 * Part of Project: Assignment 3
 *
 * File Purpose:
 * This file holds the class that represents one of the JIT classrooms.
 *
 * Class Purpose:
 * A simple class that holds one row of the CLASSROOM_TABLE. The capacity is used in the class
 * report to add up how many students can take each class.
 */
public class Classroom
{
    // The classroom information that gets pulled from the database.
    private final String designation;
    private final int capacity;

    /**
     * The main constructor for the class.
     *
     * @param designation The letter of the classroom, such as "A"
     * @param capacity How many students the classroom can hold
     */
    public Classroom(String designation, int capacity)
    {
        this.designation = designation;
        this.capacity = capacity;
    }

    /**
     * @return The letter of the classroom
     */
    public String getDesignation()
    {
        return designation;
    }

    /**
     * @return How many students the classroom can hold
     */
    public int getCapacity()
    {
        return capacity;
    }
}
