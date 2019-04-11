# define package suffix
set(RSTUDIO_PACKAGE_SUFFIX "-")

# flags to pass to codesign executable
set(CODESIGN_FLAGS "-s" "8A388E005EF927A09B952C6E71B0E8F2F467AB26" "-i" "org.rstudio.RStudio")

# include overlay if it exists
if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/CMakeOverlay.txt")
   include(CMakeOverlay.txt)
endif()

# developer-id code signing
if (RSTUDIO_PACKAGE_BUILD)
   INSTALL(CODE "
     # don't follow symlinks in GLOB_RECURSE
     cmake_policy(SET CMP0009 NEW)
     cmake_policy(SET CMP0011 NEW)

     list (APPEND CODESIGN_TARGETS \"\${CMAKE_INSTALL_PREFIX}/RStudio.app\")

     file(GLOB_RECURSE CODESIGN_PLUGINS \"\${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/plugins\")
     list (APPEND CODESIGN_TARGETS \${CODESIGN_PLUGINS})

     file(GLOB_RECURSE CODESIGN_FRAMEWORKS \"\${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/Frameworks\")
     list (APPEND CODESIGN_TARGETS \${CODESIGN_FRAMEWORKS})

     file(GLOB_RECURSE CODESIGN_MACOS \"\${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/MacOS\")
     list (APPEND CODESIGN_TARGETS \${CODESIGN_MACOS})

     # deep sign all targets
     foreach(CODESIGN_TARGET \${CODESIGN_TARGETS})
         message(\"Signing \${CODESIGN_TARGET}...\")
         execute_process(COMMAND codesign \"--deep\" ${CODESIGN_FLAGS} \"\${CODESIGN_TARGET}\")
     endforeach()
     ")
endif()

# package attributes
set(CPACK_PACKAGE_FILE_NAME "${CPACK_PACKAGE_NAME}${RSTUDIO_PACKAGE_SUFFIX}${CPACK_PACKAGE_VERSION}")

# create drag and drop installer
set(CPACK_BINARY_DRAGNDROP ON)

include(CPack)
