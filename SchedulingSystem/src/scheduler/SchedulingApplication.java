package scheduler;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import scheduler.data.DatabaseManager;
import scheduler.data.ScheduleRepository;
import scheduler.io.RequestFileReader;
import scheduler.logic.Scheduler;
import scheduler.model.ClassRequest;
import scheduler.model.Course;
import scheduler.model.Section;
import scheduler.ui.ConsoleUI;
import scheduler.ui.MenuOption;
import scheduler.ui.ReportPrinter;

/**
 * Filename: SchedulingApplication.java
 * Part of Project: Assignment 3
 *
 * File Purpose:
 * This file controls the flow of the whole program from start to finish.
 *
 * Class Purpose:
 * This class ties all of the other pieces together. When the program starts it gets the database
 * ready, either building it for the first time or asking if it should be reset. It then asks for the
 * first file of classes to schedule and goes into the main menu, where the user can load more files
 * and view the three reports until they are finished. Before the program closes, the user gets the
 * choice to delete the database or leave it so the raw SQLite file can be looked at.
 */
public class SchedulingApplication
{
    // The name of the database file. It gets created in whatever folder the program is run from.
    private static final Path DATABASE_FILE = Paths.get("JITSchedule.db");

    // The objects that do the actual work. They are put at the class level so every method has
    // access to them without having to pass them around.
    private final ConsoleUI ui;
    private final DatabaseManager database;
    private final ScheduleRepository repository;
    private final ReportPrinter reportPrinter;

    /**
     * The main constructor for the class. It creates every object the program needs.
     *
     * @param input The Scanner used to read the keyboard
     */
    public SchedulingApplication(Scanner input)
    {
        ui = new ConsoleUI(input);
        database = new DatabaseManager(DATABASE_FILE);
        repository = new ScheduleRepository(database);
        reportPrinter = new ReportPrinter();
    }

    /**
     * This is the method that runs the whole program in the order the assignment asks for.
     */
    public void run()
    {
        // If the database couldn't be set up, there is nothing else the program can do so it stops.
        if (!prepareDatabase())
        {
            return;
        }

        // Asking for the first file of classes right away.
        runDatabaseTask(() -> loadClassFile("Enter the path of the class file to schedule (or N to skip): "));

        // Showing the menu over and over until the user picks Finished.
        MenuOption option;
        do
        {
            option = ui.showMainMenu();

            // Running whatever the user picked. Finished doesn't need a case since the loop
            // just ends.
            switch (option)
            {
                case LOAD_FILE -> runDatabaseTask(() -> loadClassFile("Enter the path of the additional class file (or N to go back to the menu): "));
                case DAY_TIME_REPORT -> runDatabaseTask(this::showDayTimeReport);
                case PROFESSOR_REPORT -> runDatabaseTask(this::showProfessorReport);
                case CLASS_REPORT -> runDatabaseTask(this::showClassReport);
            }
        }
        while (option != MenuOption.FINISHED);

        // Asking about deleting the database before closing.
        shutDown();
    }

    /**
     * Gets the database ready to use. If it doesn't exist it gets built. If it does exist the user
     * is asked if they want to reset it back to its initial base state.
     *
     * @return True if the database is ready, false if something went wrong
     */
    private boolean prepareDatabase()
    {
        // A try catch since this is the first time the program touches the database. If the SQLite
        // driver is missing, this is where it shows up.
        try
        {
            // First run, so the database gets built from scratch.
            if (!database.isBuilt())
            {
                ui.showMessage("No database found. Building " + database.getDatabaseFile());
                database.buildBaseState();
            }
            // The database is already there, so asking if it should be reset.
            else if (ui.askYesNo("Database found. Reset it to its initial base state?"))
            {
                database.buildBaseState();
                ui.showMessage("Database has been reset.");
            }
            return true;
        }
        catch (SQLException e)
        {
            // Telling the user what went wrong along with the most likely reason.
            ui.showError("Unable to prepare the database: " + e.getMessage());
            ui.showError("Make sure the SQLite JDBC driver is on the classpath.");
            return false;
        }
    }

