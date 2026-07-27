package com.example.campusescape.player;

public class PlayerModel {

    public int x;
    public int y;

    public int velocityY;

    public int lane = 1; // used in race
    public boolean isJumping = false;

    // ================= DEFAULT =================
    public PlayerModel() {
        reset(0, 0);
    }

    // ================= PARAMETERIZED =================
    public PlayerModel(int startX, int startY) {
        reset(startX, startY);
    }

    // ================= RESET =================
    public void reset(int startX, int startY) {
        x = startX;
        y = startY;
        velocityY = 0;
        lane = 1;
        isJumping = false;
    }
}