package com.example.mysynclibrary.eventbus;


import android.support.annotation.ColorInt;

/**
 * This event triggers when sort or filter preferences for climb list have been changed
 */

public class ClimbColorSelectedEvent {
    public int color;

    public ClimbColorSelectedEvent(@ColorInt int color){
        this.color = color;
    }
}
