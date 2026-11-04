package scheduler.ui;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * Filename: ConsoleUI.java
 * Part of Project: Assignment 3
 *
 * File Purpose:
 * This file holds all of the code that talks to the user through the console.
 *
 * Class Purpose:
 * Every question, prompt and message the user sees goes through this class. It also checks the
 * user's input and keeps asking until it gets something usable, so the rest of the program never
 * has to worry about bad input from the keyboard.
 */
public class ConsoleUI
{
    // The Scanner used to read the keyboard.
    private final Scanner input;

    /**
     * The main constructor for the class.
     *
     * @param input The Scanner used to read the keyboard
     */
    public ConsoleUI(Scanner input)
    {
        this.input = input;
    }

    /**
     * Shows a message to the user.
     *
     * @param message The message being shown
     */
    public void showMessage(String message)
    {
        System.out.println(message);
    }

    /**
     * Shows an error to the user with ERROR in front so it stands out.
     *
     * @param message The error being shown
     */
    public void showError(String message)
    {
        System.out.println("ERROR: " + message);
    }

    /**
     * Asks the user a yes or no question and keeps asking until they answer with Y or N.
     *
     * @param question The question being asked
     * @return True if they answered yes, false if they answered no
     */
    public boolean askYesNo(String question)
    {
        // Looping until a valid answer is given.
        while (true)
        {
            // Making the answer uppercase so y and Y both work.
            String answer = readLine(question + " (Y/N): ").toUpperCase();

            if (answer.equals("Y") || answer.equals("YES"))
            {
                return true;
            }
            if (answer.equals("N") || answer.equals("NO"))
            {
                return false;
            }

            // If the code gets here, the answer wasn't valid so the question gets asked again.
            showMessage("Please enter Y or N.");
        }
    }

    /**
     * Asks the user for the path to a file and keeps asking until the file actually exists.
     * Entering N or leaving it blank lets the user back out without loading anything.
     *
     * @param prompt The prompt shown to the user
     * @return The file, or null if the user entered N or left it blank
     */
    public Path promptForFile(String prompt)
    {
        while (true)
        {
            // Removing any quotes because Windows "Copy as path" puts quotes around the path.
            String text = readLine(prompt).replace("\"", "");

            // N or blank means the user doesn't want to load a file. equalsIgnoreCase lets n work too.
            if (text.isEmpty() || text.equalsIgnoreCase("N"))
            {
                return null;
            }

            // A try catch since a path with characters Windows doesn't allow throws an exception.
            try
            {
                Path file = Paths.get(text);

                // Checking that the file is really there before handing it back.
                if (Files.isRegularFile(file))
                {
                    return file;
                }

                // Showing the full path so the user can see exactly where the program looked.
                showMessage("File not found: " + file.toAbsolutePath());
            }
            catch (InvalidPathException e)
            {
                showMessage("Invalid file path: " + text);
            }
        }
    }

    /**
     * Shows the main menu and waits for the user to pick an option.
     *
     * @return The option the user picked
     */
    public MenuOption showMainMenu()
    {
        // Getting every option in the order it was listed in the enum.
        MenuOption[] options = MenuOption.values();

        // Printing the menu with a number in front of each option.
        System.out.println();
        System.out.println("========== JIT Scheduling System ==========");
        for (int index = 0; index < options.length; index++)
        {
            System.out.printf("  %d. %s%n", index + 1, options[index].getLabel());
        }

        // The menu starts at 1 but the array starts at 0, so 1 gets subtracted.
        return options[readChoice("Select an option: ", 1, options.length) - 1];
    }

    /**
     * Reads a number from the user and keeps asking until the number is in range.
     *
     * @param prompt The prompt shown to the user
     * @param minimum The lowest number allowed
     * @param maximum The highest number allowed
     * @return The number the user picked
     */
    private int readChoice(String prompt, int minimum, int maximum)
    {
        while (true)
        {
            String text = readLine(prompt);

            // Checking that the input is only digits before converting it. I limited it to 3 digits
            // so a crazy long number can't crash the program when it gets converted.
            if (text.matches("\\d{1,3}"))
            {
                int choice = Integer.parseInt(text);

                // Making sure it is actually one of the options.
                if (choice >= minimum && choice <= maximum)
                {
                    return choice;
                }
            }

            // If the code gets here, the input was bad so the user gets asked again.
            showMessage("Please enter a number from " + minimum + " to " + maximum + ".");
        }
    }

    /**
     * A helper method to show a prompt and read what the user types.
     *
     * @param prompt The prompt shown to the user
     * @return What the user typed with the extra spaces removed
     */
    private String readLine(String prompt)
    {
        System.out.print(prompt);
        return input.nextLine().trim();
    }
}
