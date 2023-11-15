# define package suffix
set(RSTUDIO_PACKAGE_SUFFIX "-")

# include overlay if it exists
if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/CMakeOverlay.txt")
   include(CMakeOverlay.txt)
endif()

# append our module directory to path
list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/cmake/modules")

# install ssh-console batch file into msys_ssh directory (called by shortcut)
if(NOT RSTUDIO_SESSION_WIN32)
   install(PROGRAMS src/ssh-console.bat
           DESTINATION "${RSTUDIO_INSTALL_BIN}/msys_ssh")
endif()

set(RSTUDIO_PACKAGE_FILE_NAME "${CPACK_PACKAGE_NAME}")

set(CPACK_PACKAGE_FILE_NAME "${RSTUDIO_PACKAGE_FILE_NAME}${RSTUDIO_PACKAGE_SUFFIX}${CPACK_PACKAGE_VERSION}")
if(NOT ${CMAKE_BUILD_TYPE} STREQUAL "Release")
   set(CPACK_PACKAGE_FILE_NAME "${CPACK_PACKAGE_FILE_NAME}-${CMAKE_BUILD_TYPE}")
endif()

string(REPLACE "+" "-" CPACK_PACKAGE_FILE_NAME "${CPACK_PACKAGE_FILE_NAME}")

if(RSTUDIO_ELECTRON)
   set(CPACK_NSIS_INSTALLED_ICON_NAME "rstudio.exe")
else()
   set(CPACK_NSIS_INSTALLED_ICON_NAME "bin\\\\rstudio.exe")
endif()

set(CPACK_PACKAGE_INSTALL_REGISTRY_KEY "RStudio")
if(NOT RSTUDIO_ELECTRON)
   set(CPACK_PACKAGE_EXECUTABLES "rstudio" "RStudio")
endif()
set(CPACK_INCLUDE_TOPLEVEL_DIRECTORY 0)

# To keep pathnames from getting too long, use a temp directory
file(MAKE_DIRECTORY  "c:\\\\temp\\\\ide-build")
set(CPACK_PACKAGE_DIRECTORY "c:\\\\temp\\\\ide-build")

# registry keys
if(RSTUDIO_ELECTRON)

   set(CPACK_NSIS_EXTRA_INSTALL_COMMANDS "
      WriteRegStr HKLM 'Software\\\\Microsoft\\\\Windows\\\\CurrentVersion\\\\App Paths\\\\rstudio.exe' '' '$INSTDIR\\\\rstudio.exe'

      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.R' ''
      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.RData' ''
      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.rda' ''
      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.Rd' ''
      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.Rnw' ''
      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.Rmd' ''
      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.Rmarkdown' ''
      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.qmd' ''
      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.Rhtml' ''
      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.Rpres' ''
      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.Rproj' ''
      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.rdprsp' ''

      WriteRegStr HKCR '.R\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.RData\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.rda\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.Rd\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.Rnw\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.Rmd\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.Rmarkdown\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.qmd\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.Rhtml\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.Rpres\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.Rproj\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.tex\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.md\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.mdtxt\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.markdown\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.htm\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.html\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.css\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.js\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.c\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.cpp\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.h\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.hpp\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.rdprsp\\\\OpenWithList\\\\rstudio.exe' '' '' 

      WriteRegStr HKCR '.Rproj' '' 'RStudio.Rproj'
      WriteRegStr HKCR 'RStudio.Rproj' '' 'R Project'
      WriteRegStr HKCR 'RStudio.Rproj\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-2'
      WriteRegStr HKCR 'RStudio.Rproj\\\\\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'
      
      System::Call 'Shell32::SHChangeNotify(i 0x8000000, i 0, i 0, i 0)'
   ")

else()

   set(CPACK_NSIS_EXTRA_INSTALL_COMMANDS "
      WriteRegStr HKLM 'Software\\\\Microsoft\\\\Windows\\\\CurrentVersion\\\\App Paths\\\\rstudio.exe' '' '$INSTDIR\\\\bin\\\\rstudio.exe'

      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\bin\\\\rstudio.exe \\\"%1\\\"'

      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.R' ''
      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.RData' ''
      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.rda' ''
      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.Rd' ''
      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.Rnw' ''
      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.Rmd' ''
      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.Rmarkdown' ''
      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.qmd' ''
      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.Rhtml' ''
      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.Rpres' ''
      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.Rproj' ''
      WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.rdprsp' ''

      WriteRegStr HKCR '.R\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.RData\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.rda\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.Rd\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.Rnw\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.Rmd\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.Rmarkdown\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.qmd\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.Rhtml\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.Rpres\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.Rproj\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.tex\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.md\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.mdtxt\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.markdown\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.htm\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.html\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.css\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.js\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.c\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.cpp\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.h\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.hpp\\\\OpenWithList\\\\rstudio.exe' '' ''
      WriteRegStr HKCR '.rdprsp\\\\OpenWithList\\\\rstudio.exe' '' '' 

      WriteRegStr HKCR '.Rproj' '' 'RStudio.Rproj'
      WriteRegStr HKCR 'RStudio.Rproj' '' 'R Project'
      WriteRegStr HKCR 'RStudio.Rproj\\\\DefaultIcon' '' '$INSTDIR\\\\bin\\\\rstudio.exe,-2'
      WriteRegStr HKCR 'RStudio.Rproj\\\\\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\bin\\\\rstudio.exe \\\"%1\\\"'
      
      System::Call 'Shell32::SHChangeNotify(i 0x8000000, i 0, i 0, i 0)'
   ")

endif()

set(CPACK_NSIS_EXTRA_UNINSTALL_COMMANDS "
   DeleteRegKey HKLM 'Software\\\\Microsoft\\\\Windows\\\\CurrentVersion\\\\App Paths\\\\rstudio.exe'
   DeleteRegKey HKCR 'Applications\\\\rstudio.exe'
   DeleteRegKey HKCR '.R\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR '.RData\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR '.rda\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR '.Rd\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR '.Rnw\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR '.Rmd\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR '.Rmarkdown\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR '.qmd\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR '.Rhtml\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR '.Rpres\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR '.Rproj\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR '.tex\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR '.md\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR '.mdtxt\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR '.markdown\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR '.htm\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR '.html\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR '.css\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR '.js\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR '.c\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR '.cpp\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR '.h\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR '.hpp\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR '.rdprsp\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR 'RStudio.Rproj'
#")

# build installer
include(CPack)

