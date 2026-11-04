package scheduler.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import scheduler.model.Classroom;
import scheduler.model.ClockTime;
import scheduler.model.Course;
import scheduler.model.Section;
import scheduler.model.TimeSlot;

/**
 * Filename: ScheduleRepository.java
 * Part of Project: Assignment 3
 *
 * File Purpose:
 * This file holds all of the code that reads from and saves to the database tables.
 *
 * Class Purpose:
 * Every method in this class opens its own connection, does one job, and closes the connection
 * right away. This keeps the database open for as little time as possible. The rest of the program
 * never writes any SQL, it just asks this class for what it needs.
 */
public class ScheduleRepository
{
    // Command to get the course catalog sorted by course ID so the reports come out in order.
    private static final String SELECT_COURSES =
        "SELECT COURSE_ID, COURSE_TITLE, CREDIT_HOURS FROM COURSE_TABLE ORDER BY COURSE_ID";

    // Command to get the classrooms in A, B, C order, which is the order the scheduler checks them.
    private static final String SELECT_CLASSROOMS =
        "SELECT CLASSROOM_DESIGNATION, CAPACITY FROM CLASSROOM_TABLE ORDER BY CLASSROOM_DESIGNATION";

    // Command to get every professor sorted by name.
    private static final String SELECT_PROFESSORS =
        "SELECT PROFESSOR_NAME FROM PROFESSOR_TABLE ORDER BY PROFESSOR_NAME";

    // Command to get every section with the TUIDs swapped out for the actual course ID, professor
    // name and classroom letter. The classroom uses a LEFT JOIN because unscheduled sections don't
    // have a classroom, and a regular JOIN would leave them out completely.
    private static final String SELECT_SECTIONS =
        "SELECT c.COURSE_ID, s.SECTION, p.PROFESSOR_NAME, r.CLASSROOM_DESIGNATION, "
            + "s.DAYS, s.START_TIME, s.END_TIME "
            + "FROM SCHEDULE_TABLE s "
            + "JOIN COURSE_TABLE c ON c.TUID = s.COURSE_TUID "
            + "JOIN PROFESSOR_TABLE p ON p.TUID = s.PROFESSOR_TUID "
            + "LEFT JOIN CLASSROOM_TABLE r ON r.TUID = s.CLASSROOM_TUID "
            + "ORDER BY c.COURSE_ID, s.SECTION";

    // Command to save a section. The small SELECT statements inside it look up the TUIDs from the
    // course ID, classroom letter and professor name so the rest of the program never has to deal
    // with TUIDs.
    private static final String INSERT_SECTION =
        "INSERT INTO SCHEDULE_TABLE "
            + "(COURSE_TUID, SECTION, CLASSROOM_TUID, START_TIME, END_TIME, DAYS, PROFESSOR_TUID) "
            + "VALUES ("
            + "(SELECT TUID FROM COURSE_TABLE WHERE COURSE_ID = ?), ?, "
            + "(SELECT TUID FROM CLASSROOM_TABLE WHERE CLASSROOM_DESIGNATION = ?), ?, ?, ?, "
            + "(SELECT TUID FROM PROFESSOR_TABLE WHERE PROFESSOR_NAME = ?))";

    // The object used to get connections to the database.
    private final DatabaseManager database;

    /**
     * The main constructor for the class.
     *
     * @param database The object used to get connections to the database
     */
    public ScheduleRepository(DatabaseManager database)
    {
        this.database = database;
    }

