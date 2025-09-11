# Hextris

A Tetris-like game with 6-block pieces.

## Description

Hextris is a variant of Tetris that uses 6-block pieces instead of the traditional 4-block pieces. The game follows the standard Tetris rules:

- Pieces fall from the top of the screen
- The player can move the pieces left, right, and down
- The player can rotate the pieces clockwise and counter-clockwise
- When a row is filled with blocks, it is cleared and the player scores points
- The game ends when the pieces stack up to the top of the screen

## Controls

### Keyboard

- **Left Arrow** or **A**: Move piece left
- **Right Arrow** or **D**: Move piece right
- **Down Arrow** or **S**: Move piece down (soft drop)
- **Up Arrow** or **L**: Rotate piece clockwise
- **Z** or **J**: Rotate piece counter-clockwise
- **K**: Rotate piece 180 degrees
- **Space**: Hard drop (instantly drop the piece to the bottom)
- **R**: Reset game

### Controller (SNES)

- **D-Pad Left** (Button 7): Move piece left
- **D-Pad Right** (Button 8): Move piece right
- **D-Pad Down** (Button 6): Move piece down (soft drop)
- **D-Pad Up** or **L Button** (Button 9): Rotate piece clockwise
- **B Button** (Button 1): Rotate piece counter-clockwise
- **X Button** (Button 3): Rotate piece 180 degrees
- **A Button** (Button 0): Hard drop (instantly drop the piece to the bottom)
- **Y Button** (Button 2) or **R Button** (Button 10): Reset game
- **SELECT + START** (Buttons 4 + 6): Reset game

## Features

- 6-block pieces
- Next piece preview
- Score, level, and lines tracking
- Histogram display showing the height of each column
- Game over detection and restart

## Implementation Details

The game is implemented using the Kengine game engine. The main components are:

- **HextrisGame**: The main game class that handles the game loop, input, and rendering
- **Board**: Represents the game board and handles the game logic
- **Piece**: Represents a game piece with methods for rotation and getting the blocks
- **PieceType**: Defines the different types of game pieces, each with 4 rotation states

The game uses the following assets:

- **block_sprites.png**: Contains the sprites for the game pieces
- **arcade_classic.ttf**: The font used for the game text

## Credits

This game is based on the "Blocks" game from the blocks_web repository.
