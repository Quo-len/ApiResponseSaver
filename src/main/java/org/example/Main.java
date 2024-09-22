package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.sql.*;

public class Main {
    static Connection c = null;
    static Statement stmt = null;

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        createDB();
        int choice = 0;
        while (choice != 3) {
            System.out.println("1 - new record, 2 - print all, 3 - exit");
            try {
                choice = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number (1, 2, or 3).");
                continue;
            }
            switch (choice) {
                case 1:
                    String address = scanner.nextLine();
                    String response = ApiRequest(address);
                    IpDetails details = DeserializeJson(response);
                    addRecord(details);
                    break;
                case 2:
                    printTable();
                    break;
            }
        }
    }

    public static void createDB() {
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:IpInfo.db");

            stmt = c.createStatement();
            String query = "CREATE TABLE IF NOT EXISTS IpDetails" +
                    "(IP CHAR(15) PRIMARY KEY NOT NULL, " +
                    " Continent TEXT, " +
                    " Country TEXT, " +
                    " Region TEXT, " +
                    " City TEXT, " +
                    " Latitude REAL, " +
                    " Longitude REAL)";
            stmt.executeUpdate(query);
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }

    public static String ApiRequest(String address) throws IOException {
        URL url = new URL("http://ipwho.is/" + address);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "application/json");

        int responseCode = con.getResponseCode();

        StringBuilder response = new StringBuilder();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            System.out.println("API Response: " + response.toString());
        } else {
            System.out.println("API Call Failed. Response Code: " + responseCode);
        }

        return response.toString();
    }

    public static IpDetails DeserializeJson(String detailsJson) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // Ignore unknown properties
        IpDetails obj = mapper.readValue(detailsJson, IpDetails.class);
        return obj;
    }


    public static void addRecord(IpDetails details) {
        if (!details.isSuccess()) {
            System.out.println("Invalid IP address.");
            return;
        }
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:IpInfo.db");
            c.setAutoCommit(false);
            System.out.println("Opened database successfully");

            String checkQuery = String.format("SELECT COUNT(*) FROM IpDetails WHERE IP = '%s';", details.getIp());
            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery(checkQuery);
            rs.next();

            if (rs.getInt(1) == 0) {
                stmt = c.createStatement();
                String query = String.format("INSERT INTO IpDetails (IP, Continent, Country, Region, City, Latitude, Longitude) " +
                                "VALUES ('%s', '%s', '%s', '%s', '%s', %s, %s);",
                        details.getIp(), details.getContinent(), details.getCountry(), details.getRegion(), details.getCity(), details.getLatitude(), details.getLongitude());

                stmt.executeUpdate(query);
            } else {
                System.out.println("Record already exists");
            }

            stmt.close();
            c.commit();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        System.out.println("Record successfully added");
    }

    public static void printTable() {
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:IpInfo.db");
            c.setAutoCommit(false);

            stmt = c.createStatement();
            String query = "SELECT * FROM IpDetails";

            ResultSet rs = stmt.executeQuery(query);

            if (rs.isBeforeFirst()) {
                while (rs.next()) {
                    String ip = rs.getString("IP");
                    String continent = rs.getString("Continent");
                    String country = rs.getString("Country");
                    String region = rs.getString("Region");
                    String city = rs.getString("City");
                    double latitude = rs.getDouble("Latitude");
                    double longitude = rs.getDouble("Longitude");

                    System.out.printf("IP: %s, Continent: %s, Country: %s, Region: %s, City: %s, Latitude: %f, Longitude: %f%n",
                            ip, continent, country, region, city, latitude, longitude);
                }
            } else {
                System.out.println("Table is empty");
            }

            stmt.close();
            c.commit();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }
}