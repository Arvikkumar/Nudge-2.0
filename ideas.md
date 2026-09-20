# Gentle Nudge — Design Exploration

## Three stylistic approaches

### 1. Paper Morning
**Very Brief Intro:** A warm, tactile daily planner inspired by letterpress paper and botanical desk objects. It turns reminders into calm, human-sized moments rather than productivity metrics.

**Probability:** 0.07

### 2. Quiet Appliance
**Very Brief Intro:** A compact, instrumental interface inspired by well-made household objects and Japanese industrial design. It prioritizes utility, clarity, and a feeling of dependable calm.

**Probability:** 0.04

### 3. Soft Signal
**Very Brief Intro:** A contemporary notification-space built from soft blue light, translucent surfaces, and deliberate small animations. It makes each reminder feel like a considerate signal rather than an alert.

**Probability:** 0.09

---

## Chosen Approach: Paper Morning

### Design Movement

**Warm modernism with editorial stationery influences.** The product feels like a thoughtful note left on a desk: reassuring, legible, and quietly personal.

### Core Principles

1. **Calm over urgency:** No warning colors, crowded counters, or productivity jargon. Tasks are presented as small, manageable nudges.
2. **Tactile digital materials:** Warm paper tones, soft shadowing, and imperfectly geometric accents reduce the coldness of a typical task manager.
3. **Single-focus action:** The most important action—capture a reminder—is always obvious and effortless.
4. **Comfortable reading:** Large type, generous vertical rhythm, and stable visual grouping support quick scanning without pressure.

### Color Philosophy

The interface uses a warm milk-paper base so the screen feels restful rather than blank. **Gentle Nudge Blue** anchors interactive elements and reflects calm reliability, while red-orange appears only as a careful marker for an important task—not an alarm. Muted sage and lavender are used to distinguish a task’s context without asking the user to organize aggressively.

### Layout Paradigm

The product uses a **desk-note composition** rather than a centered dashboard. A pale “day marker” rail grounds the left side on desktop, while the main task sheet appears as a loose stack of reminder slips. On mobile, this collapses into an intentionally thumb-friendly single column with the primary capture control floating above the navigation.

### Signature Elements

1. **Offset paper cards:** Task surfaces have a gentle layered shadow and a small colored edge, like notes arranged on a desk.
2. **The blue nudge dot:** A round blue marker appears beside active tasks and as a repeating brand signal.
3. **Hand-drawn connective line:** A subtle organic rule joins schedule moments to imply continuity without hard grid lines.

### Interaction Philosophy

Interactions should feel acknowledgeable, not demanding. Completing a task gently folds it out of the active list; snoozing opens a light, highly legible choice tray. Capture is always available, speech is a first-class action, and every interaction provides immediate visual feedback.

### Animation

Motion is small and purposeful. Task cards lift 2px on hover and press inward on tap. Completed tasks fade and translate down by 8px over 220ms before moving to history. Panels enter with 95% scale plus opacity, using a snappy custom ease-out. Respect reduced-motion preferences by disabling nonessential movement.

### Typography System

**Fraunces** is used sparingly for the time-sensitive greeting and gentle headings, adding an editorial, human note. **DM Sans** handles all task titles, controls, and settings due to its clarity at small sizes. Headings use high contrast but never feel loud; task copy is generous at 16–18px.

### Brand Essence

**A private, offline-friendly memory assistant for people who want to remember life’s little things without adopting a productivity system.**

**Personality:** considerate, dependable, unhurried.

### Brand Voice

Headlines are direct and reassuring; CTAs are conversational and specific. The interface avoids commands and avoids urgency except where the user has deliberately marked something important.

Example lines:

> “Hold this thought. I’ll bring it back at the right time.”

> “A little later works too.”

### Wordmark & Logo

The mark is an **open blue circle interrupted by a small rising paper tab**, suggesting a gentle, recurring nudge without becoming a literal bell or checkmark. The wordmark pairs the Fraunces “G” with quiet, rounded sans-serif letterforms.

### Signature Brand Color

**Gentle Nudge Blue — `#3D70FF`**. It is used for voice capture, active tasks, and primary decisions; it should feel like a dependable tap on the shoulder.

## Style Decisions

- Task surfaces are **paper slips first and app cards second**. Each active reminder has warm paper material, a small contextual edge, slight irregular corners, and an offset-sheet shadow.
- The task list uses a dotted, hand-drawn-style blue connective line between moments. The side rails remain quiet desk furniture and recede behind the central task notes.
- **Gentle Nudge Blue `#3D70FF`** remains the sole active-signal color for capture, today focus, active nudges, and primary decisions. Sage, lavender, and red-orange only provide gentle context.
