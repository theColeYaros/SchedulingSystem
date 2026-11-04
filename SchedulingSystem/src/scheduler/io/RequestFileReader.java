package scheduler.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import scheduler.model.ClassRequest;
import scheduler.model.ClockTime;
import scheduler.model.TimeSlot;

/**
 * Filename: RequestFileReader.java
 * Part of Project: Assignment 3
 *
 * File Purpose:
 * This file holds the code that reads the input files of classes to be scheduled.
 *
 * Class Purpose:
 * Each line of an input file has five pieces of data separated by tabs: the course ID, the professor,
 * the days, the start time and the end time. This class reads the whole file and turns every line
 * into a ClassRequest. The whole file is read before anything gets scheduled, so if there is a bad
 * line, nothing from that file ends up half scheduled.
 */
public final class RequestFileReader
{
    // How many pieces of data should be on each line.
    private static final int FIELD_COUNT = 5;

    // Some Windows programs like Notepad can put a hidden character at the very start of a file.
    // If it isn't removed, the first course ID won't match anything in the database.
    private static final String BYTE_ORDER_MARK = "﻿";

    /**
     * Private constructor so nobody can make an object of this class. Everything in here is static.
     */
    private RequestFileReader()
    {
    }

    /**
     * Reads every line of the file and turns it into a list of requests in the same order they were
     * in the file, since scheduling is first come first serve.
     *
     * @param file The input file being read
     * @return Every request in the file
     * @throws IOException If the file can't be read
     */
    public static List<ClassRequest> read(Path file) throws IOException
    {
        List<ClassRequest> requests = new ArrayList<>();

        // Opening the file in a try with resources so it gets closed automatically even if
        // something goes wrong.
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8))
        {
            String line;
            int lineNumber = 0;

            // Reading one line at a time until the end of the file.
            while ((line = reader.readLine()) != null)
            {
                // Keeping count of the line for any error messages.
                lineNumber++;

                // Cleaning off the hidden character and any extra spaces.
                line = line.replace(BYTE_ORDER_MARK, "").trim();

                // Blank lines, like the one at the end of most files, just get skipped.
                if (!line.isEmpty())
                {
                    requests.add(parseLine(line, lineNumber));
                }
            }
        }
        return requests;
    }

    /**
     * Splits one line into its pieces and builds a request from them.
     *
     * @param line The line being read
     * @param lineNumber The line number for error messages
     * @return The request built from the line
     */
    private static ClassRequest parseLine(String line, int lineNumber)
    {
        // Splitting the line on the tabs.
        String[] fields = line.split("\t");

        // Making sure the line has the right amount of data before using it.
        if (fields.length != FIELD_COUNT)
        {
            throw new IllegalArgumentException("Line " + lineNumber + " does not contain "
                    + FIELD_COUNT + " tab separated values");
        }

        // A try catch so if a time is bad, the error message can say which line it was on.
        try
        {
            // Building the requested time slot from the days, start time and end time.
            TimeSlot requestedSlot = new TimeSlot(fields[2].trim(), ClockTime.parse(fields[3]), ClockTime.parse(fields[4]));
            return new ClassRequest(lineNumber, fields[0].trim(), fields[1].trim(), requestedSlot);
        }
        catch (IllegalArgumentException e)
        {
            // Throwing the error again with the line number added to the front of the message.
            throw new IllegalArgumentException("Line " + lineNumber + ": " + e.getMessage(), e);
        }
    }
}
