package com.example.campusescape;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

import com.example.campusescape.CafeMaze.CafeMaze.HealthDelegate;
import com.example.campusescape.race.CampusRushMiniGame;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

public class CampusRushMiniGameTest {

    private CampusRushMiniGame game;
    private MockHealth health;

    private static class MockHealth implements HealthDelegate {
        int health = 3;

        @Override public int getHealth()         { return health; }
        @Override public int getMaxHealth()      { return 3; }
        @Override public void onGhostCollision() { health--; }
    }

    private static Context buildMockContext() {
        Context      context   = mock(Context.class);
        Resources    resources = mock(Resources.class);
        AssetManager assets    = mock(AssetManager.class);

        when(context.getResources()).thenReturn(resources);
        when(context.getAssets()).thenReturn(assets);
        when(resources.getIdentifier(anyString(), anyString(), anyString())).thenReturn(0);

        return context;
    }

    @Before
    public void setup() {
        health = new MockHealth();
        game   = new CampusRushMiniGame(buildMockContext(), health);
        game.setScreenSize(1080, 1920);
        game.start();
    }

    @Test
    public void testGameStartsCorrectly() {
        assertFalse(game.isFinished());
        assertFalse(game.isLevelComplete());
    }

    @Test
    public void testResetRestoresState() {
        game.reset();
        assertFalse(game.isFinished());
        assertFalse(game.isLevelComplete());
    }

    @Test
    public void testHealthDecreasesOnCollision() {
        health.onGhostCollision();
        assertEquals(2, health.getHealth());
    }

    @Test
    public void testHealthReachesZeroAfterThreeCollisions() {
        health.onGhostCollision();
        health.onGhostCollision();
        health.onGhostCollision();
        assertEquals(0, health.getHealth());
    }

    @Test
    public void testDelegateReportsZeroHealth() {
        health.health = 1;
        health.onGhostCollision();
        assertEquals(0, health.getHealth());
    }

    @Test
    public void testFallingOffWorldFinishesGame() throws Exception {
        // VIRTUAL_HEIGHT = 640, so playerY > 640 + 80 = 720 triggers the ending.
        Field playerYField = CampusRushMiniGame.class.getDeclaredField("playerY");
        playerYField.setAccessible(true);
        playerYField.setFloat(game, 730f); // well past the 720f threshold

        game.update();

        assertTrue(game.isFinished());
        assertFalse(game.isLevelComplete());
    }

    @Test
    public void testFallingOffWorldIsLoss() throws Exception {
        Field playerYField = CampusRushMiniGame.class.getDeclaredField("playerY");
        playerYField.setAccessible(true);
        playerYField.setFloat(game, 730f);

        game.update();
        assertFalse(game.isLevelComplete());
    }

    @Test
    public void testUpdateDoesNotCrash() {
        for (int i = 0; i < 50; i++) {
            game.update();
        }
        assertFalse(game.isLevelComplete());
    }

    @Test
    public void testTouchJumpDoesNotCrash() {
        game.onTouch(100, 100);
        assertFalse(game.isFinished());
    }

    @Test
    public void testSwipeInputDoesNotCrash() {
        game.onTouch(100, 100, 300, 100);
        game.onTouch(300, 300, 100, 100);
        assertFalse(game.isFinished());
    }
}