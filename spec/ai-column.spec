# Introduction

This document outlines requirements for adding a new pane to the RStudio IDE to display AI
chat features.

Currently RStudio has four quadrants:

- Source quadrant: only shown if at least one file or other data view is open
- Console quadrant: the RStudio console and terminal, plus other assorted tabs
- TabSet1 and TabSet2: these two regions hold a variety of other panes such as Files and Environment

Additionally, users can create up to three "Source Columns" which are full height and shown to
the left of the other quqdrants.

The screenshot in @spec/layout.png shows an example of the layout with one source column added.

Users can customize some aspects of this layout via "Pane Layout" in Global Options, as seen
in the screenshot @spec/pane-layout-options.png.

In this document we refer to these "regions" as panes and the individual features shown within
them as "tabs" since they use a tabbed interface.

## New Pane

The "Chat" pane will be a new pane shown as a column either to the left or right of all other
panes. By default it will show on the right but a user preference will allow it to be displayed
on the left, instead. The pane should be full height, like the source columns.

For the initial implementation we will just show placeholder text in the pane reading
"AI Chat Coming Soon!" with a icon of a robot emjoi above it.

The vertical splitter between this pane and the others can be adjusted to make the pane
wider or narrower. There should be a reasonable minimum width. The maximum width will rely on
other panes having minimum widths.

## Step One

Come up with a recommendation on implementing this new pane as described. Think very hard.
