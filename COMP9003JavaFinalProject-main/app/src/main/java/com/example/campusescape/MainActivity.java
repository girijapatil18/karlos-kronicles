package com.example.campusescape;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

// Main launcher activity
public class MainActivity extends AppCompatActivity {

    // Main game screen
    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Creates game view
        gameView = new GameView(this);

        // Sets game screen as main content
        setContentView(gameView);
    }

    // Resumes game when app opens
    @Override
    protected void onResume() {

        super.onResume();

        gameView.resume();
    }

    // Pauses game when app closes/minimizes
    @Override
    protected void onPause() {

        super.onPause();

        gameView.pause();
    }
}