package com.kengine.input.controller.controls

/**
 * Unless otherwise specified by controllers, button ordering follows Nintendo's button ordering.
 *
 *  Z Y X
 *  C B A
 *
 * 4-Button controller example.
 *
 *                     Z1            Z2
 *   L1  _=====_        |            |         _=====_  R1
 *  L2  / _____ \      |/           |/        / _____ \  R2
 *    +.-'_____'-.---------------------------.-'_____'-.+
 *   /   |     |  '.                       .'  |     |   \
 *  / ___| /|\ |___ \                     / ___|  X  |___ \
 * / |      |      | ;  __           _   ; |             | ;
 * | | <---   ---> | | |__|         |_:> | |  Y       A  | ;
 * | |___   |   ___| ;SELECT       START ; |___       ___| ;
 * |\    | \|/ |    /  _     ___      _   \    |  B  |    /|
 * | \   |_____|  .','" "', |___|  ,'" "', '.  |_____|  .' |
 * |  '-.______.-' /       \ANALOG/       \  '-._____.-'   |
 * |               |  L3   |------|   R3  |                |
 * |              /\       /      \       /\               |
 * |             /  '.___.'        '.___.'  \              |
 * |            /                            \             |
 *  \          /                              \           /
 *   \________/                                \_________/
 *
 *
 * 6-Button controller example.
 *
 *                     Z1            Z2
 *   L1  _=====_        |            |         _=====_  R1
 *  L2  / _____ \      |/           |/        / _____ \  R2
 *    +.-'_____'-.---------------------------.-'_____'-.+
 *   /   |     |  '.                       .' /        \ \
 *  / ___| /|\ |___ \                     / /      X    \ \
 * / |      |      | ;  __           _   ; |    Y        | ;
 * | | <---   ---> | | |__|         |_:> | |  Z       A  | ;
 * | |___   |   ___| ;SELECT       START ; |       B     | ;
 * |\    | \|/ |    /  _     ___      _   \ \   C       / /|
 * | \   |_____|  .','" "', |___|  ,'" "', '. \_______/  .' |
 * |  '-.______.-' /       \ANALOG/       \  '-._____.-'   |
 * |               |  L3   |------|   R3  |                |
 * |              /\       /      \       /\               |
 * |             /  '.___.'        '.___.'  \              |
 * |            /                            \             |
 *  \          /                              \           /
 *   \________/                                \_________/
 *
 * Playstation Button layout
 *
 *   L1  _=====_                               _=====_  R1
 *  L2  / _____ \                             / _____ \  R2
 *    +.-'_____'-.---------------------------.-'_____'-.+
 *   /   |     |  '.        S O N Y        .'  |  _  |   \
 *  / ___| /|\ |___ \                     / ___| /_\ |___ \
 * / |      |      | ;  __           _   ; | _         _ | ;
 * | | <---   ---> | | |__|         |_|  | ||_|       (_)| |
 * | |___   |   ___| ;SELECT       START ; |___       ___| ;
 * |\    | \|/ |    /  _     ___      _   \    | (X) |    /|
 * | \   |_____|  .','" "', |___|  ,'" "', '.  |_____|  .' |
 * |  '-.______.-' /       \ANALOG/       \  '-._____.-'   |
 * |               |       |------|       |                |
 * |              /\       /      \       /\               |
 * |             /  '.___.'        '.___.'  \              |
 * |            /                            \             |
 *  \          /                              \           /
 *   \________/                                \_________/
 *
 * These all work for the same button:
 * controller.isButtonPressed(GamepadButton.A)        // Generic mapping
 * controller.isButtonPressed(GamepadButton.CIRCLE)   // PS-specific mapping
 * controller.isButtonPressed(Playstation4.O)         // Raw PS4 button code
 * controller.isButtonPressed(1)                      // Raw PS4 button code as int
 */
enum class Buttons {

    /**
     * O on PS, B on Xbox
     */
    A,

    /**
     * X on PS, A on Xbox
     */
    B,

    /**
     * (6-button controllers)
     */
    C,

    /**
     * Triangle on PS, Y on Xbox
     */
    X,

    /**
     * Square on PS, X on Xbox
     */
    Y,

    /**
     * (6-button controllers)
     */
    Z,

    /**
     * Playstation square button
     */
    SQUARE,

    /**
     * Playstation triangle button
     */
    TRIANGLE,

    /**
     * Playstation circle button
     */
    CIRCLE,

    // special buttons

    /**
     * share/select/minus
     */
    SELECT,

    /**
     * options/start/plus
     */
    START,

    // shoulder buttons
    /**
     * left bumper
     */
    L1,

    /**
     * right bumper
     */
    R1,

    // shoulder triggers
    /**
     * left trigger
     */
    L2,

    /**
     *  right trigger
     */
    R2,

    // stick buttons
    /**
     * left stick press
     */
    L3,

    /**
     * right stick press
     */
    R3,

    // bottom triggers
    /**
     * bottom left trigger
     */
    Z1,

    /**
     * bottom right trigger
     */
    Z2,

    // D-Pad
    DPAD_UP,
    DPAD_DOWN,
    DPAD_LEFT,
    DPAD_RIGHT
}
