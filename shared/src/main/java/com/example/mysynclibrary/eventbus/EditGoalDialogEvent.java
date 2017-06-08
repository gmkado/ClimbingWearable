package com.example.mysynclibrary.eventbus;


/**
 * Created by Grant on 10/22/2016.
 * This event bus is to communicate when fragments want the activity to open an "edit climb" dialog box and when the dialog box is dismissed
 */

public class EditGoalDialogEvent {
    public enum DialogActionType {
        OPEN_REQUEST,
        DISMISSED;
    }

    public DialogActionType type;
    public String goalUUID;

    public EditGoalDialogEvent(DialogActionType type, String uuid){
        this.type = type;
        this.goalUUID = uuid; // null if not OPEN_REQUEST or adding a climb
    }
}
