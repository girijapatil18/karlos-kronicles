package com.example.campusescape;

import static org.junit.Assert.*;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.campusescape.CafeMaze.CafeMaze.HealthDelegate;
import com.example.campusescape.race.CampusRushMiniGame;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;

@RunWith(AndroidJUnit4.class)
public class CampusRushMiniGameInstrumentedTest {

    private CampusRushMiniGame game;
    private MutableHealth health;

    private static class MutableHealth implements HealthDelegate {
        int health = 3;

        @Override public int getHealth()         { return health; }
        @Override public int getMaxHealth()      { return 3; }
        @Override public void onGhostCollision() { health = Math.max(0, health - 1); }
    }

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        health = new MutableHealth();
        game = new CampusRushMiniGame(context, health);
        game.setScreenSize(1080, 1920);
        game.start();
    }

    private void setFloatField(String name, float value) throws Exception {
        Field f = CampusRushMiniGame.class.getDeclaredField(name);
        f.setAccessible(true);
        f.setFloat(game, value);
    }

    private Canvas makeCanvas() {
        return new Canvas(Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888));
    }


    @Test
    public void testGameStartsNotFinished() {
        assertFalse(game.isFinished());
    }

    @Test
    public void testGameStartsNotLevelComplete() {
        assertFalse(game.isLevelComplete());
    }

    @Test
    public void testTouchJumpSafe() {
        game.onTouch(200, 400);
        assertFalse(game.isFinished());
    }

    @Test
    public void testSwipeRightDoesNotCrash() {
        game.onTouch(100, 400, 400, 400); // swipe right → nudgeRight
        assertFalse(game.isFinished());
    }

    @Test
    public void testSwipeLeftDoesNotCrash() {
        game.onTouch(400, 400, 100, 400); // swipe left → nudgeLeft
        assertFalse(game.isFinished());
    }

    @Test
    public void testSwipeUpDoesNotCrash() {
        game.onTouch(200, 400, 200, 100); // vertical swipe → jump
        assertFalse(game.isFinished());
    }

    @Test
    public void testRapidInputDoesNotCrash() {
        for (int i = 0; i < 30; i++) {
            game.onTouch(100, 100, 200 + i, 300 + i);
        }
        assertFalse(game.isFinished());
    }

    @Test
    public void testTapJumpDoesNotCrash() {
        // Small delta → interpreted as tap/jump rather than swipe
        game.onTouch(200, 400, 205, 402);
        assertFalse(game.isFinished());
    }

    @Test
    public void testManyUpdatesDoNotCrash() {
        for (int i = 0; i < 120; i++) {
            game.update();
        }
        assertNotNull(game);
    }

    @Test
    public void testUpdateAfterResetDoesNotCrash() {
        for (int i = 0; i < 10; i++) game.update();
        game.reset();
        for (int i = 0; i < 10; i++) game.update();
        assertNotNull(game);
    }

    @Test
    public void testScreenResizeStable() {
        game.setScreenSize(1440, 2560);
        game.setScreenSize(720, 1280);
        assertFalse(game.isFinished());
    }

    @Test
    public void testResizeThenUpdateDoesNotCrash() {
        game.setScreenSize(720, 1280);
        game.update();
        game.update();
        assertFalse(game.isLevelComplete());
    }

    @Test
    public void testResetWorksAfterGameplay() {
        game.update();
        game.reset();
        assertFalse(game.isFinished());
        assertFalse(game.isLevelComplete());
    }

    @Test
    public void testDoubleResetDoesNotCrash() {
        game.reset();
        game.reset();
        assertFalse(game.isFinished());
    }

    @Test
    public void testFallingOffWorldFinishesGame() throws Exception {
        setFloatField("playerY", 730f); // past the 720f threshold
        game.update();
        assertTrue(game.isFinished());
    }

    @Test
    public void testFallingOffWorldIsNotLevelComplete() throws Exception {
        setFloatField("playerY", 730f);
        game.update();
        assertFalse(game.isLevelComplete());
    }

    @Test
    public void testResetAfterFallRestoresState() throws Exception {
        setFloatField("playerY", 730f);
        game.update();
        assertTrue(game.isFinished());

        game.reset();

        assertFalse(game.isFinished());
        assertFalse(game.isLevelComplete());
    }

    @Test
    public void testHealthDelegateDecrementsCorrectly() {
        int before = health.getHealth();
        health.onGhostCollision();
        assertEquals(before - 1, health.getHealth());
    }

    @Test
    public void testHealthDelegateClampsAtZero() {
        health.health = 0;
        health.onGhostCollision();
        assertEquals(0, health.getHealth());
    }


    @Test
    public void testDrawDoesNotCrash() {
        game.draw(makeCanvas());
    }

    @Test
    public void testDrawAfterUpdateDoesNotCrash() {
        game.update();
        game.draw(makeCanvas());
    }
}