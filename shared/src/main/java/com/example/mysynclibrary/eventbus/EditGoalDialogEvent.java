package com.example.mysynclibrary.eventbus;


/**
 * Created by Grant on 10/22/2016.
 * This event bus is to communicate when fragments want the activity to open an "edit climb" dialog box and when the dialog box is dismissed
 */

public class EditGoalDialogEvent {
    public String goalUUID;

    public EditGoalDialogEvent(String uuid){
        this.goalUUID = uuid; // null if not OPEN_REQUEST or adding a climb
    }
}
