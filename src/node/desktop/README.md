# Electron Prototype
Proof-of-concept RStudio Desktop prototype using Electron instead of QtWebEngine.

Only runnable within context of an RStudio development environment.

## Running Electron prototype (macOS)
- clone rstudio repo
- checkout and build branch `prototype/electron`
    - only build desktop, and disable crashpad by renaming or deleting /opt/rstudio/crashpad and generate cmake via:
        - `-DRSTUDIO_TARGET=Desktop -DRSTUDIO_CRASHPAD_ENABLED:BOOL=OFF -G Ninja`
    - must build into `rstudio/src/cpp/build` (otherwise tweak relative path in `main.js` to match)
- `ant desktop` from `rstudio/src/gwt` (or do a full `ant` beforehand)
- from `rstudio/src/node/desktop`
    - `yarn`
    - `yarn start`

## Initial Creation
- `mkdir -p rstudio/src/node/desktop/src`
- `cd rstudio/src/node/desktop`
- `yarn init`
- `yarn add electron@latest`
- tweak package.json to support running via `yarn start`, and to use `./src/main/app.js`
- `yarn`
- write code and test with `yarn start`
