
# append our module directory to path
list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/cmake/modules")

# cpack variables
set(CPACK_PACKAGE_FILE_NAME "${CPACK_PACKAGE_NAME}-${CPACK_PACKAGE_VERSION}")
set(CPACK_NSIS_INSTALLED_ICON_NAME "bin\\rstudio.exe")
set(CPACK_PACKAGE_INSTALL_REGISTRY_KEY "RStudio")
set(CPACK_PACKAGE_EXECUTABLES "rstudio" "RStudio")
set(CPACK_INCLUDE_TOPLEVEL_DIRECTORY 0)

# build installer
include(CPack)

