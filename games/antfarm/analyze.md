# Ant Colony Simulation Analysis

Based on the logs and visual observation, here are the key issues:

## Problems Identified

### 1. **Ants Not Digging Side Tunnels**
**Root Cause**: The digging logic only triggers when `y >= 20` (deep underground), but:
- Ants get stuck at surface (y=15) trying to harvest/carry dirt
- They never actually go deep enough to trigger the digging behavior
- The `tryMove()` function returns false, but digging only happens 30% of the time

**Evidence from logs**:
```
[DEBUG][Ant][299485] Ant stuck in loop at (78,15), forcing random movement
[DEBUG][Ant][299485] Ant stuck in loop at (95,15), forcing random movement
```
All stuck ants are at y=15 (surface level)

### 2. **No Food Storage Rooms**
**Root Cause**: There's no pheromone-driven room digging behavior:
- Ants deliver food directly to queen
- No mechanism to dig storage chambers based on food density
- No "STORAGE" pheromone type to mark good storage locations

### 3. **Entrance Blocking (Fixed)**
- Dirt now drops above surface (y=13) instead of at surface (y=15) ✓

## Recommended Fixes

### Fix 1: Make Ants Actually Go Underground
Change exploration behavior so ants actively explore downward:

```kotlin
// In exploreForFood(), replace surface bias with underground exploration
if (y < 30) {  // Not deep enough yet
    if (Random.nextFloat() < 0.5f) {  // 50% chance to go deeper
        moveDownward()
    } else {
        moveRandomly()
    }
} else {
    // Deep enough - explore horizontally and dig
    exploreAndDig()
}
```

### Fix 2: Increase Digging Frequency
Change digging chance from 30% to 80% when stuck underground:

```kotlin
if (Random.nextFloat() < 0.8f) { // 80% chance to dig when stuck
    dig()
}
```

### Fix 3: Add Storage Room Behavior
1. Add `STORAGE` pheromone type
2. When ants deliver food, drop STORAGE pheromones
3. Ants dig out areas with high STORAGE pheromone density

```kotlin
// When delivering food
world.addPheromone(x, y, PheromoneType.STORAGE, 2.0)

// When exploring near high STORAGE pheromone
if (storagePhero > 5.0 && adjacentTilesAreDirt > 3) {
    digStorageRoom()
}
```

## Current Behavior Summary

From the logs:
- **All ants stay at surface (y=15)**
- **Primary activity**: Carrying dirt, getting stuck in loops
- **No underground exploration happening**
- **No tunnel network forming**

The core issue is that ants never venture deep enough to trigger the digging behavior we implemented.

