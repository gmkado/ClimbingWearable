package com.example.grant.wearableclimbtracker;

import android.app.Application;

import com.example.mysynclibrary.realm.ClimbingModule;
import com.facebook.stetho.Stetho;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.uphyca.stetho_realm.RealmInspectorModulesProvider;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by Grant on 8/2/2016.
 */
public class MyApplication extends Application {

    public static final String ADDED_REALM = "added.realm";

    @Override
    public void onCreate() {
        super.onCreate();

        RealmConfiguration addedConfig =  new RealmConfiguration.Builder(this)
                .deleteRealmIfMigrationNeeded()
                .name(ADDED_REALM)
                .modules(new ClimbingModule())
                .build();

        Realm.setDefaultConfiguration(addedConfig);

        Stetho.initialize(
                Stetho.newInitializerBuilder(this)
                        .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                        .enableWebKitInspector(RealmInspectorModulesProvider.builder(this).build())
                        .build());
        AndroidThreeTen.init(this);
    }
}
