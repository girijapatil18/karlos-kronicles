# 🎮 CampusEscape – The Karlos Chronicles

An Android-based multi-mini-game adventure developed using **Java and Android Studio**
CampusEscape follows Karlos, a student attempting to survive and escape a mysterious campus filled with challenges. The game combines maze exploration, action-based survival, and puzzle-solving mechanics into a single interactive experience.

---

# 📖 Game Story

Karlos finds himself trapped in a chaotic version of the university campus where every location presents a new challenge. To escape, he must complete multiple mini-games while avoiding obstacles, solving puzzles, and managing his limited health.

Each mini-game represents a different stage of Karlos' journey:

1. **Haunted Cafe Escape** – Navigate through a dangerous maze and escape the ghosts.
2. **CampusRush** – Run through campus obstacles while avoiding hazards.
3. **Wordle Challenge** – Solve puzzles to unlock the next stage of the escape.

Only by completing all challenges can Karlos successfully escape the campus.

---

# 🎮 Features

## 🏰 CafeMaze – Haunted Cafe Escape

A grid-based maze survival game inspired by classic maze navigation games.

Features:
- Swipe-based player movement
- Tile-based maze generation
- Enemy ghost movement
- Collision detection
- Health-based survival system
- Exit-based level completion

Gameplay:
- Navigate Karlos through the maze
- Avoid ghost encounters
- Reach the exit before losing all health

---

## 🏃 CampusRush – Campus Survival Run

A fast-paced action mini-game focused on movement and timing.

Features:
- Real-time gameplay updates
- Obstacle avoidance
- Player interaction handling
- Collision-based challenges

Gameplay:
- Move through the campus environment
- Avoid incoming obstacles
- Survive long enough to complete the challenge

---

## 🔤 Wordle MiniGame – Puzzle Challenge

A logic-based word puzzle inspired by word guessing games.

Features:
- Limited attempts system
- Letter validation
- Row-based guessing interface
- Win and failure conditions

Gameplay:
- Guess the hidden word
- Use feedback to improve guesses
- Complete the puzzle within available attempts

---

# 🏗️ Architecture

The project follows a modular Object-Oriented Programming design.

## MiniGame Interface

All mini-games implement a common `MiniGame` interface:
MiniGame
│
├── CafeMaze
├── CampusRush
└── WordleMiniGame

This allows the main game controller to interact with different games using the same structure.

Common functions include:

- `start()`
- `update()`
- `draw(Canvas canvas)`
- `reset()`
- `isFinished()`
- `isLevelComplete()`

---

# 🧠 Object-Oriented Design Principles Used

## Encapsulation
Each mini-game manages its own:
- gameplay logic
- state
- rendering
- input handling

## Abstraction
The `MiniGame` interface defines common behaviour without exposing internal implementation.

## Polymorphism
Different mini-games can be handled uniformly through the same interface.

## Dependency Injection
The health system uses `HealthDelegate` to separate health management from individual mini-games.

This improves:
- maintainability
- testability
- scalability

---

# ⚙️ Technology Stack

| Component | Technology |
|---|---|
| Language | Java |
| IDE | Android Studio |
| Platform | Android |
| Build System | Gradle |
| Rendering | Android Canvas API |
| Testing | JUnit, Mockito, Android Instrumentation Tests |

---

# 🎨 Game Implementation

## Rendering

The game uses Android Canvas rendering for:
- maze tiles
- player sprites
- enemies
- UI elements
- game overlays

Bitmap resources are used for:
- characters
- objects
- visual assets

---

## Input Handling

The game supports:
- swipe gestures
- touch interactions
- movement commands

Touch input is converted into player actions through custom movement logic.

---

## Game Loop

Each mini-game follows a continuous update cycle:
Input
↓
Game Update
↓
Collision Detection
↓
State Update
↓
Rendering


This provides smooth and responsive gameplay.

---

# 🧪 Testing

The project uses two levels of testing.

---

## Unit Tests

Location:
app/src/test/java/

Purpose:
- Validate core game logic without Android runtime

Covered areas:
- Initial game state
- Game completion states
- Reset functionality
- Health management
- Collision behaviour
- Game termination logic

Testing approach:
- JUnit-based tests
- Mockito for mocking Android dependencies
- Fake implementations for shared systems

---

## Android Instrumentation Tests

Location:
app/src/androidTest/java/


Purpose:
- Validate behaviour on real Android runtime

Covered areas:
- Game initialization
- Touch interactions
- Runtime updates
- UI and gameplay integration
- Device-level behaviour

---

# 📱 Running the Application

## Requirements

- Android Studio
- Android SDK 35+
- Minimum Android version: API 24

---

## Build APK

To generate the APK:
Build → Generate App Bundles or APKs → Generate APK


or using Gradle:

```bash
./gradlew assembleDebug
