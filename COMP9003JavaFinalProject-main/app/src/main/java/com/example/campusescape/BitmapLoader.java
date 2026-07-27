package com.example.campusescape;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public final class BitmapLoader {

    private BitmapLoader() {
    }

    public static Bitmap decodeResource(Resources resources, int resourceId) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        return BitmapFactory.decodeResource(resources, resourceId, options);
    }
}
