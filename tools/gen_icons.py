#!/usr/bin/env python3
"""Generates the bundled vector drawables (item, category, template, UI and launcher icons).
Run from the project root: python3 tools/gen_icons.py"""
import os

OUT = "app/src/main/res/drawable"
NAVY, TEAL, TEAL_L, YEL, CORAL, CREAM, WHITE = "#1F2A44", "#5FA8A0", "#A9D6CF", "#F6C343", "#E38B7A", "#FFF8EC", "#FFFFFF"
SW = "1.5"

def rr(x, y, w, h, r):
    r = min(r, w / 2, h / 2)
    return (f"M{x+r},{y} H{x+w-r} A{r},{r} 0 0 1 {x+w},{y+r} V{y+h-r} A{r},{r} 0 0 1 {x+w-r},{y+h} "
            f"H{x+r} A{r},{r} 0 0 1 {x},{y+h-r} V{y+r} A{r},{r} 0 0 1 {x+r},{y} Z")

def circ(cx, cy, r):
    return f"M{cx-r},{cy} A{r},{r} 0 1 0 {cx+r},{cy} A{r},{r} 0 1 0 {cx-r},{cy} Z"

def shape(d, fill=None, stroke=NAVY, sw=SW):
    a = [f'android:pathData="{d}"']
    if fill: a.append(f'android:fillColor="{fill}"')
    if stroke:
        a += [f'android:strokeColor="{stroke}"', f'android:strokeWidth="{sw}"',
              'android:strokeLineCap="round"', 'android:strokeLineJoin="round"']
    return "<path " + " ".join(a) + " />"

def line(d, stroke=NAVY, sw=SW):
    return shape(d, None, stroke, sw)

