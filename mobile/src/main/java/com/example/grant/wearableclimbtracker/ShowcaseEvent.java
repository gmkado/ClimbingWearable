package com.example.grant.wearableclimbtracker;

import com.github.amlcurran.showcaseview.ShowcaseView;

/**
 * Created by Grant on 5/22/2017.
 */

class ShowcaseEvent {
    public final ShowcaseEventType type;
    public final ShowcaseView view;

    public enum ShowcaseEventType{
        goals
    }
    public ShowcaseEvent(ShowcaseView view, ShowcaseEventType type) {
        this.type = type;
        this.view = view;
    }
}
