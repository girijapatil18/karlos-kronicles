package com.example.campusescape.wordle;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

import com.example.campusescape.MiniGame;

import java.util.Random;

public class WordleMiniGame implements MiniGame {

    private static final int BACKGROUND_COLOR = Color.rgb(56, 35, 36);
    private static final int PANEL_COLOR = Color.rgb(215, 181, 139);
    private static final int PANEL_SHADOW_COLOR = Color.rgb(184, 143, 103);
    private static final int EMPTY_TILE_COLOR = Color.rgb(246, 218, 181);
    private static final int TEXT_COLOR = Color.rgb(255, 244, 190);
    private static final int DARK_TEXT_COLOR = Color.rgb(45, 28, 29);
    private static final int CORRECT_COLOR = Color.rgb(98, 148, 91);
    private static final int PRESENT_COLOR = Color.rgb(197, 157, 72);
    private static final int WRONG_COLOR = Color.rgb(103, 92, 88);
    private static final int DELETE_COLOR = Color.rgb(169, 71, 65);
    private static final int ENTER_COLOR = Color.rgb(91, 139, 86);

    private final Context context;
    private final Random random = new Random();
    private final int maxRows = 6;
    private final String[] guesses = new String[maxRows];
    private final String row1 = "QWERTYUIOP";
    private final String row2 = "ASDFGHJKL";
    private final String row3 = "ZXCVBNM";
    private static final String[] WORD_BANK = {
            "CLASS",
            "BOOKS",
            "APPLE",
            "PIZZA",
            "MONEY",
            "CHAIR",
            "TABLE",
            "CLOUD",
            "LIGHT",
            "NIGHT",
            "DREAM",
            "MAZEY",
            "GHOST",
            "BRAIN",
            "CLOCK",
            "STUDY",
            "PAPER",
            "NOTES",
            "TEACH",
            "PHONE",
            "SLEEP",
            "PANIC",
            "QUADS",
            "CAFFE",
            "LATTE",
            "STONE",
            "FIELD",
            "GRASS",
            "WATER",
            "PLANT",
            "MUSIC",
            "MAGIC",
            "SOLVE",
            "LOGIC"
    };
    private static String lastTargetWord = "";

    private int screenWidth = 1080;
    private int screenHeight = 1920;
    private int currentRow = 0;
    private String targetWord = "CLASS";
    private String currentGuess = "";
    private boolean finished = false;
    private boolean levelComplete = false;
    private boolean showAnswer = false;

    private Typeface jerseyTypeface;

    private int safeTop;
    private int boardStartX;
    private int boardStartY;
    private int tileSize;
    private int tileGap;
    private int keyGap;
    private int keyHeight;
    private int keyRow1Y;
    private int keyRow2Y;
    private int keyRow3Y;
    private final Rect enterRect = new Rect();
    private final Rect deleteRect = new Rect();

    public WordleMiniGame(Context context) {
        this.context = context;
        try {
            jerseyTypeface = Typeface.createFromAsset(context.getAssets(), "fonts/Jersey25-Regular.ttf");
        } catch (RuntimeException exception) {
            jerseyTypeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
        }

        resetGuesses();
        chooseRandomWord();
        updateLayout();
    }

    private void chooseRandomWord() {
        String nextWord;

        do {
            nextWord = WORD_BANK[random.nextInt(WORD_BANK.length)];
        } while (WORD_BANK.length > 1 && nextWord.equals(lastTargetWord));

        targetWord = nextWord;
        lastTargetWord = targetWord;
    }

    @Override
    public void update() {
    }

    @Override
    public void draw(Canvas canvas) {
        updateLayout();
        canvas.drawColor(BACKGROUND_COLOR);

        Paint titlePaint = createTextPaint(TEXT_COLOR, screenWidth * 0.072f, Paint.Align.CENTER);
        Paint tileTextPaint = createTextPaint(DARK_TEXT_COLOR, tileSize * 0.78f, Paint.Align.CENTER);
        Paint keyTextPaint = createTextPaint(DARK_TEXT_COLOR, screenWidth * 0.062f, Paint.Align.CENTER);

        canvas.drawText("UNLOCK KARLOS' LAPTOP", screenWidth / 2f, safeTop + screenHeight * 0.075f, titlePaint);

        drawBoard(canvas, tileTextPaint);
        drawKeyboard(canvas, keyTextPaint);

        if (showAnswer) {
            Paint answerPaint = createTextPaint(Color.rgb(255, 198, 110), screenWidth * 0.055f, Paint.Align.CENTER);
            canvas.drawText("WORD WAS: " + targetWord, screenWidth / 2f, keyRow3Y - screenHeight * 0.025f, answerPaint);
        }

    }

