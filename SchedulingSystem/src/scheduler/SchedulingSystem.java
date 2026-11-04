 package scheduler;

import java.util.Scanner;

/**
 * Filename: SchedulingSystem.java
 * Part of Project: Assignment 3
 *
 * File Purpose:
 * This file is the main entrance for the James Institute of Technology scheduling program.
 *
 * Class Purpose:
 * This class only holds the main method. It creates the one Scanner that the whole program shares
 * for reading the keyboard and hands it off to SchedulingApplication, which does all of the real work.
 * I kept this class small on purpose so the startup is easy to find and read.
 */
public class SchedulingSystem
{
    /**
     * The main entry point for the application.
     *
     * @param args Command line arguments, not used by this program
     */
    public static void main(String[] args)
    {
        // Creating the Scanner inside a try with resources so it automatically gets closed when the
        // program is done.
        try (Scanner input = new Scanner(System.in))
        {
            // Building the application and starting it up.
            new SchedulingApplication(input).run();
        }
    }
}
