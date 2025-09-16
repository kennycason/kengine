// Advanced Tetris Game - JavaScript Implementation
// Based on the PureBasic "Blocks" game

class BlocksGame {
    constructor() {
        this.canvas = document.getElementById('gameCanvas');
        this.ctx = this.canvas.getContext('2d');
        this.nextCanvas = document.getElementById('nextPieceCanvas');
        this.nextCtx = this.nextCanvas.getContext('2d');
        
        // Game state
        this.mode = 2; // 0=TRITRIS, 1=TETRIS, 2=HEXTRIS
        this.width = 15;
        this.height = 25;
        this.blockSize = 24;
        this.board = [];
        this.currentPiece = null;
        this.nextPiece = null;
        this.pieceX = 0;
        this.pieceY = 0;
        this.score = 0;
        this.lines = 0;
        this.level = 0;
        this.playing = false;
        this.paused = false;
        this.gameOver = false;
        
        // Timing
        this.lastTime = 0;
        this.dropTime = 0;
        this.dropSpeed = 800; // milliseconds - much slower starting speed
        this.moveTime = 0;
        this.moveSpeed = 60; // Fast repeat rate
        this.initialMoveDelay = 110; // Short initial delay before repeat starts (slightly less sensitive)
        this.rotateTime = 0;
        this.rotateSpeed = 150;
        
        // Movement state tracking
        this.keysPressed = {
            left: false,
            right: false
        };
        this.lastMoveDirection = null;
        this.moveStartTime = 0;
        
        // Slide/Wall Kick system - easily configurable
        this.slideConfig = {
            enabled: true,
            maxSlideDistance: 3, // How far pieces can slide (in blocks) - increased from 2
            slideAttempts: [
                [0, 0],   // Try original position first
                [-1, 0],  // Try left
                [1, 0],   // Try right  
                [-2, 0],  // Try further left
                [2, 0],   // Try further right
                [0, -1],  // Try up
                [-1, -1], // Try left-up
                [1, -1],  // Try right-up
                [-3, 0],  // Try max left
                [3, 0],   // Try max right
                [0, -2],  // Try further up
                [-2, -1], // Try far left-up
                [2, -1],  // Try far right-up
                [-1, -2], // Try left-far up
                [1, -2]   // Try right-far up
            ]
        };
        
        // Piece statistics
        this.pieceStats = {};
        this.totalPieces = 0;
        
        // High scores system (keyed by mode+size)
        this.highScores = this.loadHighScores();
        
        // Special message display
        this.specialMessage = '';
        this.specialMessageTime = 0;
        
        // Gamepad support
        this.gamepadIndex = null;
        this.gamepadType = null; // 'snes-switch', '8bitdo-sn30-pro', 'fallback'
        this.gamepadButtons = {};
        this.gamepadAxes = {};
        this.lastGamepadState = {};
        this.lastGamepadAxes = {};
        this.gamepadDeadzone = 0.5;
        this.gamepadMoveDelay = 40; // 40ms delay for left/right continuous movement (was 50ms)
        this.lastGamepadMoveTime = 0;
        this.gamepadMoveState = {
            left: { pressed: false, startTime: 0, hasMoved: false },
            right: { pressed: false, startTime: 0, hasMoved: false }
        };
        this.gamepadInitialDelay = 200; // Delay before continuous movement starts (between 150ms and 250ms)
        this.gamepadMoveCooldown = 80; // Minimum time between any gamepad moves (prevents double-tap accidents)
        this.gamepadDownPressed = false; // Track if down is currently pressed
        this.originalDropSpeed = this.dropSpeed; // Store original drop speed
        
        // Environment evolution system
        this.evolutionProgress = 0; // 0-100% through complete evolution cycle
        this.maxEvolutionSteps = 1000; // 0.1% per piece, 1% per line (EPIC journey!)
        this.evolutionPhases = [
            { name: 'sunny-day', start: 0, end: 6 },
            { name: 'sunset', start: 5, end: 11 },
            { name: 'night', start: 10, end: 16 },
            { name: 'space', start: 15, end: 21 },
            { name: 'nebula-storm', start: 20, end: 26 },
            { name: 'alien-world', start: 25, end: 31 },
            { name: 'underwater-realm', start: 30, end: 36 },
            { name: 'volcanic-dimension', start: 35, end: 41 },
            { name: 'ice-age', start: 40, end: 46 },
            { name: 'geometric-void', start: 45, end: 51 },
            { name: 'spiral-dimension', start: 50, end: 56 },
            { name: 'digital-matrix', start: 55, end: 61 },
            { name: 'abstract-chaos', start: 60, end: 66 },
            { name: 'microscopic-world', start: 65, end: 71 },
            { name: 'galactic-core', start: 70, end: 76 },
            { name: 'dreamscape', start: 75, end: 81 },
            { name: 'crystal-realm', start: 80, end: 86 },
            { name: 'time-fracture', start: 85, end: 91 },
            { name: 'void-between-worlds', start: 90, end: 96 },
            { name: 'returning-dawn', start: 95, end: 100 },
            
            // Extended Journey (100-140%) - More variety before cycling
            { name: 'cosmic-awakening', start: 100, end: 106 },
            { name: 'stellar-nursery', start: 105, end: 111 },
            { name: 'quantum-realm', start: 110, end: 116 },
            { name: 'time-distortion', start: 115, end: 121 },
            { name: 'dimensional-shift', start: 120, end: 126 },
            { name: 'energy-singularity', start: 125, end: 131 },
            { name: 'universal-harmony', start: 130, end: 136 },
            { name: 'cosmic-reset', start: 135, end: 140 }
        ];
        
        // Sprite sheet for piece textures
        this.spriteSheet = null;
        this.spriteSize = 24; // Each sprite is 24x24 pixels
        this.spriteGridSize = 6; // 6x6 grid
        this.spritesLoaded = false;
        
        // Block colors (consistent colors for each piece type 1-29) - fallback if sprites fail
        this.blockColors = [
            '#000000', // 0 - empty
            '#FF0000', // 1 - Red
            '#00FF00', // 2 - Green  
            '#0000FF', // 3 - Blue
            '#FFFF00', // 4 - Yellow
            '#FF00FF', // 5 - Magenta
            '#00FFFF', // 6 - Cyan
            '#FFA500', // 7 - Orange
            '#800080', // 8 - Purple
            '#008000', // 9 - Dark Green
            '#800000', // 10 - Maroon
            '#008080', // 11 - Teal
            '#808080', // 12 - Gray
            '#C0C0C0', // 13 - Silver
            '#FFB6C1', // 14 - Light Pink
            '#FF69B4', // 15 - Hot Pink
            '#FF1493', // 16 - Deep Pink
            '#DC143C', // 17 - Crimson
            '#B22222', // 18 - Fire Brick
            '#8B0000', // 19 - Dark Red
            '#FF6347', // 20 - Tomato
            '#FF4500', // 21 - Orange Red
            '#FF8C00', // 22 - Dark Orange
            '#FFD700', // 23 - Gold
            '#ADFF2F', // 24 - Green Yellow
            '#32CD32', // 25 - Lime Green
            '#90EE90', // 26 - Light Green
            '#98FB98', // 27 - Pale Green
            '#F0E68C', // 28 - Khaki
            '#DDA0DD'  // 29 - Plum
        ];
        
        this.loadSpriteSheet();
        this.initGame();
        this.updateHighScoresDisplay();
        this.setupEventListeners();
        this.setupGamepadSupport();
        this.gameLoop();
    }
    
    // Controller configurations for different gamepad types
    getControllerConfig(gamepadId) {
        // Debug: Log the actual gamepad ID to help with detection
        console.log(`🔍 Detecting controller ID: "${gamepadId}"`);
        
        // Detect controller type based on gamepad ID string
        const id = gamepadId.toLowerCase();
        
        // SNES/Switch USB Controller (preserve existing working detection)
        if (id.includes('nintendo') || id.includes('switch') || 
            id.includes('snes') || id.includes('057e') ||
            id.includes('hori') || id.includes('pokken')) {
            console.log(`✅ SNES/Switch controller detected!`);
            return {
                type: 'snes-switch',
                name: 'SNES/Switch Controller',
                config: this.getSNESSwitchConfig()
            };
        }
        
        // Nintendo Switch Pro Controller (different from SNES mapping)
        else if (id.includes('pro controller') || id.includes('switch pro') ||
                (id.includes('vendor: 057e') && id.includes('product: 2009'))) {
            return {
                type: 'switch-pro',
                name: 'Nintendo Switch Pro Controller',
                config: this.getSwitchProConfig()
            };
        }
        
        // PlayStation 4 Controller (DualShock 4)
        else if (id.includes('ps4') || id.includes('playstation 4')) {
            return {
                type: 'ps4',
                name: 'PlayStation 4 Controller',
                config: this.getPS4Config()
            };
        }
        
        // PlayStation 5 Controller (DualSense)
        else if (id.includes('dualsense') || id.includes('ps5') || id.includes('playstation 5')) {
            return {
                type: 'ps5',
                name: 'PlayStation 5 DualSense Controller',
                config: this.getPS5Config()
            };
        }
        
        // 8BitDo SN30 Pro (check before Xbox since it might use XInput mode)
        else if (id.includes('8bitdo') || id.includes('sn30') || 
                id.includes('2dc8') || id.includes('8bitdo sn30 pro') ||
                (id.includes('045e') && id.includes('028e'))) { // 8BitDo in XInput mode
            console.log(`✅ 8BitDo SN30 Pro controller detected!`);
            return {
                type: '8bitdo-sn30-pro',
                name: '8BitDo SN30 Pro',
                config: this.get8BitDoSN30ProConfig()
            };
        }
        
        // Xbox Controllers (One, Series X/S, 360)
        else if (id.includes('xbox') || id.includes('microsoft') ||
                (id.includes('045e') && !id.includes('028e'))) { // Microsoft vendor ID but not 8BitDo
            console.log(`✅ Xbox controller detected!`);
            return {
                type: 'xbox',
                name: 'Xbox Controller',
                config: this.getXboxConfig()
            };
        }
        
        // Default fallback - use 8BitDo mapping for unknown controllers
        else {
            console.log(`⚠️ Unknown controller detected, using fallback mapping for: "${gamepadId}"`);
            return {
                type: 'fallback',
                name: 'Generic USB Controller (Standard mapping)',
                config: this.get8BitDoSN30ProConfig()
            };
        }
    }
    