    /**
     * This method asks the user for a file, reads it, and schedules every class in it. Each class
     * gets saved to the database as soon as it is scheduled.
     *
     * @param prompt The prompt shown when asking for the file
     * @throws SQLException If anything goes wrong with the database
     */
    private void loadClassFile(String prompt) throws SQLException
    {
        // Asking the user for the file.
        Path file = ui.promptForFile(prompt);

        // The user left it blank so nothing gets loaded.
        if (file == null)
        {
            ui.showMessage("No file loaded.");
            return;
        }

        // Reading the whole file before scheduling anything. If any line is bad, the user is told
        // which one and nothing from the file gets scheduled.
        List<ClassRequest> requests;
        try
        {
            requests = RequestFileReader.read(file);
        }
        catch (IOException | IllegalArgumentException e)
        {
            ui.showError("Unable to read " + file.getFileName() + ": " + e.getMessage());
            return;
        }

        // Loading everything the scheduler needs from the database. The professors are put in a set
        // since I only need to check if a name is in it.
        Map<String, Course> courses = repository.loadCourses();
        Set<String> professors = new HashSet<>(repository.loadProfessorNames());

        // Giving the scheduler the sections that are already in the database so it doesn't put new
        // classes on top of them.
        Scheduler scheduler = new Scheduler(courses, repository.loadClassrooms(), repository.loadSections());

        // Variables to keep count of how everything went.
        int scheduledCount = 0;
        int unscheduledCount = 0;
        int skippedCount = 0;

        ui.showMessage("");
        ui.showMessage("Scheduling " + requests.size() + " requests from " + file.getFileName());

        // Scheduling the classes in the same order they were in the file, first come first serve.
        for (ClassRequest request : requests)
        {
            // The file is supposed to be good data, but if a course isn't in the catalog that line
            // gets skipped instead of crashing the program.
            if (!courses.containsKey(request.getCourseId()))
            {
                ui.showError("Line " + request.getLineNumber() + " skipped, unknown course " + request.getCourseId());
                skippedCount++;
            }
            // Same thing for a professor who doesn't work at JIT.
            else if (!professors.contains(request.getProfessorName()))
            {
                ui.showError("Line " + request.getLineNumber() + " skipped, unknown professor " + request.getProfessorName());
                skippedCount++;
            }
            // The request is good so it gets scheduled, saved and shown to the user.
            else
            {
                Section section = scheduler.schedule(request);
                repository.saveSection(section);
                reportPrinter.printSchedulingResult(request, section);

                // Counting whether it got scheduled or not.
                if (section.isScheduled())
                {
                    scheduledCount++;
                }
                else
                {
                    unscheduledCount++;
                }
            }
        }

        // Showing a summary of how the file went.
        ui.showMessage("");
        ui.showMessage("Scheduled: " + scheduledCount + "   Unable to schedule: " + unscheduledCount
                + "   Skipped: " + skippedCount);
    }

    /**
     * Loads the latest data from the database and prints the schedule by day/time report.
     *
     * @throws SQLException If the data can't be loaded
     */
    private void showDayTimeReport() throws SQLException
    {
        reportPrinter.printScheduleByDayTime(repository.loadSections(), repository.loadCourses());
    }

    /**
     * Loads the latest data from the database and prints the schedule by faculty member report.
     *
     * @throws SQLException If the data can't be loaded
     */
    private void showProfessorReport() throws SQLException
    {
        reportPrinter.printScheduleByProfessor(repository.loadProfessorNames(), repository.loadSections(),
                repository.loadCourses());
    }

    /**
     * Loads the latest data from the database and prints the schedule by class report.
     *
     * @throws SQLException If the data can't be loaded
     */
    private void showClassReport() throws SQLException
    {
        reportPrinter.printScheduleByClass(repository.loadCourses(), repository.loadClassrooms(),
                repository.loadSections());
    }

    /**
     * Before the program closes, the user is asked if they want to completely delete the database
     * or leave it so it can be looked at.
     */
    private void shutDown()
    {
        // Asking the user with a yes or no question.
        if (ui.askYesNo("Do you want to completely delete the database?"))
        {
            // If yes, then delete the database.
            try
            {
                database.deleteDatabase();
                ui.showMessage("Database deleted.");
            }
            catch (IOException e)
            {
                ui.showError("Unable to delete the database: " + e.getMessage());
            }
        }
        else
        {
            // Telling the user where the file is so they can find it.
            ui.showMessage("Database left intact at " + database.getDatabaseFile());
        }
    }

    /**
     * A helper method that runs a piece of code that uses the database and catches any database
     * errors. This method helps prevent me from writing the same try catch for every menu option,
     * and makes sure one database error shows a message instead of crashing the whole program.
     *
     * @param task The code being run
     */
    private void runDatabaseTask(DatabaseTask task)
    {
        try
        {
            task.run();
        }
        catch (SQLException e)
        {
            ui.showError("Database problem: " + e.getMessage());
        }
    }

    /**
     * A small interface so methods that throw SQLException can be passed into runDatabaseTask.
     * Java's built in Runnable can't throw checked exceptions, which is why I needed my own.
     */
    @FunctionalInterface
    private interface DatabaseTask
    {
        void run() throws SQLException;
    }
}
