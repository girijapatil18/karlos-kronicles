package com.example.campusescape.race;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap;

import com.example.campusescape.BitmapLoader;
import com.example.campusescape.MiniGame;
import com.example.campusescape.R;
import com.example.campusescape.player.PlayerCharacter;
import com.example.campusescape.player.PlayerModel;
import com.example.campusescape.player.PlayerMovement;
import com.example.campusescape.player.PlayerMovement.Mode;
import com.example.campusescape.ui.UIManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ObstacleRaceMiniGame implements MiniGame {

    private int screenWidth = 1080;
    private int screenHeight = 1920;

    private Context context;

    // ================= PLAYER =================
    private PlayerModel playerModel;
    private PlayerMovement playerMovement;
    private PlayerCharacter playerView;

    // ================= GAME =================
    private int laneWidth;

    private class Obstacle {
        Rect rect;
        ObstacleType type;

        Obstacle(Rect r, ObstacleType t) {
            rect = r;
            type = t;
        }
    }

    private enum ObstacleType {
        BOOK, PENCIL, LAPTOP, BEAKER
    }

    private List<Obstacle> obstacles;
    private int obstacleSpeed = 16;

    private boolean started = false;
    private boolean finished = false;
    private boolean levelComplete = false;

    private int score = 0;

    private long startTime;
    private final long GAME_DURATION = 30000;

    private Random random = new Random();

    // ================= SMOOTH PLAYER =================
    private float playerPx;
    private float playerPy;
    private float targetPx;
    private float targetPy;
    private boolean moving = false;
    private final float playerSpeed = 20f;

    // ================= OBSTACLE CONTROL =================
    private int frameCounter = 0;
    private final int SPAWN_INTERVAL = 35;
    private final int MAX_OBSTACLES = 8;

    private int safeLane = 1;

    private Bitmap bookImg, pencilImg, laptopImg, beakerImg;

    public ObstacleRaceMiniGame(Context context){
        this.context = context;
        this.playerView = new PlayerCharacter(context);
    }

    @Override
    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        laneWidth = screenWidth / 3;
    }

    @Override
    public void start() {

        started = false;
        finished = false;
        levelComplete = false;
        score = 0;

        startTime = System.currentTimeMillis();

        int startLane = 1;

        playerModel = new PlayerModel();
        playerModel.lane = startLane;

        playerPx = startLane * laneWidth + laneWidth / 2f;
        playerPy = screenHeight - 200;

        playerModel.x = (int) playerPx;
        playerModel.y = (int) playerPy;

        targetPx = playerPx;
        targetPy = playerPy;

        moving = false;

        playerMovement = new PlayerMovement(playerModel, Mode.RACE);
        playerMovement.setLaneWidth(laneWidth);

        obstacles = new ArrayList<>();

        bookImg = BitmapLoader.decodeResource(context.getResources(), R.drawable.book);
        pencilImg = BitmapLoader.decodeResource(context.getResources(), R.drawable.pencil);
        laptopImg = BitmapLoader.decodeResource(context.getResources(), R.drawable.laptop);
        beakerImg = BitmapLoader.decodeResource(context.getResources(), R.drawable.beaker);
    }

    @Override
    public void update() {

        if (finished || !started) return;

        long elapsed = System.currentTimeMillis() - startTime;

        if (elapsed >= GAME_DURATION) {
            finished = true;
            levelComplete = false;
        }

        // ================= PLAYER SMOOTH MOVE =================
        if (moving) {
            float dx = targetPx - playerPx;

            if (Math.abs(dx) < playerSpeed) {
                playerPx = targetPx;
                moving = false;
            } else {
                playerPx += Math.signum(dx) * playerSpeed;
            }

            playerModel.x = (int) playerPx;
        }

        playerModel.y = (int) playerPy;

        // ================= OBSTACLES =================
        frameCounter++;

        if (frameCounter % SPAWN_INTERVAL == 0 && obstacles.size() < MAX_OBSTACLES) {
            generatePatternRow(-200);
        }

        for (int i = 0; i < obstacles.size(); i++) {

            Obstacle o = obstacles.get(i);

            o.rect.top += obstacleSpeed;
            o.rect.bottom += obstacleSpeed;

            if (o.rect.top > screenHeight) {
                obstacles.remove(i);
                i--;
                score++;
            }
        }

        // ================= COLLISION =================
        Rect playerRect = new Rect(
                playerModel.x - 70,
                playerModel.y - 70,
                playerModel.x + 70,
                playerModel.y + 70
        );

        for (Obstacle o : obstacles) {
            if (Rect.intersects(playerRect, o.rect)) {
                finished = true;
                break;
            }
        }

        if (score >= 15) {
            levelComplete = true;
            finished = true;
        }
    }

    // ================= FIXED SWIPE METHOD =================
    public void onSwipe(float sx, float sy, float ex, float ey) {

        started = true;

        float dx = ex - sx;
        float dy = ey - sy;

        if (Math.abs(dx) > Math.abs(dy)) {

            if (dx > 0) moveRight();
            else moveLeft();

        } else {
            jump();
        }
    }

    private void moveToLane(int lane) {
        playerModel.lane = lane;
        targetPx = lane * laneWidth + laneWidth / 2f;
        moving = true;
    }

    public void moveLeft() {
        int lane = Math.max(0, playerModel.lane - 1);
        moveToLane(lane);
    }

    public void moveRight() {
        int lane = Math.min(2, playerModel.lane + 1);
        moveToLane(lane);
    }

    public void jump() {
        // optional (no-op or animation hook)
    }

    private void generatePatternRow(int y) {

        int safeLane = random.nextInt(3);

        for (int lane = 0; lane < 3; lane++) {

            if (lane == safeLane) continue;

            if (random.nextFloat() < 0.65f) {

                int x = lane * laneWidth + laneWidth / 2;

                Rect r = new Rect(
                        x - 60,
                        y,
                        x + 60,
                        y + 120
                );

                ObstacleType type;
                int pick = random.nextInt(4);

                if (pick == 0) type = ObstacleType.BOOK;
                else if (pick == 1) type = ObstacleType.PENCIL;
                else if (pick == 2) type = ObstacleType.LAPTOP;
                else type = ObstacleType.BEAKER;

                obstacles.add(new Obstacle(r, type));
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {

        Paint paint = new Paint();

        canvas.drawColor(Color.rgb(10, 15, 25));

        paint.setColor(Color.rgb(80, 80, 120));
        paint.setStrokeWidth(6);

        canvas.drawLine(laneWidth, 0, laneWidth, screenHeight, paint);
        canvas.drawLine(laneWidth * 2, 0, laneWidth * 2, screenHeight, paint);

        playerView.draw(canvas, playerModel.x, playerModel.y, 90);

        for (Obstacle o : obstacles) {

            Bitmap img = null;

            switch (o.type) {
                case BOOK: img = bookImg; break;
                case PENCIL: img = pencilImg; break;
                case LAPTOP: img = laptopImg; break;
                case BEAKER: img = beakerImg; break;
            }

            if (img != null) {
                canvas.drawBitmap(img, null, o.rect, null);
            }
        }

        if (finished) {
            UIManager.drawCenterMessage(
                    canvas,
                    levelComplete ? "WIN!" : "GAME OVER",
                    screenWidth,
                    screenHeight,
                    Color.WHITE
            );
        }
    }

    @Override public boolean isFinished() { return finished; }
    @Override public boolean isLevelComplete() { return levelComplete; }
    @Override public void reset() { start(); }
    public void stopGame() { finished = true; }
}
