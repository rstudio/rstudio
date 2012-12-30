

# shared cpack variables
set(CPACK_PACKAGE_NAME "RStudio")
set(CPACK_PACKAGE_DESCRIPTION "RStudio")
set(CPACK_PACKAGE_VENDOR "RStudio")
set(CPACK_PACKAGE_CONTACT "RStudio <info@rstudio.com>")
set(CPACK_PACKAGE_INSTALL_DIRECTORY "RStudio")

if(WIN32)

   add_subdirectory(win32)

elseif(APPLE)

   add_subdirectory(osx)

elseif(UNIX)

   add_subdirectory(linux)

endif()

