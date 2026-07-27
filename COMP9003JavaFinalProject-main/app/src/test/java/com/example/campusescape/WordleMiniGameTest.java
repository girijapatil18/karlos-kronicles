package com.example.campusescape;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

import com.example.campusescape.wordle.WordleMiniGame;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class WordleMiniGameTest {

    private WordleMiniGame game;
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
        game = new WordleMiniGame(buildMockContext());
    }

    private void setTargetWord(String word) throws Exception {
        Field f = WordleMiniGame.class.getDeclaredField("targetWord");
        f.setAccessible(true);
        f.set(game, word);
    }

    private int getIntField(String name) throws Exception {
        Field f = WordleMiniGame.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.getInt(game);
    }

    private void invokeSubmitGuess(String guess) throws Exception {
        Field cg = WordleMiniGame.class.getDeclaredField("currentGuess");
        cg.setAccessible(true);
        cg.set(game, guess);

        Method m = WordleMiniGame.class.getDeclaredMethod("submitGuess");
        m.setAccessible(true);
        m.invoke(game);
    }

    @Test
    public void testInitialState() {
        assertFalse(game.isFinished());
        assertFalse(game.isLevelComplete());
    }

    @Test
    public void testCorrectGuessWinsGame() throws Exception {
        setTargetWord("CLASS");
        invokeSubmitGuess("CLASS");
        assertTrue(game.isFinished());
        assertTrue(game.isLevelComplete());
    }

    @Test
    public void testIncorrectGuessAdvancesRow() throws Exception {
        setTargetWord("CLASS");
        invokeSubmitGuess("APPLE");
        assertEquals(1, getIntField("currentRow"));
        assertFalse(game.isFinished());
    }

    @Test
    public void testMaxRowsFinishesGame() throws Exception {
        setTargetWord("CLASS");
        for (int i = 0; i < 6; i++) {
            invokeSubmitGuess("APPLE");
        }
        assertTrue(game.isFinished());
        assertFalse(game.isLevelComplete());
    }

    @Test
    public void testResetRestoresState() throws Exception {
        invokeSubmitGuess("APPLE");
        game.reset();
        assertFalse(game.isFinished());
        assertEquals(0, getIntField("currentRow"));
    }
}
