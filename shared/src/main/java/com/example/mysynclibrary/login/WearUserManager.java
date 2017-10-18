package com.example.mysynclibrary.login;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.example.mysynclibrary.Shared;

/**
 * Created by Grant on 10/17/2017.
 */

public class WearUserManager extends UserManager {
    public static void restoreCredentialPrefs(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        mode = AUTH_MODE.values()[pref.getInt(Shared.KEY_AUTH_MODE, 0)];
        switch(mode) {
            case PASSWORD:
                email = pref.getString(Shared.KEY_AUTH_EMAIL, null);
                password = pref.getString(Shared.KEY_AUTH_PASSWORD, null);
                createUser = false;
                break;
            case FACEBOOK:
                token = pref.getString(Shared.KEY_AUTH_TOKEN, null);
                break;
            case GOOGLE:
                token = pref.getString(Shared.KEY_AUTH_TOKEN, null);
                break;
            default:
                throw new IllegalArgumentException("invalid auth mode");
        }
    }

}
