RStudio Server Global Fonts
===========================

RStudio's code editor and R console use a fixed-width font. By default users
must choose a fixed-width font supported by their browser for their R code. If
you wish to make additional fixed-width fonts available to your users, you can
place them in this directory (/etc/rstudio/fonts).

Fonts placed here will be automatically made available for selection in
RStudio's Appearances settings (Tools -> Global Options -> Appearance). 

Supported Formats
-----------------

The following font formats are supported:

- Web Open Font Format (.woff, .woff2)
- OpenType (.otf)
- Embedded OpenType (.eot)
- TrueType (.ttf)

Only fixed-width fonts are supported. Proportional fonts will cause cursor
positioning problems.

Naming and Directory Structure
------------------------------

The name of the file is presumed to be the name of the font. If you wish to
give the font a custom name, you can place it in a directory with your name of
choice. For example:

    + fonts/
    |
    +-- Coding-Font.ttf
    |
    +-- Coding Font Two/
        |
        +-- CodingFont2-Regular.woff

This directory structure would make two fonts available, *Coding-Font* and
*Coding Font Two*.

Some fonts come in many different weights and styles. If you want these weights
and styles to be treated as single font, you can place them underneath a single
folder. This is useful when a theme uses bold or italic variants of a font to
decorate code (e.g., to set comments in italics).

To do this, create subfolders with the font's weight or style. For example,
this creates a single font, "Coding Font 3", which has two weights (400 and 700
for regular and bold, respectively) and an italic style for each weight.

    + fonts/
    |
    +-- Coding Font Three/
        |
        +-- 400/
        |   |
        |   +-- CodingFont3-Regular.woff
        |   |
        |   +-- italic/
        |       |
        |       +-- CodingFont3-Italic.woff
        |
        +-- 700/
            |
            +-- CodingFont3-Bold.woff
            |
            +-- italic/
                |
                +-- CodingFont3-BoldItalic.woff

Defaults and User Configuration
-------------------------------

Fonts can also be installed for individual users rather than for the entire
server. The folder `~/.config/rstudio/fonts` works exactly as
`/etc/rstudio/fonts` does. In the case where a font exists in both folders, the
user font overrides the system font.

It is also possible to set a default font for all RStudio Server users using
the `server_editor_font_enabled` and `server_editor_font` options in
`rstudio-prefs.json`.. See the *Session User Settings* chapter of the RStudio
Server Pro Administration Guide for more information.

Browser Fonts
-------------

RStudio Server attempts to automatically detect available fixed-width fonts
that are installed on a user's browser. For security reasons, it is not
possible for RStudio Server to enumerate all the fonts on the user's machine,
so a known list of popular programming fixed-width fonts are checked for
compatibility. This list is stored in the option `browser_fixed_width_fonts` in
`rstudio-prefs.json`.