    private void drawBoard(Canvas canvas, Paint textPaint) {
        Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(Math.max(3f, screenWidth * 0.006f));
        borderPaint.setColor(PANEL_SHADOW_COLOR);

        for (int row = 0; row < maxRows; row++) {
            for (int col = 0; col < 5; col++) {
                int left = boardStartX + col * (tileSize + tileGap);
                int top = boardStartY + row * (tileSize + tileGap);
                Rect rect = new Rect(left, top, left + tileSize, top + tileSize);

                tilePaint.setColor(getTileColor(row, col));
                canvas.drawRect(rect, tilePaint);
                canvas.drawRect(rect, borderPaint);

                String letter = getTileLetter(row, col);
                if (!letter.equals("")) {
                    Paint.FontMetrics metrics = textPaint.getFontMetrics();
                    float baseline = rect.centerY() - (metrics.ascent + metrics.descent) / 2f;
                    canvas.drawText(letter, rect.centerX(), baseline, textPaint);
                }
            }
        }
    }

    private int getTileColor(int row, int col) {
        if (guesses[row].equals("")) {
            return EMPTY_TILE_COLOR;
        }

        char guessedLetter = guesses[row].charAt(col);
        char targetLetter = targetWord.charAt(col);

        if (guessedLetter == targetLetter) {
            return CORRECT_COLOR;
        }
        if (targetWord.indexOf(guessedLetter) >= 0) {
            return PRESENT_COLOR;
        }
        return WRONG_COLOR;
    }

    private String getTileLetter(int row, int col) {
        if (row < currentRow && !guesses[row].equals("")) {
            return String.valueOf(guesses[row].charAt(col));
        }
        if (row == currentRow && col < currentGuess.length()) {
            return String.valueOf(currentGuess.charAt(col));
        }
        return "";
    }

    private void drawKeyboard(Canvas canvas, Paint textPaint) {
        Paint keyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        drawKeyboardRow(canvas, row1, keyRow1Y, keyPaint, textPaint);
        drawKeyboardRow(canvas, row2, keyRow2Y, keyPaint, textPaint);
        drawKeyboardRow(canvas, row3, keyRow3Y, keyPaint, textPaint);

        drawButton(canvas, enterRect, ENTER_COLOR, "ENTER", textPaint);
        drawButton(canvas, deleteRect, DELETE_COLOR, "DELETE", textPaint);
    }

    private void drawKeyboardRow(Canvas canvas, String row, int y, Paint keyPaint, Paint textPaint) {
        int keyWidth = getKeyWidth(row.length());
        int startX = (screenWidth - row.length() * keyWidth - (row.length() - 1) * keyGap) / 2;

        for (int i = 0; i < row.length(); i++) {
            Rect rect = new Rect(
                    startX + i * (keyWidth + keyGap),
                    y,
                    startX + i * (keyWidth + keyGap) + keyWidth,
                    y + keyHeight
            );
            drawButton(canvas, rect, PANEL_COLOR, String.valueOf(row.charAt(i)), textPaint);
        }
    }

    private void drawButton(Canvas canvas, Rect rect, int color, String label, Paint textPaint) {
        Paint buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        buttonPaint.setColor(color);
        buttonPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(rect, buttonPaint);

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(PANEL_SHADOW_COLOR);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(Math.max(2f, screenWidth * 0.004f));
        canvas.drawRect(rect, borderPaint);

        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = rect.centerY() - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(label, rect.centerX(), baseline, textPaint);
    }

    public void onTouch(float x, float y) {
        if (finished) return;

        checkKeyboardRow(row1, keyRow1Y, x, y);
        checkKeyboardRow(row2, keyRow2Y, x, y);
        checkKeyboardRow(row3, keyRow3Y, x, y);

        if (enterRect.contains((int) x, (int) y)) {
            submitGuess();
        } else if (deleteRect.contains((int) x, (int) y) && currentGuess.length() > 0) {
            currentGuess = currentGuess.substring(0, currentGuess.length() - 1);
        }
    }

