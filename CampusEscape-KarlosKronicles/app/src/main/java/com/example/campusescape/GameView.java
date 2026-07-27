package com.example.campusescape;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.Bitmap;

import com.example.campusescape.campus.CampusMap;
import com.example.campusescape.CafeMaze.CafeMaze;
import com.example.campusescape.race.CampusRushMiniGame;
import com.example.campusescape.ui.UIManager;

public class GameView extends View implements Runnable, CafeMaze.HealthDelegate {

    private static final float FIGMA_FRAME_WIDTH = 412f;
    private static final float FIGMA_FRAME_HEIGHT = 917f;
    private static final boolean TEST_UNLOCK_ALL_MINIGAMES = false;

    // ================= CORE =================
    private volatile boolean isPlaying = false;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private MiniGame game;
    private static final int MAX_PLAYER_HEALTH = 7;
    private int playerHealth = MAX_PLAYER_HEALTH;

    // ================= GAME STATES =================
    private enum State {
        MENU,
        INTRO_DIALOGUE,
        CAFE_WALK,
        CAFE_INTRO,
        INSIDE_CAFE,
        MAZE_BRIEFING,
        POST_MAZE_CAFE_DIALOGUE,
        QUAD_WALK,
        QUAD_DIALOGUE,
        QUAD_EXPERIMENT_DIALOGUE,
        WORDLE_BRIEFING,
        WORDLE_UNLOCK_SUCCESS,
        POST_WORDLE_QUAD_DIALOGUE,
        CLASSROOM_BRIEFING,
        CLASSROOM_WALK,
        CLASSROOM_DIALOGUE,
        PRE_RACE_DIALOGUE,
        RACE_BRIEFING,
        CAMPUS,
        PLAYING,
        LEVEL_COMPLETE,
        GAME_OVER,
        NIGHTMARE_RETURN_HOME,
        FINAL_CONGRATULATIONS,
        FINAL_NIGHTMARE_OVERCOME,
        FINAL_WAKE_DIALOGUE,
        FINAL_WIN
    }

    private State currentState = State.MENU;

    // ================= CAMPUS =================
    private CampusMap campusMap;

    // ================= LEVEL =================
    private int currentLevel = 1;

    // ================= FPS =================
    private static final int TARGET_FPS = 60;
    private static final long FRAME_TIME = 1000 / TARGET_FPS;

    // ================= TOUCH =================
    private float touchStartX;
    private float touchStartY;

    // ================= PAINT =================
    private final Paint paint = new Paint();
    private Bitmap openingScreenImage;
    private Bitmap openingCloudsImage;
    private Bitmap introCafeSceneImage;
    private Bitmap insideCafeImage;
    private Bitmap quadSceneImage;
    private Bitmap quadCloudsImage;
    private Bitmap classroomSceneImage;
    private Bitmap karlosImage;
    private Bitmap michaelImage;
    private Bitmap karlosFrontImage;
    private Rect karlosFrontSource;
    private Bitmap[] karlosSpinImages;
    private Rect[] karlosSpinSources;
    private Bitmap[] karlosWalkImages;
    private Rect[] karlosWalkSources;
    private Bitmap heartImage;

    private Typeface jerseyTypeface;
    private final RectF startGameBounds = new RectF();
    private final RectF cafeBounds = new RectF();
    private final RectF exitButtonBounds = new RectF();
    private static final long CAFE_WALK_DURATION_MS = 2800L;
    private static final long QUAD_WALK_DURATION_MS = 3200L;
    private static final long CLASSROOM_WALK_DURATION_MS = 3200L;
    private static final long NIGHTMARE_MESSAGE_DURATION_MS = 3500L;
    private long cafeWalkStartedAt = 0L;
    private long quadWalkStartedAt = 0L;
    private long classroomWalkStartedAt = 0L;
    private long nightmareMessageStartedAt = 0L;
    private int introDialogueIndex = 0;
    private int cafeDialogueIndex = 0;
    private int insideCafeDialogueIndex = 0;
    private int postMazeCafeDialogueIndex = 0;
    private int quadDialogueIndex = 0;
    private int quadExperimentDialogueIndex = 0;
    private int postWordleQuadDialogueIndex = 0;
    private int classroomDialogueIndex = 0;
    private int preRaceDialogueIndex = 0;
    private int finalWakeDialogueIndex = 0;

    private final String[] introDialogueLines = {
            "Karlos is a an exhausted lecturer at USYD",
            "For a few days now, he's been waking up feeling agitated. " +
                    "'Must've been stress', he keeps telling himself",
            "But this particular morning, something just doesn't feel right. But he can't wrap his head around it.",
            "So he decides to go to his favorite cafe before class."
    };

    private final String[] cafeDialogueLines = {
            "The weather feels strangely ominous",
            "No time to brood over a stupid nightmare.",
            "Can't be late to the class when my students don't understand what recursion is.",
            "The day feels strange but nothing a cup of coffee can't fix.",
            "Tap the cafe to enter."
    };

    private final String[] insideCafeDialogueLines = {
            "The cafe feels much quieter than usual.",
            "wait",
            "What happened to the cafe?",
            "'BOOOOOM' ",
            "What the..."
    };

    private final String[] postMazeCafeDialogueLines = {
            "Am I hallucinating?",
            "How did the cafe turn into a maze? Where did the ghosts come from?",
            "I am glad I made it out safe, no time to dwell over it though.",
            "I need to go to the Quadrangle to sort through my lecture materials."
    };

    private final String[] quadDialogueLines = {
            "Hmm...the sky looks clearer now.",
            "But there's still something sinister in the air. ",
            "Anyways, I will just try to get some work done before class."
    };

    private final String[] quadExperimentDialogueLines = {
            "WARNING: SOLVE RIDDLE TO ACCESS YOUR DEVICE"
    };

    private final String[] postWordleQuadDialogueLines = {
            "That was strange",
            "I shouldn't have been locked out of my devices.",
            "Anyways, my work is done, I will be heading to the class before it gets late"
    };

    private final String[] classroomDialogueLines = {
            "The sky has turned dark again.",
            "It was sunny just moments ago."
    };

    private final String[] preRaceDialogueSpeakers = {
            "Karlos",
            "Michael",
            "Karlos",
            "Michael",
            "Karlos",
            "Michael"
    };

    private final String[] preRaceDialogueLines = {
            "Oh hi! Michael!",
            "Hey Karlos, how's it going?",
            "Umm...well...it's  okay (Karlos didn't want to overshare).",
            "Having weird dreams all throughout the week?",
            "Huh? How do you know?",
            "(Michael smiles a sinister smile) Shall we go inside? "
    };

    private final String[] finalWakeDialogueLines = {
            "Nightmare?",
            "It was all inside a nightmare?",
            "Makes sense..."
    };

    // ================= CONSTRUCTOR =================
    public GameView(Context context) {

        super(context);

        setFocusable(true);

        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.CYAN);

        UIManager.init(context);

        openingScreenImage = BitmapLoader.decodeResource(
                getResources(),
                R.drawable.opening_screen
        );

        openingCloudsImage = BitmapLoader.decodeResource(
                getResources(),
                R.drawable.opening_clouds
        );

        introCafeSceneImage = BitmapLoader.decodeResource(
                getResources(),
                R.drawable.intro_cafe_scene
        );

        insideCafeImage = BitmapLoader.decodeResource(
                getResources(),
                R.drawable.inside_cafe
        );

        quadSceneImage = BitmapLoader.decodeResource(
                getResources(),
                R.drawable.quad_scene
        );

        quadCloudsImage = BitmapLoader.decodeResource(
                getResources(),
                R.drawable.quad_clouds
        );

        classroomSceneImage = BitmapLoader.decodeResource(
                getResources(),
                R.drawable.classroom_scene
        );

        karlosImage = BitmapLoader.decodeResource(
                getResources(),
                R.drawable.dialogue_karlos
        );

        michaelImage = BitmapLoader.decodeResource(
                getResources(),
                R.drawable.michael_dialogue
        );

        karlosFrontImage = BitmapLoader.decodeResource(
                getResources(),
                R.drawable.karlos_front
        );
        karlosFrontSource = findVisibleBounds(karlosFrontImage);

        heartImage = BitmapLoader.decodeResource(
                getResources(),
                R.drawable.heart
        );

