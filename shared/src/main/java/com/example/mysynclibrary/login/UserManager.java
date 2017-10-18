/*
 * Copyright 2016 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.mysynclibrary.login;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.mysynclibrary.Shared;
import com.example.mysynclibrary.realm.ClimbingModule;
import com.facebook.login.LoginManager;

import io.realm.ObjectServerError;
import io.realm.Realm;
import io.realm.SyncConfiguration;
import io.realm.SyncCredentials;
import io.realm.SyncSession;
import io.realm.SyncUser;

import static com.example.mysynclibrary.Shared.AUTH_URL;
import static com.example.mysynclibrary.Shared.REALM_URL;

public abstract class UserManager {
    static AUTH_MODE mode = AUTH_MODE.PASSWORD; // default
    static String email;
    static String password;
    static String token;
    static boolean createUser;

    public static void loginAsync(SyncUser.Callback listener) {
        SyncUser.loginAsync(getCredentials(), AUTH_URL, listener);
    }

    public static void logoutActiveUser() {
        switch (mode) {
            case PASSWORD: {
                // Do nothing, handled by the `User.currentUser().logout();`
                break;
            }
            case FACEBOOK: {
                LoginManager.getInstance().logOut();
                break;
            }
            case GOOGLE: {
                // the connection is handled by `enableAutoManage` mode
                break;
            }
        }
        SyncUser.currentUser().logout();
    }

    public static SyncCredentials getCredentials() {
        switch(mode) {
            case PASSWORD:
                return SyncCredentials.usernamePassword(email, password, createUser);
            case FACEBOOK:
                return SyncCredentials.facebook(token);
            case GOOGLE:
                return SyncCredentials.google(token);
            default:
                throw new IllegalArgumentException("invalid auth mode");

        }
    }

    // Configure Realm for the current active user
    public static void setActiveUser(SyncUser user) {
        SyncConfiguration defaultConfig = new SyncConfiguration.Builder(user, REALM_URL)
                .modules(new ClimbingModule()) // this is necessary for library module
                .build();
        Realm.setDefaultConfiguration(defaultConfig);
    }

    // Supported authentication mode
    public enum AUTH_MODE {
        PASSWORD,
        FACEBOOK,
        GOOGLE
    }
}
