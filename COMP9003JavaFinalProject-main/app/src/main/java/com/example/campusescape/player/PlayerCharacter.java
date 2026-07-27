package com.example.campusescape.player;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import com.example.campusescape.BitmapLoader;
import com.example.campusescape.R;

public class PlayerCharacter {

    private Bitmap boy;
    private final Rect destination = new Rect();
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

    public PlayerCharacter(Context context) {
        boy = BitmapLoader.decodeResource(context.getResources(), R.drawable.boy);
    }

    public void draw(Canvas canvas, int centerX, int centerY, int size) {

        if (boy != null) {
            int halfSize = size / 2;
            destination.set(
                    centerX - halfSize,
                    centerY - halfSize,
                    centerX + halfSize,
                    centerY + halfSize
            );
            canvas.drawBitmap(boy, null, destination, bitmapPaint);
        }
    }
}
