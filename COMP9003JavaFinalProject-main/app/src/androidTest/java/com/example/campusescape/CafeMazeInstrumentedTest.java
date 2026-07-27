package com.example.campusescape;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.campusescape.CafeMaze.CafeMaze;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CafeMazeInstrumentedTest {

    private CafeMaze game;

    private static class DummyHealth implements CafeMaze.HealthDelegate {
        @Override public int getHealth() { return 3; }
        @Override public int getMaxHealth() { return 3; }
        @Override public void onGhostCollision() {}
    }

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        game = new CafeMaze(context, new DummyHealth());
        game.setScreenSize(1080, 1920);
        game.start();
    }

    @Test
    public void testTouchStartDoesNotCrash() {
        game.onTouchStart(100, 100);
        assertFalse(game.isFinished());
    }

    @Test
    public void testTouchMoveDoesNotCrash() {
        game.onTouchStart(100, 100);
        game.onTouchMove(300, 300);
        assertFalse(game.isFinished());
    }

    @Test
    public void testTouchEndDoesNotCrash() {
        game.onTouchStart(100, 100);
        game.onTouchEnd(200, 200);

        assertFalse(game.isFinished());
    }

    @Test
    public void testMultipleSwipesDoNotCrash() {
        for (int i = 0; i < 30; i++) {
            game.onTouchStart(i, i);
            game.onTouchEnd(i + 50, i + 80);
        }

        assertNotNull(game);
    }

    @Test
    public void testUpdateLoopDoesNotCrash() {
        for (int i = 0; i < 100; i++) {
            game.update();
        }

        assertFalse(game.isFinished());
    }

    @Test
    public void testScreenResizeSafe() {
        game.setScreenSize(1440, 2560);
        assertFalse(game.isFinished());
    }
}