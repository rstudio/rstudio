
## RStudio 1.4 "Juliet Rose" Release Notes


### Misc

* Improve detection for crashes that occur early in session initialization (#7983)
* The mouse back / forward buttons can now be used to navigate within the Help pane (#8338)
* Right-click on document tab provides menu with close, close all, close others (#1664)

### RStudio Server

* **BREAKING:** RStudio when served via `http` erroneously reported its own address as `https` during redirects if the header `X-Forwarded-Proto` was defined by a proxy. That could lead to a confusing proxy setup. That has been fixed, but existing proxy installations with redirect rewite settings matching for `https` may have to be adjusted.

### Bugfixes

* Fixed issue where reinstalling an already-loaded package could cause errors (#8265)
* Fixed issue where right-assignment with multi-line strings gave false-positive diagnostic errors (#8307)
* Fixed issue where restoring R workspace could fail when project path contained non-ASCII characters (#8321)
* Fixed issue where forked R sessions could hang after a package was loaded or unloaded (#8361)
* Fixed issue where attempting to profile lines ending in comment would fail (#8407)
