package com.example.mysynclibrary;

import android.graphics.Color;

/**
 * Created by Grant on 5/11/2017.
 */

public class ColorHelper {
    public enum BaseColor {
        RED(Color.HSVToColor(new float[]{4, .90f, .58f}),
                Color.HSVToColor(new float[]{4, .90f, .74f}), // App
                Color.HSVToColor(new float[]{4, .90f, .40f}), // Dark
                Color.HSVToColor(new float[]{4, .50f, .90f}), // Accent
                Color.HSVToColor(new float[]{4, .20f, .90f})), // Soft
        BLUE(Color.HSVToColor(new float[]{207, .90f, .54f}),
                Color.HSVToColor(new float[]{207, .90f, .74f}), // App
                Color.HSVToColor(new float[]{207, .90f, .40f}), // Dark
                Color.HSVToColor(new float[]{207, .50f, .90f}), // Accent
                Color.HSVToColor(new float[]{207, .20f, .90f})), // Soft
        GREEN(Color.HSVToColor(new float[]{122, .60f, .49f}),
                Color.HSVToColor(new float[]{122, .90f, .74f}), // App
                Color.HSVToColor(new float[]{122, .90f, .40f}), // Dark
                Color.HSVToColor(new float[]{122, .50f, .90f}), // Accent
                Color.HSVToColor(new float[]{122, .20f, .90f})); // Soft

        public int color;
        public int App; // 74%
        public int Dark; // 40%
        public int Accent; // 100%; S=50%
        public int Soft;

        BaseColor(int color, int app, int dark, int accent, int soft){
            this.color = color;
            this.App = app;
            this.Dark = dark;
            this.Accent = accent;
            this.Soft = soft;
        }
    }
}