def write(name, parts, size=24, vp=24, tint=None):
    t = f' android:tint="{tint}"' if tint else ""
    body = "\n    ".join(parts)
    with open(os.path.join(OUT, name + ".xml"), "w") as f:
        f.write(f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="{size}dp" android:height="{size}dp"
    android:viewportWidth="{vp}" android:viewportHeight="{vp}"{t}>
    {body}
</vector>
''')

items = {
    "notebook": [shape(rr(5, 3, 14, 18, 1.5), TEAL_L), line("M8,3 V21"), line("M11,8 H16 M11,11 H16")],
    "workbook": [shape(rr(5, 3, 14, 18, 1.5), YEL), shape(rr(8, 7, 8, 5, 0.8), WHITE), line("M8,16 H16")],
    "book": [shape("M3,5 C6,4 9,4 12,6 C15,4 18,4 21,5 V19 C18,18 15,18 12,20 C9,18 6,18 3,19 Z", TEAL_L), line("M12,6 V20")],
    "sketchbook": [shape(rr(4, 5, 16, 15, 1.5), WHITE), line("M7,3 V7 M10,3 V7 M13,3 V7 M16,3 V7"), line("M7,16 L10,12 L12,14 L14,11 L17,16")],
    "pencil": [shape("M4,20 L5,15 L16,4 L20,8 L9,19 Z", YEL), line("M14,6 L18,10"), shape("M4,20 L5,15 L9,19 Z", CREAM)],
    "pencil_case": [shape(rr(3, 8, 18, 10, 4), CORAL), line("M5,11 H19"), shape(circ(18, 11, 1.2), YEL)],
    "ruler": [shape("M3,16 L16,3 L21,8 L8,21 Z", YEL), line("M7,12 L9,14 M10,9 L12,11 M13,6 L15,8")],
    "scissors": [shape(circ(7, 17, 3), CORAL), shape(circ(17, 17, 3), CORAL), line("M9,15 L17,3 M15,15 L7,3")],
    "brushes": [shape("M10,3 H14 V13 H10 Z", YEL), shape("M9,13 H15 V15 C15,18 13,21 12,21 C11,21 9,18 9,15 Z", TEAL)],
    "paints": [shape("M12,3 C7,3 3,7 3,12 C3,17 7,21 11,21 C13,21 13,19 12,17 C11,15 13,14 15,14 H17 C19,14 21,13 21,10 C21,6 17,3 12,3 Z", CREAM),
               shape(circ(8, 10, 1.5), CORAL), shape(circ(12, 7, 1.5), YEL), shape(circ(16, 9, 1.5), TEAL)],
    "water_bottle": [shape(rr(10, 2, 4, 3, 0.5), NAVY), shape(rr(7, 5, 10, 17, 3), TEAL_L), line("M7,12 H17")],
    "lunchbox": [shape(rr(3, 8, 18, 12, 2), CORAL), line("M9,8 V5 H15 V8"), line("M3,13 H21"), shape(rr(10, 12, 4, 2.5, 0.6), YEL)],
    "snack": [shape("M12,7 C8,4 4,7 4,12 C4,17 8,21 12,19 C16,21 20,17 20,12 C20,7 16,4 12,7 Z", CORAL), line("M12,7 C12,5 13,3 15,3")],
    "sandwich": [shape("M3,12 L12,4 L21,12 Z", YEL), shape(rr(3, 12, 18, 3, 1), TEAL), shape(rr(3, 15, 18, 4, 1), YEL)],
    "jacket": [shape("M8,3 L4,6 L3,21 H10 V8 Z M16,3 L20,6 L21,21 H14 V8 Z", TEAL), line("M8,3 L12,7 L16,3 M12,7 V21"), line("M10,21 H14")],
    "shirt": [shape("M8,3 L3,7 L5,11 L7,10 V21 H17 V10 L19,11 L21,7 L16,3 C15,5 9,5 8,3 Z", TEAL_L)],
    "sports_clothes": [shape("M5,4 H19 L21,20 H14 L12,11 L10,20 H3 Z", TEAL), line("M5,8 H19")],
    "trainers": [shape("M2,17 V11 L7,9 L10,12 L16,13 C19,13.5 22,15 22,17 Z", WHITE), shape(rr(2, 17, 20, 3, 1), NAVY, None), line("M9,11 L11,10 M11,12.5 L13,11.5")],
    "apron": [line("M8.5,8 C8.5,3 15.5,3 15.5,8"), shape("M8,8 H16 L18,12 V21 H6 V12 Z", CORAL), line("M6,12 H3 M18,12 H21"), shape(rr(9, 14, 6, 4, 1), CREAM)],
    "hat": [shape("M5,15 C5,9 8,6 12,6 C16,6 19,9 19,15 Z", YEL), shape(rr(3, 15, 18, 3, 1.5), CORAL), shape(circ(12, 5, 1.5), CORAL)],
    "towel": [shape(rr(4, 3, 14, 18, 1.5), TEAL_L), shape("M18,6 H20 V18 H18", None), line("M4,15 H18 M4,17 H18")],
    "tissues": [shape(rr(3, 10, 18, 10, 2), TEAL_L), shape("M9,10 C9,6 11,4 12,4 C13,4 15,6 15,10", WHITE), line("M3,14 H21")],
    "activity_kit": [shape("M4,4 H11 V8 C13,8 13,11 11,11 V12 H4 Z", YEL), shape("M13,4 H20 V12 H16 C16,10 13,10 13,12 Z", TEAL),
                     shape("M4,13 H11 V20 H4 Z", CORAL), shape("M13,13 H20 V20 H13 Z", TEAL_L)],
    "keys": [shape(circ(8, 9, 5), YEL), shape(circ(8, 9, 1.6), WHITE), line("M12,12 L20,20 M17,17 L19,15 M15,15 L16.5,13.5")],
    "travel_ticket": [shape("M3,7 H21 V10 C19.5,10 19.5,14 21,14 V17 H3 V14 C4.5,14 4.5,10 3,10 Z", YEL), line("M15,7 V17", NAVY, "1.2"), line("M7,11 H12 M7,13 H11")],
    "glasses": [shape(circ(7, 14, 4), TEAL_L), shape(circ(17, 14, 4), TEAL_L), line("M11,13 C11.5,12 12.5,12 13,13 M3,14 L2,9 M21,14 L22,9")],
    "ball": [shape(circ(12, 12, 9), WHITE), shape("M12,8 L15.5,10.5 L14,14.5 H10 L8.5,10.5 Z", NAVY), line("M12,3 V8 M21,11 L15.5,10.5 M17,20 L14,14.5 M7,20 L10,14.5 M3,11 L8.5,10.5")],
    "umbrella": [shape("M3,12 C3,7 7,3 12,3 C17,3 21,7 21,12 Z", CORAL), line("M12,12 V19 C12,21 9,21 9,19")],
    "headphones": [line("M4,15 V12 C4,7 8,4 12,4 C16,4 20,7 20,12 V15"), shape(rr(3, 14, 5, 7, 1.5), TEAL), shape(rr(16, 14, 5, 7, 1.5), TEAL)],
    "wallet": [shape(rr(3, 6, 18, 14, 2), TEAL), shape(rr(14, 10, 7, 6, 1.5), YEL), shape(circ(17, 13, 1), NAVY, None)],
    "generic": [shape(rr(4, 7, 16, 13, 2), CREAM), shape("M4,7 L7,3 H17 L20,7", TEAL_L), shape(rr(9, 10, 6, 3, 1), YEL)],
}
for k, p in items.items():
    write("ic_item_" + k, p)

cats = {
    "books": [shape("M3,5 C6,4 9,4 12,6 C15,4 18,4 21,5 V19 C18,18 15,18 12,20 C9,18 6,18 3,19 Z", WHITE), line("M12,6 V20")],
    "food": [shape(rr(3, 8, 18, 12, 2), WHITE), line("M9,8 V5 H15 V8 M3,13 H21")],
    "clothes": [shape("M8,3 L3,7 L5,11 L7,10 V21 H17 V10 L19,11 L21,7 L16,3 C15,5 9,5 8,3 Z", WHITE)],
    "tools": [shape("M4,20 L5,15 L16,4 L20,8 L9,19 Z", WHITE), line("M14,6 L18,10")],
    "important": [shape("M12,2.5 L14.8,8.4 L21.2,9.1 L16.4,13.4 L17.8,19.8 L12,16.5 L6.2,19.8 L7.6,13.4 L2.8,9.1 L9.2,8.4 Z", YEL)],
}
for k, p in cats.items():
    write("ic_cat_" + k, p)

tpls = {
    "school": [shape("M3,10 L12,4 L21,10 V20 H3 Z", TEAL_L), shape(rr(10, 14, 4, 6, 0.5), YEL), shape(circ(12, 10, 1.6), WHITE)],
    "sport": items["ball"],
    "art": items["paints"],
    "weekend": [shape(circ(12, 12, 4.5), YEL), line("M12,2.5 V4.5 M12,19.5 V21.5 M2.5,12 H4.5 M19.5,12 H21.5 M5.3,5.3 L6.7,6.7 M17.3,17.3 L18.7,18.7 M5.3,18.7 L6.7,17.3 M17.3,6.7 L18.7,5.3")],
    "trip": [shape(rr(5, 7, 14, 13, 2), CORAL), line("M9,7 V4 H15 V7 M9,10 V17 M15,10 V17")],
    "custom": [shape("M4,10 V4 H10 L20,14 L14,20 Z", YEL), shape(circ(7.5, 7.5, 1.4), WHITE)],
}
for k, p in tpls.items():
    write("ic_tpl_" + k, p)

# Monochrome UI icons (tinted at use site).
ui = {
    "back": [line("M19,12 H5 M11,6 L5,12 L11,18", "#000000", "2")],
    "check": [line("M5,12.5 L10,17.5 L19,7", "#000000", "2.4")],
    "history": [line("M4,12 A8,8 0 1 0 6.3,6.3 M3,4 V8 H7 M12,8 V12 L15,14", "#000000", "2")],
    "parents": [shape(circ(9, 8, 3), None, "#000000", "2"), shape(circ(16.5, 9.5, 2.2), None, "#000000", "2"),
                line("M3,20 C3,15 6,13 9,13 C12,13 15,15 15,20 M15,14 C18,14 21,15.5 21,20", "#000000", "2")],
    "refresh": [line("M20,12 A8,8 0 1 1 17.7,6.3 M20,4 V8 H16", "#000000", "2")],
    "add": [line("M12,5 V19 M5,12 H19", "#000000", "2.2")],
    "edit": [line("M4,20 H8 L19,9 L15,5 L4,16 Z M13,7 L17,11", "#000000", "2")],
    "delete": [line("M4,7 H20 M9,7 V4 H15 V7 M6,7 L7,20 H17 L18,7 M10,11 V16 M14,11 V16", "#000000", "2")],
    "up": [line("M12,19 V5 M6,11 L12,5 L18,11", "#000000", "2")],
    "down": [line("M12,5 V19 M6,13 L12,19 L18,13", "#000000", "2")],
    "copy": [shape(rr(8, 8, 12, 12, 2), None, "#000000", "2"), line("M16,8 V5 A1,1 0 0 0 15,4 H5 A1,1 0 0 0 4,5 V15 A1,1 0 0 0 5,16 H8", "#000000", "2")],
    "close": [line("M6,6 L18,18 M18,6 L6,18", "#000000", "2.2")],
    "missing": [shape(circ(12, 12, 9), None, "#000000", "2"), line("M12,7 V13 M12,16.5 V17", "#000000", "2.4")],
    "lock": [shape(rr(5, 10, 14, 11, 2), None, "#000000", "2"), line("M8,10 V7 A4,4 0 0 1 16,7 V10", "#000000", "2")],
    "chevron": [line("M9,5 L16,12 L9,19", "#000000", "2")],
}
for k, p in ui.items():
    write("ic_ui_" + k, p)

# Launcher foreground: backpack with checkmark, inside the 66dp safe zone of a 108dp canvas.
bp = [
    shape("M44,30 C44,22 64,22 64,30", None, NAVY, "3.5"),
    shape(rr(33, 30, 42, 50, 12), TEAL, NAVY, "3"),
    shape(rr(39, 54, 30, 20, 6), "#7DBDB5", NAVY, "2.5"),
    line("M39,61 H69", NAVY, "2"),
    shape(circ(68, 39, 11), YEL, NAVY, "2.5"),
    line("M63,39 L67,43 L73.5,35.5", NAVY, "3"),
]
write("ic_launcher_foreground", bp, size=108, vp=108)
mono = [
    shape("M44,30 C44,22 64,22 64,30", None, "#000000", "3.5"),
    shape(rr(33, 30, 42, 50, 12), None, "#000000", "3.5"),
    shape(rr(39, 54, 30, 20, 6), None, "#000000", "3"),
    line("M58,40 L64,46 L74,34", "#000000", "4"),
]
write("ic_launcher_monochrome", mono, size=108, vp=108)
print("icons:", len(items), len(cats), len(tpls), len(ui))
