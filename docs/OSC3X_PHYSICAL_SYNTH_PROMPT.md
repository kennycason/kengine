# OSC3X Physical Synth Project Prompt

You are working locally. Inspect the repository first and do not assume the current structure from this prompt if the code disagrees.

You are helping me build a custom software synth controller project called OSC3X.

The goal is:

A physical USB controller built from Raspberry Pi Pico 2 W + knobs/buttons/encoders, controlling a software synth written in Kotlin/Native on my existing Kengine framework.

I want to develop and test on my MacBook Pro first, then later run the same synth/controller setup on a Raspberry Pi 5.

Please inspect my locally downloaded Kengine repository before making architectural assumptions.

My repo is:

kennycason/Kengine

Relevant existing project paths should include something like:

games/osc3x-synth/

and possibly:

games/osc3x-synth-v2/

Also inspect any existing sound/audio modules such as:

kengine-sound

and any existing SDL3 controller/input abstractions.

---

## PROJECT VISION

OSC3X is conceptually:

FL Studio 3xOsc + a simple hardware synth control surface

but implemented as a software synth.

Core synth:

```text
OSC 1 ─┐
OSC 2 ─┼→ Mixer → Filter → FX → Master → Audio Out
OSC 3 ─┘
```

Each oscillator should have its own envelope:

```text
OSC 1 → ADSR 1 ─┐
OSC 2 → ADSR 2 ─┼→ Mixer
OSC 3 → ADSR 3 ─┘
```

Eventually I want:

* 3 independent oscillators
* sine
* triangle
* saw
* square/pulse
* noise
* individual oscillator tuning
* octave/semitone/fine tuning
* individual oscillator levels
* individual ADSR envelopes
* shared filter
* resonance
* multiple LFOs
* modulation routing
* delay
* chorus
* reverb
* distortion
* bitcrusher
* ring modulation
* possibly FM between oscillators
* presets
* controller mappings
* MIDI input
* game-controller input
* custom physical USB controller input

Do NOT attempt to implement all of that immediately.

We need a clean incremental architecture.

---

## HARDWARE I ALREADY OWN

I have already purchased and received:

* Raspberry Pi Pico 2 W
  * RP2350-based
  * pre-soldered headers
  * Micro-USB
* solderless breadboards
* jumper wires
* RK097 B10K linear potentiometers
  * 10K
  * linear taper
  * 3-pin
  * panel/PCB style
* black 6 mm knob caps
* blue trim potentiometers
* KY-040 rotary encoder modules
  * rotary encoder
  * integrated push button
* tactile momentary push buttons
* Micro-USB data cable

Assume I have a Raspberry Pi 5 available later.

---

## CONTROLLER ARCHITECTURE

The intended physical controller architecture is:

```text
Knobs
Buttons
Rotary Encoders
     │
     ▼
Raspberry Pi Pico 2 W
     │
     │ USB
     ▼
USB MIDI / HID / custom protocol
     │
     ▼
MacBook Pro initially
Raspberry Pi 5 later
     │
     ▼
Kengine OSC3X software synth
```

I originally planned to use USB MIDI CC messages from the Pico.

Please research and decide whether:

1. USB MIDI is the best protocol,
2. USB HID would be better,
3. a custom serial protocol would be better,
4. or whether we should support more than one.

Prefer something:

* simple
* low latency
* portable across macOS and Linux
* easy to debug
* easy to integrate with Kotlin/Native

Explain the tradeoff before implementing.

---

## IMPORTANT HARDWARE FACT

The Pico has only a small number of direct ADC inputs.

For the first prototype, we only need about 3 knobs.

Initial physical controls:

```text
OSC1 LEVEL     knob
OSC1 TUNE      knob
OSC1 ATTACK    knob
WAVEFORM       button or encoder
OCTAVE DOWN    button
OCTAVE UP      button
```

We can expand later using:

