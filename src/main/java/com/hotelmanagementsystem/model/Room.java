package com.hotelmanagementsystem.model;

public class Room {
    private int roomNumber;
    private String roomType;
    private double pricePerDay;
    private boolean available;
    private String guestName;
    private String guestContact;

    public int getRoomNumber()         { return roomNumber; }
    public void setRoomNumber(int v)   { this.roomNumber = v; }

    public String getRoomType()        { return roomType; }
    public void setRoomType(String v)  { this.roomType = v; }

    public double getPricePerDay()          { return pricePerDay; }
    public void setPricePerDay(double v)    { this.pricePerDay = v; }

    public boolean isAvailable()           { return available; }
    public void setAvailable(boolean v)    { this.available = v; }

    public String getGuestName()           { return guestName != null ? guestName : "—"; }
    public void setGuestName(String v)     { this.guestName = v; }

    public String getGuestContact()        { return guestContact != null ? guestContact : "—"; }
    public void setGuestContact(String v)  { this.guestContact = v; }
}