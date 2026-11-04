package scheduler.ui;

/**
 * Filename: MenuOption.java
 * Part of Project: Assignment 3
 *
 * File Purpose:
 * This file holds every option on the main menu.
 *
 * Class Purpose:
 * I used an enum for the menu so each option has a name instead of just a number. The menu numbers
 * come from the order the options are listed here, so adding a new option is as easy as adding a
 * new line.
 */
public enum MenuOption
{
    // Every option and the text that shows on the menu for it.
    LOAD_FILE("Load an additional class file"),
    DAY_TIME_REPORT("Report: Schedule by day/time"),
    PROFESSOR_REPORT("Report: Schedule by faculty member"),
    CLASS_REPORT("Report: Schedule by class"),
    FINISHED("Finished");

    // The text shown on the menu.
    private final String label;

    /**
     * The constructor for each option.
     *
     * @param label The text shown on the menu
     */
    MenuOption(String label)
    {
        this.label = label;
    }

    /**
     * @return The text shown on the menu
     */
    public String getLabel()
    {
        return label;
    }
}
