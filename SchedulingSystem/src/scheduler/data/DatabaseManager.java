package scheduler.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Filename: DatabaseManager.java
 * Part of Project: Assignment 3
 *
 * File Purpose:
 * This file is where the SQLite database gets created, connected to, reset and deleted.
 *
 * Class Purpose:
 * Since this is a database driven application, this class handles everything about the database
 * itself. It builds the tables from scratch using DDL and fills them with the JIT professors,
 * classrooms and course catalog using DML. Every other class gets its connections from here so the
 * connection details only live in one spot.
 */
public class DatabaseManager
{
    // Commands to remove every table. SCHEDULE_TABLE goes first because it points to the other three
    // tables, and SQLite won't let a table be dropped while rows in another table still point to it.
    private static final String[] DROP_TABLES =
    {
        "DROP TABLE IF EXISTS SCHEDULE_TABLE",
        "DROP TABLE IF EXISTS COURSE_TABLE",
        "DROP TABLE IF EXISTS CLASSROOM_TABLE",
        "DROP TABLE IF EXISTS PROFESSOR_TABLE"
    };

    // The DDL commands that build every table. The CHECK constraints stop bad data like a negative
    // capacity from ever getting into the database. UNIQUE on the SCHEDULE_TABLE makes sure a course
    // can never end up with two sections with the same number.
    private static final String[] CREATE_TABLES =
    {
        "CREATE TABLE PROFESSOR_TABLE ("
            + "TUID INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "PROFESSOR_NAME VARCHAR(30) NOT NULL UNIQUE)",

        "CREATE TABLE CLASSROOM_TABLE ("
            + "TUID INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "CLASSROOM_DESIGNATION CHAR(1) NOT NULL UNIQUE, "
            + "CAPACITY INTEGER NOT NULL CHECK (CAPACITY > 0))",

        "CREATE TABLE COURSE_TABLE ("
            + "TUID INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "COURSE_ID VARCHAR(10) NOT NULL UNIQUE, "
            + "COURSE_TITLE VARCHAR(60) NOT NULL, "
            + "CREDIT_HOURS INTEGER NOT NULL CHECK (CREDIT_HOURS BETWEEN 1 AND 4))",

