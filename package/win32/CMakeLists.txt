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
file(MAKE_DIRECTORY  "C:/rsbuild")
set(CPACK_PACKAGE_DIRECTORY "C:/rsbuild")

# registry keys
set(CPACK_NSIS_EXTRA_INSTALL_COMMANDS "
   WriteRegStr SHCTX 'Software\\\\Microsoft\\\\Windows\\\\CurrentVersion\\\\App Paths\\\\rstudio.exe' '' '$INSTDIR\\\\rstudio.exe'

   WriteRegStr SHCTX 'Software\\\\Classes\\\\Applications\\\\rstudio.exe\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr SHCTX 'Software\\\\Classes\\\\Applications\\\\rstudio.exe\\\\SupportedTypes' '.R' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\Applications\\\\rstudio.exe\\\\SupportedTypes' '.RData' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\Applications\\\\rstudio.exe\\\\SupportedTypes' '.rda' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\Applications\\\\rstudio.exe\\\\SupportedTypes' '.Rd' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\Applications\\\\rstudio.exe\\\\SupportedTypes' '.Rnw' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\Applications\\\\rstudio.exe\\\\SupportedTypes' '.Rmd' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\Applications\\\\rstudio.exe\\\\SupportedTypes' '.Rmarkdown' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\Applications\\\\rstudio.exe\\\\SupportedTypes' '.qmd' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\Applications\\\\rstudio.exe\\\\SupportedTypes' '.Rhtml' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\Applications\\\\rstudio.exe\\\\SupportedTypes' '.Rpres' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\Applications\\\\rstudio.exe\\\\SupportedTypes' '.Rproj' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\Applications\\\\rstudio.exe\\\\SupportedTypes' '.tex' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\Applications\\\\rstudio.exe\\\\SupportedTypes' '.Rtex' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\Applications\\\\rstudio.exe\\\\SupportedTypes' '.py' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\Applications\\\\rstudio.exe\\\\SupportedTypes' '.Rprofvis' ''

   WriteRegStr SHCTX 'Software\\\\Classes\\\\.R\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.RData\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.rda\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rd\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rnw\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rmd\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rmarkdown\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.qmd\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rhtml\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rpres\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rproj\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.tex\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rtex\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.md\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.mdtxt\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.markdown\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.htm\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.html\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.css\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.js\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.c\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.cpp\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.h\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.hpp\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.py\\\\OpenWithList\\\\rstudio.exe' '' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rprofvis\\\\OpenWithList\\\\rstudio.exe' '' ''

   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rproj' '' 'RStudio.Rproj'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rproj\\\\OpenWithProgids' 'RStudio.Rproj' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.Rproj' '' 'RStudio Project File'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.Rproj\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-2'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.Rproj\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr SHCTX 'Software\\\\Classes\\\\.R' '' 'RStudio.r'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.R\\\\OpenWithProgids' 'RStudio.r' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.r' '' 'R File'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.r\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-3'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.r\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rprofvis' '' 'RStudio.rprofvis'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rprofvis\\\\OpenWithProgids' 'RStudio.rprofvis' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.rprofvis' '' 'R Profile File'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.rprofvis\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-3'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.rprofvis\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr SHCTX 'Software\\\\Classes\\\\.css\\\\OpenWithProgids' 'RStudio.css' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.css' '' 'CSS File'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.css\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-4'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.css\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr SHCTX 'Software\\\\Classes\\\\.htm\\\\OpenWithProgids' 'RStudio.html' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.html\\\\OpenWithProgids' 'RStudio.html' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.html' '' 'HTML File'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.html\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-5'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.html\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr SHCTX 'Software\\\\Classes\\\\.js\\\\OpenWithProgids' 'RStudio.js' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.js' '' 'JavaScript File'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.js\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-6'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.js\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr SHCTX 'Software\\\\Classes\\\\.md\\\\OpenWithProgids' 'RStudio.markdown' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.mdtxt\\\\OpenWithProgids' 'RStudio.markdown' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.markdown\\\\OpenWithProgids' 'RStudio.markdown' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.markdown' '' 'Markdown File'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.markdown\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-7'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.markdown\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr SHCTX 'Software\\\\Classes\\\\.qmd' '' 'RStudio.quarto'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.qmd\\\\OpenWithProgids' 'RStudio.quarto' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.quarto' '' 'Quarto Markdown File'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.quarto\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-8'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.quarto\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr SHCTX 'Software\\\\Classes\\\\.rda' '' 'RStudio.rdata'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.rda\\\\OpenWithProgids' 'RStudio.rdata' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.RData' '' 'RStudio.rdata'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.RData\\\\OpenWithProgids' 'RStudio.rdata' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.rdata' '' 'R Data File'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.rdata\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-9'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.rdata\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rd' '' 'RStudio.rdoc'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rd\\\\OpenWithProgids' 'RStudio.rdoc' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.rdoc' '' 'R Doc File'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.rdoc\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-10'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.rdoc\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rhtml' '' 'RStudio.rhtml'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rhtml\\\\OpenWithProgids' 'RStudio.rhtml' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.rhtml' '' 'R HTML File'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.rhtml\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-11'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.rhtml\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rmd' '' 'RStudio.rmarkdown'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rmd\\\\OpenWithProgids' 'RStudio.rmarkdown' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rmarkdown' '' 'RStudio.rmarkdown'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rmarkdown\\\\OpenWithProgids' 'RStudio.rmarkdown' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.rmarkdown' '' 'R HTML File'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.rmarkdown\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-12'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.rmarkdown\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rpres' '' 'RStudio.rpres'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rpres\\\\OpenWithProgids' 'RStudio.rpres' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.rpres' '' 'R Presentation File'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.rpres\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-13'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.rpres\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rnw' '' 'RStudio.rnw'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rnw\\\\OpenWithProgids' 'RStudio.rnw' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.rnw' '' 'Sweave File'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.rnw\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-14'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.rnw\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rtex' '' 'RStudio.rtex'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\.Rtex\\\\OpenWithProgids' 'RStudio.rtex' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.rtex' '' 'Sweave File'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.rtex\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-14'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.rtex\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'

   WriteRegStr SHCTX 'Software\\\\Classes\\\\.tex\\\\OpenWithProgids' 'RStudio.tex' ''
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.tex' '' 'TeX File'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.tex\\\\DefaultIcon' '' '$INSTDIR\\\\rstudio.exe,-15'
   WriteRegStr SHCTX 'Software\\\\Classes\\\\RStudio.tex\\\\shell\\\\open\\\\command' '' '$INSTDIR\\\\rstudio.exe \\\"%1\\\"'


   System::Call 'Shell32::SHChangeNotify(i 0x8000000, i 0, i 0, i 0)'
")


set(CPACK_NSIS_EXTRA_UNINSTALL_COMMANDS "
   DeleteRegKey SHCTX 'Software\\\\Microsoft\\\\Windows\\\\CurrentVersion\\\\App Paths\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\Applications\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.R\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.RData\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.rda\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.Rd\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.Rnw\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.Rmd\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.Rmarkdown\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.qmd\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.Rhtml\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.Rpres\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.Rproj\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.tex\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.md\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.mdtxt\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.markdown\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.htm\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.html\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.css\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.js\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.c\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.cpp\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.h\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.hpp\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.Rtex\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.py\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\.Rprofvis\\\\OpenWithList\\\\rstudio.exe'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\RStudio.Rproj'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\RStudio.r'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\RStudio.css'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\RStudio.html'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\RStudio.js'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\RStudio.markdown'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\RStudio.quarto'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\RStudio.rdata'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\RStudio.rdoc'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\RStudio.rhtml'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\RStudio.rmarkdown'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\RStudio.rpres'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\RStudio.rnw'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\RStudio.tex'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\RStudio.rtex'
   DeleteRegKey SHCTX 'Software\\\\Classes\\\\RStudio.rprofvis'
   DeleteRegValue SHCTX 'Software\\\\Classes\\\\.Rproj\\\\OpenWithProgids' 'RStudio.Rproj'
   DeleteRegValue SHCTX 'Software\\\\Classes\\\\.R\\\\OpenWithProgids' 'RStudio.r'
   DeleteRegValue SHCTX 'Software\\\\Classes\\\\.css\\\\OpenWithProgids' 'RStudio.css'
   DeleteRegValue SHCTX 'Software\\\\Classes\\\\.htm\\\\OpenWithProgids' 'RStudio.html'
   DeleteRegValue SHCTX 'Software\\\\Classes\\\\.html\\\\OpenWithProgids' 'RStudio.html'
   DeleteRegValue SHCTX 'Software\\\\Classes\\\\.js\\\\OpenWithProgids' 'RStudio.js'
   DeleteRegValue SHCTX 'Software\\\\Classes\\\\.md\\\\OpenWithProgids' 'RStudio.markdown'
   DeleteRegValue SHCTX 'Software\\\\Classes\\\\.mdtxt\\\\OpenWithProgids' 'RStudio.markdown'
   DeleteRegValue SHCTX 'Software\\\\Classes\\\\.markdown\\\\OpenWithProgids' 'RStudio.markdown'
   DeleteRegValue SHCTX 'Software\\\\Classes\\\\.qmd\\\\OpenWithProgids' 'RStudio.quarto'
   DeleteRegValue SHCTX 'Software\\\\Classes\\\\.rda\\\\OpenWithProgids' 'RStudio.rdata'
   DeleteRegValue SHCTX 'Software\\\\Classes\\\\.RData\\\\OpenWithProgids' 'RStudio.rdata'
   DeleteRegValue SHCTX 'Software\\\\Classes\\\\.Rd\\\\OpenWithProgids' 'RStudio.rdoc'
   DeleteRegValue SHCTX 'Software\\\\Classes\\\\.Rhtml\\\\OpenWithProgids' 'RStudio.rhtml'
   DeleteRegValue SHCTX 'Software\\\\Classes\\\\.Rmd\\\\OpenWithProgids' 'RStudio.rmarkdown'
   DeleteRegValue SHCTX 'Software\\\\Classes\\\\.Rmarkdown\\\\OpenWithProgids' 'RStudio.rmarkdown'
   DeleteRegValue SHCTX 'Software\\\\Classes\\\\.Rpres\\\\OpenWithProgids' 'RStudio.rpres'
   DeleteRegValue SHCTX 'Software\\\\Classes\\\\.Rnw\\\\OpenWithProgids' 'RStudio.rnw'
   DeleteRegValue SHCTX 'Software\\\\Classes\\\\.tex\\\\OpenWithProgids' 'RStudio.tex'
   DeleteRegValue SHCTX 'Software\\\\Classes\\\\.Rprofvis\\\\OpenWithProgids' 'RStudio.rprofvis'
   DeleteRegValue SHCTX 'Software\\\\Classes\\\\.Rtex\\\\OpenWithProgids' 'RStudio.rtex'
#")

# the installer requires NsisMultiUser; check at configure time since a missing
# include directory only surfaces deep inside makensis as a cryptic
# '!include: could not find: "UAC.nsh"' error
set(NSIS_MULTIUSER_DIR "${CPACK_DEPENDENCIES_DIR}/nsis-multiuser/${CPACK_NSIS_MULTIUSER_VERSION}")
if(NOT EXISTS "${NSIS_MULTIUSER_DIR}/Include/NsisMultiUser.nsh")
   message(FATAL_ERROR "NsisMultiUser not found at ${NSIS_MULTIUSER_DIR}. Run dependencies/windows/install-dependencies.cmd to install it.")
endif()

# build installer
include(CPack)

