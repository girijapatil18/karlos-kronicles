package com.example.campusescape.ui;

import android.content.Context;
import android.graphics.*;

import com.example.campusescape.BitmapLoader;
import com.example.campusescape.R;

public class UIManager {

    private static final Paint paint = new Paint();

    private static Bitmap scoreIcon;
    private static Bitmap timerIcon;
    private static boolean initialized = false;

    // =====================================================
    // INIT (CALL ONCE FROM GameView)
    // =====================================================
    public static void init(Context context) {

        if (initialized) return;

        scoreIcon = BitmapLoader.decodeResource(
                context.getResources(),
                R.drawable.star
        );

        timerIcon = BitmapLoader.decodeResource(
                context.getResources(),
                R.drawable.timer
        );

        // scale icons properly
        if (scoreIcon != null) {
            scoreIcon = Bitmap.createScaledBitmap(scoreIcon, 55, 55, true);
        }
        if (timerIcon != null) {
            timerIcon = Bitmap.createScaledBitmap(timerIcon, 55, 55, true);
        }

        initialized = true;
    }

    // =====================================================
    // HUD (Score + Timer)
    // =====================================================
    public static void drawHUD(
            Canvas canvas,
            int score,
            long timeLeft,
            int screenWidth
    ) {

        paint.reset();
        paint.setAntiAlias(true);

        int topPadding = 80;   // avoids status bar clash
        int boxHeight = 130;

        // ================= SCORE PANEL =================
        paint.setColor(Color.argb(180, 0, 0, 0));
        canvas.drawRoundRect(
                25,
                topPadding,
                330,
                topPadding + boxHeight,
                35,
                35,
                paint
        );

        if (scoreIcon != null) {
            canvas.drawBitmap(scoreIcon, 45, topPadding + 35, null);
        }

        paint.setColor(Color.WHITE);
        paint.setTextSize(52);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setShadowLayer(8, 0, 0, Color.BLACK);

        canvas.drawText(
                String.valueOf(score),
                120,
                topPadding + 85,
                paint
        );

        // ================= TIMER PANEL =================
        paint.setShadowLayer(0, 0, 0, Color.TRANSPARENT);

        paint.setColor(Color.argb(180, 0, 0, 0));
        canvas.drawRoundRect(
                screenWidth - 330,
                topPadding,
                screenWidth - 25,
                topPadding + boxHeight,
                35,
                35,
                paint
        );

        if (timerIcon != null) {
            canvas.drawBitmap(timerIcon, screenWidth - 305, topPadding + 35, null);
        }

        paint.setColor(Color.WHITE);
        paint.setTextSize(52);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        canvas.drawText(
                String.valueOf(timeLeft),
                screenWidth - 210,
                topPadding + 85,
                paint
        );
    }

    // =====================================================
    // CENTER MESSAGE
    // =====================================================
    public static void drawCenterMessage(
            Canvas canvas,
            String message,
            int screenWidth,
            int screenHeight,
            int color
    ) {

        paint.reset();
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.DEFAULT_BOLD);

        // shadow
        paint.setColor(Color.BLACK);
        paint.setTextSize(105);
        canvas.drawText(
                message,
                screenWidth / 2f + 4,
                screenHeight / 2f + 4,
                paint
        );

        // main text
        paint.setColor(color);
        paint.setTextSize(100);

        canvas.drawText(
                message,
                screenWidth / 2f,
                screenHeight / 2f,
                paint
        );
    }

    // =====================================================
    // TITLE (FIXED VISIBILITY)
    // =====================================================
    public static void drawTitle(
            Canvas canvas,
            String title,
            int y
    ) {

        paint.reset();
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // glow background shadow
        paint.setColor(Color.BLACK);
        paint.setTextSize(135);

        canvas.drawText(
                title,
                canvas.getWidth() / 2f + 5,
                y + 5,
                paint
        );

        // main title (bright + visible)
        paint.setColor(Color.rgb(120, 220, 255));
        paint.setTextSize(130);

        canvas.drawText(
                title,
                canvas.getWidth() / 2f,
                y,
                paint
        );
    }

    // =====================================================
    // SUBTITLE
    // =====================================================
    public static void drawSubtitle(
            Canvas canvas,
            String text,
            int y
    ) {

        paint.reset();
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);

        paint.setColor(Color.rgb(220, 220, 220));
        paint.setTextSize(50);

        canvas.drawText(
                text,
                canvas.getWidth() / 2f,
                y,
                paint
        );
    }
}
