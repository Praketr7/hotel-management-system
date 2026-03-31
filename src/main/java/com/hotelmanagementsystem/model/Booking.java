package com.hotelmanagementsystem.model;

public class Booking {
    private int bookingId;
    private int customerId;
    private int roomNumber;
    private int days;
    private double totalBill;
    private String status;

    public Booking(int bookingId, int customerId, int roomNumber,
                   int days, double totalBill, String status) {
        this.bookingId = bookingId;
        this.customerId = customerId;
        this.roomNumber = roomNumber;
        this.days = days;
        this.totalBill = totalBill;
        this.status = status;
    }
}