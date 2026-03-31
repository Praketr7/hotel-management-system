package com.hotelmanagementsystem.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB {
    public static Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:hotel.db");
    }
}