# Inventory Interaction Highlight

[![RuneLite Plugin Hub](https://img.shields.io/badge/RuneLite-Plugin%20Hub-blue.svg)](https://runelite.net/plugin-hub)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A customizable RuneLite plugin that provides dynamic visual overlays for items during **hover**, **active click press**, **item selection ("Use")**, **shift-drop**, **bank vault interaction**, and **item filtering**.

![Plugin Icon](icon.png)

---

## 🌟 Key Features

* **Customizable Hover Highlights**:
  * **Outline Styles**: *Item Silhouette*, *Corner Brackets*, or *Box*.
  * **Fill Styles**: *Item Silhouette*, *Background Only*, or *Box*.
  * Adjustable border width (1–5px) and custom fill opacity.
  <img width="104" height="48" alt="hover_highlight" src="https://github.com/user-attachments/assets/730a16ba-02cc-4140-911e-b8b19dd1eb04" />

* **Tactile Active Click Feedback**:
  * Insets highlight (1px) and boosts brightness during mouse press down for tactile visual feedback.
  <img width="104" height="52" alt="click_effect" src="https://github.com/user-attachments/assets/f1947770-81d3-430f-be08-29e56c8bb083" />

* **Game-Tick Synchronized Selection Flash**:
  * Rhythmically flashes a background-only fill once per server tick when an item is selected for **"Use"**.
  <img width="104" height="52" alt="select_flash" src="https://github.com/user-attachments/assets/e1488046-ddf0-4b7e-9782-5644643529c7" />

* **Dynamic Shift-Drop Highlighting**:
  * Automatically switches highlight color to Red when the default left-click action is **"Drop"** (e.g. while holding `Shift`).

* **Bank Interface Highlighting**:
  * Dynamic highlight overlays for items within the main bank vault interface with optional support for empty bank placeholders.

* **Ignored Items Filtering**:
  * Easily exclude specific items from highlights using a comma-separated list supporting wildcard patterns (e.g. `Coins, *bones, Rune*`).

---

## ⚙️ Configuration Menu

The plugin settings are organized into 5 clean, intuitive sections:

### 1. 📁 **General Settings**
* **Highlight Color**: Main color picker used across hover highlights, click feedback, and selection flashing (Default: *Quest Helper Cyan Blue*).

### 2. 📁 **Hover Settings**
* **Enable Hover Highlight**: Toggle hover overlays.
* **Outline Settings**: Toggle outline, choose style (*Silhouette*, *Corner Brackets*, *Box*), and set border width.
* **Fill Settings**: Toggle fill, choose style (*Silhouette*, *Background Only*, *Box*), and set opacity.

### 3. 📁 **Interaction Settings**
* **Enable Click Press Feedback**: Toggle 1px inset & brightness boost during mouse press down.
* **Enable Selection Flash**: Toggle 300ms game-tick flash on selected items ("Use").
* **Enable Drop Highlight**: Toggle dynamic red highlight when default left-click option is "Drop".
* **Drop Color**: Custom color picker for drop highlights (Default: *Red*).

### 4. 📁 **Bank Interface Settings**
* **Enable Bank Highlight**: Toggle highlight overlay when hovering over items in the main bank vault.
* **Highlight Placeholders**: Toggle whether empty bank placeholder items should be highlighted.

### 5. 📁 **Ignored Items Settings**
* **Enable Ignored Items**: Toggle ignoring specific item names from highlight overlays.
* **Ignored Items**: Comma-separated list of item names or wildcard patterns to exclude from highlights (case-insensitive, e.g. `Coins, *bones, Rune*`).

---

## 📥 Installation

Search for **"Inventory Interaction Highlight"** in the official RuneLite **Plugin Hub** in-game and click **Install**.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
