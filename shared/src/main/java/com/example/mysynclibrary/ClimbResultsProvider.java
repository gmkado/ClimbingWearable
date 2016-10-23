package com.example.mysynclibrary;

/**
 * Created by Grant on 10/23/2016.
 */
public interface ClimbResultsProvider {
    public Shared.ClimbType getType();
    public Shared.DateRange getDateRange();
}
