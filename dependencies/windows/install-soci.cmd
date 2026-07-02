@echo off
cd install-soci
R --vanilla -s -f install-soci.R
if ERRORLEVEL 1 (
  echo !! ERROR: SOCI build failed.
  cd ..
  exit /b 1
)
cd ..