    // SNES/Switch controller configuration (preserve existing working logic)
    getSNESSwitchConfig() {
        return {
            buttons: {
                rotateCW: [1, 5],      // A or R shoulder
                rotateCCW: [0, 4],     // B or L shoulder
                rotate180: [2, 3],     // Y or X
                pause: [9],            // Start
                select: [8]            // Select
            },
            dpad: {
                useAxes: true,
                axes: {
                    horizontal: 9,     // Axis 9 for horizontal movement
                    vertical: 10       // Axis 10 for vertical movement
                },
                axisMapping: {
                    // Axis 9 horizontal mapping (preserved from working code)
                    left: { min: 0.5, max: 1.0 },     // 0.71 value = LEFT
                    right: { min: -0.6, max: -0.2 },  // -0.4285714 = RIGHT
                    down: { min: 0.1, max: 0.3 }      // 0.14285719 = DOWN (soft drop)
                },
                // Axis 10 vertical mapping
                verticalAxis: {
                    down: { min: 0.3 },   // Down on vertical axis
                    up: { min: -0.3 }     // Up on vertical axis (negative)
                },
                // Standard D-pad buttons as fallback
                buttons: {
                    up: 12,
                    down: 13,
                    left: 14,
                    right: 15
                }
            }
        };
    }
    
    // 8BitDo SN30 Pro controller configuration (and fallback for unknown controllers)
    get8BitDoSN30ProConfig() {
        return {
            buttons: {
                rotateCW: [1, 5],      // A or R shoulder
                rotateCCW: [0, 4],     // B or L shoulder  
                rotate180: [2, 3],     // Y or X
                pause: [9],            // Start
                select: [8]            // Select
            },
            dpad: {
                useAxes: true,
                axes: {
                    horizontal: 0,     // Left stick horizontal for 8BitDo
                    vertical: 1        // Left stick vertical for 8BitDo
                },
                axisMapping: {
                    // Standard axis mapping for 8BitDo
                    left: { min: -1.0, max: -0.5 },
                    right: { min: 0.5, max: 1.0 },
                    down: { min: 0.5, max: 1.0 }      // Down on vertical axis
                },
                verticalAxis: {
                    down: { min: 0.5 },   // Down on vertical axis
                    up: { min: -0.5 }     // Up on vertical axis (negative)
                },
                // Standard D-pad buttons
                buttons: {
                    up: 12,
                    down: 13,
                    left: 14,
                    right: 15
                }
            }
        };
    }
    
    // Nintendo Switch Pro Controller configuration
    getSwitchProConfig() {
        return {
            buttons: {
                rotateCW: [1, 5],      // A or R shoulder
                rotateCCW: [0, 4],     // B or L shoulder  
                rotate180: [2, 3],     // Y or X
                pause: [9],            // Plus/Start
                select: [8]            // Minus/Select
            },
            dpad: {
                useAxes: true,
                axes: {
                    horizontal: 0,     // Left stick horizontal
                    vertical: 1        // Left stick vertical
                },
                axisMapping: {
                    left: { min: -1.0, max: -0.5 },
                    right: { min: 0.5, max: 1.0 },
                    down: { min: 0.5, max: 1.0 }
                },
                verticalAxis: {
                    down: { min: 0.5 },
                    up: { min: -0.5 }
                },
                // D-pad buttons (hat switch)
                buttons: {
                    up: 12,
                    down: 13,
                    left: 14,
                    right: 15
                }
            }
        };
    }
    
    // PlayStation 4 (DualShock 4) controller configuration
    getPS4Config() {
        return {
            buttons: {
                rotateCW: [1, 5],      // Circle or R1
                rotateCCW: [0, 4],     // Cross or L1
                rotate180: [2, 3],     // Square or Triangle
                pause: [9],            // Options
                select: [8]            // Share
            },
            dpad: {
                useAxes: true,
                axes: {
                    horizontal: 0,     // Left stick horizontal
                    vertical: 1        // Left stick vertical
                },
                axisMapping: {
                    left: { min: -1.0, max: -0.5 },
                    right: { min: 0.5, max: 1.0 },
                    down: { min: 0.5, max: 1.0 }
                },
                verticalAxis: {
                    down: { min: 0.5 },
                    up: { min: -0.5 }
                },
                // D-pad buttons
                buttons: {
                    up: 12,
                    down: 13,
                    left: 14,
                    right: 15
                }
            }
        };
    }
    
    // PlayStation 5 (DualSense) controller configuration
    getPS5Config() {
        return {
            buttons: {
                rotateCW: [1, 5],      // Circle or R1
                rotateCCW: [0, 4],     // Cross or L1
                rotate180: [2, 3],     // Square or Triangle
                pause: [9],            // Options
                select: [8]            // Create (was Share)
            },
            dpad: {
                useAxes: true,
                axes: {
                    horizontal: 0,     // Left stick horizontal
                    vertical: 1        // Left stick vertical
                },
                axisMapping: {
                    left: { min: -1.0, max: -0.5 },
                    right: { min: 0.5, max: 1.0 },
                    down: { min: 0.5, max: 1.0 }
                },
                verticalAxis: {
                    down: { min: 0.5 },
                    up: { min: -0.5 }
                },
                // D-pad buttons
                buttons: {
                    up: 12,
                    down: 13,
                    left: 14,
                    right: 15
                }
            }
        };
    }
    
    // Xbox Controller configuration (Xbox One, Series X/S, 360)
    getXboxConfig() {
        return {
            buttons: {
                rotateCW: [1, 5],      // B or RB
                rotateCCW: [0, 4],     // A or LB
                rotate180: [2, 3],     // X or Y
                pause: [9],            // Menu/Start
                select: [8]            // View/Back
            },
            dpad: {
                useAxes: true,
                axes: {
                    horizontal: 0,     // Left stick horizontal
                    vertical: 1        // Left stick vertical
                },
                axisMapping: {
                    left: { min: -1.0, max: -0.5 },
                    right: { min: 0.5, max: 1.0 },
                    down: { min: 0.5, max: 1.0 }
                },
                verticalAxis: {
                    down: { min: 0.5 },
                    up: { min: -0.5 }
                },
                // D-pad buttons
                buttons: {
                    up: 12,
                    down: 13,
                    left: 14,
                    right: 15
                }
            }
        };
    }
    
    loadSpriteSheet() {
        this.spriteSheet = new Image();
        this.spriteSheet.onload = () => {
            this.spritesLoaded = true;
            console.log('🖼️ Sprite sheet loaded successfully!');
        };
        this.spriteSheet.onerror = () => {
            console.warn('⚠️ Failed to load sprite sheet, falling back to solid colors');
            this.spritesLoaded = false;
        };
        this.spriteSheet.src = 'piece_sprites.png';
    }
    
    initGame() {
        this.resizeCanvas();
        this.initBoard();
        this.generateNextPiece();
        this.spawnNewPiece();
        this.updateUI();
        this.playing = true;
    }
    
    resizeCanvas() {
        this.canvas.width = this.width * this.blockSize;
        this.canvas.height = this.height * this.blockSize;
    }
    
    initBoard() {
        this.board = [];
        for (let y = 0; y < this.height; y++) {
            this.board[y] = [];
            for (let x = 0; x < this.width; x++) {
                this.board[y][x] = 0;
            }
        }
    }
    
    // Piece definitions based on the original PureBasic code
    getPieceDefinition(type, mode) {
        const pieces = {
            // TRITRIS mode pieces (3 blocks each - triominoes)
            tritris: [
                { image: 1, blocks: [[0,0], [0,0], [0,0]] }, // dot
                { image: 2, blocks: [[0,-1], [0,0], [0,1]] }, // line
                { image: 3, blocks: [[-1,0], [1,0], [1,0]] }, // spaced
                { image: 4, blocks: [[-1,0], [0,0], [0,1]] }, // L
                { image: 5, blocks: [[0,0], [1,0], [0,0]] }, // two line
                { image: 6, blocks: [[-1,-1], [0,0], [1,1]] }, // zig
                { image: 7, blocks: [[-1,-1], [0,0], [-1,1]] }, // bent
                { image: 8, blocks: [[0,0], [0,0], [0,0]] } // random
            ],
            
            // TETRIS mode pieces (standard Tetris - 4 blocks each - tetrominoes)
            tetris: [
                { image: 1, blocks: [[0,0], [1,0], [0,1], [1,1]] }, // square
                { image: 2, blocks: [[-1,1], [-1,0], [0,0], [1,0]] }, // L
                { image: 3, blocks: [[-1,0], [0,0], [1,0], [1,1]] }, // L-backwards
                { image: 4, blocks: [[-1,0], [0,0], [1,0], [2,0]] }, // line
                { image: 5, blocks: [[1,0], [0,0], [0,1], [-1,1]] }, // N
                { image: 6, blocks: [[1,1], [0,1], [0,0], [-1,0]] }, // N-backwards
                { image: 7, blocks: [[0,1], [-1,0], [0,0], [1,0]] } // T
            ],
            
            // HEXTRIS mode pieces (up to 6 blocks each - complex pieces/hexominoes)
            hextris: [
                { image: 1, blocks: [[0,0], [1,0], [0,1], [1,1]] }, // square
                { image: 2, blocks: [[-1,1], [-1,0], [0,0], [1,0]] }, // L
                { image: 3, blocks: [[-1,0], [0,0], [1,0], [1,1]] }, // L-backwards
                { image: 4, blocks: [[-1,0], [0,0], [1,0], [2,0]] }, // line
                { image: 5, blocks: [[1,0], [0,0], [0,1], [-1,1]] }, // N
                { image: 6, blocks: [[1,1], [0,1], [0,0], [-1,0]] }, // N-backwards
                { image: 7, blocks: [[0,1], [-1,0], [0,0], [1,0]] }, // T
                { image: 8, blocks: [[0,0]] }, // dot
                { image: 9, blocks: [[-1,-1], [-1,0], [0,0], [1,0], [1,1]] }, // screw
                { image: 10, blocks: [[1,-1], [1,0], [0,0], [-1,0], [-1,1]] }, // screw backwards
                { image: 11, blocks: [[-1,0], [0,0], [1,0], [0,-1], [0,1]] }, // long cross
                { image: 12, blocks: [[-1,0], [0,0], [1,0], [0,-1], [0,1], [2,0]] }, // cross
                { image: 13, blocks: [[-1,-1], [0,-1], [1,-1], [-1,1], [0,1], [1,1]] }, // layers
                { image: 14, blocks: [[-1,-1], [-1,0], [0,0], [0,1], [1,-1], [1,0]] }, // Y
                { image: 15, blocks: [[-1,0], [-1,1], [0,1], [1,0], [1,1]] }, // U
                { image: 16, blocks: [[-2,0], [-1,0], [0,0], [1,0], [2,0]] }, // 5 line
                { image: 17, blocks: [[-2,0], [-1,0], [0,0], [1,0], [2,0], [3,0]] }, // 6 line
                { image: 18, blocks: [[-1,0], [0,0], [1,0], [-1,1], [0,1], [1,1]] }, // 3x2 block
                { image: 19, blocks: [[-1,0], [0,1], [1,0]] }, // zig-zag
                { image: 20, blocks: [[-1,0], [0,0], [-1,1], [0,1], [1,0]] }, // 2x2 + notch top
                { image: 21, blocks: [[-1,0], [0,0], [-1,1], [0,1], [1,1]] }, // 2x2 + notch bottom
                { image: 22, blocks: [[0,0], [0,1]] }, // 2-line
                { image: 23, blocks: [[-1,-1], [0,-1], [1,-1], [0,0], [0,1]] }, // big T
                { image: 24, blocks: [[-1,0], [-1,1], [1,0], [1,1]] }, // short parallel
                { image: 25, blocks: [[-1,-1], [0,-1], [1,-1], [1,0], [1,1]] }, // big L backwards
                { image: 26, blocks: [[-2,0], [-1,0], [0,0], [1,0], [2,0], [0,1]] }, // TO
                { image: 27, blocks: [[-1,0], [0,0], [0,1]] }, // small L
                { image: 28, blocks: [[-1,0], [0,0], [1,0]] } // 3x1 line
            ]
        };
        
        const modeNames = ['tritris', 'tetris', 'hextris'];
        const modeKey = modeNames[mode];
        const pieceArray = pieces[modeKey];
        
        if (type >= pieceArray.length) {
            type = 0;
        }
        
        return pieceArray[type];
    }
    
