package com.example.mysynclibrary.eventbus;

import com.example.mysynclibrary.ClimbStats;
import com.example.mysynclibrary.Shared;

import org.threeten.bp.temporal.ChronoUnit;

/**
 * Created by Grant on 10/22/2016.
 */

public class ListScrollEvent {
    public ScrollType type;

    public enum ScrollType {
        up, down;
    }

    public ListScrollEvent(ScrollType type){
        this.type = type;
    }
}
