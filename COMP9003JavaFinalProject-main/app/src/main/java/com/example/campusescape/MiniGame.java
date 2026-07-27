package com.example.campusescape;

import android.graphics.Canvas;

public interface MiniGame {

    void start();

    void update();

    void draw(Canvas canvas);

    boolean isFinished();

    boolean isLevelComplete();

    void reset();

    void setScreenSize(int width, int height);
}