    generateNextPiece() {
        let maxPieces;
        switch(this.mode) {
            case 0: maxPieces = 8; break;  // TRITRIS mode
            case 1: maxPieces = 7; break;  // TETRIS mode
            case 2: maxPieces = 28; break; // HEXTRIS mode
        }
        
        const type = Math.floor(Math.random() * maxPieces);
        this.nextPiece = this.getPieceDefinition(type, this.mode);
        this.nextPiece.type = type;
    }
    
    spawnNewPiece() {
        this.currentPiece = this.nextPiece;
        this.pieceX = Math.floor(this.width / 2);
        this.pieceY = 0;
        
        // Track piece statistics
        if (this.currentPiece) {
            const pieceType = this.currentPiece.type;
            this.pieceStats[pieceType] = (this.pieceStats[pieceType] || 0) + 1;
            this.totalPieces++;
            this.updateHistogram();
        }
        
        this.generateNextPiece();
        
        // Check if game over
        if (this.isCollision(this.currentPiece, this.pieceX, this.pieceY)) {
            this.gameOver = true;
            this.playing = false;
            this.checkAndUpdateHighScore();
            this.showGameOver();
        }
    }
    
    isCollision(piece, x, y) {
        for (let block of piece.blocks) {
            const blockX = x + block[0];
            const blockY = y + block[1];
            
            if (blockX < 0 || blockX >= this.width || 
                blockY >= this.height ||
                (blockY >= 0 && this.board[blockY][blockX] !== 0)) {
                return true;
            }
        }
        return false;
    }
    
    placePiece() {
        for (let block of this.currentPiece.blocks) {
            const blockX = this.pieceX + block[0];
            const blockY = this.pieceY + block[1];
            
            if (blockY >= 0) {
                this.board[blockY][blockX] = this.currentPiece.image;
            }
        }
        
        this.score += 30 + (10 * this.level);
        this.checkLines();
        this.evolveEnvironment(); // Evolve environment with each piece
        this.spawnNewPiece();
    }
    
    checkLines() {
        let linesCleared = 0;
        
        for (let y = this.height - 1; y >= 0; y--) {
            let fullLine = true;
            for (let x = 0; x < this.width; x++) {
                if (this.board[y][x] === 0) {
                    fullLine = false;
                    break;
                }
            }
            
            if (fullLine) {
                // Remove the line
                this.board.splice(y, 1);
                // Add empty line at top
                this.board.unshift(new Array(this.width).fill(0));
                linesCleared++;
                y++; // Check the same line again
            }
        }
        
        if (linesCleared > 0) {
            this.lines += linesCleared;
            this.updateScore(linesCleared);
            this.updateLevel();
            this.evolveEnvironmentByLines(linesCleared);
        }
    }
    
    updateScore(linesCleared) {
        let bonus = 0;
        let message = '';
        
        // Message arrays for each line clear count
        const messageArrays = {
            1: ['SINGLE!', 'GOOD!', 'WOAH!',  'ONE!'],
            2: ['DOUBLE!', 'X2!', 'TWO!', 'PAIR!', 'SWEET!'],
            3: ['TRIPLE!', 'WHAMMO!', 'THREE!', 'AMAZING!'],
            4: ['TETRIS!', 'AWESOME!', 'QUAD!', 'FOUR!', 'FANTASTIC!'],
            5: ['PENTRIS!', 'FIVE!', 'INCREDIBLE!', 'SUPER!'],
            6: ['HEXTRIS!', 'SIX!', 'ULTIMATE!', 'LEGENDARY!', 'EPIC!']
        };
        
        if (linesCleared === 1) {
            bonus = 150;
        } else if (linesCleared === 2) {
            bonus = 350;
        } else if (linesCleared === 3) {
            bonus = 2500;
        } else if (linesCleared === 4) {
            bonus = 5000;
        } else if (linesCleared === 5) {
            bonus = 10000;
        } else if (linesCleared >= 6) {
            bonus = 15000;
        }
        
        // Pick random message from appropriate array
        if (linesCleared >= 1) {
            const messages = messageArrays[Math.min(linesCleared, 6)];
            message = messages[Math.floor(Math.random() * messages.length)];
        }
        
        this.score += bonus;
        
        if (message) {
            this.showSpecialMessage(message, linesCleared);
        }
    }
    
    updateLevel() {
        const newLevel = Math.floor(this.lines / 10);
        if (newLevel > this.level) {
            this.level = newLevel;
            this.calculateDropSpeed();
            console.log(`📈 Level ${this.level}: Drop speed now ${this.dropSpeed}ms`);
        }
    }
    
    calculateDropSpeed() {
        // Calculate the correct drop speed for current level
        if (this.level <= 10) {
            // Levels 0-10: Decrease by 50ms each level
            this.dropSpeed = Math.max(300, 800 - (this.level * 50));
        } else if (this.level <= 20) {
            // Levels 11-20: Decrease by 20ms each level  
            this.dropSpeed = Math.max(100, 300 - ((this.level - 10) * 20));
        } else if (this.level <= 29) {
            // Levels 21-29: Very small decreases
            this.dropSpeed = Math.max(50, 100 - ((this.level - 20) * 5));
        } else {
            // Level 30+: Cap at reasonable speed
            this.dropSpeed = 50;
        }
    }
    
    showSpecialMessage(message, linesCleared = 0) {
        this.specialMessage = message;
        this.specialMessageTime = 1200; // Show for 1.2 seconds (200ms longer)
        
        const messageEl = document.getElementById('specialMessage');
        messageEl.textContent = message;
        messageEl.style.display = 'block';
        
        // Apply visual effects based on lines cleared
        this.applyMessageEffects(messageEl, linesCleared);
    }
    
    applyMessageEffects(element, linesCleared) {
        // Reset all effects first
        element.className = 'special-message';
        
        if (linesCleared === 1) {
            // Basic glow
            element.classList.add('effect-glow-basic');
        } else if (linesCleared === 2) {
            // Stronger glow with pulse
            element.classList.add('effect-glow-pulse');
        } else if (linesCleared === 3) {
            // Color changing glow
            element.classList.add('effect-glow-color');
        } else if (linesCleared === 4) {
            // Fireworks effect
            element.classList.add('effect-fireworks');
        } else if (linesCleared === 5) {
            // Intense fireworks with shake
            element.classList.add('effect-fireworks-intense');
        } else if (linesCleared >= 6) {
            // Rainbow ultimate effect
            element.classList.add('effect-rainbow-ultimate');
        }
    }
    
    rotatePiece(direction) {
        if (!this.currentPiece) return;
        
        const rotatedBlocks = this.currentPiece.blocks.map(block => {
            if (direction === 1) { // Rotate clockwise (90 degrees)
                return [-block[1], block[0]];
            } else if (direction === -1) { // Rotate counterclockwise (90 degrees)
                return [block[1], -block[0]];
            } else if (direction === 2) { // Rotate 180 degrees
                return [-block[0], -block[1]];
            }
        });
        
        const rotatedPiece = { ...this.currentPiece, blocks: rotatedBlocks };
        
        // Try rotation with slide/wall kick system
        if (this.tryPlacement(rotatedPiece, this.pieceX, this.pieceY)) {
            this.currentPiece = rotatedPiece;
        }
    }
    
    movePiece(dx, dy) {
        if (!this.currentPiece) return false;
        
        const newX = this.pieceX + dx;
        const newY = this.pieceY + dy;
        
        // For horizontal movement, try sliding if enabled
        if (dx !== 0 && this.slideConfig.enabled) {
            if (this.tryPlacement(this.currentPiece, newX, newY)) {
                return true;
            }
        } else {
            // Regular movement (vertical or slide disabled)
            if (!this.isCollision(this.currentPiece, newX, newY)) {
                this.pieceX = newX;
                this.pieceY = newY;
                return true;
            }
        }
        
        return false;
    }
    
    tryPlacement(piece, targetX, targetY) {
        if (!this.slideConfig.enabled) {
            // If sliding is disabled, just try the exact position
            if (!this.isCollision(piece, targetX, targetY)) {
                this.pieceX = targetX;
                this.pieceY = targetY;
                return true;
            }
            return false;
        }
        
        // Try each slide attempt in order of preference
        for (let attempt of this.slideConfig.slideAttempts) {
            const testX = targetX + attempt[0];
            const testY = targetY + attempt[1];
            
            // Skip if slide distance exceeds maximum
            if (Math.abs(attempt[0]) > this.slideConfig.maxSlideDistance || 
                Math.abs(attempt[1]) > this.slideConfig.maxSlideDistance) {
                continue;
            }
            
            if (!this.isCollision(piece, testX, testY)) {
                this.pieceX = testX;
                this.pieceY = testY;
                return true;
            }
        }
        
        return false;
    }
    
    dropPiece() {
        if (!this.movePiece(0, 1)) {
            this.placePiece();
        }
    }
    
    softDrop() {
        // Same as normal drop, just moves piece down one row
        this.dropPiece();
    }
    
    hardDrop() {
        // Drop piece as far down as possible instantly
        if (!this.currentPiece) return;
        
        let dropDistance = 0;
        while (this.movePiece(0, 1)) {
            dropDistance++;
        }
        
        // Place the piece immediately
        this.placePiece();
    }
    