        // The classroom, times and days are allowed to be null for classes that couldn't be scheduled.
        "CREATE TABLE SCHEDULE_TABLE ("
            + "TUID INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "COURSE_TUID INTEGER NOT NULL REFERENCES COURSE_TABLE (TUID), "
            + "SECTION INTEGER NOT NULL, "
            + "CLASSROOM_TUID INTEGER REFERENCES CLASSROOM_TABLE (TUID), "
            + "START_TIME CHAR(5), "
            + "END_TIME CHAR(5), "
            + "DAYS VARCHAR(2), "
            + "PROFESSOR_TUID INTEGER NOT NULL REFERENCES PROFESSOR_TABLE (TUID), "
            + "UNIQUE (COURSE_TUID, SECTION))"
    };

    // The DML commands that fill the database with its starting data. This is the "initial base
    // state" the program goes back to when the database gets reset.
    private static final String[] BASE_DATA =
    {
        // The six JIT faculty members.
        "INSERT INTO PROFESSOR_TABLE (PROFESSOR_NAME) VALUES ('James')",
        "INSERT INTO PROFESSOR_TABLE (PROFESSOR_NAME) VALUES ('Smith')",
        "INSERT INTO PROFESSOR_TABLE (PROFESSOR_NAME) VALUES ('Jones')",
        "INSERT INTO PROFESSOR_TABLE (PROFESSOR_NAME) VALUES ('Vasquez')",
        "INSERT INTO PROFESSOR_TABLE (PROFESSOR_NAME) VALUES ('Abdul')",
        "INSERT INTO PROFESSOR_TABLE (PROFESSOR_NAME) VALUES ('Thomas')",

        // The three classrooms and how many students each one holds.
        "INSERT INTO CLASSROOM_TABLE (CLASSROOM_DESIGNATION, CAPACITY) VALUES ('A', 30)",
        "INSERT INTO CLASSROOM_TABLE (CLASSROOM_DESIGNATION, CAPACITY) VALUES ('B', 25)",
        "INSERT INTO CLASSROOM_TABLE (CLASSROOM_DESIGNATION, CAPACITY) VALUES ('C', 20)",

        // The full computer science course catalog.
        "INSERT INTO COURSE_TABLE (COURSE_ID, COURSE_TITLE, CREDIT_HOURS) VALUES ('CSC 105', 'Computers and Programming', 4)",
        "INSERT INTO COURSE_TABLE (COURSE_ID, COURSE_TITLE, CREDIT_HOURS) VALUES ('CSC 107', 'Introduction to Code Preparation', 1)",
        "INSERT INTO COURSE_TABLE (COURSE_ID, COURSE_TITLE, CREDIT_HOURS) VALUES ('CSC 116', 'Programming I', 4)",
        "INSERT INTO COURSE_TABLE (COURSE_ID, COURSE_TITLE, CREDIT_HOURS) VALUES ('CSC 216', 'Programming II', 4)",
        "INSERT INTO COURSE_TABLE (COURSE_ID, COURSE_TITLE, CREDIT_HOURS) VALUES ('CSC 227', 'Commenting and Naming Conventions', 2)",
        "INSERT INTO COURSE_TABLE (COURSE_ID, COURSE_TITLE, CREDIT_HOURS) VALUES ('CSC 316', 'Data Structures & Algorithms', 4)",
        "INSERT INTO COURSE_TABLE (COURSE_ID, COURSE_TITLE, CREDIT_HOURS) VALUES ('CSC 416', 'Advanced Algorithm Analysis', 3)",
        "INSERT INTO COURSE_TABLE (COURSE_ID, COURSE_TITLE, CREDIT_HOURS) VALUES ('CSC 211', 'Introductory .NET Development', 3)",
        "INSERT INTO COURSE_TABLE (COURSE_ID, COURSE_TITLE, CREDIT_HOURS) VALUES ('CSC 311', 'Advanced .NET Development', 4)",
        "INSERT INTO COURSE_TABLE (COURSE_ID, COURSE_TITLE, CREDIT_HOURS) VALUES ('CSC 313', 'Real World Application Development', 3)",
        "INSERT INTO COURSE_TABLE (COURSE_ID, COURSE_TITLE, CREDIT_HOURS) VALUES ('CSC 411', 'Data Driven Systems', 3)",
        "INSERT INTO COURSE_TABLE (COURSE_ID, COURSE_TITLE, CREDIT_HOURS) VALUES ('CSC 412', 'Sensor Systems', 3)",
        "INSERT INTO COURSE_TABLE (COURSE_ID, COURSE_TITLE, CREDIT_HOURS) VALUES ('CSC 413', 'Artificial Intelligence Systems', 3)",
        "INSERT INTO COURSE_TABLE (COURSE_ID, COURSE_TITLE, CREDIT_HOURS) VALUES ('CSC 496', 'Software Engineering I', 4)",
        "INSERT INTO COURSE_TABLE (COURSE_ID, COURSE_TITLE, CREDIT_HOURS) VALUES ('CSC 497', 'Software Engineering II', 4)",
        "INSERT INTO COURSE_TABLE (COURSE_ID, COURSE_TITLE, CREDIT_HOURS) VALUES ('CSC 498', 'Software Engineering III', 4)"
    };

    // The location of the database file and the string used to connect to it.
    private final Path databaseFile;
    private final String connectionUrl;

    /**
     * The main constructor for the class.
     *
     * @param databaseFile The path to the SQLite database file
     */
    public DatabaseManager(Path databaseFile)
    {
        this.databaseFile = databaseFile;

        // Assembling the complete string used for connecting to the database. If the file doesn't
        // exist yet, SQLite creates it on the first connection.
        this.connectionUrl = "jdbc:sqlite:" + databaseFile;
    }

    /**
     * @return The full path to the database file so the user knows where to find it
     */
    public Path getDatabaseFile()
    {
        return databaseFile.toAbsolutePath();
    }

    /**
     * Opens a new connection to the database. Whoever calls this is responsible for closing it,
     * which every method in this program does with a try with resources.
     *
     * @return An open connection to the database
     * @throws SQLException If the connection can't be made
     */
    public Connection getConnection() throws SQLException
    {
        // Opening the connection to the database.
        Connection connection = DriverManager.getConnection(connectionUrl);

        // SQLite doesn't check foreign keys unless it is told to on every new connection. Without
        // this, a schedule row could point to a course that doesn't exist.
        try (Statement statement = connection.createStatement())
        {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        catch (SQLException e)
        {
            // If that fails, closing the connection so it isn't left open and passing the error up.
            connection.close();
            throw e;
        }
        return connection;
    }

    /**
     * Checks if the database has already been built. This works like my professor's DBTest by
     * looking in sqlite_master, which is SQLite's own list of every table in the database.
     *
     * @return True if the database file exists and has the tables, false if it needs to be built
     * @throws SQLException If the database can't be read
     */
    public boolean isBuilt() throws SQLException
    {
        // If the file isn't there, there's no reason to even connect.
        if (!Files.exists(databaseFile))
        {
            return false;
        }

        // Asking SQLite if the SCHEDULE_TABLE exists. The connection and statement both get closed
        // automatically when the try ends.
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?"))
        {
            // Setting the placeholder to the table name.
            statement.setString(1, "SCHEDULE_TABLE");

            // True if the count is anything above zero.
            try (ResultSet results = statement.executeQuery())
            {
                return results.next() && results.getInt(1) > 0;
            }
        }
    }

    /**
     * This method builds the database back to its initial base state. It drops every table, builds
     * them again and fills them with the starting data. It is used for both the first run of the
     * program and for when the user asks to reset the database.
     *
     * @throws SQLException If anything goes wrong while building
     */
    public void buildBaseState() throws SQLException
    {
        // Opening one connection for the whole build since it is all one job.
        try (Connection connection = getConnection())
        {
            // Turning off auto commit so everything happens as one transaction. If any step fails,
            // the database gets put back the way it was instead of being left half built.
            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement())
            {
                // Removing the old tables, building new ones and filling them.
                executeAll(statement, DROP_TABLES);
                executeAll(statement, CREATE_TABLES);
                executeAll(statement, BASE_DATA);

                // Everything worked so saving all of the changes at once.
                connection.commit();
            }
            catch (SQLException e)
            {
                // Something failed so undoing every change and passing the error up.
                connection.rollback();
                throw e;
            }
        }
    }

    /**
     * Completely deletes the database file.
     *
     * @throws IOException If the file can't be deleted
     */
    public void deleteDatabase() throws IOException
    {
        Files.deleteIfExists(databaseFile);
    }

    /**
     * A helper method to run a group of SQL commands together. Adding them to a batch sends them
     * all at once instead of one at a time.
     *
     * @param statement The statement used to run the commands
     * @param sqlStatements The SQL commands being run
     * @throws SQLException If any of the commands fail
     */
    private void executeAll(Statement statement, String[] sqlStatements) throws SQLException
    {
        // Adding every command to the batch.
        for (String sql : sqlStatements)
        {
            statement.addBatch(sql);
        }

        // Running them all.
        statement.executeBatch();
    }
}
