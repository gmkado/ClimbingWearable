package com.example.mysynclibrary.eventbus;

import java.util.Date;

/**
 * Created by Grant on 10/22/2016.
 */

public class DaySelectedEvent {
    public Date date;


    public DaySelectedEvent(Date day){
        this.date = day;
    }
}