* external ADCs
* analog multiplexers such as CD74HC4067
* multiple ADC devices

Research what is best when we reach that stage.

---

## FIRST HARDWARE MILESTONE

First prove:

```text
physical knob
     ↓
Pico ADC
     ↓
USB message
     ↓
MacBook
     ↓
Kengine
     ↓
OSC parameter changes
     ↓
audible sound changes
```

I want the first working prototype to be very small.

Ideally:

```text
3 B10K pots
3 buttons
1 KY-040 encoder
Pico 2 W
USB
breadboard
```

---

## SYNTH SOFTWARE ARCHITECTURE

Please inspect the existing Kengine audio implementation before designing this.

The critical architectural requirement is:

audio synthesis must NOT depend on the 60 Hz render/update loop.

It should conceptually be:

```text
UI / GAME LOOP
~60 Hz
    │
    ├─ controller state
    ├─ MIDI
    ├─ visual UI
    └─ parameter updates
AUDIO CALLBACK
44.1kHz or 48kHz
    │
    ├─ osc1 sample
    ├─ osc2 sample
    ├─ osc3 sample
    ├─ envelopes
    ├─ mixer
    ├─ filter
    ├─ effects
    └─ audio output
```

Avoid allocation, locking, blocking I/O, or garbage-producing operations in the audio callback if possible.

Research the current Kotlin/Native + SDL3 audio API if necessary.

---

## DESIRED SOFTWARE MODEL

I think something like this is reasonable:

```text
SynthEngine
│
├── Voice
│   │
│   ├── Oscillator 1
│   │    ├── waveform
│   │    ├── pitch
│   │    ├── level
│   │    └── ADSR
│   │
│   ├── Oscillator 2
│   │    ├── waveform
│   │    ├── pitch
│   │    ├── level
│   │    └── ADSR
│   │
│   └── Oscillator 3
│        ├── waveform
│        ├── pitch
│        ├── level
│        └── ADSR
│
├── Mixer
├── Filter
├── Modulation
├── Effects
└── Master
```

But inspect existing Kengine conventions and adapt accordingly.

Do not force this structure if Kengine already has a cleaner abstraction.

---

## PARAMETER SYSTEM

I want synth controls to be addressable independently from their input source.

For example:

```text
OSC1.PITCH
OSC1.LEVEL
OSC1.WAVEFORM
OSC1.ENV.ATTACK
OSC1.ENV.DECAY
OSC1.ENV.SUSTAIN
OSC1.ENV.RELEASE
OSC2...
OSC3...
FILTER.CUTOFF
FILTER.RESONANCE
LFO1.RATE
LFO1.DEPTH
FX.DELAY.MIX
```

Input mappings should conceptually be separate:

```text
MIDI CC 20
    → OSC1.LEVEL
Pico Knob 1
    → OSC1.TUNE
Gamepad RightStickY
    → FILTER.CUTOFF
```

Please design a lightweight parameter/mapping system that fits Kotlin/Native well.

---

## GAME CONTROLLER SUPPORT

Kengine already has game controller support.

Please inspect it.

Eventually I want things like:

```text
left stick X
    → detune
right stick Y
    → filter cutoff
trigger
    → distortion
gyro
    → modulation
```

Do not implement gyro unless it is easy and supported.

Just keep the architecture open to controller mappings.

---

## PICO FIRMWARE

Research the best firmware stack for Pico 2 W.

Candidates include:

* CircuitPython
* MicroPython
* Pico SDK / C++

My preference is to start with the simplest reliable option.

If CircuitPython can cleanly expose USB MIDI on Pico 2 W, use it initially.

First Pico firmware should:

* read 3 analog potentiometers
* debounce buttons
* read a KY-040 encoder
* expose USB control messages
* avoid flooding USB with unchanged values
* optionally smooth noisy ADC input

Example behavior:

```text
knob ADC
0..65535
normalize
0.0 .. 1.0
optionally quantize
send only if delta > threshold
```