    private void checkKeyboardRow(String row, int y, float x, float yTouch) {
        int keyWidth = getKeyWidth(row.length());
        int startX = (screenWidth - row.length() * keyWidth - (row.length() - 1) * keyGap) / 2;

        for (int i = 0; i < row.length(); i++) {
            Rect rect = new Rect(
                    startX + i * (keyWidth + keyGap),
                    y,
                    startX + i * (keyWidth + keyGap) + keyWidth,
                    y + keyHeight
            );

            if (rect.contains((int) x, (int) yTouch) && currentGuess.length() < 5) {
                currentGuess += row.charAt(i);
                return;
            }
        }
    }

    private void submitGuess() {
        if (currentGuess.length() != 5) return;

        guesses[currentRow] = currentGuess;

        if (currentGuess.equals(targetWord)) {
            levelComplete = true;
            finished = true;
            return;
        }

        currentRow++;
        currentGuess = "";

        if (currentRow >= maxRows) {
            finished = true;
            showAnswer = true;
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

    @Override
    public void start() {
        updateLayout();
    }

    @Override
    public void reset() {
        finished = false;
        levelComplete = false;
        showAnswer = false;
        currentGuess = "";
        currentRow = 0;
        chooseRandomWord();
        resetGuesses();
        updateLayout();
    }

    @Override
    public void setScreenSize(int width, int height) {
        screenWidth = width;
        screenHeight = height;
        updateLayout();
    }

    private void updateLayout() {
        safeTop = Math.max(getStatusBarHeight() + 22, screenHeight / 18);

        int horizontalPadding = Math.max(10, screenWidth / 70);
        int bottomPadding = Math.max(74, screenHeight / 22);
        int hudHeight = Math.max(230, screenHeight / 7);
        int hudBottom = safeTop + hudHeight;
        int availableWidth = screenWidth - horizontalPadding * 2;

        keyGap = Math.max(4, screenWidth / 110);
        keyHeight = Math.max(40, screenHeight / 28);
        int buttonHeight = Math.max(keyHeight, screenHeight / 27);
        int keyboardGap = Math.max(8, screenHeight / 120);
        int buttonGap = Math.max(10, screenHeight / 95);

        deleteRect.set(
                screenWidth - horizontalPadding - availableWidth * 29 / 100,
                screenHeight - bottomPadding - buttonHeight,
                screenWidth - horizontalPadding,
                screenHeight - bottomPadding
        );
        enterRect.set(
                horizontalPadding,
                deleteRect.top,
                horizontalPadding + availableWidth * 29 / 100,
                deleteRect.bottom
        );

        keyRow3Y = enterRect.top - buttonGap - keyHeight;
        keyRow2Y = keyRow3Y - keyboardGap - keyHeight;
        keyRow1Y = keyRow2Y - keyboardGap - keyHeight;

        int boardBottomLimit = keyRow1Y - Math.max(12, screenHeight / 105);
        int boardTop = hudBottom + Math.max(8, screenHeight / 140);
        tileGap = Math.max(5, screenWidth / 90);
        int maxTileWidth = (availableWidth - tileGap * 4) / 5;
        int maxTileHeight = (boardBottomLimit - boardTop - tileGap * (maxRows - 1)) / maxRows;
        tileSize = Math.max(40, Math.min(maxTileWidth, maxTileHeight));

        int boardWidth = tileSize * 5 + tileGap * 4;
        boardStartX = (screenWidth - boardWidth) / 2;
        boardStartY = boardBottomLimit - (tileSize * maxRows + tileGap * (maxRows - 1));
    }

    private int getKeyWidth(int rowLength) {
        int horizontalPadding = Math.max(10, screenWidth / 70);
        int availableWidth = screenWidth - horizontalPadding * 2;
        return Math.max(24, (availableWidth - keyGap * (rowLength - 1)) / rowLength);
    }

    private int getStatusBarHeight() {
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private Paint createTextPaint(int color, float size, Paint.Align align) {
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(color);
        textPaint.setTextAlign(align);
        textPaint.setTypeface(jerseyTypeface);
        textPaint.setTextSize(Math.max(24f, size));
        return textPaint;
    }

    private void resetGuesses() {
        for (int i = 0; i < maxRows; i++) {
            guesses[i] = "";
        }
    }
}
