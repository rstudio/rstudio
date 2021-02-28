# don't follow symlinks in GLOB_RECURSE
cmake_policy(SET CMP0009 NEW)
cmake_policy(SET CMP0011 NEW)

# flags to pass to codesign executable
set(CODESIGN_FLAGS
   --options runtime
   --timestamp
   --entitlements "@CMAKE_CURRENT_SOURCE_DIR@/entitlements.plist"
   --deep
   -s 8A388E005EF927A09B952C6E71B0E8F2F467AB26
   -i org.rstudio.RStudio)

list(APPEND CODESIGN_TARGETS "${CMAKE_INSTALL_PREFIX}/RStudio.app")

file(GLOB_RECURSE CODESIGN_PLUGINS "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/plugins")
list(APPEND CODESIGN_TARGETS ${CODESIGN_PLUGINS})

file(GLOB_RECURSE CODESIGN_FRAMEWORKS "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/Frameworks")
list(APPEND CODESIGN_TARGETS ${CODESIGN_FRAMEWORKS})

file(GLOB_RECURSE CODESIGN_MACOS "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/MacOS")
list(APPEND CODESIGN_TARGETS ${CODESIGN_MACOS})

# deep sign all targets
foreach(CODESIGN_TARGET ${CODESIGN_TARGETS})
	message(STATUS "Signing ${CODESIGN_TARGET}...")
	execute_process(COMMAND codesign ${CODESIGN_FLAGS} "${CODESIGN_TARGET}")
endforeach()

