# define package suffix
set(RSTUDIO_PACKAGE_SUFFIX "-")

# include overlay if it exists
if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/CMakeOverlay.txt")
   include(CMakeOverlay.txt)
endif()

# append our module directory to path
list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/cmake/modules")

set(RSTUDIO_PACKAGE_FILE_NAME "${CPACK_PACKAGE_NAME}")

set(CPACK_PACKAGE_FILE_NAME "${RSTUDIO_PACKAGE_FILE_NAME}${RSTUDIO_PACKAGE_SUFFIX}${CPACK_PACKAGE_VERSION}")
if(NOT ${CMAKE_BUILD_TYPE} STREQUAL "Release")
   set(CPACK_PACKAGE_FILE_NAME "${CPACK_PACKAGE_FILE_NAME}-${CMAKE_BUILD_TYPE}")
endif()

string(REPLACE "+" "-" CPACK_PACKAGE_FILE_NAME "${CPACK_PACKAGE_FILE_NAME}")

set(CPACK_NSIS_INSTALLED_ICON_NAME "rstudio.exe")
set(CPACK_NSIS_EXECUTABLES_DIRECTORY ".")
set(CPACK_PACKAGE_INSTALL_REGISTRY_KEY "RStudio")
set(CPACK_PACKAGE_EXECUTABLES "rstudio" "RStudio")
set(CPACK_INCLUDE_TOPLEVEL_DIRECTORY 0)

# To keep pathnames from getting too long, use a temp directory
file(MAKE_DIRECTORY  "c:\\\\temp\\\\ide-build")
set(CPACK_PACKAGE_DIRECTORY "c:\\\\temp\\\\ide-build")

