package com.example.campusescape;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import com.example.campusescape.player.PlayerModel;
import com.example.campusescape.player.PlayerMovement;

@RunWith(AndroidJUnit4.class)
public class PlayerMovementInstrumentedTest {

    static class FakePlayerModel extends PlayerModel {
        FakePlayerModel() {
            this.x = 1;
            this.y = 1;
            this.velocityY = 0;
            this.lane = 1;
            this.isJumping = false;
        }
    }


    @Test
    public void testFlappyGravityApplies() {
        FakePlayerModel player = new FakePlayerModel();
        PlayerMovement movement = new PlayerMovement(player, PlayerMovement.Mode.FLAPPY);

        player.velocityY = 0;
        player.y = 100;

        movement.update();

        assertTrue(player.velocityY > 0);
        assertTrue(player.y > 100);
    }

    @Test
    public void testFlapSetsJumpPower() {
        FakePlayerModel player = new FakePlayerModel();
        PlayerMovement movement = new PlayerMovement(player, PlayerMovement.Mode.FLAPPY);

        movement.flap();

        assertEquals(-28, player.velocityY);
    }

    @Test
    public void testCafeMazeMoveAllowed() {
        FakePlayerModel player = new FakePlayerModel();
        PlayerMovement movement = new PlayerMovement(player, PlayerMovement.Mode.CAFE_MAZE);

        int[][] map = {
                {0, 0},
                {0, 0}
        };

        player.x = 0;
        player.y = 0;

        movement.moveGrid(1, 0, map);

        assertEquals(1, player.x);
        assertEquals(0, player.y);
    }

    @Test
    public void testCafeMazeMoveBlockedByWall() {
        FakePlayerModel player = new FakePlayerModel();
        PlayerMovement movement = new PlayerMovement(player, PlayerMovement.Mode.CAFE_MAZE);

        int[][] map = {
                {0, 1},
                {0, 0}
        };

        player.x = 0;
        player.y = 0;

        movement.moveGrid(1, 0, map);

        assertEquals(0, player.x);
        assertEquals(0, player.y);
    }

    @Test
    public void testCafeMazeOutOfBoundsCrash() {
        FakePlayerModel player = new FakePlayerModel();
        PlayerMovement movement = new PlayerMovement(player, PlayerMovement.Mode.CAFE_MAZE);

        int[][] map = {
                {0, 0},
                {0, 0}
        };

        player.x = 1;
        player.y = 1;

        try {
            movement.moveGrid(1, 1, map);
            fail("Expected ArrayIndexOutOfBoundsException or crash risk");
        } catch (Exception e) {
            // expected
        }
    }


    @Test
    public void testRaceJumpStarts() {
        FakePlayerModel player = new FakePlayerModel();
        PlayerMovement movement = new PlayerMovement(player, PlayerMovement.Mode.RACE);

        player.isJumping = false;
        player.lane = 1;

        movement.jump();

        assertTrue(player.isJumping);
        assertEquals(-28, player.velocityY);
    }

    @Test
    public void testRaceMoveLeftRight() {
        FakePlayerModel player = new FakePlayerModel();
        PlayerMovement movement = new PlayerMovement(player, PlayerMovement.Mode.RACE);

        player.lane = 1;

        movement.moveLeft();
        assertEquals(0, player.lane);

        movement.moveRight();
        assertEquals(1, player.lane);
    }

    @Test
    public void testRaceGravityAndLanding() {
        FakePlayerModel player = new FakePlayerModel();
        PlayerMovement movement = new PlayerMovement(player, PlayerMovement.Mode.RACE);

        player.isJumping = true;
        player.velocityY = 10;
        player.y = 50;
        player.lane = 50;

        movement.update();

        assertFalse(player.isJumping && player.y > player.lane);
    }


    @Test
    public void testFlapDoesNothingInOtherModes() {
        FakePlayerModel player = new FakePlayerModel();
        PlayerMovement movement =
                new PlayerMovement(player, PlayerMovement.Mode.RACE);

        int originalVelocity = player.velocityY;

        movement.flap();

        assertEquals(originalVelocity, player.velocityY);
    }

    @Test
    public void testMoveGridIgnoredOutsideMaze() {
        FakePlayerModel player = new FakePlayerModel();
        PlayerMovement movement =
                new PlayerMovement(player, PlayerMovement.Mode.FLAPPY);

        int[][] map = {{0, 0}};

        player.x = 0;
        player.y = 0;

        movement.moveGrid(1, 0, map);

        assertEquals(0, player.x);
    }
}