        karlosSpinImages = new Bitmap[] {
                BitmapLoader.decodeResource(getResources(), R.drawable.karlos_spin_front),
                BitmapLoader.decodeResource(getResources(), R.drawable.karlos_spin_right),
                BitmapLoader.decodeResource(getResources(), R.drawable.karlos_spin_back),
                BitmapLoader.decodeResource(getResources(), R.drawable.karlos_spin_left)
        };
        karlosSpinSources = new Rect[karlosSpinImages.length];
        for (int i = 0; i < karlosSpinImages.length; i++) {
            karlosSpinSources[i] = findVisibleBounds(karlosSpinImages[i]);
        }

        karlosWalkImages = new Bitmap[] {
                BitmapLoader.decodeResource(getResources(), R.drawable.karlos_spin_walk_right),
                BitmapLoader.decodeResource(getResources(), R.drawable.karlos_spin_right)
        };
        karlosWalkSources = new Rect[karlosWalkImages.length];
        for (int i = 0; i < karlosWalkImages.length; i++) {
            karlosWalkSources[i] = findVisibleBounds(karlosWalkImages[i]);
        }

        try {
            jerseyTypeface = Typeface.createFromAsset(
                    getContext().getAssets(),
                    "fonts/Jersey25-Regular.ttf"
            );
        } catch (RuntimeException exception) {
            jerseyTypeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
        }
    }

    // ================= START LEVEL =================
    // Starts selected mini game based on level
    private void startGame(int level) {

        currentLevel = level;

        if (campusMap == null) {
            campusMap = new CampusMap(getContext());
            campusMap.setScreenSize(
                    getWidth() > 0 ? getWidth() : 1080,
                    getHeight() > 0 ? getHeight() : 1920
            );
        }

        switch (level) {
            // LEVEL 1 = CAFE MAZE
            case 1:
                game = new CafeMaze(getContext(), this);
                break;
            // LEVEL 2 = WORDLE
            case 2:
                game = new com.example.campusescape.wordle.WordleMiniGame(getContext());
                break;
            // LEVEL 3 = CAMPUS RUSH
            case 3:
                game = new CampusRushMiniGame(getContext(), this);
                break;
        }

        if (game != null) {

            game.setScreenSize(
                    getWidth() > 0 ? getWidth() : 1080,
                    getHeight() > 0 ? getHeight() : 1920
            );

            game.start();
        }

        currentState = State.PLAYING;
    }

    // ================= MAIN LOOP =================
    @Override
    public void run() {
        if (!isPlaying) return;

        update();
        invalidate();

        handler.postDelayed(this, FRAME_TIME);
    }

    // ================= UPDATE =================
    private void update() {

        // CAMPUS UPDATE
        if (currentState == State.CAMPUS && campusMap != null) {
            campusMap.update();
            return;
        }

        if (currentState == State.CAFE_WALK) {
            return;
        }

        if (currentState == State.QUAD_WALK) {
            return;
        }

        if (currentState == State.CLASSROOM_WALK) {
            return;
        }

        if (currentState == State.NIGHTMARE_RETURN_HOME) {
            if (System.currentTimeMillis() - nightmareMessageStartedAt >= NIGHTMARE_MESSAGE_DURATION_MS) {
                returnToHomePage();
            }
            return;
        }

        // ONLY UPDATE GAME WHEN PLAYING
        if (currentState != State.PLAYING || game == null) {
            return;
        }

        game.update();

        if (currentState != State.PLAYING) {
            return;
        }

        // GAME STILL RUNNING
        if (!game.isFinished()) {
            return;
        }

        // ================= WIN =================
        if (game.isLevelComplete()) {

            if (campusMap != null) {

                campusMap.completeLevel(currentLevel);

                // unlock next level
                if (currentLevel < 3) {
                    campusMap.unlockLevel(currentLevel + 1);
                }
            }

            if (currentLevel == 1) {
                postMazeCafeDialogueIndex = 0;
                currentState = State.POST_MAZE_CAFE_DIALOGUE;
                return;
            }

            if (currentLevel == 2) {
                currentState = State.WORDLE_UNLOCK_SUCCESS;
                return;
            }

            if (currentLevel == 3) {
                finalWakeDialogueIndex = 0;
                currentState = State.FINAL_CONGRATULATIONS;
            } else {
                currentState = State.LEVEL_COMPLETE;
            }
        }

        // ================= LOSE =================
        else {

            if (playerHealth > 0) {
                playerHealth = Math.max(0, playerHealth - 1);
            }

            if (playerHealth <= 0) {
                showGameOverScreen();
            } else {
                game.reset();
                currentState = State.PLAYING;
            }
        }
    }

    // ================= RESTART =================
    private void restartCurrentLevel() {

        if (game != null) {
            game.reset();
        }
    }

    // ================= NEXT LEVEL =================
    private void nextLevel() {
        if (TEST_UNLOCK_ALL_MINIGAMES) {
            showCampusSelector();
            return;
        }

        // RETURN TO CAMPUS MAP
        if (currentLevel < 3) {

            currentState = State.CAMPUS;

            if (campusMap != null) {
                campusMap.unlockLevel(currentLevel + 1);
            }

            return;
        }

        // FINAL WIN
        currentState = State.FINAL_WIN;
    }

    // ================= DRAW =================
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.BLACK);

        switch (currentState) {

            case MENU:
                drawMenu(canvas);
                break;

            case INTRO_DIALOGUE:
                drawIntroDialogue(canvas);
                break;

            case CAFE_WALK:
                drawCafeWalk(canvas);
                break;

            case CAFE_INTRO:
                drawCafeIntro(canvas);
                break;

            case INSIDE_CAFE:
                drawInsideCafe(canvas);
                break;

            case MAZE_BRIEFING:
                drawBlackStoryScreen(canvas, "Suddenly, everything shifts...");
                break;

            case POST_MAZE_CAFE_DIALOGUE:
                drawPostMazeCafeDialogue(canvas);
                break;

            case QUAD_WALK:
                drawQuadWalk(canvas);
                break;

            case QUAD_DIALOGUE:
                drawQuadDialogue(canvas);
                break;

            case QUAD_EXPERIMENT_DIALOGUE:
                drawQuadExperimentDialogue(canvas);
                break;

            case WORDLE_BRIEFING:
                drawBlackStoryScreen(canvas, "A word lock blocks the way...");
                break;

            case WORDLE_UNLOCK_SUCCESS:
                drawBlackStoryScreen(canvas, "Unlock Successful");
                break;

            case POST_WORDLE_QUAD_DIALOGUE:
                drawPostWordleQuadDialogue(canvas);
                break;

            case CLASSROOM_WALK:
                drawClassroomWalk(canvas);
                break;

            case CLASSROOM_DIALOGUE:
                drawClassroomDialogue(canvas);
                break;

            case PRE_RACE_DIALOGUE:
                drawPreRaceDialogue(canvas);
                break;

            case RACE_BRIEFING:
                drawBlackStoryScreen(canvas, "Karlos got pulled even deeper inside the nightmare...RUN is all he heard");
                break;

            case CAMPUS:

                if (campusMap != null) {
                    campusMap.draw(canvas);
                }
                drawPersistentHealth(canvas);

                break;

            case PLAYING:

                if (game != null) {
                    game.draw(canvas);
                }
                if (!(game instanceof CafeMaze)) {
                    drawPersistentHealth(canvas);
                }
                drawExitButton(canvas);

                break;

            case LEVEL_COMPLETE:
                drawLevelComplete(canvas);
                drawPersistentHealth(canvas);
                break;

            case GAME_OVER:
                drawGameOver(canvas);
                break;

            case NIGHTMARE_RETURN_HOME:
                drawNightmareReturnHome(canvas);
                break;

            case FINAL_CONGRATULATIONS:
                drawBlackFinalSequenceScreen(canvas, "Congratulations");
                break;

            case FINAL_NIGHTMARE_OVERCOME:
                drawBlackFinalSequenceScreen(canvas, "You have successfully overcome the nightmare");
                break;

            case FINAL_WAKE_DIALOGUE:
                drawFinalWakeDialogue(canvas);
                break;

            case FINAL_WIN:
                drawFinalScreen(canvas);
                break;
        }
    }

    // ================= MENU =================
    private void drawMenu(Canvas canvas) {

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        drawCenterCropBitmap(canvas, openingScreenImage, new RectF(0, 0, width, height));
        drawOpeningClouds(canvas, width, height, 0.95f);

        Paint titlePaint = createPixelTextPaint(Color.rgb(255, 255, 220), width * 0.15f, Paint.Align.CENTER);
        titlePaint.setShadowLayer(Math.max(3f, width * 0.008f), 0, Math.max(3f, width * 0.008f), Color.rgb(62, 101, 111));

        canvas.drawText("Karlos", width / 2f, height * 0.31f, titlePaint);
        canvas.drawText("Kronicles", width / 2f, height * 0.39f, titlePaint);

        drawSpinningKarlos(canvas, width / 2f, height * 0.55f, width * 0.50f);

        Paint tapPaint = createPixelTextPaint(Color.rgb(17, 34, 44), width * 0.055f, Paint.Align.CENTER);
        tapPaint.setShadowLayer(Math.max(2f, width * 0.004f), 0, Math.max(2f, width * 0.004f), Color.WHITE);
        canvas.drawText("Tap to start", width / 2f, height * 0.77f, tapPaint);

        startGameBounds.set(0, 0, width, height);
    }

    private void drawIntroDialogue(Canvas canvas) {

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        drawCenterCropBitmap(canvas, openingScreenImage, new RectF(0, 0, width, height));
        drawOpeningClouds(canvas, width, height, 0.75f);

        RectF dialogueRect = new RectF(
                width * 0.04f,
                height * 0.20f,
                width * 0.96f,
                height * 0.68f
        );

        drawDialogueBox(canvas, dialogueRect);
        drawSpinningKarlos(canvas, dialogueRect.centerX(), dialogueRect.top + dialogueRect.height() * 0.60f, dialogueRect.width() * 0.42f);

        Paint textPaint = createPixelTextPaint(Color.BLACK, width * 0.048f, Paint.Align.LEFT);
        drawWrappedText(
                canvas,
                introDialogueLines[introDialogueIndex],
                dialogueRect.left + width * 0.10f,
                dialogueRect.top + height * 0.07f,
                dialogueRect.width() - width * 0.20f,
                textPaint,
                textPaint.getTextSize() * 1.25f
        );
    }

    private void drawCafeWalk(Canvas canvas) {

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        drawCenterCropBitmap(canvas, introCafeSceneImage, new RectF(0, 0, width, height));

        long elapsed = Math.max(0L, System.currentTimeMillis() - cafeWalkStartedAt);
        float progress = clamp(elapsed / (float) CAFE_WALK_DURATION_MS, 0f, 1f);
        float easedProgress = 1f - (1f - progress) * (1f - progress);

        float spriteHeight = width * 0.26f;
        float startX = -width * 0.08f;
        float endX = width * 0.35f;
        float centerX = startX + (endX - startX) * easedProgress;
        float groundY = height * 0.985f;

        drawWalkingOrIdleKarlos(canvas, centerX, groundY, spriteHeight, progress);

        if (progress >= 1f) {
            drawWalkContinuePrompt(canvas, Color.rgb(255, 255, 220), Color.rgb(40, 58, 76));
        }
    }

    private void drawCafeIntro(Canvas canvas) {

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        cafeBounds.set(
                width * 0.14f,
                height * 0.21f,
                width * 0.86f,
                height * 0.60f
        );

        RectF dialogueRect = new RectF(
                figmaX(width, 15f),
                figmaY(height, 558f),
                figmaX(width, 396f),
                figmaY(height, 909f)
        );
        float portraitSize = figmaX(width, 251f);
        float tagHeight = figmaY(height, 38f);

        drawCafeDialogueBackground(canvas, width, dialogueRect.top + height * 0.03f);
        drawDialogueKarlosPortrait(canvas, figmaX(width, 25f), dialogueRect.top - portraitSize + portraitSize * 0.03f, portraitSize);
        drawDialogueBox(canvas, dialogueRect);
        drawDialogueNameTag(canvas, width, height, figmaX(width, 59f), dialogueRect.top - tagHeight / 2f);

        Paint textPaint = createPixelTextPaint(Color.BLACK, width * 0.046f, Paint.Align.LEFT);
        drawWrappedText(
                canvas,
                cafeDialogueLines[cafeDialogueIndex],
                dialogueRect.left + width * 0.08f,
                dialogueRect.top + height * 0.07f,
                dialogueRect.width() - width * 0.22f,
                textPaint,
                textPaint.getTextSize() * 1.25f
        );
    }

    private void drawInsideCafe(Canvas canvas) {

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        canvas.drawColor(Color.rgb(30, 30, 30));

        RectF imageRect = new RectF(
                0,
                0,
                width,
                height * 0.86f
        );
        drawCenterCropBitmap(canvas, insideCafeImage, imageRect);

        RectF dialogueRect = new RectF(
                figmaX(width, 15f),
                figmaY(height, 558f),
                figmaX(width, 396f),
                figmaY(height, 909f)
        );
        float portraitSize = figmaX(width, 251f);
        float tagHeight = figmaY(height, 38f);

        drawDialogueKarlosPortrait(canvas, figmaX(width, 96f), dialogueRect.top - portraitSize + portraitSize * 0.03f, portraitSize);
        drawDialogueBox(canvas, dialogueRect);
        drawDialogueNameTag(canvas, width, height, figmaX(width, 157f), dialogueRect.top - tagHeight / 2f);

        Paint textPaint = createPixelTextPaint(Color.BLACK, width * 0.046f, Paint.Align.LEFT);
        drawWrappedText(
                canvas,
                insideCafeDialogueLines[insideCafeDialogueIndex],
                dialogueRect.left + width * 0.08f,
                dialogueRect.top + height * 0.07f,
                dialogueRect.width() - width * 0.22f,
                textPaint,
                textPaint.getTextSize() * 1.25f
        );
    }

    private void drawBlackStoryScreen(Canvas canvas, String message) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        canvas.drawColor(Color.BLACK);

        Paint textPaint = createPixelTextPaint(Color.WHITE, width * 0.045f, Paint.Align.CENTER);
        drawWrappedText(
                canvas,
                message,
                width / 2f,
                height / 2f,
                width * 0.78f,
                textPaint,
                textPaint.getTextSize() * 1.4f
        );
    }

    private void drawPostMazeCafeDialogue(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        RectF dialogueRect = new RectF(
                figmaX(width, 15f),
                figmaY(height, 558f),
                figmaX(width, 396f),
                figmaY(height, 909f)
        );
        float portraitSize = figmaX(width, 251f);
        float tagHeight = figmaY(height, 38f);

        drawCafeDialogueBackground(canvas, width, dialogueRect.top + height * 0.03f);
        drawDialogueKarlosPortrait(canvas, figmaX(width, 96f), dialogueRect.top - portraitSize + portraitSize * 0.03f, portraitSize);
        drawDialogueBox(canvas, dialogueRect);
        drawDialogueNameTag(canvas, width, height, figmaX(width, 157f), dialogueRect.top - tagHeight / 2f);

        Paint textPaint = createPixelTextPaint(Color.BLACK, width * 0.046f, Paint.Align.LEFT);
        drawWrappedText(
                canvas,
                postMazeCafeDialogueLines[postMazeCafeDialogueIndex],
                dialogueRect.left + width * 0.08f,
                dialogueRect.top + height * 0.07f,
                dialogueRect.width() - width * 0.22f,
                textPaint,
                textPaint.getTextSize() * 1.25f
        );
    }

    private void drawQuadWalk(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        drawQuadScene(canvas);

        long elapsed = Math.max(0L, System.currentTimeMillis() - quadWalkStartedAt);
        float progress = clamp(elapsed / (float) QUAD_WALK_DURATION_MS, 0f, 1f);
        float easedProgress = 1f - (1f - progress) * (1f - progress);

        float spriteHeight = width * 0.20f;
        float startX = -width * 0.08f;
        float endX = width * 0.54f;
        float centerX = startX + (endX - startX) * easedProgress;
        float groundY = height * 0.975f;

        drawWalkingOrIdleKarlos(canvas, centerX, groundY, spriteHeight, progress);

        if (progress >= 1f) {
            drawWalkContinuePrompt(canvas, Color.rgb(255, 255, 220), Color.rgb(40, 58, 76));
        }
    }

    private void drawQuadDialogue(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        RectF dialogueRect = new RectF(
                figmaX(width, 15f),
                figmaY(height, 558f),
                figmaX(width, 396f),
                figmaY(height, 909f)
        );
        float portraitSize = figmaX(width, 251f);
        float tagHeight = figmaY(height, 38f);

        drawQuadDialogueBackground(canvas, width, dialogueRect.top + height * 0.03f);
        drawDialogueKarlosPortrait(canvas, figmaX(width, 96f), dialogueRect.top - portraitSize + portraitSize * 0.03f, portraitSize);
        drawDialogueBox(canvas, dialogueRect);
        drawDialogueNameTag(canvas, width, height, figmaX(width, 157f), dialogueRect.top - tagHeight / 2f);

        Paint textPaint = createPixelTextPaint(Color.BLACK, width * 0.046f, Paint.Align.LEFT);
        drawWrappedText(
                canvas,
                quadDialogueLines[quadDialogueIndex],
                dialogueRect.left + width * 0.08f,
                dialogueRect.top + height * 0.07f,
                dialogueRect.width() - width * 0.22f,
                textPaint,
                textPaint.getTextSize() * 1.25f
        );
    }

    private void drawQuadExperimentDialogue(Canvas canvas) {
        drawBlackStoryScreen(canvas, quadExperimentDialogueLines[quadExperimentDialogueIndex]);
    }

    private void drawPostWordleQuadDialogue(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        RectF dialogueRect = getStoryDialogueRect(width, height);
        float portraitSize = figmaX(width, 251f);
        float tagHeight = figmaY(height, 38f);

        drawQuadDialogueBackground(canvas, width, dialogueRect.top + height * 0.03f);
        drawDialogueKarlosPortrait(canvas, figmaX(width, 96f), dialogueRect.top - portraitSize + portraitSize * 0.03f, portraitSize);
        drawDialogueBox(canvas, dialogueRect);
        drawDialogueNameTag(canvas, width, height, figmaX(width, 157f), dialogueRect.top - tagHeight / 2f);

        Paint textPaint = createPixelTextPaint(Color.BLACK, width * 0.046f, Paint.Align.LEFT);
        drawWrappedText(
                canvas,
                postWordleQuadDialogueLines[postWordleQuadDialogueIndex],
                dialogueRect.left + width * 0.08f,
                dialogueRect.top + height * 0.07f,
                dialogueRect.width() - width * 0.22f,
                textPaint,
                textPaint.getTextSize() * 1.25f
        );
    }

    private void drawClassroomWalk(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        drawClassroomScene(canvas);

        long elapsed = Math.max(0L, System.currentTimeMillis() - classroomWalkStartedAt);
        float progress = clamp(elapsed / (float) CLASSROOM_WALK_DURATION_MS, 0f, 1f);
        float easedProgress = 1f - (1f - progress) * (1f - progress);

        float spriteHeight = width * 0.21f;
        float startX = -width * 0.08f;
        float endX = width * 0.33f;
        float centerX = startX + (endX - startX) * easedProgress;
        float groundY = height * 0.985f;

        drawWalkingOrIdleKarlos(canvas, centerX, groundY, spriteHeight, progress);

        if (progress >= 1f) {
            drawWalkContinuePrompt(canvas, Color.rgb(255, 255, 220), Color.rgb(40, 58, 76));
        }
    }

    private void drawClassroomDialogue(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        RectF dialogueRect = getStoryDialogueRect(width, height);
        float portraitSize = figmaX(width, 251f);
        float tagHeight = figmaY(height, 38f);

        drawClassroomDialogueBackground(canvas, width, dialogueRect.top + height * 0.03f);
        drawDialogueKarlosPortrait(canvas, figmaX(width, 96f), dialogueRect.top - portraitSize + portraitSize * 0.03f, portraitSize);
        drawDialogueBox(canvas, dialogueRect);
        drawDialogueNameTag(canvas, width, height, figmaX(width, 157f), dialogueRect.top - tagHeight / 2f);

        Paint textPaint = createPixelTextPaint(Color.BLACK, width * 0.046f, Paint.Align.LEFT);
        drawWrappedText(
                canvas,
                classroomDialogueLines[classroomDialogueIndex],
                dialogueRect.left + width * 0.08f,
                dialogueRect.top + height * 0.07f,
                dialogueRect.width() - width * 0.22f,
                textPaint,
                textPaint.getTextSize() * 1.25f
        );
    }

    private void drawPreRaceDialogue(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        RectF dialogueRect = getStoryDialogueRect(width, height);
        float portraitSize = figmaX(width, 251f);
        float tagHeight = figmaY(height, 38f);
        String speaker = preRaceDialogueSpeakers[preRaceDialogueIndex];
        boolean michaelSpeaking = speaker.equals("Michael");

        drawClassroomDialogueBackground(canvas, width, dialogueRect.top + height * 0.03f);
        if (michaelSpeaking) {
            drawDialoguePortrait(canvas, michaelImage, figmaX(width, 120f), dialogueRect.top - portraitSize + portraitSize * 0.03f, portraitSize);
        } else {
            drawDialogueKarlosPortrait(canvas, figmaX(width, 25f), dialogueRect.top - portraitSize + portraitSize * 0.03f, portraitSize);
        }

        drawDialogueBox(canvas, dialogueRect);
        drawDialogueNameTag(
                canvas,
                width,
                height,
                michaelSpeaking ? figmaX(width, 215f) : figmaX(width, 59f),
                dialogueRect.top - tagHeight / 2f,
                speaker
        );

        Paint textPaint = createPixelTextPaint(Color.BLACK, width * 0.046f, Paint.Align.LEFT);
        drawWrappedText(
                canvas,
                preRaceDialogueLines[preRaceDialogueIndex],
                dialogueRect.left + width * 0.08f,
                dialogueRect.top + height * 0.07f,
                dialogueRect.width() - width * 0.22f,
                textPaint,
                textPaint.getTextSize() * 1.25f
        );
    }

    private RectF getStoryDialogueRect(int width, int height) {
        return new RectF(
                figmaX(width, 15f),
                figmaY(height, 558f),
                figmaX(width, 396f),
                figmaY(height, 909f)
        );
    }

    private void drawQuadScene(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        drawCenterCropBitmap(canvas, quadSceneImage, new RectF(0, 0, width, height));
        drawCloudLayer(canvas, quadCloudsImage, width, height, 0.82f);
    }

    private void drawClassroomScene(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        drawCenterCropBitmap(canvas, classroomSceneImage, new RectF(0, 0, width, height));
        drawCloudLayer(canvas, openingCloudsImage, width, height, 0.70f);
    }

    private void drawQuadDialogueBackground(Canvas canvas, int width, float bottom) {
        if (quadSceneImage == null) {
            canvas.drawColor(Color.rgb(96, 173, 205));
            return;
        }

        RectF backgroundRect = new RectF(0, 0, width, bottom);
        Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        bitmapPaint.setAntiAlias(false);
        bitmapPaint.setFilterBitmap(false);
        canvas.drawBitmap(quadSceneImage, null, backgroundRect, bitmapPaint);
        drawCloudLayer(canvas, quadCloudsImage, width, (int) bottom, 0.82f);
    }

    private void drawClassroomDialogueBackground(Canvas canvas, int width, float bottom) {
        if (classroomSceneImage == null) {
            canvas.drawColor(Color.rgb(88, 70, 112));
            return;
        }

        RectF backgroundRect = new RectF(0, 0, width, bottom);
        Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        bitmapPaint.setAntiAlias(false);
        bitmapPaint.setFilterBitmap(false);
        canvas.drawBitmap(classroomSceneImage, null, backgroundRect, bitmapPaint);
        drawCloudLayer(canvas, openingCloudsImage, width, (int) bottom, 0.70f);
    }

    private void drawCafeDialogueBackground(Canvas canvas, int width, float bottom) {
        if (introCafeSceneImage == null) {
            canvas.drawColor(Color.rgb(95, 170, 202));
            return;
        }

        RectF backgroundRect = new RectF(0, 0, width, bottom);
        Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        bitmapPaint.setAntiAlias(false);
        bitmapPaint.setFilterBitmap(false);
        canvas.drawBitmap(introCafeSceneImage, null, backgroundRect, bitmapPaint);
    }

    private void drawCafeGreenery(Canvas canvas, RectF dialogueRect) {
        float unit = canvas.getWidth() / 360f;
        float groundTop = dialogueRect.top - 10f * unit;
        float groundBottom = canvas.getHeight();

        Paint grassPaint = new Paint();
        grassPaint.setAntiAlias(false);
        grassPaint.setStyle(Paint.Style.FILL);
        grassPaint.setColor(Color.rgb(122, 170, 105));
        canvas.drawRect(0, groundTop, canvas.getWidth(), groundBottom, grassPaint);

        grassPaint.setColor(Color.rgb(91, 139, 86));
        for (int i = 0; i < 18; i++) {
            float x = i * 23f * unit - 5f * unit;
            float y = groundTop + (i % 3) * 9f * unit;
            canvas.drawRect(x, y, x + 12f * unit, y + 5f * unit, grassPaint);
        }

        grassPaint.setColor(Color.rgb(154, 194, 118));
        for (int i = 0; i < 12; i++) {
            float x = i * 31f * unit + 7f * unit;
            float y = dialogueRect.bottom + 5f * unit + (i % 2) * 8f * unit;
            canvas.drawRect(x, y, x + 14f * unit, y + 4f * unit, grassPaint);
        }

        float bottomY = Math.min(canvas.getHeight() - 18f * unit, dialogueRect.bottom + 8f * unit);
        float topY = dialogueRect.top - 24f * unit;

        drawPixelShrub(canvas, dialogueRect.left - 8f * unit, topY, 18f * unit, true);
        drawPixelShrub(canvas, dialogueRect.right - 11f * unit, topY + 2f * unit, 16f * unit, false);
        drawPixelShrub(canvas, 4f * unit, dialogueRect.centerY() - 20f * unit, 13f * unit, false);
        drawPixelShrub(canvas, canvas.getWidth() - 26f * unit, dialogueRect.centerY() + 14f * unit, 15f * unit, true);
        drawPixelShrub(canvas, dialogueRect.left + 10f * unit, dialogueRect.bottom - 3f * unit, 14f * unit, false);
        drawPixelShrub(canvas, dialogueRect.right - 26f * unit, dialogueRect.bottom - 5f * unit, 17f * unit, true);
        drawPixelShrub(canvas, dialogueRect.centerX() - 18f * unit, bottomY, 13f * unit, true);

        Paint flowerPaint = new Paint();
        flowerPaint.setAntiAlias(false);
        flowerPaint.setStyle(Paint.Style.FILL);

        drawPixelFlower(canvas, flowerPaint, dialogueRect.left - 1f * unit, topY + 12f * unit, unit, Color.rgb(255, 210, 84));
        drawPixelFlower(canvas, flowerPaint, dialogueRect.right - 5f * unit, topY + 15f * unit, unit, Color.rgb(255, 154, 185));
        drawPixelFlower(canvas, flowerPaint, dialogueRect.left + 27f * unit, dialogueRect.bottom + 1f * unit, unit, Color.rgb(255, 210, 84));
        drawPixelFlower(canvas, flowerPaint, dialogueRect.right - 36f * unit, dialogueRect.bottom - 2f * unit, unit, Color.rgb(255, 154, 185));
    }

    private void drawDialogueKarlosPortrait(Canvas canvas, RectF dialogueRect, float portraitHeight) {
        drawDialogueKarlosPortrait(canvas, dialogueRect.left, dialogueRect.top - portraitHeight * 0.96f, portraitHeight);
    }

    private void drawDialogueKarlosPortrait(Canvas canvas, RectF dialogueRect, float portraitHeight, boolean alignRight) {
        float left = alignRight
                ? dialogueRect.right - portraitHeight
                : dialogueRect.left;
        drawDialogueKarlosPortrait(canvas, left, dialogueRect.top - portraitHeight * 0.96f, portraitHeight);
    }

    private void drawDialogueKarlosPortrait(
            Canvas canvas,
            float left,
            float top,
            float size
    ) {
        drawDialoguePortrait(canvas, karlosImage, left, top, size);
    }

    private void drawDialoguePortrait(
            Canvas canvas,
            Bitmap portrait,
            float left,
            float top,
            float size
    ) {
        if (portrait == null) return;

        RectF portraitRect = new RectF(
                left,
                top,
                left + size,
                top + size
        );

        canvas.drawBitmap(portrait, null, portraitRect, new Paint(Paint.FILTER_BITMAP_FLAG));
    }

    private void drawDialogueNameTag(Canvas canvas, int width, int height, float left, float top) {
        drawDialogueNameTag(canvas, width, height, left, top, "Karlos");
    }

    private void drawDialogueNameTag(Canvas canvas, int width, int height, float left, float top, String name) {
        float tagWidth = figmaX(width, 136f);
        float tagHeight = figmaY(height, 38f);
        RectF tagRect = new RectF(
                left,
                top,
                left + tagWidth,
                top + tagHeight
        );

        Paint tagPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tagPaint.setColor(Color.rgb(235, 196, 164));
        tagPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(tagRect, tagHeight * 0.45f, tagHeight * 0.45f, tagPaint);

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.rgb(150, 72, 66));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(Math.max(3f, width * 0.003f));
        canvas.drawRoundRect(tagRect, tagHeight * 0.45f, tagHeight * 0.45f, borderPaint);

        Paint namePaint = createPixelTextPaint(Color.BLACK, width * 0.04f, Paint.Align.CENTER);
        Paint.FontMetrics metrics = namePaint.getFontMetrics();
        float baseline = tagRect.centerY() - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(name, tagRect.centerX(), baseline, namePaint);
    }

    private void drawPixelShrub(Canvas canvas, float x, float y, float size, boolean darkFirst) {
        Paint shrubPaint = new Paint();
        shrubPaint.setAntiAlias(false);
        shrubPaint.setStyle(Paint.Style.FILL);

        int dark = Color.rgb(64, 106, 82);
        int mid = Color.rgb(88, 141, 96);
        int light = Color.rgb(124, 174, 112);

        shrubPaint.setColor(darkFirst ? dark : mid);
        canvas.drawRect(x, y + size * 0.35f, x + size * 1.6f, y + size, shrubPaint);
        canvas.drawRect(x + size * 0.28f, y, x + size * 1.22f, y + size * 0.72f, shrubPaint);

        shrubPaint.setColor(darkFirst ? mid : dark);
        canvas.drawRect(x + size * 0.82f, y + size * 0.18f, x + size * 1.88f, y + size * 0.9f, shrubPaint);

        shrubPaint.setColor(light);
        canvas.drawRect(x + size * 0.35f, y + size * 0.24f, x + size * 0.55f, y + size * 0.42f, shrubPaint);
        canvas.drawRect(x + size * 1.16f, y + size * 0.38f, x + size * 1.36f, y + size * 0.56f, shrubPaint);
    }

    private void drawPixelFlower(Canvas canvas, Paint flowerPaint, float x, float y, float unit, int color) {
        flowerPaint.setColor(Color.rgb(55, 128, 74));
        canvas.drawRect(x, y + 4f * unit, x + 2f * unit, y + 12f * unit, flowerPaint);

        flowerPaint.setColor(color);
        canvas.drawRect(x - 3f * unit, y, x + 5f * unit, y + 3f * unit, flowerPaint);
        canvas.drawRect(x - 1f * unit, y - 2f * unit, x + 3f * unit, y + 5f * unit, flowerPaint);

        flowerPaint.setColor(Color.rgb(255, 238, 132));
        canvas.drawRect(x, y + unit, x + 2f * unit, y + 3f * unit, flowerPaint);
    }

    private void drawDialogueBox(Canvas canvas, RectF rect) {
        Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boxPaint.setColor(Color.rgb(255, 229, 203));
        boxPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(rect, rect.width() * 0.09f, rect.width() * 0.09f, boxPaint);

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.rgb(150, 72, 66));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(Math.max(3f, rect.width() * 0.006f));
        canvas.drawRoundRect(rect, rect.width() * 0.09f, rect.width() * 0.09f, borderPaint);
    }

    private Paint createPixelTextPaint(int color, float size, Paint.Align align) {
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(color);
        textPaint.setTextAlign(align);
        textPaint.setTypeface(jerseyTypeface);
        textPaint.setTextSize(clamp(size, 28f, 120f));
        return textPaint;
    }

    private void drawWrappedText(
            Canvas canvas,
            String text,
            float x,
            float y,
            float maxWidth,
            Paint textPaint,
            float lineHeight
    ) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        float currentY = y;

        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;

            if (textPaint.measureText(candidate) <= maxWidth) {
                line = new StringBuilder(candidate);
            } else {
                canvas.drawText(line.toString(), x, currentY, textPaint);
                line = new StringBuilder(word);
                currentY += lineHeight;
            }
        }

        if (line.length() > 0) {
            canvas.drawText(line.toString(), x, currentY, textPaint);
        }
    }

    private void drawWrappedCenteredText(
            Canvas canvas,
            String text,
            float centerX,
            float y,
            float maxWidth,
            Paint textPaint,
            float lineHeight
    ) {
        Paint centeredPaint = new Paint(textPaint);
        centeredPaint.setTextAlign(Paint.Align.CENTER);
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        float currentY = y;

        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;

            if (centeredPaint.measureText(candidate) <= maxWidth) {
                line = new StringBuilder(candidate);
            } else {
                canvas.drawText(line.toString(), centerX, currentY, centeredPaint);
                line = new StringBuilder(word);
                currentY += lineHeight;
            }
        }

        if (line.length() > 0) {
            canvas.drawText(line.toString(), centerX, currentY, centeredPaint);
        }
    }

    private void drawExitButton(Canvas canvas) {
        int width = canvas.getWidth();
        int buttonWidth = Math.max(104, Math.round(width * 0.19f));
        int buttonHeight = Math.max(42, Math.round(width * 0.062f));
        int top = Math.max(getStatusBarHeight() + 48, Math.round(canvas.getHeight() * 0.072f));
        float rightPadding = Math.max(14f, width * 0.035f);
        float left = width - rightPadding - buttonWidth;

        exitButtonBounds.set(left, top, left + buttonWidth, top + buttonHeight);

        Paint buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        buttonPaint.setStyle(Paint.Style.FILL);
        buttonPaint.setColor(Color.rgb(255, 229, 203));
        canvas.drawRoundRect(exitButtonBounds, buttonHeight * 0.35f, buttonHeight * 0.35f, buttonPaint);

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(Math.max(3f, width * 0.006f));
        borderPaint.setColor(Color.rgb(150, 72, 66));
        canvas.drawRoundRect(exitButtonBounds, buttonHeight * 0.35f, buttonHeight * 0.35f, borderPaint);

        Paint textPaint = createPixelTextPaint(Color.rgb(45, 28, 29), width * 0.046f, Paint.Align.CENTER);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = exitButtonBounds.centerY() - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText("Exit", exitButtonBounds.centerX(), baseline, textPaint);
    }

    private void drawCenterCropBitmap(Canvas canvas, Bitmap bitmap, RectF destination) {
        if (bitmap == null) return;

        float bitmapRatio = bitmap.getWidth() / (float) bitmap.getHeight();
        float destinationRatio = destination.width() / destination.height();

        Rect source;

        if (bitmapRatio > destinationRatio) {
            int sourceWidth = Math.round(bitmap.getHeight() * destinationRatio);
            int sourceLeft = (bitmap.getWidth() - sourceWidth) / 2;
            source = new Rect(sourceLeft, 0, sourceLeft + sourceWidth, bitmap.getHeight());
        } else {
            int sourceHeight = Math.round(bitmap.getWidth() / destinationRatio);
            int sourceTop = (bitmap.getHeight() - sourceHeight) / 2;
            source = new Rect(0, sourceTop, bitmap.getWidth(), sourceTop + sourceHeight);
        }

        Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(bitmap, source, destination, bitmapPaint);
    }

    private void drawOpeningClouds(Canvas canvas, int width, int height, float alpha) {
        drawCloudLayer(canvas, openingCloudsImage, width, height, alpha);
    }

    private void drawCloudLayer(Canvas canvas, Bitmap cloudBitmap, int width, int height, float alpha) {
        if (cloudBitmap == null) return;

        Paint cloudPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        cloudPaint.setAlpha((int) (255 * clamp(alpha, 0f, 1f)));

        float layerHeight = height;
        float layerWidth = layerHeight * cloudBitmap.getWidth() / (float) cloudBitmap.getHeight();
        if (layerWidth <= 0f) return;

        float seconds = (System.currentTimeMillis() % 120000L) / 1000f;
        float drift = (seconds * Math.max(10f, width * 0.025f)) % layerWidth;
        float firstX = -drift - layerWidth;

        RectF cloudRect = new RectF();
        for (float x = firstX; x < width + layerWidth; x += layerWidth) {
            cloudRect.set(x, 0, x + layerWidth, layerHeight);
            canvas.drawBitmap(cloudBitmap, null, cloudRect, cloudPaint);
        }
    }

    private void drawSpinningKarlos(Canvas canvas, float centerX, float centerY, float height) {
        if (karlosSpinImages == null || karlosSpinImages.length == 0) return;

        int frameIndex = (int) ((System.currentTimeMillis() / 650L) % karlosSpinImages.length);
        Bitmap frame = karlosSpinImages[frameIndex];
        Rect source = karlosSpinSources[frameIndex];
        if (frame == null || source == null) return;

        float width = height * source.width() / (float) source.height();
        RectF destination = new RectF(
                centerX - width / 2f,
                centerY - height / 2f,
                centerX + width / 2f,
                centerY + height / 2f
        );

        Paint spritePaint = new Paint();
        spritePaint.setAntiAlias(false);
        spritePaint.setFilterBitmap(false);
        canvas.drawBitmap(frame, source, destination, spritePaint);
    }

    private void drawWalkingKarlos(Canvas canvas, float centerX, float bottomY, float height) {
        if (karlosWalkImages == null || karlosWalkImages.length == 0) return;

        int frameIndex = (int) ((System.currentTimeMillis() / 220L) % karlosWalkImages.length);
        Bitmap frame = karlosWalkImages[frameIndex];
        Rect source = karlosWalkSources[frameIndex];
        if (frame == null || source == null) return;

        float width = height * source.width() / (float) source.height();
        RectF destination = new RectF(
                centerX - width / 2f,
                bottomY - height,
                centerX + width / 2f,
                bottomY
        );

        Paint spritePaint = new Paint();
        spritePaint.setAntiAlias(false);
        spritePaint.setFilterBitmap(false);
        canvas.drawBitmap(frame, source, destination, spritePaint);
    }

    private void drawWalkingOrIdleKarlos(Canvas canvas, float centerX, float bottomY, float height, float progress) {
        if (progress >= 1f) {
            drawFrontKarlos(canvas, centerX, bottomY, height);
        } else {
            drawWalkingKarlos(canvas, centerX, bottomY, height);
        }
    }

    private void drawFrontKarlos(Canvas canvas, float centerX, float bottomY, float height) {
        if (karlosFrontImage == null || karlosFrontSource == null) {
            drawWalkingKarlos(canvas, centerX, bottomY, height);
            return;
        }

        float width = height * karlosFrontSource.width() / (float) karlosFrontSource.height();
        RectF destination = new RectF(
                centerX - width / 2f,
                bottomY - height,
                centerX + width / 2f,
                bottomY
        );

        Paint spritePaint = new Paint();
        spritePaint.setAntiAlias(false);
        spritePaint.setFilterBitmap(false);
        canvas.drawBitmap(karlosFrontImage, karlosFrontSource, destination, spritePaint);
    }

    private void drawWalkContinuePrompt(Canvas canvas, int textColor, int shadowColor) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        Paint tapPaint = createPixelTextPaint(textColor, width * 0.045f, Paint.Align.CENTER);
        tapPaint.setShadowLayer(
                Math.max(3f, width * 0.006f),
                0,
                Math.max(3f, width * 0.006f),
                shadowColor
        );
        canvas.drawText("Tap to continue", width / 2f, height * 0.46f, tapPaint);
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
            return new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        }

        return new Rect(minX, minY, maxX + 1, maxY + 1);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float figmaX(int width, float value) {
        return width * value / FIGMA_FRAME_WIDTH;
    }

    private float figmaY(int height, float value) {
        return height * value / FIGMA_FRAME_HEIGHT;
    }

    private void drawPersistentHealth(Canvas canvas) {
        int visibleHealth = Math.max(0, playerHealth);
        int heartSize = Math.max(30, canvas.getWidth() / 19);
        int gap = Math.max(4, heartSize / 8);
        int leftStart = Math.max(14, canvas.getWidth() / 35);
        int top = Math.max(22, getStatusBarHeight() + 18);
        Rect heartRect = new Rect();
        Paint healthPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        for (int i = 0; i < visibleHealth; i++) {
            int left = leftStart + i * (heartSize + gap);
            heartRect.set(left, top, left + heartSize, top + heartSize);
            canvas.drawBitmap(heartImage, null, heartRect, healthPaint);
        }

        healthPaint.setStyle(Paint.Style.STROKE);
        healthPaint.setStrokeWidth(Math.max(2, heartSize / 12f));
        healthPaint.setColor(Color.rgb(115, 84, 94));
        for (int i = visibleHealth; i < MAX_PLAYER_HEALTH; i++) {
            int left = leftStart + i * (heartSize + gap);
            heartRect.set(left, top, left + heartSize, top + heartSize);
            canvas.drawRect(heartRect, healthPaint);
        }
    }

    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    // ================= LEVEL COMPLETE =================
    private void drawLevelComplete(Canvas canvas) {

        canvas.drawColor(Color.rgb(0, 40, 60));

        UIManager.drawCenterMessage(
                canvas,
                "★ LEVEL COMPLETE ★",
                canvas.getWidth(),
                canvas.getHeight(),
                Color.YELLOW
        );

        UIManager.drawSubtitle(
                canvas,
                "Tap To Return To Campus",
                900
        );
    }

    private void drawGameOver(Canvas canvas) {

        canvas.drawColor(Color.BLACK);

        Paint titlePaint = createPixelTextPaint(Color.rgb(255, 82, 82), canvas.getWidth() * 0.12f, Paint.Align.CENTER);
        titlePaint.setShadowLayer(
                Math.max(4f, canvas.getWidth() * 0.008f),
                0,
                Math.max(4f, canvas.getWidth() * 0.008f),
                Color.rgb(82, 0, 0)
        );
        canvas.drawText("GAME OVER", canvas.getWidth() / 2f, canvas.getHeight() * 0.43f, titlePaint);

        Paint subtitlePaint = createPixelTextPaint(Color.WHITE, canvas.getWidth() * 0.052f, Paint.Align.CENTER);
        canvas.drawText("Tap to continue", canvas.getWidth() / 2f, canvas.getHeight() * 0.52f, subtitlePaint);
    }

    private void drawNightmareReturnHome(Canvas canvas) {
        canvas.drawColor(Color.BLACK);

        Paint textPaint = createPixelTextPaint(Color.WHITE, canvas.getWidth() * 0.055f, Paint.Align.CENTER);
        drawWrappedCenteredText(
                canvas,
                "Karlos feels himself getting sucked deeper and deeper into a whirlwind of nightmares",
                canvas.getWidth() / 2f,
                canvas.getHeight() * 0.43f,
                canvas.getWidth() * 0.78f,
                textPaint,
                textPaint.getTextSize() * 1.28f
        );
    }

    private void drawBlackFinalSequenceScreen(Canvas canvas, String message) {
        canvas.drawColor(Color.BLACK);

        Paint textPaint = createPixelTextPaint(Color.WHITE, canvas.getWidth() * 0.07f, Paint.Align.CENTER);
        drawWrappedCenteredText(
                canvas,
                message,
                canvas.getWidth() / 2f,
                canvas.getHeight() * 0.45f,
                canvas.getWidth() * 0.72f,
                textPaint,
                textPaint.getTextSize() * 1.15f
        );
    }

    private void drawFinalWakeDialogue(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        drawCenterCropBitmap(canvas, openingScreenImage, new RectF(0, 0, width, height));
        drawOpeningClouds(canvas, width, height, 0.75f);

        RectF dialogueRect = new RectF(
                width * 0.08f,
                height * 0.22f,
                width * 0.92f,
                height * 0.60f
        );

        drawDialogueBox(canvas, dialogueRect);

        Paint textPaint = createPixelTextPaint(Color.BLACK, width * 0.046f, Paint.Align.LEFT);
        drawWrappedText(
                canvas,
                finalWakeDialogueLines[finalWakeDialogueIndex],
                dialogueRect.left + width * 0.08f,
                dialogueRect.top + height * 0.07f,
                dialogueRect.width() - width * 0.16f,
                textPaint,
                textPaint.getTextSize() * 1.25f
        );
    }

    // ================= FINAL SCREEN =================
    // Draws final victory screen after all levels completed
    private void drawFinalScreen(Canvas canvas) {

        canvas.drawColor(Color.rgb(40, 0, 20));
        Paint border = new Paint();
        border.setColor(Color.MAGENTA);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(15);

        canvas.drawRect(20, 20,
                canvas.getWidth()-20,
                canvas.getHeight()-20,
                border);

        UIManager.drawCenterMessage(
                canvas,
                "KARLOS ESCAPED!!!",
                canvas.getWidth(),
                canvas.getHeight(),
                Color.GREEN
        );

        UIManager.drawSubtitle(
                canvas,
                "Michael Could Not Catch You!!!",
                900
        );

        UIManager.drawSubtitle(
                canvas,
                "Semester Survived YAYYYY",
                1050
        );
    }

    // ================= TOUCH =================
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:

                touchStartX = event.getX();
                touchStartY = event.getY();
                if (currentState == State.PLAYING && game instanceof CafeMaze) {
                    ((CafeMaze) game).onTouchStart(touchStartX, touchStartY);
                }

                return true;

            case MotionEvent.ACTION_MOVE:

                if (currentState == State.PLAYING && game instanceof CafeMaze) {
                    ((CafeMaze) game).onTouchMove(event.getX(), event.getY());
                }

                return true;

            case MotionEvent.ACTION_UP:

                float endX = event.getX();
                float endY = event.getY();

                handleTouch(
                        touchStartX,
                        touchStartY,
                        endX,
                        endY
                );

                return true;
        }

        return true;
    }

    // ================= HANDLE TOUCH =================
    private void handleTouch(
            float startX,
            float startY,
            float endX,
            float endY
    ) {

        // ================= MENU =================
        if (currentState == State.MENU) {

            if (startGameBounds.contains(endX, endY)) {
                playerHealth = MAX_PLAYER_HEALTH;
                if (TEST_UNLOCK_ALL_MINIGAMES) {
                    showCampusSelector();
                } else {
                    introDialogueIndex = 0;
                    currentState = State.INTRO_DIALOGUE;
                }
            }

            return;
        }

        // ================= INTRO DIALOGUE =================
        if (currentState == State.INTRO_DIALOGUE) {

            if (introDialogueIndex < introDialogueLines.length - 1) {
                introDialogueIndex++;
            } else {
                cafeWalkStartedAt = System.currentTimeMillis();
                currentState = State.CAFE_WALK;
            }

            return;
        }

        // ================= CAFE WALK =================
        if (currentState == State.CAFE_WALK) {
            if (System.currentTimeMillis() - cafeWalkStartedAt >= CAFE_WALK_DURATION_MS) {
                cafeDialogueIndex = 0;
                currentState = State.CAFE_INTRO;
            }
            return;
        }

        // ================= CAFE INTRO =================
        if (currentState == State.CAFE_INTRO) {

            if (cafeDialogueIndex < cafeDialogueLines.length - 1) {
                cafeDialogueIndex++;
            } else {
                insideCafeDialogueIndex = 0;
                currentState = State.INSIDE_CAFE;
            }

            return;
        }

        // ================= INSIDE CAFE =================
        if (currentState == State.INSIDE_CAFE) {

            if (insideCafeDialogueIndex < insideCafeDialogueLines.length - 1) {
                insideCafeDialogueIndex++;
            } else {
                currentState = State.MAZE_BRIEFING;
            }

            return;
        }

        // ================= MAZE BRIEFING =================
        if (currentState == State.MAZE_BRIEFING) {
            startGame(1);
            return;
        }

        // ================= POST MAZE CAFE DIALOGUE =================
        if (currentState == State.POST_MAZE_CAFE_DIALOGUE) {
            if (postMazeCafeDialogueIndex < postMazeCafeDialogueLines.length - 1) {
                postMazeCafeDialogueIndex++;
            } else {
                quadWalkStartedAt = System.currentTimeMillis();
                quadDialogueIndex = 0;
                quadExperimentDialogueIndex = 0;
                currentState = State.QUAD_WALK;
            }
            return;
        }

        // ================= QUAD WALK =================
        if (currentState == State.QUAD_WALK) {
            if (System.currentTimeMillis() - quadWalkStartedAt >= QUAD_WALK_DURATION_MS) {
                quadDialogueIndex = 0;
                quadExperimentDialogueIndex = 0;
                currentState = State.QUAD_DIALOGUE;
            }
            return;
        }

        // ================= QUAD DIALOGUE =================
        if (currentState == State.QUAD_DIALOGUE) {
            if (quadDialogueIndex < quadDialogueLines.length - 1) {
                quadDialogueIndex++;
            } else {
                quadExperimentDialogueIndex = 0;
                currentState = State.QUAD_EXPERIMENT_DIALOGUE;
            }
            return;
        }

        // ================= QUAD EXPERIMENT DIALOGUE =================
        if (currentState == State.QUAD_EXPERIMENT_DIALOGUE) {
            if (quadExperimentDialogueIndex < quadExperimentDialogueLines.length - 1) {
                quadExperimentDialogueIndex++;
            } else {
                currentState = State.WORDLE_BRIEFING;
            }
            return;
        }

        // ================= WORDLE BRIEFING =================
        if (currentState == State.WORDLE_BRIEFING) {
            startGame(2);
            return;
        }

        // ================= WORDLE UNLOCK SUCCESS =================
        if (currentState == State.WORDLE_UNLOCK_SUCCESS) {
            postWordleQuadDialogueIndex = 0;
            currentState = State.POST_WORDLE_QUAD_DIALOGUE;
            return;
        }

        // ================= POST WORDLE QUAD DIALOGUE =================
        if (currentState == State.POST_WORDLE_QUAD_DIALOGUE) {
            if (postWordleQuadDialogueIndex < postWordleQuadDialogueLines.length - 1) {
                postWordleQuadDialogueIndex++;
            } else {
                classroomWalkStartedAt = System.currentTimeMillis();
                classroomDialogueIndex = 0;
                currentState = State.CLASSROOM_WALK;
            }
            return;
        }

        // ================= CLASSROOM BRIEFING =================
        if (currentState == State.CLASSROOM_BRIEFING) {
            classroomWalkStartedAt = System.currentTimeMillis();
            classroomDialogueIndex = 0;
            currentState = State.CLASSROOM_WALK;
            return;
        }

        // ================= CLASSROOM WALK =================
        if (currentState == State.CLASSROOM_WALK) {
            if (System.currentTimeMillis() - classroomWalkStartedAt >= CLASSROOM_WALK_DURATION_MS) {
                classroomDialogueIndex = 0;
                currentState = State.CLASSROOM_DIALOGUE;
            }
            return;
        }

        // ================= CLASSROOM DIALOGUE =================
        if (currentState == State.CLASSROOM_DIALOGUE) {
            if (classroomDialogueIndex < classroomDialogueLines.length - 1) {
                classroomDialogueIndex++;
            } else {
                preRaceDialogueIndex = 0;
                currentState = State.PRE_RACE_DIALOGUE;
            }
            return;
        }

        // ================= PRE RACE DIALOGUE =================
        if (currentState == State.PRE_RACE_DIALOGUE) {
            if (preRaceDialogueIndex < preRaceDialogueLines.length - 1) {
                preRaceDialogueIndex++;
            } else {
                currentState = State.RACE_BRIEFING;
            }
            return;
        }

        // ================= RACE BRIEFING =================
        if (currentState == State.RACE_BRIEFING) {
            startGame(3);
            return;
        }

        // ================= CAMPUS =================
        if (currentState == State.CAMPUS) {

            int selectedLevel = campusMap.handleTouch(startX, startY);

            if (selectedLevel == 1 || selectedLevel == 2 || selectedLevel == 3) {

                startGame(selectedLevel);
                currentState = State.PLAYING;
            }

            return;
        }

        // ================= LEVEL COMPLETE =================
        if (currentState == State.LEVEL_COMPLETE) {

            nextLevel();
            return;
        }

        // ================= NIGHTMARE RETURN HOME =================
        if (currentState == State.NIGHTMARE_RETURN_HOME) {
            returnToHomePage();
            return;
        }

        // ================= GAME OVER =================
        if (currentState == State.GAME_OVER) {

            showNightmareReturnHomeMessage();
            return;
        }

        // ================= FINAL SEQUENCE =================
        if (currentState == State.FINAL_CONGRATULATIONS) {
            currentState = State.FINAL_NIGHTMARE_OVERCOME;
            return;
        }

        if (currentState == State.FINAL_NIGHTMARE_OVERCOME) {
            finalWakeDialogueIndex = 0;
            currentState = State.FINAL_WAKE_DIALOGUE;
            return;
        }

        if (currentState == State.FINAL_WAKE_DIALOGUE) {
            if (finalWakeDialogueIndex < finalWakeDialogueLines.length - 1) {
                finalWakeDialogueIndex++;
            } else {
                returnToHomePage();
            }
            return;
        }

        // ================= FINAL WIN =================
        if (currentState == State.FINAL_WIN) {

            playerHealth = MAX_PLAYER_HEALTH;
            currentState = State.MENU;
            return;
        }

        // ================= GAME INPUT =================
        if (currentState == State.PLAYING && exitButtonBounds.contains(endX, endY)) {
            returnToHomePage();
            return;
        }

        if (game == null) return;



        // CAFE MAZE
        if (game instanceof CafeMaze) {

            ((CafeMaze) game)
                    .onTouchStart(startX, startY);

            ((CafeMaze) game)
                    .onTouchEnd(endX, endY);
        }

        // RACE
        else if (game instanceof CampusRushMiniGame) {

            ((CampusRushMiniGame) game)
                    .onTouch(startX, startY, endX, endY);
        }
        else if (game instanceof com.example.campusescape.wordle.WordleMiniGame) {

            ((com.example.campusescape.wordle.WordleMiniGame) game)
                    .onTouch(endX, endY);
        }
    }

    // ================= RESUME =================
    public void resume() {

        if (isPlaying) return;

        isPlaying = true;

        handler.post(this);
    }

    // ================= PAUSE =================
    public void pause() {

        isPlaying = false;
        handler.removeCallbacks(this);
    }

    @Override
    public int getHealth() {
        return playerHealth;
    }

    @Override
    public int getMaxHealth() {
        return MAX_PLAYER_HEALTH;
    }

    @Override
    public void onGhostCollision() {
        playerHealth = Math.max(0, playerHealth - 1);
        if (playerHealth <= 0) {
            showGameOverScreen();
        }
    }

    private void showGameOverScreen() {
        game = null;
        currentState = State.GAME_OVER;
    }

    private void showNightmareReturnHomeMessage() {
        nightmareMessageStartedAt = System.currentTimeMillis();
        currentState = State.NIGHTMARE_RETURN_HOME;
    }

    private void returnToHomePage() {
        game = null;
        playerHealth = MAX_PLAYER_HEALTH;
        currentLevel = 1;
        introDialogueIndex = 0;
        cafeDialogueIndex = 0;
        insideCafeDialogueIndex = 0;
        postMazeCafeDialogueIndex = 0;
        quadDialogueIndex = 0;
        quadExperimentDialogueIndex = 0;
        postWordleQuadDialogueIndex = 0;
        classroomDialogueIndex = 0;
        preRaceDialogueIndex = 0;
        finalWakeDialogueIndex = 0;
        exitButtonBounds.setEmpty();
        currentState = State.MENU;
    }

    private void showCampusSelector() {
        if (campusMap == null) {
            campusMap = new CampusMap(getContext());
        }
        campusMap.setScreenSize(
                getWidth() > 0 ? getWidth() : 1080,
                getHeight() > 0 ? getHeight() : 1920
        );
        campusMap.unlockLevel(1);
        campusMap.unlockLevel(2);
        campusMap.unlockLevel(3);
        currentState = State.CAMPUS;
    }

}
