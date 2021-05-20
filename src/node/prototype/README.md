# Electron Prototype

This folder contains a throwaway proof-of-concept for the Electron desktop project. The code should not be taken as an example of _good_ TypeScript code.

## Building

Steps below assume `rstudio/rstudio` repo is cloned to `~/rstudio`. Adjust as needed.

Requires all developer dependencies to have been installed. Additionally, requires `yarn` and `node.js` installed and on the path. Strongly recommend using `nvm` to manage node versions, and select v14.x (latest LTS release).

- Build the C++ code, generated into `rstudio/build`; this is where the build is generated if you open the root of the repo into VSCode; to do it manually:
  - `cd ~/rstudio`
  - `mkdir build`
  - `cd build`
  - `cmake -GNinja ..`
  - `ninja`
- Build the GWT code, you can either use `ant desktop` or a full `ant` build:
  - `cd ~/rstudio/src/gwt`
  - `ant` or `ant desktop`
- Install Electron dependencies:
  - `cd ~/rstudio/src/node/prototype`
  - `yarn`
- Tweak and run the prototype:
  - The path to R is hardcoded and must be edited to match your system; edit `~/rstudio/src/node/prototype/src/main/main.ts` and look for `prepareEnvironment()`
- Run the prototype:
  - `yarn start`
  - If you are using `ant desktop` you might get a blank window, if so, close and restart and it should be fine after that
