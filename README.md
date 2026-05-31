# 🧪 Potion Cauldron: Alchemist Companion

**Potion Cauldron** (com.example.ui.CauldronApp) is a highly polished, immersive alchemical simulator and helper app designed for TTRPG gamers (such as D&D 5e / Pathfinder) navigating the **Galenor Campaign Timeline**. 

It provides an offline-first, Material 3 themed interface to manage active alchemical brews, discover recipes, handle alchemical catalyst residues, and dynamically resolve magical brewing attempts using standard polyhedral dice mechanics (d20 & d100).

---

## 🌟 Key Features

### 📅 Dynamics of the Galenor Campaign Timeline
*   **Time Block Progression:** Track campaign time across years, months, days, and discrete periods (`DAWN`, `NOON`, `DUSK`, `MIDNIGHT`).
*   **Active Time Synced Brewing:** Brewing times shrink as you advance the campaign time block-by-block.

### 🧪 Progressive Active Cauldrons
*   **Real-time Crafting Clocks:** Cauldron card timers dynamically decrease when you advance the campaign calendar.
*   **Detailed Analytics:** Each active cauldron shows the potion name, start-date stamp, remaining days calculated precisely via cost, and attached additives.

### 🔬 Alchemical Residue Catalysts (The d100 Anomaly Engine)
When crafting a potion, you can recycle standard or masterwork **residue elements** harvested from previous distillations to catalyze the process, triggering a mandatory **d100 percentage roll** for immediate chaotic effects:
*   **1 - 33 | Accelerated Batch:** Cooking duration is halved!
*   **34 - 66 | Potency Strain:** Craft's finished yield is 50% more potent.
*   **67 - 99 | Secondary Separation:** Gathers a bonus random potion of equivalent rarity on completion.
*   **100 | Supermutation!** The batch mutates completely, changing into a random high-tier *Rare* item!

### 🎲 Polyhedral Resolution System (The d20 Batch Quality Engine)
When active cooking timers hit zero, resolve the batch with a physical or simulator-driven **d20 roll**:
*   **Natural 1 (Critical Failure):** Half the batch's total volume is ruined.
*   **2 - 5 (Low Quality):** 25% chance of diluted potency, normal yield.
*   **6 - 15 (Standard Success):** Brews the exact quantity successfully.
*   **16 - 19 (Great Success):** Standard potion yield + extracts a *Standard Residue* catalyst for future recipes.
*   **Natural 20 (Masterwork Dilution):** Generates 150% potion yield + extracts a *Masterwork Catalyst* residue!

### 🗄️ Fully Persistent Architecture
*   Powered by a local **Room Database** providing robust local persistence of active pots, timeline settings, historical brewing logs, and residue catalog items.

---

## 🛠️ How It Works (The Gameplay Loop)

```
 [Select Recipe] ──> [Add Catalyst Residue?] ──> [d100 Anomaly Check]
                         │
                         ▼
                  [Active Cauldron] ──> [Advance Galenor Timeline]
                                                 │
                                                 ▼
                  [Archive Log] <── [Resolve with d20 Die Roll]
```

1.  **Review the Recipes:** Check the **Recipe Book** containing basic alchemical formulas categorized by rarity (Common, Uncommon, Rare) and pricing database.
2.  **Fire Up the Cauldron:** Click **Start Brewing** (FAB), set the target quantity, and optionally select a residue modifier.
3.  **Advance Kampanya Time:** In the dashboard top bar, advance the campaign calendar as time passes in your tabletop session.
4.  **Resolve the Potion:** Tap **Resolve** once cooking completes. Input your physical d100/d20 rolls or let the app evaluate the roll.
5.  **Reap the Harvest:** Move completed potions to historical archives, and extract any new alchemical catalysts directly into your inventory!

---

## 🎨 Design and Layout Structure

The applet utilizes Material 3 guidelines styled with custom tabletop aesthetics:
*   **Deep Obsidian Canvas (`#0C0A0E`):** A beautiful eye-friendly theme suited for dark-room gaming sessions.
*   **Arcane Colors:** Accented with *Wizard Purple* (`#8D5BFA`) and *Legendary Gold* (`#F1C40F`) to represent high-magic fantasy elements.
*   **Adaptive Icons:** Built with high-fidelity, resolution-independent vector art in `app_icon.xml`, rendering a sharp, scaling magical flask in any viewport or screen launcher.
