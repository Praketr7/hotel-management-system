package com.hotelmanagementsystem.dao;

import com.hotelmanagementsystem.model.Room;
import com.hotelmanagementsystem.util.DB;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HotelDAO {

    private static final String LOG_FILE = "hotel_log.txt";
    private static final DateTimeFormatter LOG_FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public HotelDAO() {
        try (Connection conn = DB.connect();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS rooms (" +
                    "room_number INTEGER PRIMARY KEY, room_type TEXT, price_per_day REAL, available INTEGER)");
            stmt.execute("CREATE TABLE IF NOT EXISTS customers (" +
                    "customer_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, contact TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS bookings (" +
                    "booking_id INTEGER PRIMARY KEY AUTOINCREMENT, customer_id INTEGER, " +
                    "room_number INTEGER, days INTEGER, total_bill REAL, status TEXT)");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Logging ───────────────────────────────────────────────

    private void writeLog(String entry) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            pw.println("[" + LocalDateTime.now().format(LOG_FMT) + "]  " + entry);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Room ops ──────────────────────────────────────────────

    public void addRoom(int roomNumber, String type, double price) throws Exception {
        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO rooms VALUES (?, ?, ?, 1)")) {
            ps.setInt(1, roomNumber);
            ps.setString(2, type);
            ps.setDouble(3, price);
            ps.executeUpdate();
        }
        writeLog(String.format("ROOM ADDED     | Room %-4d | Type: %-8s | Rate: ₹%.2f/day",
                roomNumber, type, price));
    }

    public void deleteRoom(int roomNumber) throws Exception {
        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM rooms WHERE room_number=?")) {
            ps.setInt(1, roomNumber);
            ps.executeUpdate();
        }
        writeLog(String.format("ROOM DELETED   | Room %-4d", roomNumber));
    }

    public boolean isRoomAvailable(int roomNumber) throws Exception {
        try (Connection conn = DB.connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT available FROM rooms WHERE room_number=?")) {
            ps.setInt(1, roomNumber);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt("available") == 1;
        }
    }

    public void bookRoom(String name, String contact, int roomNumber, int days) throws Exception {
        try (Connection conn = DB.connect()) {
            conn.setAutoCommit(false);
            try {
                PreparedStatement customerPs = conn.prepareStatement(
                        "INSERT INTO customers(name, contact) VALUES (?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                customerPs.setString(1, name);
                customerPs.setString(2, contact);
                customerPs.executeUpdate();
                ResultSet keys = customerPs.getGeneratedKeys();
                keys.next();
                int customerId = keys.getInt(1);

                PreparedStatement roomPs = conn.prepareStatement(
                        "SELECT price_per_day FROM rooms WHERE room_number=?");
                roomPs.setInt(1, roomNumber);
                ResultSet roomRs = roomPs.executeQuery();
                roomRs.next();
                double price = roomRs.getDouble("price_per_day");
                double total = price * days;

                PreparedStatement bookingPs = conn.prepareStatement(
                        "INSERT INTO bookings(customer_id, room_number, days, total_bill, status) " +
                        "VALUES (?, ?, ?, ?, 'BOOKED')");
                bookingPs.setInt(1, customerId);
                bookingPs.setInt(2, roomNumber);
                bookingPs.setInt(3, days);
                bookingPs.setDouble(4, total);
                bookingPs.executeUpdate();

                PreparedStatement updatePs = conn.prepareStatement(
                        "UPDATE rooms SET available=0 WHERE room_number=?");
                updatePs.setInt(1, roomNumber);
                updatePs.executeUpdate();

                conn.commit();

                writeLog(String.format(
                        "BOOKING        | Room %-4d | Guest: %-20s | Contact: %-15s | Days: %d | Total: ₹%.2f",
                        roomNumber, name, contact, days, total));

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void checkoutWithVerification(int roomNumber, String name, String contact) throws Exception {
        try (Connection conn = DB.connect()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT c.name, c.contact, b.total_bill, b.days FROM bookings b " +
                    "JOIN customers c ON b.customer_id = c.customer_id " +
                    "WHERE b.room_number = ? AND b.status = 'BOOKED' " +
                    "ORDER BY b.booking_id DESC LIMIT 1");
            ps.setInt(1, roomNumber);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) throw new Exception("No active booking found for room " + roomNumber);

            String dbName    = rs.getString("name").trim();
            String dbContact = rs.getString("contact").trim();
            double bill      = rs.getDouble("total_bill");
            int    days      = rs.getInt("days");

            if (!dbName.equalsIgnoreCase(name.trim()) || !dbContact.equals(contact.trim()))
                throw new Exception("Name or contact does not match booking records.");

            PreparedStatement updateRoom = conn.prepareStatement(
                    "UPDATE rooms SET available=1 WHERE room_number=?");
            updateRoom.setInt(1, roomNumber);
            updateRoom.executeUpdate();

            PreparedStatement updateBooking = conn.prepareStatement(
                    "UPDATE bookings SET status='CHECKED_OUT' WHERE room_number=? AND status='BOOKED'");
            updateBooking.setInt(1, roomNumber);
            updateBooking.executeUpdate();

            writeLog(String.format(
                    "CHECKOUT       | Room %-4d | Guest: %-20s | Contact: %-15s | Days: %d | Bill: ₹%.2f",
                    roomNumber, dbName, dbContact, days, bill));
        }
    }

    public BillDetails getBillDetails(int roomNumber, String name, String contact) throws Exception {
        try (Connection conn = DB.connect()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT c.name, c.contact, b.days, b.total_bill, " +
                    "r.room_type, r.price_per_day " +
                    "FROM bookings b " +
                    "JOIN customers c ON b.customer_id = c.customer_id " +
                    "JOIN rooms r ON b.room_number = r.room_number " +
                    "WHERE b.room_number=? AND b.status='BOOKED' " +
                    "ORDER BY b.booking_id DESC LIMIT 1");
            ps.setInt(1, roomNumber);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) throw new Exception("No active booking for room " + roomNumber);

            String dbName    = rs.getString("name").trim();
            String dbContact = rs.getString("contact").trim();

            if (!dbName.equalsIgnoreCase(name.trim()) || !dbContact.equals(contact.trim()))
                throw new Exception("Name or contact does not match records.");

            BillDetails b = new BillDetails();
            b.guestName    = dbName;
            b.guestContact = dbContact;
            b.roomNumber   = roomNumber;
            b.roomType     = rs.getString("room_type");
            b.days         = rs.getInt("days");
            b.pricePerDay  = rs.getDouble("price_per_day");
            b.totalBill    = rs.getDouble("total_bill");
            b.checkInDate  = LocalDate.now().minusDays(b.days)
                               .format(DateTimeFormatter.ofPattern("dd MMM yyyy"));

            writeLog(String.format(
                    "BILL VIEWED    | Room %-4d | Guest: %-20s | Total: ₹%.2f",
                    roomNumber, dbName, b.totalBill));

            return b;
        }
    }

    public Analytics getDetailedAnalytics() throws Exception {
        try (Connection conn = DB.connect()) {
            Analytics a = new Analytics();

            ResultSet r1 = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) AS total FROM bookings");
            a.totalBookings = r1.next() ? r1.getInt("total") : 0;

            ResultSet r2 = conn.createStatement().executeQuery(
                    "SELECT COALESCE(SUM(total_bill),0) AS rev FROM bookings");
            a.totalRevenue = r2.next() ? r2.getDouble("rev") : 0;

            ResultSet r3 = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) AS occ FROM rooms WHERE available=0");
            a.occupiedRooms = r3.next() ? r3.getInt("occ") : 0;

            ResultSet r4 = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) AS avail FROM rooms WHERE available=1");
            a.availableRooms = r4.next() ? r4.getInt("avail") : 0;

            ResultSet r5 = conn.createStatement().executeQuery(
                    "SELECT r.room_type, COUNT(b.booking_id) AS cnt, " +
                    "COALESCE(SUM(b.total_bill),0) AS rev " +
                    "FROM rooms r LEFT JOIN bookings b ON r.room_number=b.room_number " +
                    "GROUP BY r.room_type ORDER BY rev DESC");
            while (r5.next()) {
                a.typeNames.add(r5.getString("room_type"));
                a.typeCounts.add(r5.getInt("cnt"));
                a.typeRevenues.add(r5.getDouble("rev"));
            }

            return a;
        }
    }

    public List<Room> getAllRooms() throws Exception {
        List<Room> list = new ArrayList<>();
        try (Connection conn = DB.connect()) {
            String sql =
                "SELECT r.room_number, r.room_type, r.price_per_day, r.available, " +
                "       c.name AS guest_name, c.contact AS guest_contact " +
                "FROM rooms r " +
                "LEFT JOIN bookings b ON r.room_number = b.room_number AND b.status = 'BOOKED' " +
                "LEFT JOIN customers c ON b.customer_id = c.customer_id " +
                "ORDER BY r.room_number";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next()) {
                Room room = new Room();
                room.setRoomNumber(rs.getInt("room_number"));
                room.setRoomType(rs.getString("room_type"));
                room.setPricePerDay(rs.getDouble("price_per_day"));
                room.setAvailable(rs.getInt("available") == 1);
                room.setGuestName(rs.getString("guest_name"));
                room.setGuestContact(rs.getString("guest_contact"));
                list.add(room);
            }
        }
        return list;
    }

    public String getAnalytics() throws Exception {
        try (Connection conn = DB.connect()) {
            ResultSet bookings = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) AS total FROM bookings");
            ResultSet revenue = conn.createStatement().executeQuery(
                    "SELECT COALESCE(SUM(total_bill), 0) AS revenue FROM bookings");
            ResultSet occupied = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) AS occ FROM rooms WHERE available=0");
            ResultSet total = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) AS total FROM rooms");

            bookings.next(); revenue.next(); occupied.next(); total.next();

            return String.format(
                    "Total Bookings: %d  |  Revenue: ₹%.2f  |  Occupied: %d/%d rooms",
                    bookings.getInt("total"), revenue.getDouble("revenue"),
                    occupied.getInt("occ"), total.getInt("total"));
        }
    }

    // ── Inner classes ─────────────────────────────────────────

    public static class BillDetails {
        public String guestName, guestContact, roomType, checkInDate;
        public int roomNumber, days;
        public double pricePerDay, totalBill;
    }

    public static class Analytics {
        public int totalBookings, occupiedRooms, availableRooms;
        public double totalRevenue;
        public List<String>  typeNames    = new ArrayList<>();
        public List<Integer> typeCounts   = new ArrayList<>();
        public List<Double>  typeRevenues = new ArrayList<>();
    }
}