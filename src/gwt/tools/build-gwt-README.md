# GWT for RStudio

## RStudio 1.3
RStudio uses a custom build of GWT 2.8.2, with the resulting SDK checked directly into
the repo under `rstudio/src/gwt/lib/gwt/gwt-rstudio`.

The GWT fork is at https://github.com/rstudio/gwt. The customizations are in the `rstudio/v1.3` 
branch, based on GWT 2.8.2 (branched from `tags/2.8.2`).

## RStudio 1.4
RStudio uses a custom build of GWT 2.9.0, in the `rstudio/v1.4` branch.

### Changing GWT
To make GWT changes, use `./sync-gwt` to clone the repo into `./gwt/gwt`, and `./build-gwt` to
compile the SDK and copy the results into the source tree.

Changes must be submitted both to `rstudio` to update the SDK included in the repo, and to
the `gwt` repo to track changes to the sources.

### Updating to a new GWT
See comments in the `./sync-gwt` and `./build-gwt` scripts on steps to updating to a new 
GWT SDK version and/or a new RStudio release.