# registry keys
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
   WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.tex' ''
   WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.Rtex' ''
   WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.py' ''
   WriteRegStr HKCR 'Applications\\\\rstudio.exe\\\\SupportedTypes' '.Rprofvis' ''

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
   WriteRegStr HKCR '.Rtex\\\\OpenWithList\\\\rstudio.exe' '' ''
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
   WriteRegStr HKCR '.py\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr HKCR '.Rprofvis\\\\OpenWithList\\\\rstudio.exe' '' ''

   WriteRegStr HKCR '.Rproj' '' 'RStudio.Rproj'
   WriteRegStr HKCR '.Rproj\\\\OpenWithProgids' 'RStudio.Rproj' ''
   WriteRegStr HKCR 'RStudio.Rproj' '' 'RStudio Project File'
   WriteRegStr HKCR 'RStudio.Rproj\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-2'
   WriteRegStr HKCR 'RStudio.Rproj\\\\\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr HKCR '.R' '' 'RStudio.r'
   WriteRegStr HKCR '.R\\\\OpenWithProgids' 'RStudio.r' ''
   WriteRegStr HKCR 'RStudio.r' '' 'R File'
   WriteRegStr HKCR 'RStudio.r\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-3'
   WriteRegStr HKCR 'RStudio.r\\\\\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr HKCR '.Rprofvis' '' 'RStudio.rprofvis'
   WriteRegStr HKCR '.Rprofvis\\\\OpenWithProgids' 'RStudio.rprofvis' ''
   WriteRegStr HKCR 'RStudio.rprofvis' '' 'R Profile File'
   WriteRegStr HKCR 'RStudio.rprofvis\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-3'
   WriteRegStr HKCR 'RStudio.rprofvis\\\\\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr HKCR '.css\\\\OpenWithProgids' 'RStudio.css' ''
   WriteRegStr HKCR 'RStudio.css' '' 'CSS File'
   WriteRegStr HKCR 'RStudio.css\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-4'
   WriteRegStr HKCR 'RStudio.css\\\\\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr HKCR '.htm\\\\OpenWithProgids' 'RStudio.html' ''
   WriteRegStr HKCR '.html\\\\OpenWithProgids' 'RStudio.html' ''
   WriteRegStr HKCR 'RStudio.html' '' 'HTML File'
   WriteRegStr HKCR 'RStudio.html\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-5'
   WriteRegStr HKCR 'RStudio.html\\\\\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr HKCR '.js\\\\OpenWithProgids' 'RStudio.js' ''
   WriteRegStr HKCR 'RStudio.js' '' 'JavaScript File'
   WriteRegStr HKCR 'RStudio.js\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-6'
   WriteRegStr HKCR 'RStudio.js\\\\\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr HKCR '.md\\\\OpenWithProgids' 'RStudio.markdown' ''
   WriteRegStr HKCR '.mdtxt\\\\OpenWithProgids' 'RStudio.markdown' ''
   WriteRegStr HKCR '.markdown\\\\OpenWithProgids' 'RStudio.markdown' ''
   WriteRegStr HKCR 'RStudio.markdown' '' 'Markdown File'
   WriteRegStr HKCR 'RStudio.markdown\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-7'
   WriteRegStr HKCR 'RStudio.markdown\\\\\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr HKCR '.qmd' '' 'RStudio.quarto'
   WriteRegStr HKCR '.qmd\\\\OpenWithProgids' 'RStudio.quarto' ''
   WriteRegStr HKCR 'RStudio.quarto' '' 'Quarto Markdown File'
   WriteRegStr HKCR 'RStudio.quarto\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-8'
   WriteRegStr HKCR 'RStudio.quarto\\\\\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr HKCR '.rda' '' 'RStudio.rdata'
   WriteRegStr HKCR '.rda\\\\OpenWithProgids' 'RStudio.rdata' ''
   WriteRegStr HKCR '.RData' '' 'RStudio.rdata'
   WriteRegStr HKCR '.RData\\\\OpenWithProgids' 'RStudio.rdata' ''
   WriteRegStr HKCR 'RStudio.rdata' '' 'R Data File'
   WriteRegStr HKCR 'RStudio.rdata\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-9'
   WriteRegStr HKCR 'RStudio.rdata\\\\\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr HKCR '.Rd' '' 'RStudio.rdoc'
   WriteRegStr HKCR '.Rd\\\\OpenWithProgids' 'RStudio.rdoc' ''
   WriteRegStr HKCR 'RStudio.rdoc' '' 'R Doc File'
   WriteRegStr HKCR 'RStudio.rdoc\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-10'
   WriteRegStr HKCR 'RStudio.rdoc\\\\\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr HKCR '.Rhtml' '' 'RStudio.rhtml'
   WriteRegStr HKCR '.Rhtml\\\\OpenWithProgids' 'RStudio.rhtml' ''
   WriteRegStr HKCR 'RStudio.rhtml' '' 'R HTML File'
   WriteRegStr HKCR 'RStudio.rhtml\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-11'
   WriteRegStr HKCR 'RStudio.rhtml\\\\\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr HKCR '.Rmd' '' 'RStudio.rmarkdown'
   WriteRegStr HKCR '.Rmd\\\\OpenWithProgids' 'RStudio.rmarkdown' ''
   WriteRegStr HKCR '.Rmarkdown' '' 'RStudio.rmarkdown'
   WriteRegStr HKCR '.Rmarkdown\\\\OpenWithProgids' 'RStudio.rmarkdown' ''
   WriteRegStr HKCR 'RStudio.rmarkdown' '' 'R HTML File'
   WriteRegStr HKCR 'RStudio.rmarkdown\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-12'
   WriteRegStr HKCR 'RStudio.rmarkdown\\\\\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr HKCR '.Rpres' '' 'RStudio.rpres'
   WriteRegStr HKCR '.Rpres\\\\OpenWithProgids' 'RStudio.rpres' ''
   WriteRegStr HKCR 'RStudio.rpres' '' 'R Presentation File'
   WriteRegStr HKCR 'RStudio.rpres\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-13'
   WriteRegStr HKCR 'RStudio.rpres\\\\\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr HKCR '.Rnw' '' 'RStudio.rnw'
   WriteRegStr HKCR '.Rnw\\\\OpenWithProgids' 'RStudio.rnw' ''
   WriteRegStr HKCR 'RStudio.rnw' '' 'Sweave File'
   WriteRegStr HKCR 'RStudio.rnw\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-14'
   WriteRegStr HKCR 'RStudio.rnw\\\\\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr HKCR '.Rtex\\\\OpenWithProgids' 'RStudio.rtex' ''
   WriteRegStr HKCR '.Rtex\\\\OpenWithProgids' 'RStudio.rtex' ''
   WriteRegStr HKCR 'RStudio.rtex' '' 'Sweave File'
   WriteRegStr HKCR 'RStudio.rtex\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-14'
   WriteRegStr HKCR 'RStudio.rtex\\\\\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr HKCR '.tex\\\\OpenWithProgids' 'RStudio.tex' ''
   WriteRegStr HKCR 'RStudio.tex' '' 'TeX File'
   WriteRegStr HKCR 'RStudio.tex\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-15'
   WriteRegStr HKCR 'RStudio.tex\\\\\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'


   System::Call 'Shell32::SHChangeNotify(i 0x8000000, i 0, i 0, i 0)'
