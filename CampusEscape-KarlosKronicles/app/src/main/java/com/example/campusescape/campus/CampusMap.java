package com.example.campusescape.campus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.example.campusescape.player.PlayerCharacter;

import java.util.ArrayList;
import java.util.List;

public class CampusMap {

    private int screenWidth;
    private int screenHeight;

    // player
    private int playerX = 200;
    private int playerY = 1400;

    private int playerSpeed = 20;

    private PlayerCharacter player;

    // locations
    private List<CampusLocation> locations;

    public CampusMap(Context context) {

        player = new PlayerCharacter(context);

        locations = new ArrayList<>();
    }

    // ================= SCREEN =================

    public void setScreenSize(int width, int height) {

        screenWidth = width;
        screenHeight = height;

        createLocations();
    }

    // ================= LOCATIONS =================

    private void createLocations() {

        locations.clear();

        // LEVEL 1 — Cafe
        locations.add(
                new CampusLocation(
                        "Cafe",
                        new Rect(100, 200, 350, 450),
                        1,
                        true
                )
        );

        // LEVEL 2 — Building
        locations.add(
                new CampusLocation(
                        "Building",
                        new Rect(400, 500, 700, 800),
                        2,
                        true
                )
        );

        // LEVEL 3 — Classroom
        locations.add(
                new CampusLocation(
                        "Classroom",
                        new Rect(750, 250, 1000, 550),
                        3,
                        true
                )
        );
    }

    // ================= DRAW =================

    public void draw(Canvas canvas) {

        Paint paint = new Paint();

        // background
        canvas.drawColor(Color.rgb(30, 45, 60));

        // roads
        paint.setColor(Color.DKGRAY);

        canvas.drawRect(250, 0, 350, screenHeight, paint);
        canvas.drawRect(0, 900, screenWidth, 1000, paint);

        // locations
        paint.setTextSize(45);
        paint.setTextAlign(Paint.Align.CENTER);

        for (CampusLocation location : locations) {

            if (location.isCompleted()) {
                paint.setColor(Color.GREEN);
            }
            else if (location.isUnlocked()) {
                paint.setColor(Color.YELLOW);
            }
            else {
                paint.setColor(Color.GRAY);
            }

            Rect r = location.getArea();

            canvas.drawRect(r, paint);

            paint.setColor(Color.BLACK);

            canvas.drawText(
                    location.getName(),
                    r.centerX(),
                    r.centerY(),
                    paint
            );
        }
        // player
        player.draw(canvas, playerX, playerY, 100);
    }

    // ================= MOVEMENT =================

    public void moveUp() {
        playerY -= playerSpeed;
    }

    public void moveDown() {
        playerY += playerSpeed;
    }

    public void moveLeft() {
        playerX -= playerSpeed;
    }

    public void moveRight() {
        playerX += playerSpeed;
    }

    // ================= TOUCH =================

    public int handleTouch(float x, float y) {

        Rect playerRect = new Rect(
                playerX - 40,
                playerY - 40,
                playerX + 40,
                playerY + 40
        );

        // check if tapping a location first
        for (CampusLocation location : locations) {

            if (location.getArea().contains((int)x, (int)y)) {

                return location.getLevelNumber();
            }
        }

        // otherwise movement controls
        if (x < screenWidth / 3f) {
            moveLeft();
        }
        else if (x > screenWidth * 2f / 3f) {
            moveRight();
        }
        else if (y < screenHeight / 2f) {
            moveUp();
        }
        else {
            moveDown();
        }

        constrainPlayer();
        return -1;
    }


    public int getSelectedLevel(float x, float y) {

        for (CampusLocation location : locations) {

            if (location.getArea().contains((int)x, (int)y)) {

                return location.getLevelNumber();
            }
        }

        return 0;
    }

    // ================= BOUNDS =================

    private void constrainPlayer() {

        playerX = Math.max(50, Math.min(screenWidth - 50, playerX));

        playerY = Math.max(50, Math.min(screenHeight - 50, playerY));
    }

    // ================= LEVEL CHECK =================

    public int checkLevelCollision() {

        Rect playerRect = new Rect(
                playerX - 40,
                playerY - 40,
                playerX + 40,
                playerY + 40
        );

        for (CampusLocation location : locations) {

            if (Rect.intersects(playerRect, location.getArea())) {

                return location.getLevelNumber();
            }
        }

        return -1;
    }

    // ================= LEVEL PROGRESSION =================

    public void completeLevel(int level) {

        for (CampusLocation location : locations) {

            if (location.getLevelNumber() == level) {
                location.setCompleted(true);
            }

            if (location.getLevelNumber() == level + 1) {

                // only unlock if previous is completed
                if (location.getLevelNumber() == level + 1) {
                    location.setUnlocked(true);
                }
            }
        }
    }

    public void update() {

        // future campus animations can go here

    }

    public void unlockLevel(int level) {

        for (CampusLocation location : locations) {

            if (location.getLevelNumber() == level) {
                location.setUnlocked(true);
            }
        }
    }
}
