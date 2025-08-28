# Background

Currently we install node.js 22.18.0 with RStudio, but only the x86_64 version. We need to 
install both x86_64 and the arm64 version of node.

The dependency scripts put the x86_64 version in @dependencies/common/node/22.18.0-installed and
the arm64 version in @dependencies/common/node/22.18.0-arm64-installed.

The installation of the x86_64 version happens via @src/cpp/session/CMakeLists.txt, after
the comment `# install node`.

Do not add the installation steps for arm64 node to this file. Instead, follow the example of
installing `rsession-arm64` in @package/osx/cmake/prepare-package.cmake. You should use the
existing logic to decide if installing arm64 version of node (i.e. inside the following
`if(EXISTS "@RSESSION_ARM64_PATH@")` block).

We want the arm64 version of node installed in ${RSTUDIO_INSTALL_BIN}/node-arm64/bin/node.

## Task

Implement this change in `prepare-package.cmake`.
