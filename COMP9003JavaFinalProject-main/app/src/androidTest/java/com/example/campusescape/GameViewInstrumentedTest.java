package com.example.campusescape;

import static org.junit.Assert.*;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.campusescape.CafeMaze.CafeMaze;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class GameViewInstrumentedTest {

    private GameView gameView;
    private Context context;

    private static final int MAX_PLAYER_HEALTH = 7;
    private static final int SCREEN_W = 1080;
    private static final int SCREEN_H = 1920;
    private static final int INTRO_DIALOGUE_COUNT        = 4;
    private static final int CAFE_DIALOGUE_COUNT         = 5;
    private static final int INSIDE_CAFE_DIALOGUE_COUNT  = 5;
    private static final int POST_MAZE_DIALOGUE_COUNT    = 4;
    private static final int QUAD_DIALOGUE_COUNT         = 3;
    private static final int QUAD_EXPERIMENT_COUNT       = 1;
    private static final int POST_WORDLE_DIALOGUE_COUNT  = 3;
    private static final int CLASSROOM_DIALOGUE_COUNT    = 2;
    private static final int PRE_RACE_DIALOGUE_COUNT     = 6;
    private static final int FINAL_WAKE_DIALOGUE_COUNT   = 3;

    @Before
    public void setup() throws Exception {
        context = ApplicationProvider.getApplicationContext();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<GameView> ref = new AtomicReference<>();

        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            GameView gv = new GameView(context);
            gv.measure(
                    android.view.View.MeasureSpec.makeMeasureSpec(SCREEN_W, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(SCREEN_H, android.view.View.MeasureSpec.EXACTLY)
            );
            gv.layout(0, 0, SCREEN_W, SCREEN_H);
            // Warmup draw populates startGameBounds = (0,0,1080,1920)
            Canvas warmup = new Canvas(
                    Bitmap.createBitmap(SCREEN_W, SCREEN_H, Bitmap.Config.ARGB_8888));
            gv.draw(warmup);
            ref.set(gv);
            latch.countDown();
        });

        latch.await();
        gameView = ref.get();
        assertNotNull(gameView);
    }

    private Object getField(String name) throws Exception {
        Field f = GameView.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(gameView);
    }

    private void setField(String name, Object value) throws Exception {
        Field f = GameView.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(gameView, value);
    }

    private int getIntField(String name) throws Exception {
        Field f = GameView.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.getInt(gameView);
    }

    private void setIntField(String name, int value) throws Exception {
        Field f = GameView.class.getDeclaredField(name);
        f.setAccessible(true);
        f.setInt(gameView, value);
    }

    private Object invokeWithInt(String name, int arg) throws Exception {
        Method m = GameView.class.getDeclaredMethod(name, int.class);
        m.setAccessible(true);
        return m.invoke(gameView, arg);
    }

    private void invokeHandleTouch(float sx, float sy, float ex, float ey)
            throws Exception {
        Method m = GameView.class.getDeclaredMethod(
                "handleTouch", float.class, float.class, float.class, float.class);
        m.setAccessible(true);
        m.invoke(gameView, sx, sy, ex, ey);
    }

    private void tap(int times) throws Exception {
        for (int i = 0; i < times; i++) {
            invokeHandleTouch(100, 100, 100, 100);
        }
    }

    private String getStateName() throws Exception {
        return getField("currentState").toString();
    }

    private void setState(String name) throws Exception {
        setField("currentState", getEnumValue(name));
    }

    private Object getEnumValue(String name) throws Exception {
        for (Class<?> inner : GameView.class.getDeclaredClasses()) {
            if (inner.isEnum() && inner.getSimpleName().equals("State")) {
                for (Object c : inner.getEnumConstants()) {
                    if (c.toString().equals(name)) return c;
                }
            }
        }
        throw new IllegalArgumentException("No State constant: " + name);
    }

    private Canvas makeCanvas() {
        return new Canvas(
                Bitmap.createBitmap(SCREEN_W, SCREEN_H, Bitmap.Config.ARGB_8888));
    }

    private void runOnMainThread(RunnableWithException r) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            try { r.run(); }
            catch (Exception e) { error.set(e); }
            finally { latch.countDown(); }
        });
        latch.await();
        if (error.get() != null) throw error.get();
    }

    private void runOnMainThread(Runnable r) throws Exception {
        runOnMainThread((RunnableWithException) r::run);
    }

    @FunctionalInterface
    interface RunnableWithException { void run() throws Exception; }

    @Test
    public void testGameViewCreatedSuccessfully() {
        assertNotNull(gameView);
    }

    @Test
    public void testInitialStateIsMenu() throws Exception {
        assertEquals("MENU", getStateName());
    }

    @Test
    public void testInitialHealthIsMax() throws Exception {
        assertEquals(MAX_PLAYER_HEALTH, getIntField("playerHealth"));
    }

    @Test
    public void testInitialLevelIsOne() throws Exception {
        assertEquals(1, getIntField("currentLevel"));
    }

    @Test
    public void testGameFieldIsNullBeforeStart() throws Exception {
        assertNull(getField("game"));
    }

    @Test
    public void testGetHealthReturnsPlayerHealth() throws Exception {
        setIntField("playerHealth", 5);
        assertEquals(5, gameView.getHealth());
    }

    @Test
    public void testGetMaxHealthReturnsConstant() {
        assertEquals(MAX_PLAYER_HEALTH, gameView.getMaxHealth());
    }

    @Test
    public void testOnGhostCollisionDecrementsHealth() throws Exception {
        setIntField("playerHealth", MAX_PLAYER_HEALTH);
        gameView.onGhostCollision();
        assertEquals(MAX_PLAYER_HEALTH - 1, gameView.getHealth());
    }

    @Test
    public void testOnGhostCollisionClampsAtZero() throws Exception {
        setIntField("playerHealth", 1);
        gameView.onGhostCollision(); // hits 0 → triggers showGameOverScreen()
        assertEquals(0, gameView.getHealth());
    }

    @Test
    public void testOnGhostCollisionNeverGoesNegative() throws Exception {
        setIntField("playerHealth", 1);
        gameView.onGhostCollision();
        gameView.onGhostCollision(); // state is GAME_OVER now, health stays 0
        assertTrue(gameView.getHealth() >= 0);
    }

    @Test
    public void testAllCollisionsExhaustHealth() {
        for (int i = 0; i < MAX_PLAYER_HEALTH; i++) {
            gameView.onGhostCollision();
        }
        assertEquals(0, gameView.getHealth());
    }

    @Test
    public void testOnGhostCollisionAtZeroHealthTriggersGameOver() throws Exception {
        setIntField("playerHealth", 1);
        gameView.onGhostCollision();
        assertEquals("GAME_OVER", getStateName());
        assertNull(getField("game"));
    }

    @Test
    public void testTapMenuTransitionsToIntroDialogue() throws Exception {
        invokeHandleTouch(500, 500, 500, 500);
        assertEquals("INTRO_DIALOGUE", getStateName());
    }

    @Test
    public void testHealthResetsToMaxOnMenuTap() throws Exception {
        setIntField("playerHealth", 2);
        invokeHandleTouch(500, 500, 500, 500);
        assertEquals(MAX_PLAYER_HEALTH, getIntField("playerHealth"));
    }

    @Test
    public void testIntroDialogueAdvancesToCafeWalk() throws Exception {
        setState("INTRO_DIALOGUE");
        setIntField("introDialogueIndex", 0);

        tap(INTRO_DIALOGUE_COUNT - 1); // advance through lines 0→3
        assertEquals("INTRO_DIALOGUE", getStateName());

        tap(1); // last line → CAFE_WALK
        assertEquals("CAFE_WALK", getStateName());
    }

    @Test
    public void testCafeIntroAdvancesToInsideCafe() throws Exception {
        setState("CAFE_INTRO");
        setIntField("cafeDialogueIndex", 0);

        tap(CAFE_DIALOGUE_COUNT - 1);
        assertEquals("CAFE_INTRO", getStateName());

        tap(1);
        assertEquals("INSIDE_CAFE", getStateName());
    }

    @Test
    public void testInsideCafeAdvancesToMazeBriefing() throws Exception {
        setState("INSIDE_CAFE");
        setIntField("insideCafeDialogueIndex", 0);

        tap(INSIDE_CAFE_DIALOGUE_COUNT - 1); // advance through lines 0→4
        assertEquals("INSIDE_CAFE", getStateName());

        tap(1); // last line → MAZE_BRIEFING
        assertEquals("MAZE_BRIEFING", getStateName());
    }

    @Test
    public void testPostMazeCafeDialogueAdvancesToQuadWalk() throws Exception {
        setState("POST_MAZE_CAFE_DIALOGUE");
        setIntField("postMazeCafeDialogueIndex", 0);

        tap(POST_MAZE_DIALOGUE_COUNT - 1);
        assertEquals("POST_MAZE_CAFE_DIALOGUE", getStateName());

        tap(1);
        assertEquals("QUAD_WALK", getStateName());
    }

    @Test
    public void testQuadDialogueAdvancesToQuadExperiment() throws Exception {
        setState("QUAD_DIALOGUE");
        setIntField("quadDialogueIndex", 0);

        tap(QUAD_DIALOGUE_COUNT - 1);
        assertEquals("QUAD_DIALOGUE", getStateName());

        tap(1);
        assertEquals("QUAD_EXPERIMENT_DIALOGUE", getStateName());
    }

    @Test
    public void testQuadExperimentAdvancesToWordleBriefing() throws Exception {
        setState("QUAD_EXPERIMENT_DIALOGUE");
        setIntField("quadExperimentDialogueIndex", 0);

        tap(1); // only 1 line → immediately transitions
        assertEquals("WORDLE_BRIEFING", getStateName());
    }

    @Test
    public void testPostWordleQuadDialogueAdvancesToClassroomBriefing() throws Exception {
        setState("POST_WORDLE_QUAD_DIALOGUE");
        setIntField("postWordleQuadDialogueIndex", 0);

        tap(POST_WORDLE_DIALOGUE_COUNT - 1);
        assertEquals("POST_WORDLE_QUAD_DIALOGUE", getStateName());

        tap(1);
        assertEquals("CLASSROOM_BRIEFING", getStateName());
    }

    @Test
    public void testClassroomDialogueAdvancesToPreRace() throws Exception {
        setState("CLASSROOM_DIALOGUE");
        setIntField("classroomDialogueIndex", 0);

        tap(CLASSROOM_DIALOGUE_COUNT - 1);
        assertEquals("CLASSROOM_DIALOGUE", getStateName());

        tap(1);
        assertEquals("PRE_RACE_DIALOGUE", getStateName());
    }

    @Test
    public void testPreRaceDialogueAdvancesToRaceBriefing() throws Exception {
        setState("PRE_RACE_DIALOGUE");
        setIntField("preRaceDialogueIndex", 0);

        tap(PRE_RACE_DIALOGUE_COUNT - 1);
        assertEquals("PRE_RACE_DIALOGUE", getStateName());

        tap(1);
        assertEquals("RACE_BRIEFING", getStateName());
    }

    @Test
    public void testFinalWakeDialogueAdvancesToMenu() throws Exception {
        setState("FINAL_WAKE_DIALOGUE");
        setIntField("finalWakeDialogueIndex", 0);

        tap(FINAL_WAKE_DIALOGUE_COUNT - 1);
        assertEquals("FINAL_WAKE_DIALOGUE", getStateName());

        tap(1);
        assertEquals("MENU", getStateName());
    }

    @Test
    public void testGameOverTapGoesToNightmareReturnHome() throws Exception {
        setState("GAME_OVER");
        invokeHandleTouch(100, 100, 100, 100);
        assertEquals("NIGHTMARE_RETURN_HOME", getStateName());
    }

    @Test
    public void testFinalWinTapReturnsToMenu() throws Exception {
        setState("FINAL_WIN");
        invokeHandleTouch(100, 100, 100, 100);
        assertEquals("MENU", getStateName());
    }

    @Test
    public void testMazeBriefingTapStartsLevel1() throws Exception {
        setState("MAZE_BRIEFING");
        runOnMainThread((RunnableWithException) () -> invokeHandleTouch(100, 100, 100, 100));
        assertEquals("PLAYING", getStateName());
        assertNotNull(getField("game"));
        assertTrue(getField("game") instanceof CafeMaze);
    }

    @Test
    public void testWordleBriefingTapStartsLevel2() throws Exception {
        setState("WORDLE_BRIEFING");
        runOnMainThread((RunnableWithException) () -> invokeHandleTouch(100, 100, 100, 100));
        assertEquals("PLAYING", getStateName());
        // Updated package: com.example.campusescape.wordle (not flappybird)
        assertTrue(getField("game") instanceof com.example.campusescape.wordle.WordleMiniGame);
    }

    @Test
    public void testRaceBriefingTapStartsLevel3() throws Exception {
        setState("RACE_BRIEFING");
        runOnMainThread((RunnableWithException) () -> invokeHandleTouch(100, 100, 100, 100));
        assertEquals("PLAYING", getStateName());
        assertTrue(getField("game") instanceof com.example.campusescape.race.CampusRushMiniGame);
    }

    @Test
    public void testStartGameLevel1CreatesCafeMaze() throws Exception {
        runOnMainThread((RunnableWithException) () -> invokeWithInt("startGame", 1));
        assertTrue(getField("game") instanceof CafeMaze);
        assertEquals("PLAYING", getStateName());
    }

    @Test
    public void testStartGameLevel2CreatesWordle() throws Exception {
        runOnMainThread((RunnableWithException) () -> invokeWithInt("startGame", 2));
        assertTrue(getField("game") instanceof com.example.campusescape.wordle.WordleMiniGame);
        assertEquals("PLAYING", getStateName());
    }

    @Test
    public void testStartGameLevel3CreatesCampusRush() throws Exception {
        runOnMainThread((RunnableWithException) () -> invokeWithInt("startGame", 3));
        assertTrue(getField("game") instanceof com.example.campusescape.race.CampusRushMiniGame);
        assertEquals("PLAYING", getStateName());
    }

    @Test
    public void testResumeStartsLoop() throws Exception {
        runOnMainThread((RunnableWithException) () -> gameView.resume());
        assertTrue((boolean) getField("isPlaying"));
        runOnMainThread((RunnableWithException) () -> gameView.pause());
    }

    @Test
    public void testPauseStopsLoop() throws Exception {
        runOnMainThread((RunnableWithException) () -> gameView.resume());
        runOnMainThread((RunnableWithException) () -> gameView.pause());
        assertFalse((boolean) getField("isPlaying"));
    }

    @Test
    public void testDoubleResumeIsNoop() throws Exception {
        runOnMainThread((RunnableWithException) () -> {
            gameView.resume();
            gameView.resume();
        });
        assertTrue((boolean) getField("isPlaying"));
        runOnMainThread((RunnableWithException) () -> gameView.pause());
    }

    @Test
    public void testPauseWithoutResumeDoesNotCrash() {
        gameView.pause();
    }

    @Test public void testDrawMenuDoesNotCrash() throws Exception {
        setState("MENU");
        runOnMainThread((RunnableWithException) () -> gameView.draw(makeCanvas()));
    }

    @Test
    public void testDrawIntroDialogueDoesNotCrash() throws Exception {
        setState("INTRO_DIALOGUE");
        runOnMainThread((RunnableWithException) () -> gameView.draw(makeCanvas()));
    }

    @Test
    public void testDrawCafeIntroDoesNotCrash() throws Exception {
        setState("CAFE_INTRO");
        runOnMainThread((RunnableWithException) () -> gameView.draw(makeCanvas()));
    }

    @Test
    public void testDrawInsideCafeDoesNotCrash() throws Exception {
        setState("INSIDE_CAFE");
        runOnMainThread((RunnableWithException) () -> gameView.draw(makeCanvas()));
    }

    @Test
    public void testDrawMazeBriefingDoesNotCrash() throws Exception {
        setState("MAZE_BRIEFING");
        runOnMainThread((RunnableWithException) () -> gameView.draw(makeCanvas()));
    }

    @Test
    public void testDrawPostMazeCafeDialogueDoesNotCrash() throws Exception {
        setState("POST_MAZE_CAFE_DIALOGUE");
        runOnMainThread((RunnableWithException) () -> gameView.draw(makeCanvas()));
    }

    @Test
    public void testDrawQuadDialogueDoesNotCrash() throws Exception {
        setState("QUAD_DIALOGUE");
        runOnMainThread((RunnableWithException) () -> gameView.draw(makeCanvas()));
    }

    @Test
    public void testDrawQuadExperimentDialogueDoesNotCrash() throws Exception {
        setState("QUAD_EXPERIMENT_DIALOGUE");
        runOnMainThread((RunnableWithException) () -> gameView.draw(makeCanvas()));
    }

    @Test
    public void testDrawWordleBriefingDoesNotCrash() throws Exception {
        setState("WORDLE_BRIEFING");
        runOnMainThread((RunnableWithException) () -> gameView.draw(makeCanvas()));
    }

    @Test
    public void testDrawWordleUnlockSuccessDoesNotCrash() throws Exception {
        setState("WORDLE_UNLOCK_SUCCESS");
        runOnMainThread((RunnableWithException) () -> gameView.draw(makeCanvas()));
    }

    @Test
    public void testDrawPostWordleQuadDialogueDoesNotCrash() throws Exception {
        setState("POST_WORDLE_QUAD_DIALOGUE");
        runOnMainThread((RunnableWithException) () -> gameView.draw(makeCanvas()));
    }

    @Test
    public void testDrawClassroomDialogueDoesNotCrash() throws Exception {
        setState("CLASSROOM_DIALOGUE");
        runOnMainThread((RunnableWithException) () -> gameView.draw(makeCanvas()));
    }

    @Test
    public void testDrawPreRaceDialogueDoesNotCrash() throws Exception {
        setState("PRE_RACE_DIALOGUE");
        runOnMainThread((RunnableWithException) () -> gameView.draw(makeCanvas()));
    }

    @Test
    public void testDrawRaceBriefingDoesNotCrash() throws Exception {
        setState("RACE_BRIEFING");
        runOnMainThread((RunnableWithException) () -> gameView.draw(makeCanvas()));
    }

    @Test
    public void testDrawGameOverDoesNotCrash() throws Exception {
        setState("GAME_OVER");
        runOnMainThread((RunnableWithException) () -> gameView.draw(makeCanvas()));
    }

    @Test
    public void testDrawNightmareReturnHomeDoesNotCrash() throws Exception {
        setState("NIGHTMARE_RETURN_HOME");
        runOnMainThread((RunnableWithException) () -> gameView.draw(makeCanvas()));
    }

    @Test
    public void testDrawLevelCompleteDoesNotCrash() throws Exception {
        setState("LEVEL_COMPLETE");
        runOnMainThread((RunnableWithException) () -> gameView.draw(makeCanvas()));
    }

    @Test
    public void testDrawFinalCongratulationsDoesNotCrash() throws Exception {
        setState("FINAL_CONGRATULATIONS");
        runOnMainThread((RunnableWithException) () -> gameView.draw(makeCanvas()));
    }

    @Test
    public void testDrawFinalNightmareOvercomeDoesNotCrash() throws Exception {
        setState("FINAL_NIGHTMARE_OVERCOME");
        runOnMainThread((RunnableWithException) () -> gameView.draw(makeCanvas()));
    }

    @Test
    public void testDrawFinalWakeDialogueDoesNotCrash() throws Exception {
        setState("FINAL_WAKE_DIALOGUE");
        runOnMainThread((RunnableWithException) () -> gameView.draw(makeCanvas()));
    }

    @Test
    public void testDrawFinalWinDoesNotCrash() throws Exception {
        setState("FINAL_WIN");
        runOnMainThread((RunnableWithException) () -> gameView.draw(makeCanvas()));
    }


    @Test
    public void testFigmaXAtFigmaWidth() throws Exception {
        Method m = GameView.class.getDeclaredMethod("figmaX", int.class, float.class);
        m.setAccessible(true);
        assertEquals(100f, (float) m.invoke(gameView, 412, 100f), 0.01f);
    }

    @Test
    public void testFigmaYAtFigmaHeight() throws Exception {
        Method m = GameView.class.getDeclaredMethod("figmaY", int.class, float.class);
        m.setAccessible(true);
        assertEquals(100f, (float) m.invoke(gameView, 917, 100f), 0.01f);
    }

    @Test
    public void testFigmaXScalesDown() throws Exception {
        Method m = GameView.class.getDeclaredMethod("figmaX", int.class, float.class);
        m.setAccessible(true);
        float full = (float) m.invoke(gameView, 412, 200f);
        float half = (float) m.invoke(gameView, 206, 200f);
        assertEquals(full / 2f, half, 0.01f);
    }

    @Test
    public void testClampBelowMin() throws Exception {
        Method m = GameView.class.getDeclaredMethod(
                "clamp", float.class, float.class, float.class);
        m.setAccessible(true);
        assertEquals(0f, (float) m.invoke(gameView, -5f, 0f, 1f), 0f);
    }

    @Test
    public void testClampAboveMax() throws Exception {
        Method m = GameView.class.getDeclaredMethod(
                "clamp", float.class, float.class, float.class);
        m.setAccessible(true);
        assertEquals(1f, (float) m.invoke(gameView, 99f, 0f, 1f), 0f);
    }

    @Test
    public void testClampInRange() throws Exception {
        Method m = GameView.class.getDeclaredMethod(
                "clamp", float.class, float.class, float.class);
        m.setAccessible(true);
        assertEquals(0.5f, (float) m.invoke(gameView, 0.5f, 0f, 1f), 0f);
    }
}