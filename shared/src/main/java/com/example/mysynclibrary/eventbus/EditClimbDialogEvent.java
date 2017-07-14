package com.example.mysynclibrary.eventbus;


import android.app.Dialog;

import com.google.android.gms.wearable.DataEventBuffer;

/**
 * Created by Grant on 10/22/2016.
 * This event bus is to communicate when fragments want the activity to open an "edit climb" dialog box and when the dialog box is dismissed
 */

public class EditClimbDialogEvent {
    public enum EditClimbMode{
        ADD_SEND,
        ADD_PROJECT,
        EDIT
    }

    public String climbUUID;
    public EditClimbMode mode;

    public EditClimbDialogEvent(String uuid, EditClimbMode mode){
        this.mode = mode;
        this.climbUUID = uuid; // null if not OPEN_REQUEST or adding a climb
    }
}
