// Need SQLite Drivers from
// https://github.com/xerial/sqlite-jdbc?tab-readme-ov-file#download
// Put in same folder as this source code file
//
// Compiled with: javac DBTest.java
//
// 09-24-2026 This next line still works:
// Executed with: java -classpath ".;sqlite-jdbc-3.50.3.0.jar" --enable-native-access=ALL-UNNAMED DBTest
//
// Good through 3.51.0.0 -- 3.53.0.0 broke something...
//
// Former Execution: java -classpath ".;sqlite-jdbc-3.50.3.0.jar;slf4j-api-1.7.36.jar" DBTest

import java.sql.*;

public class DBTest 
{

    private static Connection getConnection() throws ClassNotFoundException, SQLException {
        // Database path -- if it's a new database, it will be created in the project folder
        Connection connection = DriverManager.getConnection("jdbc:sqlite:sample.db");
        return connection;
    }

    private static void buildDatabase(boolean DBExists) throws ClassNotFoundException, SQLException {
        Connection con;
        Statement state, state2;
        ResultSet res;
        PreparedStatement prep;

        if (!DBExists) {
            DBExists = true;
            con = getConnection();

            // Check for database table existence and if it's not there, create it and add 2 records
            state = con.createStatement();
            res = state.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='User'");

            if (!res.next()) {
                System.out.println("Building the USER table");
                state2 = con.createStatement();
                state2.executeUpdate("CREATE TABLE User(" +
                        "ID INTEGER," +
                        "FirstName VARCHAR(60)," +
                        "LastName VARCHAR(60)," +
                        "PRIMARY KEY (ID));");

                // Add a couple of records using parameters
                System.out.println("Add record 1 to USER table");
                prep = con.prepareStatement("INSERT INTO User VALUES(?,?,?);");
                prep.setInt(1, 1001);
                prep.setString(2, "Sue");
                prep.setString(3, "Smith");
                prep.execute();

                System.out.println("Add record 2 to USER table");
                prep = con.prepareStatement("INSERT INTO User VALUES(?,?,?);");
                prep.setInt(1, 1002);
                prep.setString(2, "John");
                prep.setString(3, "Jones");
                prep.execute();
            }
        }
    }

    public static ResultSet displayUsers() throws SQLException, ClassNotFoundException {
        Statement state;
        ResultSet res;
        Connection con = null;

        if (con == null) {
            // Get Database Connection
            con = getConnection();
        }

        state = con.createStatement();
        res = state.executeQuery("SELECT ID, FirstName, LastName FROM User");
        return res;
    }

    public static void main(String[] args) {
        ResultSet rs;

        try {
            buildDatabase(false);

            rs = displayUsers();
            while (rs.next()) {
                System.out.println(rs.getInt("ID") + " " +
                        rs.getString("FirstName") + " " +
                        rs.getString("LastName"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