If using MIDI:

```text
CC20 = OSC1 level
CC21 = OSC1 tune
CC22 = OSC1 attack
```

Buttons could send note/control messages.

Encoder could send relative increments.

Research sensible MIDI conventions.

---

## SCHEMATICS / WIRING

I need explicit breadboard instructions.

After researching the Pico 2 W pinout, produce a wiring plan for:

```text
B10K pot 1
B10K pot 2
B10K pot 3
3 tactile buttons
1 KY-040 encoder
```

For each potentiometer:

```text
outer pin → 3.3V
middle/wiper → ADC pin
other outer pin → GND
```

VERIFY the correct ADC-capable GPIO pins for Pico 2 W before giving instructions.

Never put 5V into an ADC.

The KY-040 may have:

```text
CLK
DT
SW
+
GND
```

Research whether its module is safe at 3.3V and how best to wire it to RP2350 GPIO.

Produce:

* pin table
* breadboard layout
* ASCII schematic
* warnings
* test procedure

Please be conservative with voltages.

---

## GPIO / KENGINE QUESTION

I am unsure whether Kengine itself should contain a Raspberry Pi GPIO module.

Investigate this.

My instinct is that the physical controller should communicate over USB, meaning:

Kengine should NOT need GPIO at all for the synth controller.

That gives:

```text
controller hardware
    ↓ USB
Mac or Pi
    ↓
Kengine
```

This is preferable because it keeps the app portable.

However, I may eventually want a reusable Kotlin/Native module for Raspberry Pi GPIO for other Kengine projects.

Please separately evaluate whether something like:

kengine-gpio

would make sense.

If you recommend it, research Linux GPIO options such as:

* libgpiod
* GPIO character device interface
* existing C libraries usable through Kotlin/Native cinterop

Avoid old/deprecated sysfs GPIO approaches unless required.

Do NOT couple synth parameter input to GPIO.

Treat GPIO support as a reusable optional platform module.

---

## MAC FIRST, PI LATER

Development sequence:

### Stage 1

MacBook:

```text
OSC3X software synth
keyboard input
existing controller input
```

### Stage 2

Pico controller:

```text
pots/buttons
    ↓
USB
    ↓
MacBook
    ↓
Kengine
```

### Stage 3

Raspberry Pi 5:

```text
Pico
    ↓
USB
    ↓
Pi 5
    ↓
Kengine OSC3X
    ↓
USB/I2S audio output
```

We should aim for identical synth/controller behavior on Mac and Pi.

---

## RASPBERRY PI 5 SETUP

Research what will eventually be required to build and run the Kotlin/Native project on Raspberry Pi 5.

Determine:

* OS recommendation
* ARM64 target support
* Kotlin/Native target compatibility
* SDL3 dependencies
* audio dependencies
* build tooling
* USB MIDI support
* audio device selection
* startup/boot options

If Kengine currently only targets Linux x64, identify exactly what needs to change for Linux ARM64.

Do not make speculative changes without checking current Kotlin/Native support.

---

## AUDIO

Start simple.

First implementation:

1 voice
3 oscillators

Support:

* sine
* square
* triangle
* saw

Use phase accumulation:

```text
phase += frequency / sampleRate
if phase >= 1:
    phase -= 1
```

Basic waveforms:

```text
sine:
sin(phase * 2π)
saw:
2 * phase - 1
square:
phase < 0.5 ? 1 : -1
triangle:
derive from phase
```

Initially naive oscillators are okay.

Later research:

* PolyBLEP
* bandlimited oscillators
* anti-aliasing

Do not prematurely optimize.

---

## ENVELOPES

Each oscillator must have its own ADSR.

State machine:

```text
IDLE
ATTACK
DECAY
SUSTAIN
RELEASE
```

Example:

```text
OSC1
short pluck
OSC2
medium body
OSC3
long swell
```

