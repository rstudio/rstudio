
## RStudio 1.4 "Juliet Rose" Release Notes


### Misc

* Added support for the `|>` pipe operator, and `\(x)` function shorthand syntax (#8543)
* Added preference toggle for inserting the |> pipe operator when the Insert Pipe Operator command is used (#8534)
* Improve detection for crashes that occur early in session initialization (#7983)
* The mouse back / forward buttons can now be used to navigate within the Help pane (#8338)
* Right-click on document tab provides menu with close, close all, close others (#1664)
* Rename File added to document tab context menu (#8374)
* Compilation of Sweave documents now uses tinytex when requested (#2788)
* Preliminary compatibility with UCRT builds of R-devel (#8461)
* Update Windows Desktop to openSSL 1.1.1i (#8574)
* Improve ordering of items in Command Palette list and search results (#7567, #7956)
* Update embedded Pandoc to v2.11.3

### RStudio Server

* **BREAKING:** RStudio when served via `http` erroneously reported its own address as `https` during redirects if the header `X-Forwarded-Proto` was defined by a proxy. That could lead to a confusing proxy setup. That has been fixed, but existing proxy installations with redirect rewite settings matching for `https` may have to be adjusted.

### Bugfixes

* Fix Windows Desktop installer to support running from path with characters from other codepages (#8421)
* Fixed issue where reinstalling an already-loaded package could cause errors (#8265)
* Fixed issue where right-assignment with multi-line strings gave false-positive diagnostic errors (#8307)
* Fixed issue where restoring R workspace could fail when project path contained non-ASCII characters (#8321)
* Fixed issue where forked R sessions could hang after a package was loaded or unloaded (#8361)
* Fixed issue where attempting to profile lines ending in comment would fail (#8407)
* Fixed issue where warnings + messages were mis-encoded in chunk outputs on Windows (#8565)
* Fixed issue where C++ compilation database was not invalidated when compiler was updated (#8588)
* Improved checks for non-writable R library paths on startup (Pro #2184)
* Code chunks in the visual editor now respect the "Tab Key Always Moves Focus" accessibility setting (#8584)
* The commands "Execute Previous Chunks" and "Execute Subsequent Chunks" now work when the cursor is outside a code chunk in the visual editor (#8500)