    updateHistogram() {
        const histogramDiv = document.getElementById('pieceHistogram');
        if (!histogramDiv) return;
        
        histogramDiv.innerHTML = '';
        
        // Get max pieces for current mode
        let maxPieces;
        switch(this.mode) {
            case 0: maxPieces = 8; break;  // TRITRIS mode
            case 1: maxPieces = 7; break;  // TETRIS mode  
            case 2: maxPieces = 28; break; // HEXTRIS mode
        }
        
        // Create histogram entries for each piece type
        for (let i = 0; i < maxPieces; i++) {
            const count = this.pieceStats[i] || 0;
            const piece = this.getPieceDefinition(i, this.mode);
            
            const statDiv = document.createElement('div');
            statDiv.className = 'piece-stat';
            
            // Create mini canvas for piece preview
            const canvas = document.createElement('canvas');
            canvas.className = 'piece-preview';
            canvas.width = 62;
            canvas.height = 34;
            const ctx = canvas.getContext('2d');
            
            // Draw piece preview
            ctx.fillStyle = '#333';
            ctx.fillRect(0, 0, 62, 34);
            
            if (piece && piece.blocks) {
                const blockSize = 9; // Slightly larger blocks for bigger preview
                const centerX = 31; // Adjusted for wider canvas (62/2 = 31)
                const centerY = 17; // Adjusted for taller canvas (34/2 = 17)
                
                for (let block of piece.blocks) {
                    const x = centerX + (block[0] * blockSize) - blockSize/2;
                    const y = centerY + (block[1] * blockSize) - blockSize/2;
                    
                    if (this.spritesLoaded && this.spriteSheet) {
                        // Draw from sprite sheet
                        this.drawSprite(ctx, x, y, piece.image, blockSize);
                    } else {
                        // Fallback to solid colors
                        ctx.fillStyle = this.blockColors[piece.image] || '#666';
                        ctx.fillRect(x, y, blockSize, blockSize);
                        
                        // Add border for better visibility
                        ctx.strokeStyle = '#fff';
                        ctx.lineWidth = 0.5;
                        ctx.strokeRect(x, y, blockSize, blockSize);
                    }
                }
            }
            
            // Create count display
            const countSpan = document.createElement('span');
            countSpan.className = 'piece-count';
            countSpan.textContent = `×${count}`;
            
            statDiv.appendChild(canvas);
            statDiv.appendChild(countSpan);
            histogramDiv.appendChild(statDiv);
        }
    }
    
    draw() {
        // Clear canvas
        this.ctx.fillStyle = '#000';
        this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);
        
        // Draw board
        for (let y = 0; y < this.height; y++) {
            for (let x = 0; x < this.width; x++) {
                if (this.board[y][x] !== 0) {
                    this.drawBlock(x, y, this.board[y][x]);
                }
            }
        }
        
        // Draw current piece
        if (this.currentPiece && !this.gameOver) {
            for (let block of this.currentPiece.blocks) {
                const x = this.pieceX + block[0];
                const y = this.pieceY + block[1];
                if (y >= 0) {
                    this.drawBlock(x, y, this.currentPiece.image);
                }
            }
        }
        
        // Draw next piece
        this.drawNextPiece();
        
