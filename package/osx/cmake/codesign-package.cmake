
# CMake's message is suppressed during install stage so just use echo here
function(echo MESSAGE)
	execute_process(COMMAND echo "-- ${MESSAGE}")
endfunction()

# don't follow symlinks in GLOB_RECURSE
cmake_policy(SET CMP0009 NEW)
cmake_policy(SET CMP0011 NEW)

# flags to pass to codesign executable
set(CODESIGN_FLAGS
   --options runtime
   --timestamp
   --entitlements "@CMAKE_CURRENT_SOURCE_DIR@/entitlements.plist"
   --force
   --deep)

# NOTE: we always attempt to sign a package build of RStudio
# (even if it's just a development build) as our usages of
# install_name_tool will invalidate existing signatures on
# bundled libraries and macOS will refuse to launch RStudio
# with the older invalid signature
if(@RSTUDIO_CODESIGN_USE_CREDENTIALS@)
   echo("codesign: using RStudio's credentials")
   list(APPEND CODESIGN_FLAGS
      -s 4D663D999011E80361D8848C8487D70E4C41DB60
      -i org.rstudio.RStudio)
else()
   echo("codesign: using ad-hoc signature")
   list(APPEND CODESIGN_FLAGS -s -)
endif()

list(APPEND CODESIGN_TARGETS "${CMAKE_INSTALL_PREFIX}/RStudio.app")

file(GLOB_RECURSE CODESIGN_PLUGINS "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/plugins")
list(APPEND CODESIGN_TARGETS ${CODESIGN_PLUGINS})

file(GLOB_RECURSE CODESIGN_FRAMEWORKS "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/Frameworks")
list(APPEND CODESIGN_TARGETS ${CODESIGN_FRAMEWORKS})

file(GLOB_RECURSE CODESIGN_MACOS "${CMAKE_INSTALL_PREFIX}/RStudio.app/Contents/MacOS")
list(APPEND CODESIGN_TARGETS ${CODESIGN_MACOS})

# deep sign all targets
foreach(CODESIGN_TARGET ${CODESIGN_TARGETS})
   echo("Signing ${CODESIGN_TARGET}")
	execute_process(COMMAND codesign ${CODESIGN_FLAGS} "${CODESIGN_TARGET}")
endforeach()

