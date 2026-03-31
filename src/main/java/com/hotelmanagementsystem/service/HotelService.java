package com.hotelmanagementsystem.service;

import com.hotelmanagementsystem.dao.HotelDAO;

public class HotelService {
    private HotelDAO dao = new HotelDAO();

    public void bookIfAvailable(String name, String contact, int room, int days) throws Exception {
        if (!dao.isRoomAvailable(room)) {
            throw new Exception("Room occupied");
        }
        dao.bookRoom(name, contact, room, days);
    }
}