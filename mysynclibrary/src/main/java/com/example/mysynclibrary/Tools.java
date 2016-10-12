package com.example.mysynclibrary;

import com.google.android.gms.wearable.Asset;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
/**
 * Created by Grant on 9/28/2016.
 */


public class Tools {
    public static final String REALM_SYNC_PATH = "/sync-data"; // changes here need to be changed in mobile manifest
    public static final String COUNT_KEY = "com.example.key.count";

    public static Asset assetFromFile(String path) {
        try {
            return Asset.createFromBytes(readFile(path));
        } catch (IOException e) {
            return null;
        }
    }

    public static Asset assetFromFile(File file) {
        try {
            return Asset.createFromBytes(readFile(file));
        } catch (IOException e) {
            return null;
        }
    }

    public static byte[] readFile(String file) throws IOException {
        return readFile(new File(file));
    }

    public static byte[] readFile(File file) throws IOException {
        // Open file
        RandomAccessFile f = new RandomAccessFile(file, "r");
        try {
            // Get and check length
            long longlength = f.length();
            int length = (int) longlength;
            if (length != longlength)
                throw new IOException("File size >= 2 GB");
            // Read file and return data
            byte[] data = new byte[length];
            f.readFully(data);
            return data;
        } finally {
            f.close();
        }
    }

}