        // Update special message
        if (this.specialMessageTime > 0) {
            this.specialMessageTime -= 16; // Assuming 60 FPS
            if (this.specialMessageTime <= 0) {
                document.getElementById('specialMessage').style.display = 'none';
            }
        }
    }
    
    drawBlock(x, y, colorIndex) {
        const pixelX = x * this.blockSize;
        const pixelY = y * this.blockSize;
        
        if (this.spritesLoaded && this.spriteSheet && colorIndex > 0) {
            // Draw from sprite sheet
            this.drawSprite(this.ctx, pixelX, pixelY, colorIndex, this.blockSize);
        } else {
            // Fallback to solid colors
            this.ctx.fillStyle = this.blockColors[colorIndex] || '#666';
            this.ctx.fillRect(pixelX, pixelY, this.blockSize, this.blockSize);
            
            // Add border for better visibility
            this.ctx.strokeStyle = '#fff';
            this.ctx.lineWidth = 1;
            this.ctx.strokeRect(pixelX, pixelY, this.blockSize, this.blockSize);
        }
    }
    
    drawSprite(ctx, destX, destY, spriteIndex, destSize) {
        if (!this.spriteSheet || spriteIndex <= 0) return;
        
        // Calculate source position in sprite sheet (1-based to 0-based)
        const gridIndex = spriteIndex - 1;
        const srcX = (gridIndex % this.spriteGridSize) * this.spriteSize;
        const srcY = Math.floor(gridIndex / this.spriteGridSize) * this.spriteSize;
        
        // Draw the sprite scaled to the destination size
        ctx.drawImage(
            this.spriteSheet,
            srcX, srcY, this.spriteSize, this.spriteSize,  // Source rectangle
            destX, destY, destSize, destSize              // Destination rectangle
        );
    }
    
    drawNextPiece() {
        this.nextCtx.fillStyle = '#333';
        this.nextCtx.fillRect(0, 0, this.nextCanvas.width, this.nextCanvas.height);
        
        if (this.nextPiece) {
            const centerX = this.nextCanvas.width / 2;
            const centerY = this.nextCanvas.height / 2;
            const blockSize = 16;
            
            for (let block of this.nextPiece.blocks) {
                const x = centerX + (block[0] * blockSize) - blockSize/2;
                const y = centerY + (block[1] * blockSize) - blockSize/2;
                
                if (this.spritesLoaded && this.spriteSheet) {
                    // Draw from sprite sheet
                    this.drawSprite(this.nextCtx, x, y, this.nextPiece.image, blockSize);
                } else {
                    // Fallback to solid colors
                    this.nextCtx.fillStyle = this.blockColors[this.nextPiece.image] || '#666';
                    this.nextCtx.fillRect(x, y, blockSize, blockSize);
                    
                    this.nextCtx.strokeStyle = '#fff';
                    this.nextCtx.lineWidth = 1;
                    this.nextCtx.strokeRect(x, y, blockSize, blockSize);
                }
            }
        }
    }
    
    updateUI() {
        document.getElementById('currentLevel').textContent = this.level;
        document.getElementById('currentLines').textContent = this.lines;
        document.getElementById('currentScore').textContent = this.score;
        

    }
    
    getSizeKey() {
        if (this.width === 10 && this.height === 10) return 'small';
        if (this.width === 10 && this.height === 20) return 'medium';
        return 'big';
    }
    
    resetHighScores() {
        if (confirm('Are you sure you want to reset all high scores?')) {
            this.highScores = {}; // Reset to empty object for keyed system
            localStorage.removeItem('blocksHighScores');
            this.updateHighScoresDisplay();
        }
    }
    
    newGame() {
        this.gameOver = false;
        this.paused = false;
        this.score = 0;
        this.lines = 0;
        this.level = 0;
        this.dropSpeed = 800;
        this.pieceStats = {};
        this.totalPieces = 0;
        
        // Reset gamepad state
        this.gamepadDownPressed = false;
        this.originalDropSpeed = this.dropSpeed;
        
        // Reset gamepad movement state
        this.gamepadMoveState.left = { pressed: false, startTime: 0, hasMoved: false };
        this.gamepadMoveState.right = { pressed: false, startTime: 0, hasMoved: false };
        
        // Reset environment evolution
        this.evolutionProgress = 0;
        this.updateEnvironmentMorphing(); // Start with sunny day
        
        document.getElementById('gameOverOverlay').style.display = 'none';
        document.getElementById('pauseOverlay').style.display = 'none';
        this.initGame();
        this.updateHistogram();
    }
    
    togglePause() {
        if (this.gameOver) return;
        
        this.paused = !this.paused;
        document.getElementById('pauseOverlay').style.display = this.paused ? 'block' : 'none';
    }
    
    showGameOver() {
        document.getElementById('gameOverOverlay').style.display = 'block';
    }
    
    changeMode(newMode) {
        this.mode = parseInt(newMode);
        this.updateHighScoresDisplay(); // Update display for new mode+size combination
        this.newGame();
    }
    
    changeBoardSize(size) {
        switch(size) {
            case 'small':
                this.width = 10;
                this.height = 10;
                break;
            case 'medium':
                this.width = 10;
                this.height = 20;
                break;
            case 'big':
                this.width = 15;
                this.height = 25;
                break;
        }
        this.resizeCanvas();
        this.updateHighScoresDisplay(); // Update display for new mode+size combination
        this.newGame();
    }
    
    evolveEnvironment() {
        // Advance evolution by 0.1% each piece (slow, steady progress)
        this.evolutionProgress += 0.2;
        
        // Cycle back to beginning after 140% (extended journey)
        if (this.evolutionProgress >= 140) {
            this.evolutionProgress = 0;
        }
        
        this.updateEnvironmentMorphing();
    }
    
    evolveEnvironmentByLines(linesCleared) {
        // Advance evolution by 1% per line cleared (major progress!)
        this.evolutionProgress += linesCleared * 1.0;
        
        // Cycle back to beginning after 140% (extended journey)
        if (this.evolutionProgress >= 140) {
            this.evolutionProgress = 0;
        }
        
        this.updateEnvironmentMorphing();
    }
    
    updateEnvironmentMorphing() {
        const progress = this.evolutionProgress;
        
        // Update each element based on current progress
        this.morphSun(progress);
        this.morphSky(progress);
        this.morphClouds(progress);
        this.morphSurface(progress);
        this.morphAtmosphere(progress);
        this.morphAbstractElements(progress);
        
        console.log(`🌍 Environment evolution: ${progress.toFixed(1)}%`);
    }
    
    morphSun(progress) {
        const sun = document.querySelector('.sun');
        if (!sun) return;
        
        // Sun transforms: Sun -> Moon -> Planet -> Star -> Sun
        let hue = 45; // Golden yellow
        let size = 80;
        let brightness = 1;
        let position = { top: 5, right: 8 };
        
        if (progress < 25) {
            // Sunny day: Bright yellow/gold sun
            hue = 45;
            brightness = 1;
        } else if (progress < 50) {
            // Sunset: Orange to red
            const t = (progress - 25) / 25;
            hue = 45 - (t * 30); // 45 to 15 (orange to red)
            position.top = 5 + (t * 20); // Lower in sky
        } else if (progress < 75) {
            // Night: Moon (gray/silver)
            const t = (progress - 50) / 25;
            hue = 0;
            brightness = 0.3 + (t * 0.4); // Dimmer, silvery
            size = 60 + (t * 20); // Smaller to larger
            position.top = 15 + (t * 10);
        } else {
            // Space: Bright star
            const t = (progress - 75) / 25;
            hue = 200 + (t * 200); // Blue to white
            brightness = 1.5;
            size = 40 + (t * 40); // Small bright star
            position.top = 5;
            position.right = 8 + (t * 15);
        }
        
        sun.style.filter = `hue-rotate(${hue}deg) brightness(${brightness})`;
        sun.style.width = `${size}px`;
        sun.style.height = `${size}px`;
        sun.style.top = `${position.top}%`;
        sun.style.right = `${position.right}%`;
    }
    
    morphSky(progress) {
        const body = document.body;
        let gradient;
        
        if (progress < 12) {
            // Sunny Day: Blue sky
            gradient = `linear-gradient(to bottom, 
                #87CEEB 0%, #98D8E8 30%, #B6E5F0 60%, #32CD32 85%, #228B22 100%)`;
        } else if (progress < 22) {
            // Sunset: Orange/pink sky
            const t = (progress - 12) / 10;
            gradient = `linear-gradient(to bottom, 
                ${this.lerpColor('#87CEEB', '#FF6B35', t)} 0%, 
                ${this.lerpColor('#98D8E8', '#FF8E53', t)} 30%, 
                ${this.lerpColor('#B6E5F0', '#FFB07A', t)} 60%, 
                #32CD32 85%, #228B22 100%)`;
        } else if (progress < 32) {
            // Night: Dark sky
            const t = (progress - 22) / 10;
            gradient = `linear-gradient(to bottom, 
                ${this.lerpColor('#FF6B35', '#191970', t)} 0%, 
                ${this.lerpColor('#FF8E53', '#1E1E3F', t)} 30%, 
                ${this.lerpColor('#FFB07A', '#0F0F2A', t)} 60%, 
                #1A4E1A 85%, #0F2F0F 100%)`;
        } else if (progress < 42) {
            // Space: Deep space
            const t = (progress - 32) / 10;
            gradient = `radial-gradient(ellipse at center, 
                ${this.lerpColor('#191970', '#1a0033', t)} 0%, 
                ${this.lerpColor('#1E1E3F', '#0d0019', t)} 30%, 
                ${this.lerpColor('#0F0F2A', '#000000', t)} 60%, 
                #000000 100%)`;
        } else if (progress < 52) {
            // Nebula Storm: Purple/magenta space
            const t = (progress - 42) / 10;
            gradient = `radial-gradient(ellipse at center, 
                ${this.lerpColor('#1a0033', '#4B0082', t)} 0%, 
                ${this.lerpColor('#0d0019', '#8B008B', t)} 30%, 
                ${this.lerpColor('#000000', '#4B0082', t)} 60%, 
                #1a0033 100%)`;
        } else if (progress < 62) {
            // Geometric Void: Sharp contrasts
            const t = (progress - 52) / 10;
            gradient = `conic-gradient(from 0deg, 
                ${this.lerpColor('#4B0082', '#000000', t)}, 
                ${this.lerpColor('#8B008B', '#FFFFFF', t)}, 
                ${this.lerpColor('#4B0082', '#000000', t)}, 
                ${this.lerpColor('#1a0033', '#808080', t)})`;
        } else if (progress < 72) {
            // Spiral Dimension: Swirling patterns
            const t = (progress - 62) / 10;
            gradient = `conic-gradient(from ${t * 360}deg, 
                #FF00FF, #00FFFF, #FFFF00, #FF0000, #0000FF, #00FF00, #FF00FF)`;
        } else if (progress < 82) {
            // Abstract Chaos: Wild colors
            const t = (progress - 72) / 10;
            gradient = `radial-gradient(ellipse at ${20 + t * 60}% ${30 + t * 40}%, 
                #FF1493 0%, #00CED1 30%, #FFD700 60%, #8A2BE2 100%)`;
        } else if (progress < 92) {
            // Crystal Realm: Crystalline patterns
            const t = (progress - 82) / 10;
            gradient = `linear-gradient(${t * 180}deg, 
                #E6E6FA 0%, #DDA0DD 25%, #DA70D6 50%, #C71585 75%, #B0E0E6 100%)`;
        } else if (progress < 106) {
            // Cosmic Awakening: Bright cosmic energy
            const t = (progress - 100) / 6;
            gradient = `radial-gradient(ellipse at center, 
                ${this.lerpColor('#E6E6FA', '#FFD700', t)} 0%, 
                ${this.lerpColor('#DDA0DD', '#FFA500', t)} 30%, 
                ${this.lerpColor('#DA70D6', '#FF6347', t)} 60%, 
                ${this.lerpColor('#C71585', '#FF4500', t)} 80%, 
                ${this.lerpColor('#B0E0E6', '#8B0000', t)} 100%)`;
        } else if (progress < 116) {
            // Stellar Nursery: Star birth regions
            const t = (progress - 106) / 10;
            gradient = `radial-gradient(circle at 30% 70%, 
                ${this.lerpColor('#FFD700', '#FF69B4', t)} 0%, 
                ${this.lerpColor('#FFA500', '#FF1493', t)} 25%, 
                ${this.lerpColor('#FF6347', '#8A2BE2', t)} 50%, 
                ${this.lerpColor('#FF4500', '#4B0082', t)} 75%, 
                ${this.lerpColor('#8B0000', '#191970', t)} 100%)`;
        } else if (progress < 126) {
            // Quantum Realm: Particle physics
            const t = (progress - 116) / 10;
            gradient = `conic-gradient(from 45deg, 
                ${this.lerpColor('#FF69B4', '#00FFFF', t)}, 
                ${this.lerpColor('#FF1493', '#00FF00', t)}, 
                ${this.lerpColor('#8A2BE2', '#FFFF00', t)}, 
                ${this.lerpColor('#4B0082', '#FF00FF', t)}, 
                ${this.lerpColor('#191970', '#00FFFF', t)})`;
        } else if (progress < 136) {
            // Time Distortion & Dimensional Shift: Reality bending
            const t = (progress - 126) / 10;
            gradient = `linear-gradient(${progress * 3}deg, 
                ${this.lerpColor('#00FFFF', '#000000', t)} 0%, 
                ${this.lerpColor('#00FF00', '#FFFFFF', t)} 25%, 
                ${this.lerpColor('#FFFF00', '#808080', t)} 50%, 
                ${this.lerpColor('#FF00FF', '#C0C0C0', t)} 75%, 
                ${this.lerpColor('#00FFFF', '#000000', t)} 100%)`;
        } else {
            // Cosmic Reset: Return to beginning
            const t = (progress - 136) / 4;
            gradient = `linear-gradient(to bottom, 
                ${this.lerpColor('#000000', '#87CEEB', t)} 0%, 
                ${this.lerpColor('#FFFFFF', '#98D8E8', t)} 30%, 
                ${this.lerpColor('#808080', '#B6E5F0', t)} 60%, 
                ${this.lerpColor('#C0C0C0', '#32CD32', t)} 85%, 
                ${this.lerpColor('#000000', '#228B22', t)} 100%)`;
        }
        
        body.style.background = gradient;
    }
    
    morphClouds(progress) {
        const clouds = document.querySelectorAll('.cloud');
        
        clouds.forEach((cloud, index) => {
            if (progress < 50) {
                // Day/sunset: Visible clouds
                cloud.style.opacity = '0.8';
                cloud.style.display = 'block';
                
                if (progress > 25) {
                    // Sunset: Orange tint
                    const t = (progress - 25) / 25;
                    cloud.style.filter = `hue-rotate(${t * 30}deg) brightness(${1 + t * 0.5})`;
                }
            } else if (progress < 75) {
                // Night: Fade out clouds
                const t = (progress - 50) / 25;
                cloud.style.opacity = `${0.8 - t * 0.8}`;
            } else {
                // Space: Hide clouds
                cloud.style.display = 'none';
            }
        });
    }
    
    morphSurface(progress) {
        const hills = document.querySelector('.hills');
        const grass = document.querySelector('.grass');
        
        if (progress < 75) {
            // Land visible
            if (hills) hills.style.display = 'block';
            if (grass) grass.style.display = 'block';
            
            if (progress > 50) {
                // Night: Darken surface
                const t = (progress - 50) / 25;
                if (hills) hills.style.filter = `brightness(${1 - t * 0.6})`;
                if (grass) grass.style.filter = `brightness(${1 - t * 0.6})`;
            }
        } else {
            // Space: Hide surface
            if (hills) hills.style.display = 'none';
            if (grass) grass.style.display = 'none';
        }
    }
    
    morphAtmosphere(progress) {
        const stars = document.querySelector('.stars');
        const nebula = document.querySelector('.nebula');
        const galaxy = document.querySelector('.galaxy-center');
        
        if (progress < 50) {
            // Day/sunset: Hide space elements
            if (stars) stars.style.display = 'none';
            if (nebula) nebula.style.display = 'none';
            if (galaxy) galaxy.style.display = 'none';
        } else if (progress < 75) {
            // Night: Show stars
            if (stars) {
                stars.style.display = 'block';
                const t = (progress - 50) / 25;
                stars.style.opacity = `${t * 0.8}`;
            }
            if (nebula) nebula.style.display = 'none';
            if (galaxy) galaxy.style.display = 'none';
        } else {
            // Space: Show all space elements
            if (stars) {
                stars.style.display = 'block';
                stars.style.opacity = '1';
            }
            if (nebula) {
                nebula.style.display = 'block';
                const t = (progress - 75) / 25;
                nebula.style.opacity = `${t * 0.5}`;
            }
            if (galaxy) {
                galaxy.style.display = 'block';
                const t = (progress - 75) / 25;
                galaxy.style.opacity = `${t * 0.8}`;
            }
        }
    }
    
    lerpColor(color1, color2, t) {
        // Linear interpolation between two hex colors
        const hex1 = color1.replace('#', '');
        const hex2 = color2.replace('#', '');
        
        const r1 = parseInt(hex1.substr(0, 2), 16);
        const g1 = parseInt(hex1.substr(2, 2), 16);
        const b1 = parseInt(hex1.substr(4, 2), 16);
        
        const r2 = parseInt(hex2.substr(0, 2), 16);
        const g2 = parseInt(hex2.substr(2, 2), 16);
        const b2 = parseInt(hex2.substr(4, 2), 16);
        
        const r = Math.round(r1 + (r2 - r1) * t);
        const g = Math.round(g1 + (g2 - g1) * t);
        const b = Math.round(b1 + (b2 - b1) * t);
        
        return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`;
    }
    
    morphAbstractElements(progress) {
        // Get all abstract elements
        const geometric = document.querySelector('.geometric-patterns');
        const spirals = document.querySelectorAll('.spiral-1, .spiral-2, .spiral-3');
        const chaos = document.querySelector('.chaos-particles');
        const crystals = document.querySelector('.crystal-formations');
        const energy = document.querySelector('.energy-waves');
        const portal = document.querySelector('.portal-rings');
        
        // Get new spectacular elements
        const lightning = document.querySelector('.lightning-storm');
        const aurora = document.querySelector('.aurora-borealis');
        const meteors = document.querySelector('.meteor-shower');
        const rifts = document.querySelector('.dimensional-rifts');
        const plasma = document.querySelector('.plasma-field');
        const tentacles = document.querySelector('.void-tentacles');
        const fractals = document.querySelector('.fractal-trees');
        const timeWarp = document.querySelector('.time-distortion');
        
        // Hide all abstract elements by default
        [geometric, chaos, crystals, energy, portal, lightning, aurora, meteors, 
         rifts, plasma, tentacles, fractals, timeWarp, ...spirals].forEach(el => {
            if (el) el.style.display = 'none';
        });
        
        // Weather effects during early phases
        if (progress >= 15 && progress < 35) {
            // Lightning storms during sunset/night transition
            if (lightning) {
                lightning.style.display = 'block';
                const t = (progress - 15) / 20;
                lightning.style.opacity = `${0.3 + Math.sin(t * Math.PI) * 0.4}`;
            }
        }
        
        if (progress >= 25 && progress < 45) {
            // Aurora during night/space transition
            if (aurora) {
                aurora.style.display = 'block';
                const t = (progress - 25) / 20;
                aurora.style.opacity = `${0.4 + t * 0.5}`;
            }
        }
        
        if (progress >= 35 && progress < 55) {
            // Meteor shower in space
            if (meteors) {
                meteors.style.display = 'block';
                const t = (progress - 35) / 20;
                meteors.style.opacity = `${0.5 + Math.sin(t * Math.PI * 2) * 0.3}`;
            }
        }
        
        if (progress >= 50 && progress < 62) {
            // Geometric Void phase with dimensional rifts
            if (geometric) {
                geometric.style.display = 'block';
                const t = (progress - 50) / 12;
                geometric.style.opacity = `${Math.sin(t * Math.PI) * 0.8}`;
            }
            if (rifts) {
                rifts.style.display = 'block';
                const t = (progress - 50) / 12;
                rifts.style.opacity = `${0.4 + t * 0.4}`;
            }
        } else if (progress >= 60 && progress < 72) {
            // Spiral Dimension phase with time distortion
            spirals.forEach((spiral, index) => {
                if (spiral) {
                    spiral.style.display = 'block';
                    const t = (progress - 60) / 12;
                    spiral.style.opacity = `${0.3 + Math.sin(t * Math.PI + index) * 0.5}`;
                    spiral.style.transform = `scale(${0.5 + t * 0.8}) rotate(${t * 720 + index * 120}deg)`;
                }
            });
            if (timeWarp) {
                timeWarp.style.display = 'block';
                const t = (progress - 60) / 12;
                timeWarp.style.opacity = `${0.2 + t * 0.5}`;
            }
        } else if (progress >= 70 && progress < 82) {
            // Abstract Chaos phase with plasma and void tentacles
            if (chaos) {
                chaos.style.display = 'block';
                const t = (progress - 70) / 12;
                chaos.style.opacity = `${0.5 + Math.sin(t * Math.PI * 3) * 0.4}`;
                chaos.style.transform = `scale(${0.8 + Math.sin(t * Math.PI * 2) * 0.4})`;
            }
            if (plasma) {
                plasma.style.display = 'block';
                const t = (progress - 70) / 12;
                plasma.style.opacity = `${0.4 + Math.sin(t * Math.PI * 1.5) * 0.3}`;
            }
            if (tentacles) {
                tentacles.style.display = 'block';
                const t = (progress - 70) / 12;
                tentacles.style.opacity = `${0.3 + t * 0.4}`;
            }
        } else if (progress >= 80 && progress < 92) {
            // Crystal Realm phase with fractal trees
            if (crystals) {
                crystals.style.display = 'block';
                const t = (progress - 80) / 12;
                crystals.style.opacity = `${0.4 + t * 0.5}`;
                crystals.style.transform = `rotate(${t * 180}deg) scale(${0.9 + t * 0.2})`;
            }
            if (energy) {
                energy.style.display = 'block';
                const t = (progress - 80) / 12;
                energy.style.opacity = `${0.3 + Math.sin(t * Math.PI * 2) * 0.3}`;
            }
            if (fractals) {
                fractals.style.display = 'block';
                const t = (progress - 80) / 12;
                fractals.style.opacity = `${0.5 + t * 0.3}`;
            }
        }
        
        // Extended journey effects (100-140%)
        if (progress >= 100 && progress < 112) {
            // Cosmic Awakening & Stellar Nursery: Aurora and energy waves
            if (aurora) {
                aurora.style.display = 'block';
                const t = (progress - 100) / 12;
                aurora.style.opacity = `${0.6 + Math.sin(t * Math.PI * 2) * 0.4}`;
                aurora.style.transform = `rotate(${t * 360}deg) scale(${1 + t * 0.5})`;
            }
            if (energy) {
                energy.style.display = 'block';
                const t = (progress - 100) / 12;
                energy.style.opacity = `${0.8 + Math.sin(t * Math.PI * 3) * 0.2}`;
            }
        } else if (progress >= 110 && progress < 126) {
            // Quantum Realm & Time Distortion: All reality-bending effects
            if (timeWarp) {
                timeWarp.style.display = 'block';
                const t = (progress - 110) / 16;
                timeWarp.style.opacity = `${0.7 + Math.sin(t * Math.PI * 2) * 0.3}`;
                timeWarp.style.transform = `scale(${1 + Math.sin(t * Math.PI) * 0.5})`;
            }
            if (rifts) {
                rifts.style.display = 'block';
                const t = (progress - 110) / 16;
                rifts.style.opacity = `${0.5 + t * 0.4}`;
            }
            spirals.forEach((spiral, index) => {
                if (spiral) {
                    spiral.style.display = 'block';
                    const t = (progress - 110) / 16;
                    spiral.style.opacity = `${0.4 + Math.sin(t * Math.PI + index) * 0.4}`;
                    spiral.style.transform = `scale(${0.8 + t * 0.6}) rotate(${t * 1080 + index * 120}deg)`;
                }
            });
        } else if (progress >= 125 && progress < 140) {
            // Energy Singularity to Cosmic Reset: Maximum chaos then calm
            const t = (progress - 125) / 15;
            if (t < 0.7) {
                // Maximum chaos phase
                [lightning, plasma, tentacles, chaos, energy].forEach(el => {
                    if (el) {
                        el.style.display = 'block';
                        el.style.opacity = `${0.8 + Math.sin(t * Math.PI * 4) * 0.2}`;
                        el.style.transform = `scale(${1 + Math.sin(t * Math.PI * 6) * 0.3})`;
                    }
                });
            } else {
                // Cosmic reset: Fade everything out
                const fadeT = (t - 0.7) / 0.3;
                [lightning, plasma, tentacles, chaos, energy, aurora, timeWarp, rifts, ...spirals].forEach(el => {
                    if (el) {
                        el.style.display = 'block';
                        el.style.opacity = `${(1 - fadeT) * 0.5}`;
                        el.style.transform = `scale(${1 - fadeT * 0.5})`;
                    }
                });
            }
        }
        
        // Portal rings appear during major transitions (including new ones)
        if ((progress >= 48 && progress <= 54) || (progress >= 68 && progress <= 74) || 
            (progress >= 88 && progress <= 94) || (progress >= 108 && progress <= 114) || 
            (progress >= 128 && progress <= 134)) {
            if (portal) {
                portal.style.display = 'block';
                const intensity = Math.sin((progress % 6) * Math.PI / 6);
                portal.style.opacity = `${0.2 + intensity * 0.6}`;
                portal.style.transform = `translate(-50%, -50%) scale(${0.8 + intensity * 0.4}) rotate(${progress * 10}deg)`;
            }
        }
    }
    
    setupEventListeners() {
        // Keyboard controls
        document.addEventListener('keydown', (e) => {
            if (!this.playing || this.paused) {
                if (e.key === 'w' || e.key === 'W') {
                    this.togglePause();
                }
                if (e.key === 'r' || e.key === 'R') {
                    this.newGame();
                }
                return;
            }
            
            const now = Date.now();
            
            switch(e.key) {
                case 'a':
                case 'A':
                    if (!this.keysPressed.left) {
                        this.keysPressed.left = true;
                        this.movePiece(-1, 0); // Immediate move
                        this.lastMoveDirection = -1;
                        this.moveStartTime = now;
                        this.moveTime = now;
                    }
                    break;
                case 'd':
                case 'D':
                    if (!this.keysPressed.right) {
                        this.keysPressed.right = true;
                        this.movePiece(1, 0); // Immediate move
                        this.lastMoveDirection = 1;
                        this.moveStartTime = now;
                        this.moveTime = now;
                    }
                    break;
                case 's':
                case 'S':
                    this.dropSpeed = 50; // Fast drop - safe but still fast
                    break;
                case 'w':
                case 'W':
                    this.togglePause();
                    break;
                case 'j':
                case 'J':
                    if (now - this.rotateTime > this.rotateSpeed) {
                        this.rotatePiece(-1); // Rotate counterclockwise (CCW)
                        this.rotateTime = now;
                    }
                    break;
                case 'k':
                case 'K':
                    if (now - this.rotateTime > this.rotateSpeed) {
                        this.rotatePiece(2); // Rotate 180 degrees
                        this.rotateTime = now;
                    }
                    break;
                case 'l':
                case 'L':
                    if (now - this.rotateTime > this.rotateSpeed) {
                        this.rotatePiece(1); // Rotate clockwise (CW)
                        this.rotateTime = now;
                    }
                    break;
                case 'r':
                case 'R':
                    this.newGame();
                    break;
                case 'Escape':
                    this.togglePause();
                    break;
            }
            
            e.preventDefault();
        });
        
        // Handle key release
        document.addEventListener('keyup', (e) => {
            switch(e.key) {
                case 'a':
                case 'A':
                    this.keysPressed.left = false;
                    if (this.lastMoveDirection === -1) {
                        this.lastMoveDirection = null;
                    }
                    break;
                case 'd':
                case 'D':
                    this.keysPressed.right = false;
                    if (this.lastMoveDirection === 1) {
                        this.lastMoveDirection = null;
                    }
                    break;
                case 's':
                case 'S':
                    // Reset drop speed when fast drop key is released - restore proper speed for current level
                    this.calculateDropSpeed();
                    break;
            }
        });
        
        // UI controls
        document.getElementById('gameMode').addEventListener('change', (e) => {
            this.changeMode(e.target.value);
        });
        
        document.getElementById('boardSize').addEventListener('change', (e) => {
            this.changeBoardSize(e.target.value);
        });
        
        document.getElementById('newGame').addEventListener('click', () => {
            this.newGame();
        });
        
        document.getElementById('resetHiScore').addEventListener('click', (e) => {
            e.preventDefault();
            this.resetHighScores();
        });
    }
    
    setupGamepadSupport() {
        // Gamepad connection events
        window.addEventListener('gamepadconnected', (e) => {
            // console.log('🎮 Gamepad connected:', e.gamepad.id);
            this.gamepadIndex = e.gamepad.index;
            
            // Detect controller type and configure
            const controllerInfo = this.getControllerConfig(e.gamepad.id);
            this.gamepadType = controllerInfo.type;
            this.currentControllerConfig = controllerInfo.config;
            
            this.updateGamepadStatus(controllerInfo.name, true);
            console.log(`🎮 Controller detected: ${controllerInfo.name} (Type: ${controllerInfo.type})`);
            // Note: Detailed controller debug logging has been commented out to reduce noise
            // To re-enable debug logs, search for "Debug logging" and uncomment the sections you need
        });
        
        window.addEventListener('gamepaddisconnected', (e) => {
            // console.log('🎮 Gamepad disconnected');
            if (e.gamepad.index === this.gamepadIndex) {
                this.gamepadIndex = null;
                this.gamepadType = null;
                this.currentControllerConfig = null;
                this.updateGamepadStatus('', false);
            }
        });
        
        // Check for existing gamepads
        const gamepads = navigator.getGamepads();
        for (let i = 0; i < gamepads.length; i++) {
            if (gamepads[i]) {
                // console.log('🎮 Found existing gamepad:', gamepads[i].id);
                this.gamepadIndex = i;
                
                // Detect controller type and configure
                const controllerInfo = this.getControllerConfig(gamepads[i].id);
                this.gamepadType = controllerInfo.type;
                this.currentControllerConfig = controllerInfo.config;
                
                this.updateGamepadStatus(controllerInfo.name, true);
                console.log(`🎮 Controller detected: ${controllerInfo.name} (Type: ${controllerInfo.type})`);
                break;
            }
        }
    }
    
    updateGamepadStatus(gamepadName, isConnected) {
        const statusElement = document.getElementById('gamepad-status');
        const nameElement = document.getElementById('gamepad-name');
        
        if (isConnected) {
            // Extract just the controller name, remove vendor/product info
            const cleanName = gamepadName.split('(')[0].trim();
            nameElement.textContent = cleanName;
            statusElement.style.display = 'block';
        } else {
            statusElement.style.display = 'none';
        }
    }
    
    updateGamepad() {
        if (this.gamepadIndex === null || !this.currentControllerConfig) return;
        
        const gamepad = navigator.getGamepads()[this.gamepadIndex];
        if (!gamepad) return;
        
        // Use controller-specific update function based on detected type
        if (this.gamepadType === 'snes-switch') {
            // SNES/Switch USB controller (preserve existing working logic)
            this.updateSNESSwitchGamepad(gamepad);
        } else if (this.gamepadType === 'switch-pro' || 
                   this.gamepadType === 'ps4' || 
                   this.gamepadType === 'ps5' || 
                   this.gamepadType === 'xbox' ||
                   this.gamepadType === '8bitdo-sn30-pro' ||
                   this.gamepadType === 'fallback') {
            // All standard controllers use the 8BitDo/standard mapping
            this.update8BitDoGamepad(gamepad);
        } else {
            // Fallback to 8BitDo mapping for any unknown types
            this.update8BitDoGamepad(gamepad);
        }
    }
    
    // SNES/Switch controller update (preserves existing working logic)
    updateSNESSwitchGamepad(gamepad) {
        const buttons = gamepad.buttons;
        const axes = gamepad.axes;
        const config = this.currentControllerConfig;
        
        // Debug logging for gamepad inputs (commented out to reduce noise)
        // if (Math.random() < 0.02) { // Log occasionally to avoid spam
        //     console.log('🎮 SNES/Switch Gamepad Debug:');
        //     console.log('Buttons:', gamepad.buttons.map((btn, i) => btn.pressed ? `${i}:pressed` : null).filter(x => x));
        //     console.log('Axes:', gamepad.axes.map((axis, i) => Math.abs(axis) > 0.05 ? `${i}:${axis.toFixed(2)}` : null).filter(x => x));
        // }
        
        // Detailed axis logging (commented out to reduce noise)
        // if (Math.random() < 0.02) {
        //     console.log('Axis 9 (horizontal):', axes[hAxis]?.toFixed(2), 'Last:', this.lastGamepadAxes[hAxis]?.toFixed(2));
        //     console.log('Axis 10 (vertical):', axes[vAxis]?.toFixed(2), 'Last:', this.lastGamepadAxes[vAxis]?.toFixed(2));
        // }
        
        // Log gamepad mapping info for debugging (commented out to reduce noise)
        // if (Math.random() < 0.01) {
        //     console.log('🎮 Controller mapping:', gamepad.mapping, 'Buttons length:', buttons.length);
        //     console.log('🎮 All axes values:', axes.map((axis, i) => `${i}:${axis?.toFixed(2)}`).filter(x => x && !x.includes('0.00')));
        // }
        
        const currentState = {};
        
        // Track button states
        for (let i = 0; i < buttons.length; i++) {
            currentState[i] = buttons[i].pressed;
        }
        
        // SNES/Switch controller D-pad detection using existing working logic
        // Store previous axis values for change detection
        const hAxis = config.dpad.axes.horizontal;
        const vAxis = config.dpad.axes.vertical;
        
        if (!this.lastGamepadAxes[hAxis]) this.lastGamepadAxes[hAxis] = axes[hAxis] || 0;
        if (!this.lastGamepadAxes[vAxis]) this.lastGamepadAxes[vAxis] = axes[vAxis] || 0;
        
        // D-pad detection
        let dpadLeft = false, dpadRight = false, dpadUp = false, dpadDown = false;
        
        // Standard D-pad button detection (buttons 12-15) - try first
        if (buttons[config.dpad.buttons.up] && buttons[config.dpad.buttons.up].pressed) {
            dpadUp = true;
        }
        if (buttons[config.dpad.buttons.down] && buttons[config.dpad.buttons.down].pressed) {
            dpadDown = true;
        }
        if (buttons[config.dpad.buttons.left] && buttons[config.dpad.buttons.left].pressed) {
            dpadLeft = true;
        }
        if (buttons[config.dpad.buttons.right] && buttons[config.dpad.buttons.right].pressed) {
            dpadRight = true;
        }
        
        // PRESERVED SNES/Switch axis detection logic (working!)
        // Handle horizontal movement (axis 9) - NOTE: axis 9 can only be one value at a time
        if (axes[hAxis] !== undefined) {
            const axisValue = axes[hAxis];
            // LEFT detection - preserved exact working values
            if (axisValue > 0.5 && axisValue < 1.0) {  // 0.71 value = LEFT (working!)
                dpadLeft = true;
            } 
            // RIGHT detection - preserved exact working values
            else if (axisValue < -0.2 && axisValue > -0.6) {  // -0.4285714 = RIGHT (working!)
                dpadRight = true;
            }
            // DOWN detection from axis 9 (when no horizontal movement) - preserved
            else if (axisValue > 0.1 && axisValue < 0.3) {  // 0.14285719 = DOWN (soft drop like 'S')
                dpadDown = true;
            }
        }
        
        // SEPARATE vertical axis (axis 10) for diagonal support - preserved logic
        if (axes[vAxis] !== undefined) {
            if (axes[vAxis] > 0.3) {
                dpadDown = true;
            }
            if (axes[vAxis] < -0.3) {
                dpadUp = true;
            }
        }
        
        // Update last known axes values
        this.lastGamepadAxes[hAxis] = axes[hAxis] || 0;
        this.lastGamepadAxes[vAxis] = axes[vAxis] || 0;
        
        // All debug logging below has been commented out to reduce console noise
        // Uncomment individual sections if you need to debug specific controller issues
        
        // Change detection logging (commented out)
        // const hAxisChange = Math.abs((axes[hAxis] || 0) - this.lastGamepadAxes[hAxis]);
        // const vAxisChange = Math.abs((axes[vAxis] || 0) - this.lastGamepadAxes[vAxis]);
        // if (Math.random() < 0.02) {
        //     console.log('Axis change detection:', hAxisChange.toFixed(2), vAxisChange.toFixed(2));
        // }
        
        // Process button and movement inputs
        this.processGamepadInputs(currentState, { dpadLeft, dpadRight, dpadUp, dpadDown });
    }
    
    // 8BitDo SN30 Pro and fallback controller update
    update8BitDoGamepad(gamepad) {
        const buttons = gamepad.buttons;
        const axes = gamepad.axes;
        const config = this.currentControllerConfig;
        
        // Debug logging for gamepad inputs (commented out to reduce noise)
        // if (Math.random() < 0.02) { // Log occasionally to avoid spam
        //     console.log('🎮 8BitDo Gamepad Debug:');
        //     console.log('Buttons:', gamepad.buttons.map((btn, i) => btn.pressed ? `${i}:pressed` : null).filter(x => x));
        //     console.log('Axes:', gamepad.axes.map((axis, i) => Math.abs(axis) > 0.05 ? `${i}:${axis.toFixed(2)}` : null).filter(x => x));
        // }
        
        // Additional debug logging (commented out to reduce noise)
        // if (Math.random() < 0.01) {
        //     console.log('🎮 8BitDo Controller mapping:', gamepad.mapping, 'Buttons length:', buttons.length);
        //     console.log('🎮 8BitDo All axes values:', axes.map((axis, i) => `${i}:${axis?.toFixed(2)}`).filter(x => x && !x.includes('0.00')));
        // }
        
        const currentState = {};
        
        // Track button states
        for (let i = 0; i < buttons.length; i++) {
            currentState[i] = buttons[i].pressed;
        }
        
        // D-pad detection
        let dpadLeft = false, dpadRight = false, dpadUp = false, dpadDown = false;
        
        // Standard D-pad button detection first
        if (buttons[config.dpad.buttons.up] && buttons[config.dpad.buttons.up].pressed) {
            dpadUp = true;
        }
        if (buttons[config.dpad.buttons.down] && buttons[config.dpad.buttons.down].pressed) {
            dpadDown = true;
        }
        if (buttons[config.dpad.buttons.left] && buttons[config.dpad.buttons.left].pressed) {
            dpadLeft = true;
        }
        if (buttons[config.dpad.buttons.right] && buttons[config.dpad.buttons.right].pressed) {
            dpadRight = true;
        }
        
        // Analog stick detection for 8BitDo
        const hAxis = config.dpad.axes.horizontal;
        const vAxis = config.dpad.axes.vertical;
        
        if (axes[hAxis] !== undefined) {
            const axisValue = axes[hAxis];
            if (axisValue < config.dpad.axisMapping.left.max) {
                dpadLeft = true;
            } else if (axisValue > config.dpad.axisMapping.right.min) {
                dpadRight = true;
            }
        }
        
        if (axes[vAxis] !== undefined) {
            const axisValue = axes[vAxis];
            if (axisValue > config.dpad.verticalAxis.down.min) {
                dpadDown = true;
            } else if (axisValue < config.dpad.verticalAxis.up.min) {
                dpadUp = true;
            }
        }
        
        // Process button and movement inputs
        this.processGamepadInputs(currentState, { dpadLeft, dpadRight, dpadUp, dpadDown });
    }
    
    // Common gamepad input processing for all controller types
    processGamepadInputs(currentState, dpadState) {
        const config = this.currentControllerConfig;
        const { dpadLeft, dpadRight, dpadUp, dpadDown } = dpadState;
        
        // Only trigger on button press (not hold)
        const wasPressed = (buttonIndex) => {
            return currentState[buttonIndex] && !this.lastGamepadState[buttonIndex];
        };
        
        if (!this.gameOver && !this.paused) {
            // Rotation controls - check all configured buttons
            if (config.buttons.rotateCW.some(btn => wasPressed(btn))) {
                this.rotatePiece(1);
            }
            if (config.buttons.rotateCCW.some(btn => wasPressed(btn))) {
                this.rotatePiece(-1);
            }
            if (config.buttons.rotate180.some(btn => wasPressed(btn))) {
                this.rotatePiece(2);
            }
            
            // Advanced movement controls with initial delay (like keyboard)
            const now = Date.now();
            this.handleGamepadMovement('left', dpadLeft, -1, now);
            this.handleGamepadMovement('right', dpadRight, 1, now);
            
            // Handle down button like keyboard 'S' key - change drop speed
            if (dpadDown && !this.gamepadDownPressed) {
                this.gamepadDownPressed = true;
                this.originalDropSpeed = this.dropSpeed; // Store current drop speed
                this.dropSpeed = 50; // Fast drop speed same as keyboard 'S'
            } else if (!dpadDown && this.gamepadDownPressed) {
                this.gamepadDownPressed = false;
                this.dropSpeed = this.originalDropSpeed; // Restore original speed
                this.calculateDropSpeed(); // Recalculate proper speed for current level
            }
            
            if (dpadUp) { // D-pad Up (hard drop)
                this.hardDrop();
            }
        }
        
        // Pause toggle (during game) - check all configured pause buttons
        if (config.buttons.pause.some(btn => wasPressed(btn))) {
            if (this.gameOver) {
                // If game is over, start button restarts the game
                this.newGame();
            } else {
                // During game, start button pauses/unpauses
                this.togglePause();
            }
        }
        
        // Store current state for next frame
        this.lastGamepadState = { ...currentState };
    }
    
    handleGamepadMovement(direction, isPressed, moveDirection, now) {
        const state = this.gamepadMoveState[direction];
        
        // Handle button press start
        if (isPressed && !state.pressed) {
            // Check cooldown to prevent accidental rapid moves
            const timeSinceLastMove = now - this.lastGamepadMoveTime;
            if (timeSinceLastMove < this.gamepadMoveCooldown) {
                // console.log(`🎮 ${direction.toUpperCase()} button pressed but still in cooldown (${timeSinceLastMove}ms)`);
                return; // Ignore this press, still in cooldown
            }
            
            // console.log(`🎮 ${direction.toUpperCase()} button pressed - immediate move!`);
            state.pressed = true;
            state.startTime = now;
            state.hasMoved = true;
            
            // Immediate first move (like keyboard)
            this.movePiece(moveDirection, 0);
            this.lastGamepadMoveTime = now; // Update last move time
        }
        
        // Handle button release
        if (!isPressed && state.pressed) {
            // console.log(`🎮 ${direction.toUpperCase()} button released`);
            state.pressed = false;
            state.hasMoved = false;
            state.startTime = 0;
        }
        
        // Handle continuous movement after initial delay
        if (isPressed && state.pressed && state.hasMoved) {
            const timeSinceStart = now - state.startTime;
            
            // After initial delay, start continuous movement
            if (timeSinceStart > this.gamepadInitialDelay) {
                const timeSinceLastMove = now - this.lastGamepadMoveTime;
                
                if (timeSinceLastMove > this.gamepadMoveDelay) {
                    // console.log(`🎮 ${direction.toUpperCase()} continuous movement`);
                    this.movePiece(moveDirection, 0);
                    this.lastGamepadMoveTime = now;
                }
            }
        }
    }
    
    gameLoop() {
        const now = Date.now();
        const deltaTime = now - this.lastTime;
        this.lastTime = now;
        
        // Update gamepad input
        this.updateGamepad();
        
        if (this.playing && !this.paused && !this.gameOver) {
            this.dropTime += deltaTime;
            
            if (this.dropTime >= this.dropSpeed) {
                this.dropPiece();
                this.dropTime = 0;
            }
            
            // Handle continuous movement
            this.handleContinuousMovement(now);
            
            this.updateUI();
        }
        
        this.draw();
        requestAnimationFrame(() => this.gameLoop());
    }
    
    handleContinuousMovement(now) {
        if (this.lastMoveDirection !== null) {
            const timeSinceStart = now - this.moveStartTime;
            const timeSinceLastMove = now - this.moveTime;
            
            // After initial delay, start repeating moves
            if (timeSinceStart > this.initialMoveDelay && timeSinceLastMove > this.moveSpeed) {
                if ((this.lastMoveDirection === -1 && this.keysPressed.left) ||
                    (this.lastMoveDirection === 1 && this.keysPressed.right)) {
                    this.movePiece(this.lastMoveDirection, 0);
                    this.moveTime = now;
                }
            }
        }
    }
    
    // Easy configuration methods for slide system
    configureSlide(options = {}) {
        if (options.enabled !== undefined) {
            this.slideConfig.enabled = options.enabled;
        }
        if (options.maxSlideDistance !== undefined) {
            this.slideConfig.maxSlideDistance = options.maxSlideDistance;
        }
        if (options.customAttempts !== undefined) {
            this.slideConfig.slideAttempts = options.customAttempts;
        }
        
        console.log('Slide configuration updated:', this.slideConfig);
    }
    
    // Preset slide configurations
    setSlidePreset(preset) {
        switch(preset) {
            case 'disabled':
                this.configureSlide({ enabled: false });
                break;
            case 'minimal':
                this.configureSlide({
                    enabled: true,
                    maxSlideDistance: 1,
                    customAttempts: [
                        [0, 0],   // Original position
                        [-1, 0],  // Left
                        [1, 0]    // Right
                    ]
                });
                break;
            case 'standard':
                this.configureSlide({
                    enabled: true,
                    maxSlideDistance: 2,
                    customAttempts: [
                        [0, 0],   // Original position
                        [-1, 0],  // Left
                        [1, 0],   // Right
                        [-2, 0],  // Further left
                        [2, 0],   // Further right
                        [0, -1]   // Up
                    ]
                });
                break;
            case 'aggressive':
                this.configureSlide({
                    enabled: true,
                    maxSlideDistance: 3,
                    customAttempts: [
                        [0, 0],   // Original position
                        [-1, 0], [1, 0],     // Adjacent sides
                        [-2, 0], [2, 0],     // Further sides
                        [0, -1],             // Up
                        [-1, -1], [1, -1],   // Diagonal up
                        [-3, 0], [3, 0],     // Max sides
                        [0, -2],             // Further up
                        [-2, -1], [2, -1]    // Far diagonal
                    ]
                });
                break;
            default:
                console.log('Unknown preset. Available: disabled, minimal, standard, aggressive');
        }
    }
    
    // High Scores Management (keyed by mode+size)
    loadHighScores() {
        try {
            const saved = localStorage.getItem('blocksHighScores');
            if (saved) {
                return JSON.parse(saved);
            }
        } catch (error) {
            console.warn('Failed to load high scores:', error);
        }
        
        // Return default empty high scores object
        return {};
    }
    
    getHighScoreKey() {
        return `${this.mode}_${this.getSizeKey()}`;
    }
    
    getCurrentHighScores() {
        const key = this.getHighScoreKey();
        let scores = this.highScores[key];
        
        // Ensure scores is always an array
        if (!Array.isArray(scores)) {
            scores = [
                { score: 0, lines: 0, name: '', isEmpty: true },
                { score: 0, lines: 0, name: '', isEmpty: true },
                { score: 0, lines: 0, name: '', isEmpty: true }
            ];
        }
        
        // Ensure we always have exactly 3 entries
        while (scores.length < 3) {
            scores.push({ score: 0, lines: 0, name: '', isEmpty: true });
        }
        
        return scores;
    }
    
    saveHighScores() {
        try {
            localStorage.setItem('blocksHighScores', JSON.stringify(this.highScores));
        } catch (error) {
            console.warn('Failed to save high scores:', error);
        }
    }
    
    checkAndUpdateHighScore() {
        let isNewHighScore = false;
        const key = this.getHighScoreKey();
        const currentScores = this.getCurrentHighScores();
        
        // Check if current score qualifies for top 3
        for (let i = 0; i < 3; i++) {
            const existing = currentScores[i];
            
            // If this slot is empty or current score is higher
            if (!existing || existing.isEmpty || this.score > existing.score || 
                (this.score === existing.score && this.lines > existing.lines)) {
                
                // Get player name
                const name = prompt('New High Score! Enter your name:', '') || 'Anonymous';
                const currentScore = { 
                    score: this.score, 
                    lines: this.lines, 
                    name: name.substring(0, 10), // Limit to 10 characters
                    isEmpty: false 
                };
                
                // Shift existing scores down
                for (let j = 2; j > i; j--) {
                    currentScores[j] = { ...currentScores[j-1] };
                }
                
                // Insert new high score
                currentScores[i] = currentScore;
                
                // Save back to main scores object
                this.highScores[key] = currentScores;
                
                isNewHighScore = true;
                break;
            }
        }
        
        if (isNewHighScore) {
            this.saveHighScores();
            this.updateHighScoresDisplay();
            
            // Get fresh scores to find the rank
            try {
                const updatedScores = this.getCurrentHighScores();
                if (Array.isArray(updatedScores)) {
                    const rank = updatedScores.findIndex(hs => 
                        hs && hs.score === this.score && hs.lines === this.lines) + 1;
                    this.showSpecialMessage(`NEW HIGH SCORE! #${rank}`);
                } else {
                    console.error('updatedScores is not an array:', updatedScores);
                    this.showSpecialMessage('NEW HIGH SCORE!');
                }
            } catch (error) {
                console.error('Error processing high score message:', error);
                this.showSpecialMessage('NEW HIGH SCORE!');
            }
        }
        
        return isNewHighScore;
    }
    
    updateHighScoresDisplay() {
        const hiscoresContainer = document.querySelector('.hiscores-list');
        if (!hiscoresContainer) return;
        
        const currentScores = this.getCurrentHighScores();
        
        let html = '';
        for (let i = 0; i < 3; i++) {
            const hs = currentScores[i];
            if (!hs || hs.isEmpty) {
                html += `<div class="hiscore-entry">
                    <span class="rank">${i + 1}.</span>
                    <span class="score">-----</span>
                    <span class="lines">-----</span>
                    <span class="name">-----</span>
                </div>`;
            } else {
                html += `<div class="hiscore-entry">
                    <span class="rank">${i + 1}.</span>
                    <span class="score">${(hs.score || 0).toLocaleString()}</span>
                    <span class="lines">${hs.lines || 0}</span>
                    <span class="name">${hs.name || 'NO NAME'}</span>
                </div>`;
            }
        }
        hiscoresContainer.innerHTML = html;
    }
}

// Initialize the game when the page loads
window.addEventListener('load', () => {
    window.game = new BlocksGame(); // Make game accessible globally for easy tweaking
    
    // Quick access functions for console
    window.slidePreset = (preset) => window.game.setSlidePreset(preset);
    window.configSlide = (options) => window.game.configureSlide(options);
    
    console.log('🎮 HEXTRIS game loaded!');
    console.log('🖼️ Using sprite sheet for piece textures');
    console.log('💡 Try these in console:');
    console.log('   slidePreset("minimal")    - Basic sliding');
    console.log('   slidePreset("standard")   - Default sliding (current)');
    console.log('   slidePreset("aggressive") - Maximum sliding');
    console.log('   slidePreset("disabled")   - No sliding');
    console.log('   configSlide({enabled: false}) - Custom config');
});
