@echo off
setlocal EnableDelayedExpansion

docker run --rm -it -v %CD%\..:C:\rstudio rstudio:windows
