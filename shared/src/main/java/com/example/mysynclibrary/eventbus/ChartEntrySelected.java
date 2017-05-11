package com.example.mysynclibrary.eventbus;

import org.threeten.bp.temporal.ChronoUnit;

import java.util.Date;

/**
 * Created by Grant on 10/22/2016.
 */

public class ChartEntrySelected {
    public Date date;
    public ChronoUnit daterange;


    public ChartEntrySelected(Date day, ChronoUnit daterange){
        this.date = day;
        this.daterange = daterange;
    }
}
