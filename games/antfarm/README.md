# Antfarm Simulator

A pheromone-driven ant colony simulation built with Kengine.

## Goals

Create a natural, emergent ant colony simulation where complex behaviors arise from simple pheromone-based rules.

### Core Features

1. **Pheromone-Driven Behavior**
   - Ants communicate through chemical trails (pheromones)
   - HOME pheromones: Guide ants back to colony (ultra-slow decay)
   - FOOD pheromones: Mark food sources (medium decay, reinforced by hungry ants)
   - EXPLORE pheromones: Mark explored areas (ants prefer unexplored territory)
   - HUNGER pheromones: Queen signals need for food
   - DANGER pheromones: Mark hazards

2. **Natural Foraging**
   - Ants explore and find vegetation on the surface
   - Harvest plants and carry food back to colony
   - Follow pheromone trails to optimize routes
   - Store food in colony for conversion and consumption

3. **Excavation & Tunneling**
   - Ants dig through dirt to create tunnel networks
   - Excavated dirt becomes DIRT_PILE that must be hauled away
   - Ants carry dirt to surface and deposit it (creating anthill mounds)
   - Tunnels form naturally based on pheromone gradients and food needs

4. **Energy & Rest System**
   - Ants consume energy while working
   - Low-energy ants return home to rest and eat from food stores
   - Colony converts stored plants into consumable food over time
   - Queen has priority access to food and signals hunger

5. **Reproduction**
   - Queen spawns new ants when colony has sufficient food
   - Colony grows organically based on resource availability

6. **Emergent Complexity**
   - No hard-coded behaviors - everything emerges from pheromone interactions
   - Ants prefer unexplored areas (low EXPLORE pheromone)
   - Food storage rooms form naturally where ants deposit food frequently
   - Tunnel networks optimize based on traffic patterns

## Visual Design

- **Sky**: Blue background at top (~20% of screen)
- **Vegetation**: Colorful flowers (red, yellow, pink, purple, white) on green stems
- **Ground**: Thin brown surface layer where plants grow
- **Underground**: Darker brown dirt, lighter brown dirt piles
- **Ants**: Simple polygon shapes, color-coded by state
  - Red: Exploring
  - Orange with green dot: Carrying food
  - Cyan: Carrying dirt
- **Queen**: Large ant (2x size) with distinctive abdomen
- **Pheromones**: Thin colored bars at bottom of tiles (non-obstructive)
  - Blue: HOME
  - Red: FOOD
  - Green: EXPLORE

## Controls

- **R**: Reset simulation

## Technical Details

- Grid-based world (120×80 tiles)
- Pheromone strength: 0-100 per type per tile
- Per-type pheromone decay rates for natural behavior
- Conservation of matter: dirt is moved, not destroyed
- Ants use 9-tile perception (8 neighbors + self) for gradient following
