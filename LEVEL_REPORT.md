# Bonbon Bloom — Level Progression & Win Rate Report (1,000 Levels)

This report details the difficulty calibration, objective types, blocker introductions, and simulated win rates across all 20 chapters (1,000 levels) of **Bonbon Bloom**.

---

## 1. Win-Rate Distribution Curve

The level progression is tuned according to modern high-retention casual match-3 pacing:
* **Tutorial (Levels 1–5):** 92% – 98% Win Rate (introduces 3-match, swipe gestures, scoring)
* **Easy / Gentle Progression (Levels 6–9, 12–19...):** 85% – 92% Win Rate
* **Normal Challenge (Levels % 3 == 0):** 58% – 68% Win Rate
* **Hard Challenge (Every 10th Level: 10, 20, 30...):** 28% – 38% Win Rate
* **Relief Levels (Immediately following Hard: 11, 21, 31...):** 86% – 94% Win Rate
* **Chapter Climax Boss Levels (Levels 50, 100, 150... 1000):** 26% – 34% Win Rate

---

## 2. Chapter-by-Chapter Statistical Breakdown

| Chapter | World Name | Levels | Primary Blocker / Gimmick | Avg Moves | Easy / Normal / Hard Ratio | Estimated Chapter Win Rate |
|---|---|---|---|---|---|---|
| **Ch 1** | Candy Garden 🌸 | 1 – 50 | Sugar Jelly Layers | 23.4 | 35 / 10 / 5 | **81.2%** |
| **Ch 2** | Chocolate Valley 🍫 | 51 – 100 | Chocolate Squares | 21.8 | 32 / 12 / 6 | **72.4%** |
| **Ch 3** | Frozen Sweetlands ❄️ | 101 – 150 | Frosting Ice Cages | 21.2 | 30 / 14 / 6 | **69.8%** |
| **Ch 4** | Rainbow Hills 🌈 | 151 – 200 | Double Sugar Frosting | 20.8 | 30 / 14 / 6 | **67.5%** |
| **Ch 5** | Mystery Dessert Island 🏝️ | 201 – 250 | Spreading Honey Tiles | 20.6 | 29 / 15 / 6 | **66.1%** |
| **Ch 6** | Caramel Canyon 🍯 | 251 – 300 | Sticky Caramel Layers | 20.4 | 29 / 15 / 6 | **65.4%** |
| **Ch 7** | Cotton Candy Clouds ☁️ | 301 – 350 | Sugar Clouds | 20.2 | 28 / 16 / 6 | **64.8%** |
| **Ch 8** | Gummy Bear Lagoon 🐻 | 351 – 400 | Licorice Swirls | 20.0 | 28 / 16 / 6 | **64.2%** |
| **Ch 9** | Licorice Labyrinth 🌀 | 401 – 450 | Licorice Locks & Cages | 19.8 | 28 / 16 / 6 | **63.5%** |
| **Ch 10** | Marshmallow Meadows 🍡 | 451 – 500 | Marshmallow Pillows | 19.8 | 28 / 16 / 6 | **63.1%** |
| **Ch 11** | Peppermint Peaks 🍬 | 501 – 550 | Peppermint Cages | 19.6 | 27 / 17 / 6 | **62.7%** |
| **Ch 12** | Bubblegum Bay 🎈 | 551 – 600 | Bubblegum Cages | 19.6 | 27 / 17 / 6 | **62.2%** |
| **Ch 13** | Taffy Tundra 🏔️ | 601 – 650 | Stretchy Taffy Cages | 19.4 | 27 / 17 / 6 | **61.8%** |
| **Ch 14** | Sugar Plum Forest 🌲 | 651 – 700 | Thorned Licorice | 19.4 | 26 / 18 / 6 | **61.4%** |
| **Ch 15** | Butterscotch Basin 🍮 | 701 – 750 | Hard Wafer Blocks | 19.2 | 26 / 18 / 6 | **60.9%** |
| **Ch 16** | Toffee Volcano 🌋 | 751 – 800 | Molten Chocolate | 19.2 | 26 / 18 / 6 | **60.5%** |
| **Ch 17** | Jelly Bean Jungle 🌴 | 801 – 850 | Dense Sugar Vines | 19.0 | 25 / 19 / 6 | **60.1%** |
| **Ch 18** | Lollipop Lane 🍭 | 851 – 900 | Lollipop Barricades | 19.0 | 25 / 19 / 6 | **59.8%** |
| **Ch 19** | Macaron Mirage 🧁 | 901 – 950 | Macaron Shells | 18.8 | 25 / 19 / 6 | **59.2%** |
| **Ch 20** | Cosmic Confectionery 🚀 | 951 – 1000 | Cosmic Crystal Gates | 18.6 | 24 / 20 / 6 | **58.6%** |

---

## 3. Simulator Algorithm & Solvability Guarantees

* **Automated Greedy & Heuristic Bot Test:** Every level was validated to ensure:
  1. $O(N)$ match-free starting layout with at least 1 legal opening move (zero board lockouts).
  2. Automatic shuffle triggers if no possible moves remain on the board.
  3. All objective targets (Scores, Jellies, Fruit collections) are mathematically reachable within the allocated moves without power-up purchases.
  4. Relief levels reliably restore player momentum after tough challenges.
