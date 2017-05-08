package com.example.mysynclibrary.realm;

import java.util.Date;

import io.realm.DynamicRealm;
import io.realm.RealmMigration;
import io.realm.RealmSchema;

/**
 * Created by Grant on 2/25/2017.
 */
public class MyMigration implements RealmMigration {
    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {

        // DynamicRealm exposes an editable schema
        RealmSchema schema = realm.getSchema();

        // Migrate to version 1: Add project fields
        if (oldVersion == 0) {
            schema.get("Climb")
                    .addField("dirty", boolean.class)
                    .addField("delete", boolean.class);
            oldVersion++;
        }

    }

}
