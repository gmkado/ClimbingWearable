package com.example.grant.wearableclimbtracker;

import android.app.Application;
import android.util.Log;

import com.jakewharton.threetenabp.AndroidThreeTen;

import io.realm.Realm;
import io.realm.log.LogLevel;
import io.realm.log.RealmLog;
import wearprefs.WearPrefs;

/**
 * Created by Grant on 8/2/2016.
 */
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);
        /*Stetho.initialize(
                Stetho.newInitializerBuilder(this)
                        .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                        .enableWebKitInspector(RealmInspectorModulesProvider.builder(this).build())
                        .build());*/

        AndroidThreeTen.init(this);
        WearPrefs.init(this);
        RealmLog.setLevel(LogLevel.TRACE);
    }
}