This is one of the defining features of the synth.

---

## FIRST SOFTWARE MILESTONE

Build the simplest usable OSC3X:

```text
key press
    ↓
note frequency
    ↓
osc1 + osc2 + osc3
    ↓
independent ADSR
    ↓
mixer
    ↓
SDL audio
```

UI may display:

```text
OSC1 waveform
OSC1 level
OSC2 waveform
OSC2 level
OSC3 waveform
OSC3 level
```

Keyboard controls are fine initially.

---

## SECOND SOFTWARE MILESTONE

Add generic parameter mapping:

```text
ParameterId
ParameterValue
ControllerBinding
```

Then map:

```text
keyboard
gamepad
MIDI
```

to the same parameters.

---

## THIRD MILESTONE

Pico USB controller changes synth parameters live.

For example:

```text
physical knob 1
    → OSC1 LEVEL
physical knob 2
    → OSC1 TUNE
physical knob 3
    → OSC1 ATTACK
```

Verify latency and stability.

---

## RESEARCH EXPECTATIONS

You are authorized to research anything that is unclear.

In particular, verify current information for:

* Pico 2 W / RP2350 USB MIDI
* CircuitPython support
* Pico ADC pinout
* KY-040 3.3V operation
* macOS USB MIDI behavior
* Linux USB MIDI behavior
* Kotlin/Native MIDI library possibilities
* SDL3 audio APIs
* Kotlin/Native ARM64 Linux target support
* Raspberry Pi 5 Kotlin/Native build/runtime concerns
* libgpiod integration

Prefer primary documentation:

* Raspberry Pi docs
* Kotlin docs
* SDL docs
* Adafruit docs
* Linux GPIO docs
* library repositories

---

## VERY IMPORTANT

Before writing lots of new code:

1. inspect Kengine
2. inspect existing OSC3X source
3. inspect existing sound/audio systems
4. inspect existing input/controller systems
5. identify reusable abstractions
6. propose a plan

Avoid creating duplicate systems if Kengine already has equivalents.

---

## WHAT I WANT YOU TO DO FIRST

Please begin by:

1. Inspecting the Kengine repository.
2. Summarizing how the existing OSC3X implementation currently works.
3. Summarizing how Kengine audio currently works.
4. Summarizing controller/input support.
5. Determining the best path for USB MIDI input.
6. Determining whether any Kengine refactor is useful before adding synth features.
7. Researching Pico 2 W USB controller implementation.
8. Producing the exact breadboard wiring plan using my current components.
9. Producing a phased implementation plan.
10. Then begin implementing Phase 1 only, keeping commits/changes small and understandable.

Phase 1 should ideally result in:

```text
MacBook
+
Kengine
+
functioning 3 oscillator synth
+
independent ADSRs
+
generic parameter API
```

Then Phase 2:

```text
Pico firmware
+
breadboard controller
+
USB input
```

Then Phase 3:

Raspberry Pi 5 deployment

---

## OUTPUT FORMAT

Before modifying code, give me:

1. Existing Kengine findings
2. Proposed architecture
3. USB protocol recommendation
4. Pico firmware recommendation
5. Breadboard wiring / pin map
6. Kengine changes
7. Raspberry Pi 5 considerations
8. Phase-by-phase plan

Then proceed with Phase 1.

When changing code:

* show which files are added/changed
* explain why
* keep changes focused
* compile/test frequently
* do not rewrite unrelated Kengine systems
* prefer reusable Kengine abstractions where appropriate

If something is uncertain, research it instead of guessing.

The final objective is a real physical instrument:

```text
         OSC3X CONTROLLER
     knobs / buttons / encoders
                │
                │ USB
                ▼
       MacBook or Raspberry Pi 5
                │
                ▼
         Kengine OSC3X Synth
                │
                ▼
             AUDIO
```

The guiding principle is:

hardware controls, software synthesis.
