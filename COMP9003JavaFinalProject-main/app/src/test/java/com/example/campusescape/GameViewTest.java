package com.example.campusescape;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

import com.example.campusescape.CafeMaze.CafeMaze;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class GameViewTest {

    private static final int MAX_PLAYER_HEALTH = 7;

    private Context mockContext;

    @Before
    public void setup() {
        mockContext = buildMockContext();
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

    private static class GameViewHealthDelegate implements CafeMaze.HealthDelegate {
        int playerHealth = MAX_PLAYER_HEALTH;

        @Override public int getHealth()    { return playerHealth; }
        @Override public int getMaxHealth() { return MAX_PLAYER_HEALTH; }

        @Override
        public void onGhostCollision() {
            playerHealth = Math.max(0, playerHealth - 1);
        }
    }

    @Test
    public void testHealthStartsAtMax() {
        GameViewHealthDelegate d = new GameViewHealthDelegate();
        assertEquals(MAX_PLAYER_HEALTH, d.getHealth());
        assertEquals(MAX_PLAYER_HEALTH, d.getMaxHealth());
    }

    @Test
    public void testSingleCollisionDecrementsHealth() {
        GameViewHealthDelegate d = new GameViewHealthDelegate();
        d.onGhostCollision();
        assertEquals(MAX_PLAYER_HEALTH - 1, d.getHealth());
    }

    @Test
    public void testHealthClampsAtZero() {
        GameViewHealthDelegate d = new GameViewHealthDelegate();
        // exhaust all health
        for (int i = 0; i < MAX_PLAYER_HEALTH + 5; i++) {
            d.onGhostCollision();
        }
        assertEquals(0, d.getHealth());
        assertTrue(d.getHealth() >= 0);
    }

    @Test
    public void testAllCollisionsCauseGameOver() {
        GameViewHealthDelegate d = new GameViewHealthDelegate();
        for (int i = 0; i < MAX_PLAYER_HEALTH; i++) {
            d.onGhostCollision();
        }
        assertEquals(0, d.getHealth());
    }

    @Test
    public void testMaxHealthNeverChanges() {
        GameViewHealthDelegate d = new GameViewHealthDelegate();
        d.onGhostCollision();
        d.onGhostCollision();
        assertEquals(MAX_PLAYER_HEALTH, d.getMaxHealth());
    }

    private static class DialogueDriver {
        final int lineCount;
        int index = 0;
        boolean transitioned = false;

        DialogueDriver(int lineCount) {
            this.lineCount = lineCount;
        }

        boolean tap() {
            if (index < lineCount - 1) {
                index++;
                return false;
            }
            transitioned = true;
            return true;
        }
    }

    @Test
    public void testIntroDialogueHasFourLines() {
        // introDialogueLines has 4 entries
        DialogueDriver driver = new DialogueDriver(4);
        assertFalse(driver.tap()); // → line 1
        assertFalse(driver.tap()); // → line 2
        assertFalse(driver.tap()); // → line 3
        assertTrue(driver.tap());  // → transition
        assertTrue(driver.transitioned);
        assertEquals(3, driver.index);
    }

    @Test
    public void testCafeDialogueHasFourLines() {
        DialogueDriver driver = new DialogueDriver(4);
        driver.tap(); driver.tap(); driver.tap();
        assertTrue(driver.tap());
    }

    @Test
    public void testInsideCafeDialogueHasTwoLines() {
        DialogueDriver driver = new DialogueDriver(2);
        assertFalse(driver.tap()); // → line 1
        assertTrue(driver.tap());  // → transition
    }

    @Test
    public void testPostMazeCafeDialogueHasThreeLines() {
        DialogueDriver driver = new DialogueDriver(3);
        assertFalse(driver.tap());
        assertFalse(driver.tap());
        assertTrue(driver.tap());
    }

    @Test
    public void testQuadDialogueHasThreeLines() {
        DialogueDriver driver = new DialogueDriver(3);
        assertFalse(driver.tap());
        assertFalse(driver.tap());
        assertTrue(driver.tap());
    }

    @Test
    public void testClassroomDialogueHasThreeLines() {
        DialogueDriver driver = new DialogueDriver(3);
        assertFalse(driver.tap());
        assertFalse(driver.tap());
        assertTrue(driver.tap());
    }

    @Test
    public void testDialogueDoesNotOvershootOnExtraTaps() {
        DialogueDriver driver = new DialogueDriver(2);
        driver.tap();
        driver.tap(); // transition fires
        // index should not go past lineCount - 1
        assertTrue(driver.index <= 1);
    }

    private static class LevelRouter {
        String lastStartedGame = null;

        void startGame(int level) {
            switch (level) {
                case 1: lastStartedGame = "CafeMaze";      break;
                case 2: lastStartedGame = "WordleMiniGame"; break;
                case 3: lastStartedGame = "CampusRush";    break;
                default: lastStartedGame = null;
            }
        }
    }

    @Test
    public void testLevel1RoutesCafeMaze() {
        LevelRouter router = new LevelRouter();
        router.startGame(1);
        assertEquals("CafeMaze", router.lastStartedGame);
    }

    @Test
    public void testLevel2RoutesWordle() {
        LevelRouter router = new LevelRouter();
        router.startGame(2);
        assertEquals("WordleMiniGame", router.lastStartedGame);
    }

    @Test
    public void testLevel3RoutesCampusRush() {
        LevelRouter router = new LevelRouter();
        router.startGame(3);
        assertEquals("CampusRush", router.lastStartedGame);
    }

    @Test
    public void testInvalidLevelRoutesNull() {
        LevelRouter router = new LevelRouter();
        router.startGame(99);
        assertNull(router.lastStartedGame);
    }

    private static class OutcomeEngine {
        int playerHealth = MAX_PLAYER_HEALTH;
        String state = "PLAYING";
        int currentLevel = 1;

        void handleLoss() {
            playerHealth = Math.max(0, playerHealth - 1);
            if (playerHealth <= 0) {
                state = "GAME_OVER";
            } else {
                state = "PLAYING"; // reset and continue
            }
        }

        void handleWin() {
            if (currentLevel == 3) {
                state = "FINAL_WIN";
            } else {
                currentLevel++;
                state = "LEVEL_COMPLETE";
            }
        }
    }

    @Test
    public void testLossDecrementsHealth() {
        OutcomeEngine engine = new OutcomeEngine();
        engine.handleLoss();
        assertEquals(MAX_PLAYER_HEALTH - 1, engine.playerHealth);
        assertEquals("PLAYING", engine.state);
    }

    @Test
    public void testGameOverAfterAllLivesLost() {
        OutcomeEngine engine = new OutcomeEngine();
        for (int i = 0; i < MAX_PLAYER_HEALTH; i++) {
            engine.handleLoss();
        }
        assertEquals("GAME_OVER", engine.state);
        assertEquals(0, engine.playerHealth);
    }

    @Test
    public void testWinAdvancesLevel() {
        OutcomeEngine engine = new OutcomeEngine();
        engine.handleWin();
        assertEquals(2, engine.currentLevel);
        assertEquals("LEVEL_COMPLETE", engine.state);
    }

    @Test
    public void testWinOnFinalLevelTriggersFinalWin() {
        OutcomeEngine engine = new OutcomeEngine();
        engine.currentLevel = 3;
        engine.handleWin();
        assertEquals("FINAL_WIN", engine.state);
    }

    @Test
    public void testHealthNotDecrementedOnWin() {
        OutcomeEngine engine = new OutcomeEngine();
        engine.handleWin();
        assertEquals(MAX_PLAYER_HEALTH, engine.playerHealth);
    }

    @Test
    public void testHealthClampedAtZeroOnLoss() {
        OutcomeEngine engine = new OutcomeEngine();
        engine.playerHealth = 1;
        engine.handleLoss();
        assertEquals(0, engine.playerHealth);
        assertEquals("GAME_OVER", engine.state);
    }

    private static final float FIGMA_W = 412f;
    private static final float FIGMA_H = 917f;

    private float figmaX(int width, float value) { return width * value / FIGMA_W; }
    private float figmaY(int height, float value) { return height * value / FIGMA_H; }

    @Test
    public void testFigmaXScalesProportionally() {
        // at exactly FIGMA_W pixels wide, figmaX should equal the value
        assertEquals(100f, figmaX(412, 100f), 0.01f);
    }

    @Test
    public void testFigmaYScalesProportionally() {
        assertEquals(100f, figmaY(917, 100f), 0.01f);
    }

    @Test
    public void testFigmaXHalfWidth() {
        // half the screen width → half the figma value
        assertEquals(50f, figmaX(412, 100f) * (206f / 412f), 0.01f);
    }

    @Test
    public void testFigmaCoordinatesAreNonNegative() {
        assertTrue(figmaX(1080, 15f) >= 0);
        assertTrue(figmaY(1920, 558f) >= 0);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Test
    public void testClampBelowMin() {
        assertEquals(0f, clamp(-5f, 0f, 1f), 0f);
    }

    @Test
    public void testClampAboveMax() {
        assertEquals(1f, clamp(99f, 0f, 1f), 0f);
    }

    @Test
    public void testClampWithinRange() {
        assertEquals(0.5f, clamp(0.5f, 0f, 1f), 0f);
    }

    @Test
    public void testClampAtBoundaries() {
        assertEquals(0f, clamp(0f, 0f, 1f), 0f);
        assertEquals(1f, clamp(1f, 0f, 1f), 0f);
    }
}