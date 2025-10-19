# Gamepad Control Fixes

## Issues Fixed

### 1. ✅ D-PAD Not Responding
**Problem:** D-pad had no response in GAMEPAD mode.

**Root Cause:** The code was checking for HAT directions (`controller.isHatDirectionPressed()`), but in GAMEPAD mode, the D-pad is mapped to **buttons** (11-14), not HAT events.

**Fix:** Changed all D-pad checks to use button indices:
- DPAD_LEFT: Button 13
- DPAD_UP: Button 11
- DPAD_RIGHT: Button 14
- DPAD_DOWN: Button 12

**Changed Lines:**
- Line 560: Left movement
- Line 591: Right movement
- Line 622: Down/soft drop
- Line 646: Up/hard drop

---

### 2. ✅ Pause/Unpause Button Confusion
**Problem:** 
- R button would pause
- START button wouldn't pause
- START button was needed to unpause

**Root Cause:** Button mapping was incorrect:
- Code was checking buttons 7/10 for START (wrong)
- Actual mapping: START=6, SELECT=4
- Also, shoulder buttons were mapped to old indices (L=4, R=6 instead of L1=9, R1=10)

**Fix:** 
- Updated START button to 6, SELECT to 4
- Updated L1 to button 9, R1 to button 10
- Now START properly toggles pause/unpause
- R1 button (10) is used for rotation, not pause

**Changed Lines:**
- Line 784-785: START and SELECT button indices
- Line 684-685: L1 and R1 button indices

---

### 3. ✅ Timing Bug - Immediate Re-pause
**Problem:** Despite `pauseDelayMs = 1000L`, the game would sometimes immediately pause again after unpausing.

**Root Cause:** Controller input handler was using `inputDelayMs` (60ms) instead of `pauseDelayMs` (1000ms) for the timing gate.

**Fix:** 
- Changed pause/unpause timing check to use `pauseDelayMs` instead of `inputDelayMs`
- Added extensive logging to track timing behavior
- Added debug logging when timing gate prevents action

**Changed Lines:**
- Line 790, 796: Now uses `pauseDelayMs` consistently
- Line 787-808: Added comprehensive timing logging

---

### 4. ✅ Button Mappings Corrected
**Problem:** XYBA buttons might have been reversed.

**Root Cause:** Button indices were based on old JOYSTICK mode mappings.

**Fix:** Updated all button mappings based on controller mapping tool output:
```
Face buttons:  B=0, A=1, Y=2, X=3
Shoulders:     L1=9, R1=10
System:        SELECT=4, START=6
D-Pad:         LEFT=13, UP=11, RIGHT=14, DOWN=12
```

---

## Timing System Documentation Added

Added comprehensive documentation at the top of `HextrisGame` class explaining:

### Critical Concept: Absolute vs Relative Time
- **Correct Pattern:** Store absolute timestamps from `getClockContext().totalTimeMs`
- **Common Pitfall:** Never store the result of `timeSinceMs()` as a timestamp
- **Why It Matters:** Mixing durations and timestamps causes timing bugs

### Example of the Bug We Had:
```kotlin
// ❌ WRONG - stores a duration as if it were a timestamp
timeSinceOptionChangeMs = timeSinceMs(oldTime)  // ~60ms
// Later: timeSinceMs(60) calculates huge wrong value!

// ✅ CORRECT - stores absolute timestamp
timeSinceOptionChangeMs = getClockContext().totalTimeMs  // ~123456ms
// Later: timeSinceMs(123456) calculates correct elapsed time
```

---

## Enhanced Logging

Added detailed logging for pause/unpause operations:
- Logs time since last toggle
- Logs when timing gate is active (preventing action)
- Logs absolute timestamps for debugging
- Throttled debug logs to prevent spam

**Key Log Messages:**
- `"START pressed. Time since last toggle: Xms (delay: 1000ms)"`
- `"START pressed but timing gate active. Time since last: Xms < 1000ms"`
- `"Resumed. New timestamp: X"`

---

## Testing Recommendations

1. **D-Pad Test:** 
   - All four directions should work for movement
   - Up should hard drop
   - Down should soft drop (speed up)

2. **Pause/Unpause Test:**
   - START should pause when playing
   - START should unpause when paused
   - Should require 1 second between toggles
   - Check logs to verify timing gate is working

3. **Button Test:**
   - L1/R1 should rotate pieces
   - A/B/X/Y should rotate pieces
   - Verify rotation directions match expectations

4. **Timing Test:**
   - Pause with START
   - Immediately try to unpause (should be blocked for ~1 second)
   - After 1 second, unpause should work
   - Check logs for "timing gate active" messages

---

## Code Comments Added

All controller input code now has clear comments explaining:
- Which physical button maps to which index
- The difference between GAMEPAD and JOYSTICK modes
- Why we use absolute timestamps vs durations
- Critical timing requirements

---

## Summary

All four reported issues have been fixed:
1. ✅ D-pad now responds correctly (buttons not HAT)
2. ✅ START button properly pauses/unpauses
3. ✅ Timing gate uses correct 1-second delay
4. ✅ All buttons mapped to correct indices

The code now includes comprehensive documentation to prevent similar timing bugs in the future.