    /**
     * Loads the whole course catalog. I used a LinkedHashMap so the courses can be looked up by
     * course ID but still stay in the sorted order from the database.
     *
     * @return Every course, looked up by course ID
     * @throws SQLException If the courses can't be read
     */
    public Map<String, Course> loadCourses() throws SQLException
    {
        Map<String, Course> courses = new LinkedHashMap<>();

        // Opening the connection, creating the statement and running the query. All three get closed
        // automatically when the try ends.
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(SELECT_COURSES))
        {
            // Going through every row and turning it into a Course.
            while (results.next())
            {
                Course course = new Course(results.getString("COURSE_ID"),
                        results.getString("COURSE_TITLE"),
                        results.getInt("CREDIT_HOURS"));
                courses.put(course.getCourseId(), course);
            }
        }
        return courses;
    }

    /**
     * Loads every classroom.
     *
     * @return Every classroom in A, B, C order
     * @throws SQLException If the classrooms can't be read
     */
    public List<Classroom> loadClassrooms() throws SQLException
    {
        List<Classroom> classrooms = new ArrayList<>();

        // Same as loadCourses, the connection is only open long enough to read the rows.
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(SELECT_CLASSROOMS))
        {
            while (results.next())
            {
                classrooms.add(new Classroom(results.getString("CLASSROOM_DESIGNATION"), results.getInt("CAPACITY")));
            }
        }
        return classrooms;
    }

    /**
     * Loads every professor's name.
     *
     * @return Every professor's name in alphabetical order
     * @throws SQLException If the professors can't be read
     */
    public List<String> loadProfessorNames() throws SQLException
    {
        List<String> professors = new ArrayList<>();

        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(SELECT_PROFESSORS))
        {
            while (results.next())
            {
                professors.add(results.getString("PROFESSOR_NAME"));
            }
        }
        return professors;
    }

    /**
     * Loads every section in the SCHEDULE_TABLE, both scheduled and unscheduled.
     *
     * @return Every section sorted by course ID and section number
     * @throws SQLException If the sections can't be read
     */
    public List<Section> loadSections() throws SQLException
    {
        List<Section> sections = new ArrayList<>();

        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(SELECT_SECTIONS))
        {
            // Calling a helper method to turn each row into a Section.
            while (results.next())
            {
                sections.add(readSection(results));
            }
        }
        return sections;
    }

    /**
     * Saves one section to the SCHEDULE_TABLE. Each section gets its own connection that opens,
     * saves the row and closes right away.
     *
     * @param section The section being saved
     * @throws SQLException If the section can't be saved
     */
    public void saveSection(Section section) throws SQLException
    {
        // Using a PreparedStatement with parameters to prevent sql injection.
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SECTION))
        {
            // Setting the placeholders that every section has.
            statement.setString(1, section.getCourseId());
            statement.setInt(2, section.getSectionNumber());
            statement.setString(7, section.getProfessorName());

            // If the section was scheduled, filling in where and when.
            if (section.isScheduled())
            {
                TimeSlot slot = section.getTimeSlot();
                statement.setString(3, section.getClassroom());
                statement.setString(4, ClockTime.toStorage(slot.getStartMinutes()));
                statement.setString(5, ClockTime.toStorage(slot.getEndMinutes()));
                statement.setString(6, slot.getDays());
            }
            // If it couldn't be scheduled, the classroom, times and days are all left null.
            else
            {
                statement.setNull(3, Types.CHAR);
                statement.setNull(4, Types.CHAR);
                statement.setNull(5, Types.CHAR);
                statement.setNull(6, Types.VARCHAR);
            }

            // Running the command.
            statement.executeUpdate();
        }
    }

    /**
     * A helper method that turns the current row of the results into a Section.
     *
     * @param results The results, already moved to the row being read
     * @return The section from that row
     * @throws SQLException If the row can't be read
     */
    private Section readSection(ResultSet results) throws SQLException
    {
        // Reading the data that every section has.
        String courseId = results.getString("COURSE_ID");
        int sectionNumber = results.getInt("SECTION");
        String professorName = results.getString("PROFESSOR_NAME");
        String classroom = results.getString("CLASSROOM_DESIGNATION");

        // No classroom means the section was never scheduled, so there is no time to read.
        if (classroom == null)
        {
            return Section.unscheduled(courseId, sectionNumber, professorName);
        }

        // Converting the stored days and times back into a TimeSlot.
        TimeSlot slot = new TimeSlot(results.getString("DAYS"),
                ClockTime.fromStorage(results.getString("START_TIME")),
                ClockTime.fromStorage(results.getString("END_TIME")));
        return new Section(courseId, sectionNumber, professorName, classroom, slot);
    }
}
