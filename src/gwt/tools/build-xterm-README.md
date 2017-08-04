Updating xterm.js for RStudio
=============================================================================

To take a new drop of xterm.js for RStudio's terminal, follow these steps.

Getting the Code
-----------------------------------------------------------------------------

- Visit https://github.com/sourcelair/xterm.js/releases and get the hash of 
the version to integrate.

- In the RStudio source tree, change to `src/gwt/tools` and edit the shell
script build-xterm, updating the `git checkout` command and the preceding
comment with the new hash and build version.

- Remove existing xterm.js folder, i.e. `rm -rf src/gwt/tools/xterm.js`.

- Execute `./build-xterm` to copy and minify the JavaScript code and
make changes to the css allowing RStudio to do terminal theming.

- If successful, it will finish with **Done!**. If it fails, 
`tweak-xterm-css.R` requires updates.

Stylesheet Updates
-----------------------------------------------------------------------------
There are several steps in the stylesheet processing.

1) `build-xterm` copies xterm.css to 
`rstudio/src/gwt/src/org/rstudio/studio/client/workbench/views/terminal/xterm/xterm-orig.css`.
This is not used by RStudio, but is kept in source control so you can easily
examine what changed.

2) `tweak-xterm-css.R` runs at the end of `build-xterm` removing rules 
from xterm.css requiring theme-specific adjustments to `color:` and/or 
`background-color:`, saving the result in 
`rstudio/src/gwt/src/org/rstudio/studio/client/workbench/views/terminal/xterm/xterm.css`.
This is the stylesheet used by RStudio's terminal.

3) `compile-themes.R` updates the stylesheet used by the Ace editor, injecting 
additional rules needed by the terminal. This includes modified versions 
of those removed in the previous step, plus additional ones needed by 
the Console ANSI code feature.

Evaluating the Stylesheet Changes
-----------------------------------------------------------------------------
This part is tedious and error-prone. First, examine the modifications to
xterm-orig.css. If there are none, then you are done with this step and can 
proceed to evaluating the JavaScript changes.

Otherwise, follow these steps in order and you can reduce the risk of 
missing something.

1) Look for changes to declarations within selectors being targeted by
`tweak-xterm.css.R`. That is, the selectors haven't changed but the 
styles within have. You must then update `compile-themes.R` to emit
rules that take these changes into account.

2) If an existing selector's rules have changed such that there is no longer
a `color:` or `background-color:` declaration, then remove this rule from
`tweak-xterm-css.R` and `compile-themes.R`.

3) If the `tweak-xterm.css.R` step failed, adjust that script to match the 
obsolete selectors, i.e. remove selectors that have been eliminated from 
`tweak-xterm.css.R` and `compile-themes.R`

4) Examine changes in xterm.orig.css from the previous, and find any
rules containing `color:` and/or `background:`. Update `tweak-xterm.css.R`
to remove this rule, then update `compile-themes.R` to inject these into
the stylesheets it creates, with suitable colors substituted (follow 
existing examples).

Evaluting the JavaScript Changes
-----------------------------------------------------------------------------
Ideally, this would be completely handled by running the client-side unit 
tests via **ant unittest**. This is not currently the case, however on the
positive side, XTerm.js 2.x releases have rarely required code changes in 
RStudio except when we wanted to take advantage of new functionality.

TODO - author unit tests to more fully evaluate a new xterm.js, 
or provide more details on non-obvious problems to watch for in manual
testing.
