package com.example.campusescape.race;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.campusescape.BitmapLoader;
import com.example.campusescape.MiniGame;
import com.example.campusescape.R;
import com.example.campusescape.CafeMaze.CafeMaze.HealthDelegate;
import com.example.campusescape.ui.UIManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CampusRushMiniGame implements MiniGame {

    private static final int VIRTUAL_WIDTH = 360;
    private static final int VIRTUAL_HEIGHT = 640;
    private static final float LEVEL_LENGTH = 3840f;
    private static final float PLAYER_WIDTH = 32f;
    private static final float PLAYER_HEIGHT = 48f;
    private static final float MONSTER_WIDTH = 22f;
    private static final float MONSTER_HEIGHT = 44f;
    private static final float GRAVITY = 1180f;
    private static final float RUN_SPEED = 0f;
    private static final float NUDGE_SPEED = 150f;
    private static final float HORIZONTAL_ACCEL = 1450f;
    private static final float HORIZONTAL_FRICTION = 1250f;
    private static final float JUMP_SPEED = 540f;
    private static final float MAX_FALL_SPEED = 760f;
    private static final int MAX_JUMPS = 3;
    private static final long START_GRACE_DURATION = 2500L;
    private static final long DAMAGE_COOLDOWN_MS = 1100L;
    private static final float FIRST_MONSTER_SPAWN_X = 720f;
    private static final float MIN_MONSTER_SPAWN_GAP = 520f;
    private static final float MAX_MONSTER_SPAWN_GAP = 820f;
    private static final float LAST_MONSTER_SPAWN_X = LEVEL_LENGTH - 620f;
    private static final int MAX_ACTIVE_MONSTERS = 3;
    private static final long DAMAGE_EFFECT_DURATION_MS = 650L;
    private static final float MONSTER_JUMP_SPEED = 500f;

    private int screenWidth = 1080;
    private int screenHeight = 1920;
    private float worldScale = 1f;
    private float worldOffsetX = 0f;
    private float worldOffsetY = 0f;
    private float viewportWidth = VIRTUAL_WIDTH;
    private float cameraX = 0f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint();
    private final RectF scratchRect = new RectF();
    private final Rect sourceRect = new Rect();
    private final RectF pillarBodyRect = new RectF();
    private final RectF collisionRect = new RectF();
    private final RectF playerRect = new RectF();
    private final RectF previousPlayerRect = new RectF();
    private final RectF finishLine = new RectF();
    private final List<Platform> platforms = new ArrayList<>();
    private final List<Monster> monsters = new ArrayList<>();
    private final List<DamageEffect> damageEffects = new ArrayList<>();
    private final Random random = new Random();
    private final HealthDelegate healthDelegate;

    private Bitmap karlosImage;
    private Bitmap backgroundImage;
    private Bitmap moonImage;
    private Bitmap starsImage;
    private Bitmap cloudsImage;
    private Bitmap platformImage;
    private Bitmap pillar1Image;
    private Bitmap pillar2Image;
    private Bitmap[] monsterImages;
    private Bitmap healthLossImage;
    private Rect karlosSource;
    private Rect platformSource;
    private Rect pillar1Source;
    private Rect pillar2Source;
    private Rect[] monsterSources;
    private Rect healthLossSource;

    private float playerX;
    private float playerY;
    private float velocityX;
    private float velocityY;
    private float inputBoost;
    private long inputBoostUntil;
    private int jumpsUsed;
    private boolean onGround;

    private boolean finished;
    private boolean levelComplete;
    private float nextMonsterSpawnX;
    private long startTime;
    private long lastUpdateTime;
    private long lastDamageTime;
    private float animationTime;

    private static class Platform {
        final RectF rect;
        final boolean solidWall;

        Platform(float left, float top, float right, float bottom, boolean solidWall) {
            this.rect = new RectF(left, top, right, bottom);
            this.solidWall = solidWall;
        }
    }

    private static class Monster {
        final RectF rect;
        float velocityY;
        boolean onGround;
        boolean active = true;
        boolean leaving;
        final float speed;
        final int spriteIndex;
        final boolean jumpsObstacles;
        float nextJumpCheckX;

        Monster(float x, float y, float speed, int spriteIndex, boolean jumpsObstacles) {
            this.rect = new RectF(x, y, x + MONSTER_WIDTH, y + MONSTER_HEIGHT);
            this.speed = speed;
            this.spriteIndex = spriteIndex;
            this.jumpsObstacles = jumpsObstacles;
            this.nextJumpCheckX = x - 80f - (float) Math.random() * 120f;
        }
    }

    private static class DamageEffect {
        final float centerX;
        final float centerY;
        final long createdAt;

        DamageEffect(float centerX, float centerY, long createdAt) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.createdAt = createdAt;
        }
    }

    public CampusRushMiniGame(Context context) {
        this(context, null);
    }

    public CampusRushMiniGame(Context context, HealthDelegate healthDelegate) {
        this.healthDelegate = healthDelegate;
        bitmapPaint.setAntiAlias(false);
        bitmapPaint.setFilterBitmap(false);

        karlosImage = BitmapLoader.decodeResource(context.getResources(), R.drawable.game3_karlos_walk_right);
        backgroundImage = BitmapLoader.decodeResource(context.getResources(), R.drawable.game3_base_background);
        moonImage = BitmapLoader.decodeResource(context.getResources(), R.drawable.game3_moon);
        starsImage = BitmapLoader.decodeResource(context.getResources(), R.drawable.game3_stars);
        cloudsImage = BitmapLoader.decodeResource(context.getResources(), R.drawable.game3_clouds);
        platformImage = BitmapLoader.decodeResource(context.getResources(), R.drawable.game3_platform);
        pillar1Image = BitmapLoader.decodeResource(context.getResources(), R.drawable.game3_pillar1);
        pillar2Image = BitmapLoader.decodeResource(context.getResources(), R.drawable.game3_pillar2);
        monsterImages = new Bitmap[] {
                BitmapLoader.decodeResource(context.getResources(), R.drawable.monster_black_horns),
                BitmapLoader.decodeResource(context.getResources(), R.drawable.monster_red_hair),
                BitmapLoader.decodeResource(context.getResources(), R.drawable.monster_brown_hair),
                BitmapLoader.decodeResource(context.getResources(), R.drawable.monster_shadow_girl)
        };
        healthLossImage = BitmapLoader.decodeResource(context.getResources(), R.drawable.health_loss);
        karlosSource = findVisibleBounds(karlosImage);
        platformSource = findVisibleBounds(platformImage);
        pillar1Source = findVisibleBounds(pillar1Image);
        pillar2Source = findVisibleBounds(pillar2Image);
        monsterSources = new Rect[monsterImages.length];
        for (int i = 0; i < monsterImages.length; i++) {
            monsterSources[i] = findVisibleBounds(monsterImages[i]);
        }
        healthLossSource = findVisibleBounds(healthLossImage);
        buildLevel();
        reset();
    }

    @Override
    public void start() {
        reset();
    }

    @Override
    public void reset() {
        playerX = 48f;
        playerY = floorY() - PLAYER_HEIGHT;
        velocityX = RUN_SPEED;
        velocityY = 0f;
        inputBoost = 0f;
        inputBoostUntil = 0L;
        jumpsUsed = 0;
        onGround = true;
        cameraX = 0f;
        finished = false;
        levelComplete = false;
        nextMonsterSpawnX = FIRST_MONSTER_SPAWN_X;
        animationTime = 0f;
        startTime = System.currentTimeMillis();
        lastUpdateTime = startTime;
        lastDamageTime = 0L;

        monsters.clear();
        damageEffects.clear();
    }

    @Override
    public void setScreenSize(int width, int height) {
        screenWidth = Math.max(1, width);
        screenHeight = Math.max(1, height);
        worldScale = screenHeight / (float) VIRTUAL_HEIGHT;
        viewportWidth = screenWidth / worldScale;
        worldOffsetX = (screenWidth - viewportWidth * worldScale) / 2f;
        worldOffsetY = 0f;
    }

    @Override
    public void update() {
        if (finished) return;

        long now = System.currentTimeMillis();
        float delta = Math.min(0.033f, Math.max(0.001f, (now - lastUpdateTime) / 1000f));
        lastUpdateTime = now;
        animationTime += delta;

        updateHorizontalVelocity(delta, now);
        previousPlayerRect.set(playerX, playerY, playerX + PLAYER_WIDTH, playerY + PLAYER_HEIGHT);

        movePlayerHorizontally(velocityX * delta);
        movePlayerVertically(delta);
        spawnMonstersAhead();
        updateMonsters(delta, now);
        updateDamageEffects(now);

        playerRect.set(playerX, playerY, playerX + PLAYER_WIDTH, playerY + PLAYER_HEIGHT);

        if (now - startTime > START_GRACE_DURATION) {
            for (Monster monster : monsters) {
                if (!monster.active) continue;

                if (RectF.intersects(playerRect, monster.rect)) {
                    handleMonsterCollision(monster, now);
                    return;
                }
            }
        }

        if (RectF.intersects(playerRect, finishLine)) {
            finished = true;
            levelComplete = true;
        }

        cameraX = clamp(playerX - viewportWidth * 0.34f, 0f, LEVEL_LENGTH - viewportWidth);
    }

    @Override
    public void draw(Canvas canvas) {
        drawWorld(canvas);

        if (finished) {
            UIManager.drawCenterMessage(
                    canvas,
                    levelComplete ? "KARLOS ESCAPED!" : "CAUGHT!",
                    screenWidth,
                    screenHeight,
                    levelComplete ? Color.rgb(120, 255, 150) : Color.rgb(255, 95, 95)
            );
        }
    }

    public void onTouch(float x, float y) {
        jump();
    }

    public void onTouch(float startX, float startY, float endX, float endY) {
        float dx = endX - startX;
        float dy = endY - startY;
        float threshold = Math.max(32f, screenWidth * 0.045f);

        if (Math.abs(dx) < threshold && Math.abs(dy) < threshold) {
            jump();
        } else if (dx < -threshold && Math.abs(dx) > Math.abs(dy)) {
            nudgeLeft();
        } else if (dx > threshold && Math.abs(dx) > Math.abs(dy)) {
            nudgeRight();
        } else {
            jump();
        }
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public boolean isLevelComplete() {
        return levelComplete;
    }

    private void buildLevel() {
        platforms.clear();
        platforms.add(new Platform(0, floorY(), LEVEL_LENGTH, VIRTUAL_HEIGHT, false));
        platforms.add(new Platform(230, 508, 300, 520, false));
        platforms.add(new Platform(370, 438, 440, 450, false));
        platforms.add(new Platform(520, 500, 590, 512, false));
        platforms.add(new Platform(680, 410, 750, 422, false));
        platforms.add(new Platform(830, 500, 900, 512, false));
        platforms.add(new Platform(990, 420, 1060, 432, false));
        platforms.add(new Platform(320, 470, 350, 482, true));
        platforms.add(new Platform(610, 410, 640, 422, true));
        platforms.add(new Platform(900, 430, 930, 442, true));
        platforms.add(new Platform(1280, 498, 1360, 510, false));
        platforms.add(new Platform(1445, 420, 1515, 432, false));
        platforms.add(new Platform(1600, 506, 1680, 518, false));
        platforms.add(new Platform(1780, 392, 1855, 404, false));
        platforms.add(new Platform(1960, 492, 2040, 504, false));
        platforms.add(new Platform(2130, 430, 2210, 442, false));
        platforms.add(new Platform(2380, 500, 2460, 512, false));
        platforms.add(new Platform(2580, 410, 2660, 422, false));
        platforms.add(new Platform(2800, 502, 2885, 514, false));
        platforms.add(new Platform(3030, 418, 3110, 430, false));
        platforms.add(new Platform(3260, 500, 3340, 512, false));
        platforms.add(new Platform(3500, 430, 3580, 442, false));
        platforms.add(new Platform(1325, 430, 1355, 442, true));
        platforms.add(new Platform(1560, 396, 1590, 408, true));
        platforms.add(new Platform(1870, 428, 1900, 440, true));
        platforms.add(new Platform(2320, 420, 2350, 432, true));
        platforms.add(new Platform(2760, 430, 2790, 442, true));
        platforms.add(new Platform(3180, 418, 3210, 430, true));
        finishLine.set(3760, 360, 3780, floorY());
    }

    private void movePlayerHorizontally(float distance) {
        playerX = clamp(playerX + distance, 0f, LEVEL_LENGTH - PLAYER_WIDTH);
        playerRect.set(playerX, playerY, playerX + PLAYER_WIDTH, playerY + PLAYER_HEIGHT);

        for (Platform platform : platforms) {
            if (!platform.solidWall) continue;

            getCollisionRect(platform, collisionRect);
            if (!RectF.intersects(playerRect, collisionRect)) continue;

            if (distance > 0f) {
                playerX = collisionRect.left - PLAYER_WIDTH;
            } else if (distance < 0f) {
                playerX = collisionRect.right;
            }

            velocityX = 0f;
            inputBoost = 0f;
            inputBoostUntil = 0L;
            playerRect.set(playerX, playerY, playerX + PLAYER_WIDTH, playerY + PLAYER_HEIGHT);
        }
    }

    private void movePlayerVertically(float delta) {
        velocityY = Math.min(MAX_FALL_SPEED, velocityY + GRAVITY * delta);
        playerY += velocityY * delta;
        onGround = false;
        playerRect.set(playerX, playerY, playerX + PLAYER_WIDTH, playerY + PLAYER_HEIGHT);

        for (Platform platform : platforms) {
            getCollisionRect(platform, collisionRect);
            if (!RectF.intersects(playerRect, collisionRect)) continue;

            boolean wasAbove = previousPlayerRect.bottom <= collisionRect.top + 4f;
            boolean wasBelow = previousPlayerRect.top >= collisionRect.bottom - 4f;

            if (velocityY >= 0f && wasAbove) {
                playerY = collisionRect.top - PLAYER_HEIGHT;
                velocityY = 0f;
                onGround = true;
                jumpsUsed = 0;
            } else if (velocityY < 0f && wasBelow) {
                playerY = collisionRect.bottom;
                velocityY = 0f;
            }

            playerRect.set(playerX, playerY, playerX + PLAYER_WIDTH, playerY + PLAYER_HEIGHT);
        }

        if (playerY > VIRTUAL_HEIGHT + 80f) {
            finished = true;
            levelComplete = false;
        }
    }

    private void updateMonsters(float delta, long now) {
        for (Monster monster : monsters) {
            if (!monster.active) continue;

            if (!monster.leaving && monster.rect.right < cameraX - 160f) {
                monster.active = false;
                continue;
            }

            if (monster.leaving) {
                monster.rect.offset(-monster.speed * 2.4f * delta, 0f);
                if (monster.rect.right < cameraX - 90f) {
                    monster.active = false;
                }
                continue;
            }

            if (monster.rect.left > playerX + viewportWidth * 1.12f) {
                continue;
            }

            monster.rect.offset(-monster.speed * delta, 0f);
            keepMonsterInWorld(monster);

            if (monster.jumpsObstacles && monster.onGround && shouldMonsterJump(monster)) {
                monster.velocityY = -MONSTER_JUMP_SPEED;
                monster.onGround = false;
                monster.nextJumpCheckX = monster.rect.left - 140f - random.nextFloat() * 160f;
            }

            monster.velocityY = Math.min(MAX_FALL_SPEED, monster.velocityY + GRAVITY * delta);
            monster.rect.offset(0f, monster.velocityY * delta);
            keepMonsterInWorld(monster);
            monster.onGround = false;

            for (Platform platform : platforms) {
                getCollisionRect(platform, collisionRect);
                if (!RectF.intersects(monster.rect, collisionRect)) continue;

                if (monster.velocityY >= 0f && monster.rect.bottom - monster.velocityY * delta <= collisionRect.top + 6f) {
                    monster.rect.offset(0f, collisionRect.top - monster.rect.bottom);
                    monster.velocityY = 0f;
                    monster.onGround = true;
                }
            }
        }
    }

    private void spawnMonstersAhead() {
        float spawnUntilX = Math.min(LAST_MONSTER_SPAWN_X, playerX + viewportWidth * 1.55f);
        int spriteCount = monsterImages == null ? 1 : monsterImages.length;

        if (nextMonsterSpawnX > spawnUntilX || countActiveMonsters() >= MAX_ACTIVE_MONSTERS) {
            return;
        }

        float minimumAheadX = playerX + viewportWidth * 0.72f;
        if (nextMonsterSpawnX < minimumAheadX) {
            nextMonsterSpawnX = minimumAheadX + random.nextFloat() * viewportWidth * 0.32f;
        }

        float speed = 96f + random.nextFloat() * 28f + Math.min(14f, playerX / 260f);
        monsters.add(new Monster(
                nextMonsterSpawnX,
                floorY() - MONSTER_HEIGHT,
                speed,
                random.nextInt(spriteCount),
                random.nextFloat() < 0.45f
        ));

        nextMonsterSpawnX += randomMonsterSpawnGap();
    }

    private boolean shouldMonsterJump(Monster monster) {
        if (monster.rect.left <= monster.nextJumpCheckX) {
            return true;
        }

        for (Platform platform : platforms) {
            if (!platform.solidWall) continue;

            getCollisionRect(platform, collisionRect);
            boolean wallAhead = collisionRect.right < monster.rect.left
                    && collisionRect.right > monster.rect.left - 120f;
            boolean nearMonsterHeight = collisionRect.top < monster.rect.bottom + 8f
                    && collisionRect.bottom > monster.rect.top;

            if (wallAhead && nearMonsterHeight) {
                return true;
            }
        }

        return false;
    }

    private int countActiveMonsters() {
        int count = 0;

        for (Monster monster : monsters) {
            if (monster.active && !monster.leaving) {
                count++;
            }
        }

        return count;
    }

    private void updateHorizontalVelocity(float delta, long now) {
        float targetVelocity = now < inputBoostUntil ? RUN_SPEED + inputBoost : RUN_SPEED;
        float response = Math.abs(targetVelocity) > 0f ? HORIZONTAL_ACCEL : HORIZONTAL_FRICTION;
        float maxChange = response * delta;
        float velocityDelta = targetVelocity - velocityX;

        if (Math.abs(velocityDelta) <= maxChange) {
            velocityX = targetVelocity;
        } else {
            velocityX += Math.signum(velocityDelta) * maxChange;
        }

        if (Math.abs(targetVelocity) == 0f && Math.abs(velocityX) < 6f) {
            velocityX = 0f;
        }
    }

    private void handleMonsterCollision(Monster monster, long now) {
        if (now - lastDamageTime < DAMAGE_COOLDOWN_MS) {
            sendMonsterLeft(monster);
            return;
        }

        lastDamageTime = now;
        float hitX = (Math.max(playerRect.left, monster.rect.left) + Math.min(playerRect.right, monster.rect.right)) / 2f;
        float hitY = (Math.max(playerRect.top, monster.rect.top) + Math.min(playerRect.bottom, monster.rect.bottom)) / 2f;
        sendMonsterLeft(monster);

        if (healthDelegate == null) {
            showDamageEffect(hitX, hitY, now);
            finished = true;
            levelComplete = false;
            return;
        }

        healthDelegate.onGhostCollision();
        showDamageEffect(hitX, hitY, now);
        if (healthDelegate.getHealth() <= 0) {
            finished = true;
            levelComplete = false;
        }
    }

    private float randomMonsterSpawnGap() {
        return MIN_MONSTER_SPAWN_GAP + random.nextFloat() * (MAX_MONSTER_SPAWN_GAP - MIN_MONSTER_SPAWN_GAP);
    }

    private void showDamageEffect(float centerX, float centerY, long now) {
        damageEffects.add(new DamageEffect(centerX, centerY, now));
    }

    private void updateDamageEffects(long now) {
        for (int i = 0; i < damageEffects.size(); i++) {
            if (now - damageEffects.get(i).createdAt > DAMAGE_EFFECT_DURATION_MS) {
                damageEffects.remove(i);
                i--;
            }
        }
    }

    private void sendMonsterLeft(Monster monster) {
        monster.leaving = true;
        monster.velocityY = 0f;
        monster.rect.offset(-10f, 0f);
    }

    private void keepMonsterInWorld(Monster monster) {
        if (monster.rect.left < -VIRTUAL_WIDTH) {
            monster.rect.offset(-VIRTUAL_WIDTH - monster.rect.left, 0f);
        } else if (monster.rect.right > LEVEL_LENGTH) {
            monster.rect.offset(LEVEL_LENGTH - monster.rect.right, 0f);
        }

        if (monster.rect.top < 0f) {
            monster.rect.offset(0f, -monster.rect.top);
            monster.velocityY = Math.max(0f, monster.velocityY);
        }
    }

    private void jump() {
        if (onGround) {
            jumpsUsed = 0;
        }

        if (jumpsUsed >= MAX_JUMPS) return;

        velocityY = -JUMP_SPEED;
        onGround = false;
        jumpsUsed++;
    }

    private void nudgeLeft() {
        inputBoost = -NUDGE_SPEED;
        inputBoostUntil = System.currentTimeMillis() + 620L;
    }

    private void nudgeRight() {
        inputBoost = NUDGE_SPEED;
        inputBoostUntil = System.currentTimeMillis() + 620L;
    }

    private void drawWorld(Canvas canvas) {
        canvas.drawColor(Color.rgb(12, 14, 30));
        canvas.save();
        canvas.translate(worldOffsetX, worldOffsetY);
        canvas.scale(worldScale, worldScale);
        canvas.translate(-cameraX, 0f);

        drawBackground(canvas);
        drawPlatforms(canvas);
        drawFinishLine(canvas);
        drawPlayer(canvas);
        drawMonsters(canvas);
        drawDamageEffects(canvas);

        canvas.restore();
        drawTouchHints(canvas);
    }

    private void drawBackground(Canvas canvas) {
        if (backgroundImage != null) {
            drawParallaxLayer(canvas, backgroundImage, 0f, 0f, 0f, 0f, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        } else {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(21, 25, 43));
            canvas.drawRect(cameraX, 0, cameraX + viewportWidth, floorY(), paint);
            paint.setColor(Color.rgb(54, 86, 153));
            canvas.drawRect(cameraX, floorY() * 0.65f, cameraX + viewportWidth, floorY(), paint);
            paint.setColor(Color.rgb(33, 36, 37));
            canvas.drawRect(cameraX, floorY(), cameraX + viewportWidth, VIRTUAL_HEIGHT, paint);
        }

        drawParallaxLayer(canvas, starsImage, 0.12f, 8f, 0f, 0f, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        drawSingleParallaxBitmap(canvas, moonImage, 0.08f, VIRTUAL_WIDTH * 0.72f, 34f, 56f, 64f);
        drawParallaxLayer(canvas, cloudsImage, 0.28f, 18f, 0f, 0f, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
    }

    private void drawPlatforms(Canvas canvas) {
        for (Platform platform : platforms) {
            if (platform.rect.right < cameraX - 80f || platform.rect.left > cameraX + viewportWidth + 80f) {
                continue;
            }

            if (platform.solidWall) {
                Bitmap pillarImage = getPillarImage(platform);
                Rect pillarSource = getPillarSource(platform);
                if (pillarImage != null && pillarSource != null) {
                    getPillarBody(platform, scratchRect);
                    canvas.drawBitmap(pillarImage, pillarSource, scratchRect, bitmapPaint);
                } else {
                    drawFallbackPlatform(canvas, platform);
                }
            } else if (platformImage != null && platformSource != null) {
                if (platform.rect.width() > VIRTUAL_WIDTH) {
                    drawTiledPlatform(canvas, platform.rect);
                } else {
                    drawBitmapWithWidth(canvas, platformImage, platformSource, platform.rect, platform.rect.width());
                }
            } else {
                drawFallbackPlatform(canvas, platform);
            }
        }
    }

    private void drawFinishLine(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(38, 176, 82));
        canvas.drawRect(finishLine, paint);

        paint.setColor(Color.BLACK);
        float check = 12f;
        for (float y = finishLine.top; y < finishLine.bottom; y += check) {
            for (float x = finishLine.left; x < finishLine.right; x += check) {
                if (((int) ((x - finishLine.left + y - finishLine.top) / check)) % 2 == 0) {
                    canvas.drawRect(x, y, Math.min(x + check, finishLine.right), Math.min(y + check, finishLine.bottom), paint);
                }
            }
        }
    }

    private void drawPlayer(Canvas canvas) {
        if (karlosImage != null && karlosSource != null) {
            float visualHeight = PLAYER_HEIGHT * 1.9f;
            float visualWidth = visualHeight * bitmapRatio(karlosSource);
            float centerX = playerX + PLAYER_WIDTH / 2f;
            float bottom = playerY + PLAYER_HEIGHT + 3f;
            scratchRect.set(
                    centerX - visualWidth / 2f,
                    bottom - visualHeight,
                    centerX + visualWidth / 2f,
                    bottom
            );
            canvas.drawBitmap(karlosImage, karlosSource, scratchRect, bitmapPaint);
            return;
        }

        paint.setColor(Color.rgb(49, 95, 210));
        canvas.drawRect(playerX, playerY, playerX + PLAYER_WIDTH, playerY + PLAYER_HEIGHT, paint);
    }

    private void drawMonsters(Canvas canvas) {
        for (Monster monster : monsters) {
            if (!monster.active) continue;

            if (monster.rect.right < cameraX || monster.rect.left > cameraX + viewportWidth) {
                continue;
            }

            Bitmap monsterImage = monsterImages == null ? null : monsterImages[monster.spriteIndex];
            Rect monsterSource = monsterSources == null ? null : monsterSources[monster.spriteIndex];

            if (monsterImage != null && monsterSource != null) {
                float visualHeight = MONSTER_HEIGHT * 2.0f;
                float visualWidth = visualHeight * bitmapRatio(monsterSource);
                scratchRect.set(
                        monster.rect.centerX() - visualWidth / 2f,
                        monster.rect.bottom - visualHeight + 3f,
                        monster.rect.centerX() + visualWidth / 2f,
                        monster.rect.bottom + 3f
                );
                canvas.drawBitmap(monsterImage, monsterSource, scratchRect, bitmapPaint);
            } else {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.rgb(191, 54, 54));
                canvas.drawRoundRect(monster.rect, 8f, 8f, paint);
            }
        }
    }

    private void drawDamageEffects(Canvas canvas) {
        if (healthLossImage == null || healthLossSource == null) return;

        long now = System.currentTimeMillis();
        int previousAlpha = bitmapPaint.getAlpha();

        for (DamageEffect effect : damageEffects) {
            float age = (now - effect.createdAt) / (float) DAMAGE_EFFECT_DURATION_MS;
            float eased = clamp(age, 0f, 1f);
            float visualWidth = 34f;
            float visualHeight = visualWidth / bitmapRatio(healthLossSource);
            float rise = eased * 16f;

            bitmapPaint.setAlpha((int) (255f * (1f - eased * 0.65f)));
            scratchRect.set(
                    effect.centerX - visualWidth / 2f,
                    effect.centerY - visualHeight / 2f - rise,
                    effect.centerX + visualWidth / 2f,
                    effect.centerY + visualHeight / 2f - rise
            );
            canvas.drawBitmap(healthLossImage, healthLossSource, scratchRect, bitmapPaint);
        }

        bitmapPaint.setAlpha(previousAlpha);
    }

    private void drawTouchHints(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(Math.max(28f, screenWidth * 0.04f));
        paint.setColor(Color.argb(130, 0, 0, 0));
        canvas.drawText("Tap to jump | swipe left/right to move", screenWidth / 2f, screenHeight - 42f, paint);
    }

    private void getPillarBody(Platform platform, RectF outRect) {
        Rect pillarSource = getPillarSource(platform);
        float height = floorY() - platform.rect.top;
        float width = pillarSource == null
                ? platform.rect.width()
                : height * bitmapRatio(pillarSource);
        float centerX = platform.rect.centerX();

        outRect.set(
                centerX - width / 2f,
                platform.rect.top,
                centerX + width / 2f,
                floorY()
        );
    }

    private void getCollisionRect(Platform platform, RectF outRect) {
        if (platform.solidWall) {
            getPillarBody(platform, outRect);
        } else {
            outRect.set(platform.rect);
        }
    }

    private Bitmap getPillarImage(Platform platform) {
        return ((int) platform.rect.left / 500) % 2 == 0 ? pillar1Image : pillar2Image;
    }

    private Rect getPillarSource(Platform platform) {
        return getPillarImage(platform) == pillar1Image ? pillar1Source : pillar2Source;
    }

    private void drawParallaxLayer(
            Canvas canvas,
            Bitmap bitmap,
            float parallax,
            float driftSpeed,
            float xOffset,
            float y,
            float width,
            float height
    ) {
        if (bitmap == null) return;

        float layerWidth = width;
        float phase = (cameraX * parallax + animationTime * driftSpeed) % layerWidth;
        float firstX = cameraX - phase + xOffset - layerWidth;

        for (float x = firstX; x < cameraX + viewportWidth + layerWidth; x += layerWidth) {
            scratchRect.set(x, y, x + layerWidth, y + height);
            canvas.drawBitmap(bitmap, null, scratchRect, bitmapPaint);
        }
    }

    private void drawSingleParallaxBitmap(
            Canvas canvas,
            Bitmap bitmap,
            float parallax,
            float x,
            float y,
            float width,
            float height
    ) {
        if (bitmap == null) return;

        float left = cameraX + x - cameraX * parallax;
        scratchRect.set(left, y, left + width, y + height);
        canvas.drawBitmap(bitmap, null, scratchRect, bitmapPaint);
    }

    private void drawFallbackPlatform(Canvas canvas, Platform platform) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(platform.solidWall ? Color.rgb(37, 21, 41) : Color.rgb(83, 47, 65));
        canvas.drawRect(platform.rect, paint);

        if (!platform.solidWall) {
            paint.setColor(Color.rgb(116, 72, 88));
            canvas.drawRect(platform.rect.left, platform.rect.top, platform.rect.right, platform.rect.top + 7f, paint);
        }
    }

    private void drawBitmapWithWidth(Canvas canvas, Bitmap bitmap, Rect source, RectF anchor, float width) {
        float height = width / bitmapRatio(source);
        scratchRect.set(
                anchor.centerX() - width / 2f,
                anchor.top,
                anchor.centerX() + width / 2f,
                anchor.top + height
        );
        canvas.drawBitmap(bitmap, source, scratchRect, bitmapPaint);
    }

    private void drawBitmapWithHeight(Canvas canvas, Bitmap bitmap, Rect source, RectF anchor, float height) {
        float width = height * bitmapRatio(source);
        scratchRect.set(
                anchor.centerX() - width / 2f,
                anchor.bottom - height,
                anchor.centerX() + width / 2f,
                anchor.bottom
        );
        canvas.drawBitmap(bitmap, source, scratchRect, bitmapPaint);
    }

    private void drawTiledPlatform(Canvas canvas, RectF platformRect) {
        float tileHeight = platformRect.height();
        float tileWidth = tileHeight * bitmapRatio(platformSource);

        for (float x = platformRect.left; x < platformRect.right; x += tileWidth) {
            scratchRect.set(x, platformRect.top, x + tileWidth, platformRect.bottom);
            canvas.drawBitmap(platformImage, platformSource, scratchRect, bitmapPaint);
        }
    }

    private float bitmapRatio(Rect source) {
        return source.width() / (float) source.height();
    }

    private Rect findVisibleBounds(Bitmap bitmap) {
        if (bitmap == null) return null;

        int minX = bitmap.getWidth();
        int minY = bitmap.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {
                if ((bitmap.getPixel(x, y) >>> 24) == 0) continue;

                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }

        if (maxX < minX || maxY < minY) {
            sourceRect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
        } else {
            sourceRect.set(minX, minY, maxX + 1, maxY + 1);
        }

        return new Rect(sourceRect);
    }

    private float screenToWorldX(float screenX) {
        return (screenX - worldOffsetX) / worldScale + cameraX;
    }

    private float screenToWorldY(float screenY) {
        return (screenY - worldOffsetY) / worldScale;
    }

    private float floorY() {
        return VIRTUAL_HEIGHT - 58f;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
