
# developer-id code signing
if (RSTUDIO_PACKAGE_BUILD)
   INSTALL(CODE "
     list (APPEND CODESIGN_TARGETS \"\${CMAKE_INSTALL_PREFIX}/RStudio.app\")
  
     file(GLOB_RECURSE CODESIGN_PLUGINS \"\${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/plugins\")
     list (APPEND CODESIGN_TARGETS \${CODESIGN_PLUGINS})

     file(GLOB_RECURSE CODESIGN_FRAMEWORKS \"\${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/Frameworks\")
     list (APPEND CODESIGN_TARGETS \${CODESIGN_FRAMEWORKS})
  
     file(GLOB_RECURSE CODESIGN_MACOS \"\${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/MacOS\")
     list (APPEND CODESIGN_TARGETS \${CODESIGN_MACOS})
  
     foreach(CODESIGN_TARGET \${CODESIGN_TARGETS})
        execute_process(COMMAND codesign \"-s\" \"Developer ID Application: RStudio Inc.\" \"-i\" \"org.rstudio.RStudio\" \"\${CODESIGN_TARGET}\")
     endforeach()   
     ")
endif()

# package attributes
set(CPACK_PACKAGE_FILE_NAME "${CPACK_PACKAGE_NAME}-${CPACK_PACKAGE_VERSION}")

# create drag and drop installer
set(CPACK_BINARY_DRAGNDROP ON)

include(CPack)
