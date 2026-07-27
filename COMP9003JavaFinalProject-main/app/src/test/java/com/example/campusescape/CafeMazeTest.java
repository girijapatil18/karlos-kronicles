package com.example.campusescape;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

import com.example.campusescape.CafeMaze.CafeMaze;

import org.junit.Before;
import org.junit.Test;

public class CafeMazeTest {

    private CafeMaze game;
    private TestHealthDelegate health;

    private static class TestHealthDelegate implements CafeMaze.HealthDelegate {
        int health = 3;
        int maxHealth = 3;

        @Override public int getHealth()    { return health; }
        @Override public int getMaxHealth() { return maxHealth; }
        @Override public void onGhostCollision() { health--; }
    }
    private static Context buildMockContext() {
        Context  context  = mock(Context.class);
        Resources resources = mock(Resources.class);
        AssetManager assets = mock(AssetManager.class);

        when(context.getResources()).thenReturn(resources);
        when(context.getAssets()).thenReturn(assets);

        when(resources.getIdentifier(anyString(), anyString(), anyString())).thenReturn(0);

        return context;
    }

    @Before
    public void setup() {
        health = new TestHealthDelegate();
        game   = new CafeMaze(buildMockContext(), health);
        game.setScreenSize(1080, 1920);
        game.start();
    }

    @Test
    public void testGameStartsNotFinished() {
        assertFalse(game.isFinished());
        assertFalse(game.isLevelComplete());
    }

    @Test
    public void testResetKeepsHealthSystem() {
        game.reset();
        assertEquals(3, health.getHealth());
    }

    @Test
    public void testGhostCollisionReducesHealth() {
        health.onGhostCollision();
        assertEquals(2, health.getHealth());
    }

    @Test
    public void testGameOverWhenHealthZero() {
        health.onGhostCollision();
        health.onGhostCollision();
        health.onGhostCollision();

        if (health.getHealth() <= 0) {
            game.stopGame();
        }

        assertTrue(game.isFinished());
    }

    @Test
    public void testStopGameEndsGame() {
        game.stopGame();
        assertTrue(game.isFinished());
    }
}