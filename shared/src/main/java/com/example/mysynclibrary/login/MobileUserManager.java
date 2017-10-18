package com.example.mysynclibrary.login;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.realm.ClimbingModule;

import io.realm.Realm;
import io.realm.SyncConfiguration;
import io.realm.SyncUser;

import static com.example.mysynclibrary.Shared.REALM_URL;

/**
 * Created by Grant on 10/17/2017.
 */

public class MobileUserManager extends UserManager {
    public static void storeCredentialPrefs(Context context, AUTH_MODE m, String str1, String str2, boolean b) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(Shared.KEY_AUTH_MODE, m.ordinal());

        mode = m;
        switch (mode) {
            case PASSWORD:
                email = str1;
                password = str2;
                createUser = b;
                editor.putString(Shared.KEY_AUTH_EMAIL, email);
                editor.putString(Shared.KEY_AUTH_PASSWORD, password);
                // don't need createUser because user should be created in mobile
                break;
            case FACEBOOK:
                token = str1;
                editor.putString(Shared.KEY_AUTH_TOKEN, token);
                break;
            case GOOGLE:
                token = str1;
                editor.putString(Shared.KEY_AUTH_TOKEN, token);
                break;
            default:
                throw new IllegalArgumentException("invalid auth mode");
        }

        editor.apply();
    }

    public static void logoutActiveUser(Context context) {
        // notify wearable
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(Shared.KEY_SYNC_USER, false);
        editor.apply();

        UserManager.logoutActiveUser();
    }

    // Configure Realm for the current active user
    public static void setActiveUser(Context context, SyncUser user) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(Shared.KEY_SYNC_USER, true);
        editor.apply();
        UserManager.setActiveUser(user);
    }
}
