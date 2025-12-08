## RStudio 2026.02.0 "Globemaster Allium" Release Notes

### New
#### RStudio
-

#### Posit Workbench
-

### Fixed
#### RStudio
- ([#16714](https://github.com/rstudio/rstudio/issues/16714)): Fixed an issue where formatting edits with air did not behave well with the editor undo stack

#### Posit Workbench
-

- ([#16521](https://github.com/rstudio/rstudio/issues/16521)): RStudio no longer emits rsession logs to the console when log-message-format=json is set
- (rstudio-pro#8919): Fixed an issue where duplicate project entries within a user's recent project list could cause their home page to fail to load
- (rstudio-pro#8846): Improved contrast of check boxes on Workbench homepage to meet 3:1 minimum contrast (AA)
- (rstudio-pro#9386): Fixed an issue where server fonts could not be used in load-balanced Workbench configurations

### Dependencies
- Copilot Language Server 1.393.0
- Electron 38.6.0
- (rstudio-pro#9126): Fixed an issue where `vscode-user-settings.json` and `positron-user-settings.json` were not found on `XDG_CONFIG_DIRS`
- (rstudio-pro#7638): Fixed an issue where the Workbench API was accessible with a basic license tier
- (rstudio-pro#9279): Fixed an error running Workbench Jobs from Positron and VScode in Slurm clusters that do not have singularity enabled

### Upgrade Instructions

#### Posit Workbench

We have removed support for managing R and Python version selectable for running a Workbench Job in Positron and VScode. JSON formatted versions files can now be added as runtimes using these commands,

```
# Add from r-versions file
sudo rstudio-server runtimes add --r-versions /path/to/r-versions --cluster=Local

# Add from py-versions file
sudo rstudio-server runtimes add --py-versions /path/to/py-versions.json --cluster=Local
```

### Dependencies
- Ace 1.43.5
- Copilot Language Server 1.395.0
- Electron 38.7.0
- Node.js 22.18.0
- Quarto 1.8.25
- Launcher 2.21.0
- rserver-saml 0.9.2