")


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
   DeleteRegKey HKCR '.Rtex\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR '.py\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR '.Rprofvis\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey HKCR 'RStudio.Rproj'
   DeleteRegKey HKCR 'RStudio.r'
   DeleteRegKey HKCR 'RStudio.css'
   DeleteRegKey HKCR 'RStudio.html'
   DeleteRegKey HKCR 'RStudio.js'
   DeleteRegKey HKCR 'RStudio.markdown'
   DeleteRegKey HKCR 'RStudio.quarto'
   DeleteRegKey HKCR 'RStudio.rdata'
   DeleteRegKey HKCR 'RStudio.rdoc'
   DeleteRegKey HKCR 'RStudio.rhtml'
   DeleteRegKey HKCR 'RStudio.rmarkdown'
   DeleteRegKey HKCR 'RStudio.rpres'
   DeleteRegKey HKCR 'RStudio.rnw'
   DeleteRegKey HKCR 'RStudio.tex'
   DeleteRegKey HKCR 'RStudio.rtex'
   DeleteRegKey HKCR 'RStudio.rprofvis'
   DeleteRegValue HKCR '.Rproj\\\\OpenWithProgids' 'RStudio.Rproj'
   DeleteRegValue HKCR '.R\\\\OpenWithProgids' 'RStudio.r'
   DeleteRegValue HKCR '.css\\\\OpenWithProgids' 'RStudio.css'
   DeleteRegValue HKCR '.htm\\\\OpenWithProgids' 'RStudio.html'
   DeleteRegValue HKCR '.html\\\\OpenWithProgids' 'RStudio.html'
   DeleteRegValue HKCR '.js\\\\OpenWithProgids' 'RStudio.js'
   DeleteRegValue HKCR '.md\\\\OpenWithProgids' 'RStudio.markdown'
   DeleteRegValue HKCR '.mdtxt\\\\OpenWithProgids' 'RStudio.markdown'
   DeleteRegValue HKCR '.markdown\\\\OpenWithProgids' 'RStudio.markdown'
   DeleteRegValue HKCR '.qmd\\\\OpenWithProgids' 'RStudio.quarto'
   DeleteRegValue HKCR '.rda\\\\OpenWithProgids' 'RStudio.rdata'
   DeleteRegValue HKCR '.RData\\\\OpenWithProgids' 'RStudio.rdata'
   DeleteRegValue HKCR '.Rd\\\\OpenWithProgids' 'RStudio.rdoc'
   DeleteRegValue HKCR '.Rhtml\\\\OpenWithProgids' 'RStudio.rhtml'
   DeleteRegValue HKCR '.Rmd\\\\OpenWithProgids' 'RStudio.rmarkdown'
   DeleteRegValue HKCR '.Rmarkdown\\\\OpenWithProgids' 'RStudio.rmarkdown'
   DeleteRegValue HKCR '.Rpres\\\\OpenWithProgids' 'RStudio.rpres'
   DeleteRegValue HKCR '.Rnw\\\\OpenWithProgids' 'RStudio.rnw'
   DeleteRegValue HKCR '.tex\\\\OpenWithProgids' 'RStudio.tex'
   DeleteRegValue HKCR '.Rprofvis\\\\OpenWithProgids' 'RStudio.rprofvis'
   DeleteRegValue HKCR '.Rtex\\\\OpenWithProgids' 'RStudio.rprofvis'
#")

# build installer
include(CPack)

