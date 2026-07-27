package com.example.campusescape;

import static org.junit.Assert.*;

import com.example.campusescape.player.PlayerModel;
import com.example.campusescape.player.PlayerMovement;

import org.junit.Before;
import org.junit.Test;

public class PlayerMovementTest {

    private PlayerModel player;
    private PlayerMovement flappy;
    private PlayerMovement race;
    private PlayerMovement maze;

    @Before
    public void setup() {
        player = new PlayerModel();
        flappy = new PlayerMovement(player, PlayerMovement.Mode.FLAPPY);
        race   = new PlayerMovement(player, PlayerMovement.Mode.RACE);
        maze   = new PlayerMovement(player, PlayerMovement.Mode.CAFE_MAZE);
    }

    @Test
    public void testFlapSetsVelocity() {
        flappy.flap();
        assertEquals(-28, player.velocityY);
    }

    @Test
    public void testFlappyGravityUpdatesPosition() {
        player.velocityY = 0;
        player.y = 100;
        flappy.update();
        assertTrue(player.y > 100);
    }

    @Test
    public void testRaceJumpStartsJump() {
        race.jump();
        assertTrue(player.isJumping);
        assertEquals(-28, player.velocityY);
    }

    @Test
    public void testRaceJumpGravityApplies() {
        player.isJumping = true;
        player.velocityY = -10;
        player.y = 200;
        race.update();
        assertTrue(player.y < 200);
    }

    @Test
    public void testRaceLandingResetsJump() {
        player.isJumping = true;
        player.y = 100;
        player.velocityY = 50;
        race.update();
        assertFalse(player.isJumping);
        assertEquals(0, player.velocityY);
    }

    @Test
    public void testMoveLeftChangesLane() {
        player.lane = 1;
        race.moveLeft();
        assertEquals(0, player.lane);
    }

    @Test
    public void testMoveRightChangesLane() {
        player.lane = 1;
        race.moveRight();
        assertEquals(2, player.lane);
    }

    @Test
    public void testLaneBoundsLeft() {
        player.lane = 0;
        race.moveLeft();
        assertEquals(0, player.lane);
    }

    @Test
    public void testLaneBoundsRight() {
        player.lane = 2;
        race.moveRight();
        assertEquals(2, player.lane);
    }

    @Test
    public void testMazeMovementIntoFreeCell() {
        int[][] map = {{0, 0}, {0, 0}};
        player.x = 0;
        player.y = 0;
        maze.moveGrid(1, 0, map);
        assertEquals(1, player.x);
        assertEquals(0, player.y);
    }

    @Test
    public void testMazeMovementBlockedByWall() {
        int[][] map = {{0, 1}, {0, 0}};
        player.x = 0;
        player.y = 0;
        maze.moveGrid(1, 0, map);
        assertEquals(0, player.x);
    }

    @Test
    public void testMazeMovementOutOfModeDoesNothing() {
        PlayerMovement flappyMode = new PlayerMovement(player, PlayerMovement.Mode.FLAPPY);
        int[][] map = {{0, 0}, {0, 0}};
        player.x = 0;
        player.y = 0;
        flappyMode.moveGrid(1, 0, map);
        assertEquals(0, player.x);
    }

    @Test
    public void testGetPlayerReturnsSameInstance() {
        assertEquals(player, flappy.getPlayer());
    }
}