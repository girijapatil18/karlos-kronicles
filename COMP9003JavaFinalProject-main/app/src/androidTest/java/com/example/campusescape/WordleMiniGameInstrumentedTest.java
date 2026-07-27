package com.example.campusescape;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.campusescape.wordle.WordleMiniGame;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WordleMiniGameInstrumentedTest {

    private WordleMiniGame game;
    private Context context;

    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        game = new WordleMiniGame(context);
        game.setScreenSize(1080, 1920);
    }

    @Test
    public void testTouchDoesNotCrash() {
        game.onTouch(100, 100);
        game.onTouch(500, 500);

        assertFalse(game.isFinished());
    }

    @Test
    public void testResetClearsGame() {
        game.reset();

        assertFalse(game.isFinished());
        assertFalse(game.isLevelComplete());
    }

    @Test
    public void testScreenResizeUpdatesLayout() {
        game.setScreenSize(1440, 2560);

        assertFalse(game.isFinished());
    }

    @Test
    public void testMultipleTouchesDoNotCrash() {
        for (int i = 0; i < 50; i++) {
            game.onTouch(i * 10, i * 15);
        }

        assertNotNull(game);
    }

    @Test
    public void testGameStateRemainsStable() {
        game.onTouch(0, 0);
        game.onTouch(100, 200);
        game.onTouch(300, 400);

        assertFalse(game.isLevelComplete());
    }
}
