package com.example.campusescape.CafeMaze;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.example.campusescape.BitmapLoader;
import com.example.campusescape.MiniGame;
import com.example.campusescape.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CafeMaze implements MiniGame {
    // Lets the main game own health so it can persist outside this maze.
    public interface HealthDelegate {
        int getHealth();
        int getMaxHealth();
        void onGhostCollision();
    }

    // Maze legend: X = wall, P = Karlos start, E = exit, b/s/r = ghost starts.
    private static final String[] TILE_MAP = {
            "XXXPXXXXXXXXXXXXXX",
            "X   XXXXXXXXXXXX X",
            "X X            X X",
            "X X XXXX X XXX X X",
            "X   X    X   X   X",
            "XXX X XXXX X XXX X",
            "X   X X    X X   X",
            "X XXX X XXXX X XXX",
            "X     X      X   X",
            "XXX XXXXXXXX XXX X",
            "X       b        X",
            "X XXXXXXXXXXXXX XX",
            "X       X        X",
            "XXXXX X X XXXX XXX",
            "X     X X X      X",
            "X XXXXX X X XXXX X",
            "X X     X X X    X",
            "X X XXXXX X X XX X",
            "X X   s     X X  X",
            "X XXX XXXXXXX X XX",
            "X   X       X X  X",
            "XXX XXXXXXX X XX X",
            "X   X       X    X",
            "X XXX XXXXXXXX XXX",
            "X       X        X",
            "XXXXXXX X XXXXXX X",
            "X       X X    r X",
            "X XXXXXXX X XXXX X",
            "X X       X X    X",
            "X X XXXXXXX X XXXX",
            "X           X   EX",
            "XXXXXXXXXXXXXXXXXX"
    };

    private static final int EMPTY = 0;
    private static final int WALL = 1;
    private static final int GATE = 3;
    private static final int EXIT = 4;
    private static final int FLOOR_COLOR = Color.rgb(56, 35, 36);
    private static final int FLOOR_DETAIL_COLOR = Color.rgb(78, 47, 48);
    private static final int WALL_COLOR = Color.rgb(215, 181, 139);
    private static final int WALL_SHADOW_COLOR = Color.rgb(184, 143, 103);

    private final Context context;
    private final HealthDelegate healthDelegate;
    private final Random random = new Random();

    private Bitmap karlosImage;
    private Bitmap[] ghostImages;
    private Bitmap heartImage;
    private Bitmap exitSignImage;

    private int screenWidth = 1080;
    private int screenHeight = 1920;
    private int rows = TILE_MAP.length;
    private int cols = TILE_MAP[0].length();
    private int tileSize = 56;
    private int tileStepX = 56;
    private int tileStepY = 56;
    private int boardLeft = 0;
    private int boardTop = 0;
    private int safeTop = 0;
    private int hudTop = 0;
    private int hudBottom = 0;

    private int[][] map;
    private final List<Actor> ghosts = new ArrayList<>();
    private Actor player;
    private int exitX = -1;
    private int exitY = -1;

    private boolean started = false;
    private boolean finished = false;
    private boolean levelComplete = false;

    private float touchStartX;
    private float touchStartY;

    private long lastMoveTime = 0;
    private long lastInputMoveTime = 0;
    private long moveInterval = 145;
    private long lastDamageTime = 0;
    private static final long DAMAGE_COOLDOWN_MS = 900;
    private static final long INPUT_MOVE_COOLDOWN_MS = 90;

    // Creates the maze and connects it to the persistent health owner.
    public CafeMaze(Context context, HealthDelegate healthDelegate) {
        this.context = context;
        this.healthDelegate = healthDelegate;
        loadImages();
    }

    // Loads the pixel art used for Karlos, ghosts, and the health HUD.
    private void loadImages() {
        karlosImage = BitmapLoader.decodeResource(context.getResources(), R.drawable.maze_karlos_front);
        ghostImages = new Bitmap[] {
                BitmapLoader.decodeResource(context.getResources(), R.drawable.haunted_ghost1),
                BitmapLoader.decodeResource(context.getResources(), R.drawable.haunted_ghost2),
                BitmapLoader.decodeResource(context.getResources(), R.drawable.haunted_ghost3)
        };
        heartImage = BitmapLoader.decodeResource(context.getResources(), R.drawable.heart);
        exitSignImage = BitmapLoader.decodeResource(context.getResources(), R.drawable.cafe_maze_exit_sign);
    }

    // Receives the current screen size from GameView and recalculates the board layout.
    @Override
    public void setScreenSize(int width, int height) {
        screenWidth = width;
        screenHeight = height;
        updateBoardLayout();
    }

    // Fits the tile map between the top HUD and the bottom of the screen.
    private void updateBoardLayout() {
        safeTop = Math.max(getStatusBarHeight() + 22, screenHeight / 18);

        int horizontalPadding = Math.max(10, screenWidth / 70);
        int bottomPadding = Math.max(22, screenHeight / 95);
        int hudHeight = Math.max(118, screenHeight / 14);

        hudTop = safeTop;
        hudBottom = hudTop + hudHeight;

        int availableWidth = screenWidth - horizontalPadding * 2;
        int availableHeight = screenHeight - hudBottom - bottomPadding;
        int maxTileWidth = availableWidth / cols;
        int maxTileHeight = availableHeight / rows;

        tileSize = Math.max(28, Math.min(maxTileWidth, maxTileHeight));
        tileStepX = tileSize;
        tileStepY = Math.max(tileSize, availableHeight / rows);

        int boardWidth = cols * tileStepX;
        boardLeft = (screenWidth - boardWidth) / 2;
        boardTop = hudBottom;
        moveInterval = Math.max(95, Math.min(160, 9000 / Math.max(1, tileSize)));
    }

    // Android status bar height is used so the HUD does not overlap the phone UI.
    private int getStatusBarHeight() {
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    // Starts or restarts this mini-game while preserving shared health in GameView.
    @Override
    public void start() {
        finished = false;
        started = false;
        levelComplete = false;
        loadMap();
        lastMoveTime = System.currentTimeMillis();
        lastInputMoveTime = 0;
        lastDamageTime = 0;
    }

    // Converts the string map into numeric tiles and creates the player/ghost actors.
    private void loadMap() {
        map = new int[rows][cols];
        ghosts.clear();
        exitX = -1;
        exitY = -1;

        for (int y = 0; y < rows; y++) {
            String row = TILE_MAP[y];

            for (int x = 0; x < cols; x++) {
                char tile = row.charAt(x);

                switch (tile) {
                    case 'X':
                        map[y][x] = WALL;
                        break;
                    case 'O':
                        map[y][x] = GATE;
                        break;
                    case 'E':
                        map[y][x] = EXIT;
                        exitX = x;
                        exitY = y;
                        break;
                    case 'P':
                        player = new Actor(x, y);
                        map[y][x] = EMPTY;
                        break;
                    case 'b':
                    case 's':
                    case 'r':
                        Actor ghost = new Actor(x, y);
                        ghosts.add(ghost);
                        map[y][x] = EMPTY;
                        break;
                    default:
                        map[y][x] = EMPTY;
                        break;
                }
            }
        }

        for (Actor ghost : ghosts) {
            setRandomDirection(ghost);
        }
    }

    // Advances the maze by one timed step: Karlos moves, then ghosts move.
    @Override
    public void update() {
        if (finished || !started) return;

        long now = System.currentTimeMillis();
        if (now - lastMoveTime < moveInterval) return;
        lastMoveTime = now;

        movePlayer();
        if (checkGhostCollision()) {
            return;
        }
        checkExitReached();
        moveGhosts();
        checkGhostCollision();
    }

    // Moves Karlos by one cell only, then clears direction so swipes are cell-by-cell.
    private void movePlayer() {
        int nextX = player.x + player.dx;
        int nextY = player.y + player.dy;

        if (canMoveTo(nextX, nextY)) {
            player.x = wrapX(nextX);
            player.y = nextY;
        }

        player.dx = 0;
        player.dy = 0;
    }

    // Finishes the level when Karlos reaches the exit tile.
    private void checkExitReached() {
        if (map[player.y][player.x] == EXIT) {
            finished = true;
            levelComplete = true;
        }
    }

    // Moves each ghost randomly while keeping the exit path clear.
    private void moveGhosts() {
        for (Actor ghost : ghosts) {
            if (isExitBlockingTile(ghost.x, ghost.y)) {
                moveGhostAwayFromExit(ghost);
                continue;
            }

            if (random.nextInt(4) == 0) {
                setRandomDirection(ghost);
            }

            int nextX = ghost.x + ghost.dx;
            int nextY = ghost.y + ghost.dy;

            if (!canGhostMoveTo(nextX, nextY)) {
                setRandomDirection(ghost);
                nextX = ghost.x + ghost.dx;
                nextY = ghost.y + ghost.dy;
            }

            if (canGhostMoveTo(nextX, nextY)) {
                ghost.x = nextX;
                ghost.y = nextY;
            }
        }
    }

    // Applies one shared-health penalty when Karlos and a ghost occupy the same cell.
    private boolean checkGhostCollision() {
        for (Actor ghost : ghosts) {
            if (ghost.x == player.x && ghost.y == player.y) {
                long now = System.currentTimeMillis();
                if (now - lastDamageTime < DAMAGE_COOLDOWN_MS) {
                    return false;
                }
                lastDamageTime = now;

                healthDelegate.onGhostCollision();

                if (healthDelegate.getHealth() <= 0) {
                    finished = true;
                    levelComplete = false;
                } else {
                    player.dx = 0;
                    player.dy = 0;
                    setRandomDirection(ghost);
                }

                return true;
            }
        }

        return false;
    }

    // Sends Karlos and ghosts back to their map start positions after a collision.
    private void resetActors() {
        for (int y = 0; y < rows; y++) {
            String row = TILE_MAP[y];

            for (int x = 0; x < cols; x++) {
                char tile = row.charAt(x);

                if (tile == 'P') {
                    player.x = x;
                    player.y = y;
                    player.dx = 0;
                    player.dy = 0;
                }
            }
        }

        int ghostIndex = 0;
        for (int y = 0; y < rows; y++) {
            String row = TILE_MAP[y];

            for (int x = 0; x < cols; x++) {
                char tile = row.charAt(x);

                if ((tile == 'b' || tile == 's' || tile == 'r') && ghostIndex < ghosts.size()) {
                    Actor ghost = ghosts.get(ghostIndex);
                    ghost.x = x;
                    ghost.y = y;
                    setRandomDirection(ghost);
                    ghostIndex++;
                }
            }
        }
    }

    // Player movement can wrap horizontally because it uses Pac-Man tunnel behavior.
    private boolean canMoveTo(int x, int y) {
        return canMoveTo(x, y, true);
    }

    // Checks map bounds and walls for a requested cell.
    private boolean canMoveTo(int x, int y, boolean allowWrap) {
        if (y < 0 || y >= rows) return false;

        int targetX = x;
        if (allowWrap) {
            targetX = wrapX(x);
        } else if (x < 0 || x >= cols) {
            return false;
        }

        return map[y][targetX] != WALL;
    }

    // Ghosts may move on floor tiles, but never into the exit or its approach zone.
    private boolean canGhostMoveTo(int x, int y) {
        if (!canMoveTo(x, y, false)) {
            return false;
        }

        return !isExitBlockingTile(x, y);
    }

    // Keeps the exit and nearby approach tiles free so ghosts cannot block a win.
    private boolean isExitBlockingTile(int x, int y) {
        return exitX >= 0
                && exitY >= 0
                && Math.abs(x - exitX) <= 2
                && Math.abs(y - exitY) <= 2;
    }

    private void moveGhostAwayFromExit(Actor ghost) {
        int[][] directions = {
                {-1, 0},
                {0, -1},
                {0, 1},
                {1, 0}
        };

        for (int[] direction : directions) {
            int nextX = ghost.x + direction[0];
            int nextY = ghost.y + direction[1];

            if (canGhostMoveTo(nextX, nextY)) {
                ghost.x = nextX;
                ghost.y = nextY;
                ghost.dx = direction[0];
                ghost.dy = direction[1];
                return;
            }
        }

        setRandomDirection(ghost);
    }

    // Wraps horizontal movement from one side of the maze to the other.
    private int wrapX(int x) {
        if (x < 0) return cols - 1;
        if (x >= cols) return 0;
        return x;
    }

    // Picks a random valid direction for a ghost.
    private void setRandomDirection(Actor actor) {
        int[][] directions = {
                {1, 0},
                {-1, 0},
                {0, 1},
                {0, -1}
        };

        for (int tries = 0; tries < 12; tries++) {
            int[] direction = directions[random.nextInt(directions.length)];
            int nextX = actor.x + direction[0];
            int nextY = actor.y + direction[1];

            if (canGhostMoveTo(nextX, nextY)) {
                actor.dx = direction[0];
                actor.dy = direction[1];
                return;
            }
        }

        actor.dx = 0;
        actor.dy = 0;
    }

    // Draws the complete cafe maze screen: background, HUD, map, actors, and overlays.
    @Override
    public void draw(Canvas canvas) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawColor(Color.rgb(14, 18, 32));

        drawHud(canvas, paint);
        drawBoard(canvas, paint);
        drawActors(canvas, paint);
        drawOverlay(canvas, paint);
    }

    // Draws title and shared health hearts.
    private void drawHud(Canvas canvas, Paint paint) {
        paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        paint.setTextSize(Math.max(30, screenWidth * 0.04f));
        paint.setColor(Color.YELLOW);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("HAUNTED CAFE", screenWidth / 2f, hudTop + (hudBottom - hudTop) * 0.32f, paint);

        int heartSize = Math.max(26, Math.min(tileSize - 4, screenWidth / 19));
        Rect heartRect = new Rect();

        int visibleHealth = Math.max(0, healthDelegate.getHealth());
        int maxHealth = Math.max(visibleHealth, healthDelegate.getMaxHealth());

        for (int i = 0; i < visibleHealth; i++) {
            int left = Math.max(14, screenWidth / 35) + i * (heartSize + Math.max(4, heartSize / 8));
            int top = hudBottom - heartSize - Math.max(10, screenHeight / 170);
            heartRect.set(left, top, left + heartSize, top + heartSize);
            canvas.drawBitmap(heartImage, null, heartRect, null);
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2, heartSize / 12f));
        paint.setColor(Color.rgb(115, 84, 94));
        for (int i = visibleHealth; i < maxHealth; i++) {
            int left = Math.max(14, screenWidth / 35) + i * (heartSize + Math.max(4, heartSize / 8));
            int top = hudBottom - heartSize - Math.max(10, screenHeight / 170);
            heartRect.set(left, top, left + heartSize, top + heartSize);
            canvas.drawRect(heartRect, paint);
        }
        paint.setStyle(Paint.Style.FILL);

    }

    // Draws every tile in the maze, including the highlighted exit.
    private void drawBoard(Canvas canvas, Paint paint) {
        Rect tileRect = new Rect();
        Rect boardRect = new Rect(
                boardLeft,
                boardTop,
                boardLeft + cols * tileStepX,
                boardTop + rows * tileStepY
        );

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(FLOOR_COLOR);
        canvas.drawRect(boardRect, paint);

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int left = boardLeft + x * tileStepX;
                int top = boardTop + y * tileStepY;
                tileRect.set(left, top, left + tileStepX, top + tileStepY);

                if (map[y][x] == WALL) {
                    drawWallTile(canvas, paint, tileRect);
                } else {
                    drawFloorTile(canvas, paint, tileRect, x, y);
                }

                if (map[y][x] == EXIT) {
                    drawExitTile(canvas, paint, tileRect);
                }
            }
        }
    }

    // Draws the supplied exit sign image, with a text fallback if the asset cannot load.
    private void drawExitTile(Canvas canvas, Paint paint, Rect tileRect) {
        if (exitSignImage != null) {
            canvas.drawBitmap(exitSignImage, null, tileRect, null);
            return;
        }

        paint.setColor(Color.rgb(255, 214, 64));
        canvas.drawRect(tileRect, paint);
        paint.setColor(Color.rgb(35, 90, 45));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        paint.setTextSize(Math.max(14, tileSize * 0.42f));
        canvas.drawText("EXIT", tileRect.centerX(), tileRect.centerY() + tileSize * 0.15f, paint);
    }

    // Draws the dark floor pattern for walkable maze tiles.
    private void drawFloorTile(Canvas canvas, Paint paint, Rect tileRect, int x, int y) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(FLOOR_COLOR);
        canvas.drawRect(tileRect, paint);

        paint.setColor(FLOOR_DETAIL_COLOR);
        int stripeHeight = Math.max(1, tileRect.height() / 12);
        int inset = Math.max(2, tileRect.width() / 7);
        int centerY = tileRect.centerY();
        if ((x + y) % 2 == 0) {
            canvas.drawRect(tileRect.left + inset, centerY - stripeHeight, tileRect.right - inset, centerY, paint);
        } else {
            canvas.drawRect(tileRect.left + inset * 2, centerY, tileRect.right - inset, centerY + stripeHeight, paint);
        }
    }

    // Draws a wall tile with a simple pixel-art shadow.
    private void drawWallTile(Canvas canvas, Paint paint, Rect tileRect) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(WALL_COLOR);
        canvas.drawRect(tileRect, paint);

        paint.setColor(WALL_SHADOW_COLOR);
        int shadow = Math.max(2, Math.min(tileRect.width(), tileRect.height()) / 9);
        canvas.drawRect(tileRect.left, tileRect.bottom - shadow, tileRect.right, tileRect.bottom, paint);
        canvas.drawRect(tileRect.right - shadow, tileRect.top, tileRect.right, tileRect.bottom, paint);
    }

    // Draws Karlos and each ghost at their current grid positions.
    private void drawActors(Canvas canvas, Paint paint) {
        int actorSize = Math.round(Math.min(tileStepX, tileStepY) * 1.35f);
        int maxActorSize = Math.min(tileStepX + Math.max(8, tileStepX / 4), tileStepY + Math.max(8, tileStepY / 4));
        actorSize = Math.min(actorSize, maxActorSize);
        Rect actorRect = new Rect();

        setActorRect(actorRect, player.x, player.y, actorSize);
        canvas.drawBitmap(karlosImage, null, actorRect, null);

        for (int i = 0; i < ghosts.size(); i++) {
            Actor ghost = ghosts.get(i);
            setActorRect(actorRect, ghost.x, ghost.y, actorSize);

            canvas.drawBitmap(ghostImages[i % ghostImages.length], null, actorRect, null);
        }
    }

    // Converts a grid cell into a centered rectangle for drawing an actor image.
    private void setActorRect(Rect actorRect, int gridX, int gridY, int actorSize) {
        int centerX = boardLeft + gridX * tileStepX + tileStepX / 2;
        int centerY = boardTop + gridY * tileStepY + tileStepY / 2;
        int half = actorSize / 2;

        actorRect.set(
                centerX - half,
                centerY - half,
                centerX + half,
                centerY + half
        );
    }

    // Shows start, win, and game-over messages over the maze.
    private void drawOverlay(Canvas canvas, Paint paint) {
        if (!started && !finished) {
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
            paint.setTextSize(Math.max(42, screenWidth * 0.06f));
            paint.setColor(Color.WHITE);
            canvas.drawText("SWIPE TO START", screenWidth / 2f, screenHeight - Math.max(130, screenHeight * 0.08f), paint);
            return;
        }

        if (!finished) return;

        paint.setColor(levelComplete ? Color.rgb(20, 60, 35) : Color.rgb(70, 20, 20));
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        paint.setTextSize(Math.max(64, screenWidth * 0.085f));
        paint.setColor(levelComplete ? Color.GREEN : Color.RED);
        canvas.drawText(levelComplete ? "LEVEL COMPLETE!" : "GAME OVER", screenWidth / 2f, screenHeight / 2f, paint);

        paint.setTextSize(Math.max(38, screenWidth * 0.052f));
        paint.setColor(Color.WHITE);
        canvas.drawText(levelComplete ? "Karlos Found The Exit" : "Michael Caught Karlos", screenWidth / 2f, screenHeight / 2f + 95, paint);
    }

    // Stores where the swipe began so the release can choose a direction.
    public void onTouchStart(float x, float y) {
        touchStartX = x;
        touchStartY = y;
        started = true;
    }

    // Converts the swipe into a one-cell movement command.
    public void onTouchEnd(float x, float y) {
        started = true;

        float dx = x - touchStartX;
        float dy = y - touchStartY;

        if (Math.abs(dx) < 12 && Math.abs(dy) < 12) {
            moveTowardPoint(x, y);
            return;
        }

        if (Math.abs(dx) > Math.abs(dy)) {
            commandPlayerDirection(dx > 0 ? 1 : -1, 0);
        } else {
            commandPlayerDirection(0, dy > 0 ? 1 : -1);
        }
    }

    public void onTouchMove(float x, float y) {
        long now = System.currentTimeMillis();
        if (now - lastInputMoveTime < INPUT_MOVE_COOLDOWN_MS) return;

        float dx = x - touchStartX;
        float dy = y - touchStartY;
        float threshold = Math.max(18f, tileSize * 0.35f);

        if (Math.abs(dx) < threshold && Math.abs(dy) < threshold) return;

        lastInputMoveTime = now;
        touchStartX = x;
        touchStartY = y;

        if (Math.abs(dx) > Math.abs(dy)) {
            commandPlayerDirection(dx > 0 ? 1 : -1, 0);
        } else {
            commandPlayerDirection(0, dy > 0 ? 1 : -1);
        }
    }

    private void moveTowardPoint(float x, float y) {
        int playerCenterX = boardLeft + player.x * tileStepX + tileStepX / 2;
        int playerCenterY = boardTop + player.y * tileStepY + tileStepY / 2;
        float dx = x - playerCenterX;
        float dy = y - playerCenterY;

        if (Math.abs(dx) > Math.abs(dy)) {
            commandPlayerDirection(dx > 0 ? 1 : -1, 0);
        } else {
            commandPlayerDirection(0, dy > 0 ? 1 : -1);
        }
    }

    // Moves Karlos immediately when possible so input never feels stuck.
    private void commandPlayerDirection(int dx, int dy) {
        started = true;

        int nextX = player.x + dx;
        int nextY = player.y + dy;

        if (canMoveTo(nextX, nextY)) {
            player.x = wrapX(nextX);
            player.y = nextY;
            player.dx = 0;
            player.dy = 0;
            lastMoveTime = System.currentTimeMillis();
            checkGhostCollision();
            checkExitReached();
        }
    }

    // Tells GameView whether this mini-game has ended.
    @Override
    public boolean isFinished() {
        return finished;
    }

    // Tells GameView whether the ending was a win or a loss.
    @Override
    public boolean isLevelComplete() {
        return levelComplete;
    }

    // Resets the maze while leaving GameView's persistent health unchanged.
    @Override
    public void reset() {
        start();
    }

    // Allows GameView or future callers to force this mini-game to stop.
    public void stopGame() {
        finished = true;
    }

    // Simple grid actor shared by Karlos and ghosts.
    private static class Actor {
        int x;
        int y;
        int dx;
        int dy;

        // Creates an actor at a tile coordinate with no movement yet.
        Actor(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
