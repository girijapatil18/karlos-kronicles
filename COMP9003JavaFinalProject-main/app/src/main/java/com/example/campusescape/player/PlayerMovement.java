package com.example.campusescape.player;

import java.util.List;
import android.graphics.Rect;
import java.util.Random;

import com.example.campusescape.player.PlayerModel;
import com.example.campusescape.player.PlayerCharacter;
public class PlayerMovement {

    public enum Mode {
        FLAPPY,
        CAFE_MAZE,
        RACE
    }

    private Mode mode;
    private PlayerModel player;

    // physics constants (shared)
    private final int gravity = 2;
    private final int jumpPower = -28;

    // grid for cafe maze
    private int cellSize = 80;

    // lanes for race
    private int laneWidth;

    public PlayerMovement(PlayerModel player, Mode mode) {
        this.player = player;
        this.mode = mode;
    }

    public void setLaneWidth(int laneWidth) {
        this.laneWidth = laneWidth;
    }

    public void setCellSize(int cellSize) {
        this.cellSize = cellSize;
    }

    // ================= UPDATE =================
    public void update() {

        switch (mode) {

            case FLAPPY:
                player.velocityY += gravity;
                player.y += player.velocityY;
                break;

            case CAFE_MAZE:
                // movement handled externally (grid-based step movement)
                break;

            case RACE:
                // only jump physics
                if (player.isJumping) {
                    player.velocityY += gravity;
                    player.y += player.velocityY;

                    if (player.y >= player.lane) {
                        player.y = player.lane;
                        player.isJumping = false;
                        player.velocityY = 0;
                    }
                }
                break;
        }
    }

    // ================= ACTIONS =================

    public void flap() {
        if (mode == Mode.FLAPPY) {
            player.velocityY = jumpPower;
        }
    }

    public void jump() {
        if (mode == Mode.RACE && !player.isJumping) {
            player.isJumping = true;
            player.velocityY = jumpPower;
        }
    }

    public void moveLeft() {
        if (mode == Mode.RACE) {
            player.lane = Math.max(0, player.lane - 1);
        }
    }

    public void moveRight() {
        if (mode == Mode.RACE) {
            player.lane = Math.min(2, player.lane + 1);
        }
    }

    public void moveGrid(int dx, int dy, int[][] map) {
        if (mode != Mode.CAFE_MAZE) return;

        int nextX = player.x + dx;
        int nextY = player.y + dy;

        if (map[nextY][nextX] != 1) {
            player.x = nextX;
            player.y = nextY;
        }
    }

    public PlayerModel getPlayer() {
        return player;
    }
}
