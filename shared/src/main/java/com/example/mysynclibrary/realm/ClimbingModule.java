package com.example.mysynclibrary.realm;

import io.realm.annotations.RealmModule;

/**
 * Created by Grant on 10/13/2016.
 * This is required for library realm models (i.e. in shared module)
 */
@RealmModule(library = true, allClasses = true)
public class ClimbingModule {
}
