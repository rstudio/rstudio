## v1.3 - Release Notes

### Accessibility

* Dramatically improved accessibility for sight-impaired users, including:
  * Keyboard focus management and visibility upgrades
  * Improved keyboard navigation
  * Compatibility with popular screen readers
  * Compliant contrast ratios and other accessibility improvements
  * User preference for disabling user-interface animations such as when zooming panes

### Spell Check

* Real-time spell-checking engine for checking while editing
* Customizable dictionaries and word ignore lists preloaded with common R terms
* Inline correction suggestions

### Preferences and Configuration

* All user preferences and settings can now be set using a plain JSON file
* All user preferences can now have global defaults set by a system administrator
* New diagnostics commands for editing the prefs file, resetting state, and viewing pref system data
* Editor themes, snippets, file templates, and keybindings are now portable and can be installed by admins
* The content of new file templates (New R Script, New R Markdown, etc.) can now be controlled by users or administrators

### renv

* New projects can be initialized with renv, giving them an isolated project environment

### Miscellaneous

* Improved 'Comment / Uncomment' handling of empty lines around selection (#4163)
* Files with extension '.q' are no longer indexed or parsed as R files (#4696)
* Add automated crash handling and reporting
* Upgrade internal JSON parsing engine for speed improvements (#1830)
* Improved ergonomics for history prefix navigation (#2771)
* Make columns resizable in the Environment pane (#4020)
* Add Word Count command (#4237)
* Add "Safe Mode" for opening sessions without profile scripts or workspace restoration (#4338)
* PowerShell Core option in terminal (Windows-only)
* Custom terminal shell option for Windows desktop (previously only on Mac, Linux, and server)
* Keyboard shortcuts for main menu items on RStudio Server (e.g. Ctrl+Alt+F for File menu)
* Show number of characters when entering version control commit messages (#5192)
* Update embedded Qt to 5.12.5 for Chromium update, stability and bugfixes (#5399)
* Add preference for changing font size on help pane (#3282)
* Improved keyboard and touch support for iPadOS 13.1
* Support Ctrl+[ as Esc key on iPadOS 13.1 keyboards lacking physical Esc key (#4663)
* Added ability to mark auth cookies as secure via the `auth-cookies-force-secure` `rserver.conf` configuration setting

### Bugfixes

* Fix plot history when plot() called immediately after dev.off() (#3117)
* Fix diagnostics error with multibyte characters in R Markdown documents on Windows (#1866)
* Fix stale processes when invoking child R processes with large command lines (#3414)

### RStudio Professional

* Logging improvements; log destinations and levels are more configurable and can be changed in real time
* RStudio Desktop Pro can now function as a client for RStudio Server Pro

