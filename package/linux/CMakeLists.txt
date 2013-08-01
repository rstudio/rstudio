
# configure cpack install location 
set(CPACK_SET_DESTDIR "ON")
set(CPACK_INSTALL_PREFIX "${CMAKE_INSTALL_PREFIX}")

# detect architecture (packaging platform specific)
find_program(DPKG_EXECUTABLE dpkg)
find_program(RPM_EXECUTABLE rpm)
if (NOT PACKAGE_ARCHITECTURE)
   if(DPKG_EXECUTABLE)
      exec_program(dpkg ARGS --print-architecture
                   OUTPUT_VARIABLE PACKAGE_ARCHITECTURE)
   elseif(RPM_EXECUTABLE)
      exec_program(arch OUTPUT_VARIABLE PACKAGE_ARCHITECTURE) 
   endif()
endif()

# configuration specific
if(RSTUDIO_SERVER)

  # package name and description
  set(CPACK_PACKAGE_NAME "rstudio-server")
  set(CPACK_PACKAGE_DESCRIPTION "RStudio Server")

  # debian control files
  set(DEBIAN_POSTINST postinst.in)
  set(DEBIAN_POSTRM postrm.in)
  
  # rpm scripts
  set(RPM_POSTINST postinst.sh.in)
  set(RPM_POSTRM postrm.sh.in)

  # deiban dependencies -- to install the .deb from the command line with
  # automatic dependency resolution use e.g.
  #   sudo apt-get install gdebi-core
  #   sudo gdebi rstudio-server-0.97.151-amd64.deb
  set(RSTUDIO_DEBIAN_DEPENDS "psmisc, libssl0.9.8, ")

  # rpm dependencies
  set(RSTUDIO_RPM_DEPENDS "psmisc, ")

elseif(RSTUDIO_DESKTOP)
 
   # debian control files
  set(DEBIAN_POSTINST postinst-desktop.in)
  set(DEBIAN_POSTRM postrm-desktop.in)
  
  # rpm scripts
  set(RPM_POSTINST postinst-desktop.sh.in)
  set(RPM_POSTRM postrm-desktop.sh.in)

  # depend on libjpeg62 (for Qt 4.8 jpeg plugin)
  set(RSTUDIO_DEBIAN_DEPENDS "libjpeg62, ")

endif()

# define package suffix
set(RSTUDIO_PACKAGE_SUFFIX "-")

# include overlay if it exists
if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/CMakeOverlay.txt")
   include(CMakeOverlay.txt)
endif()

# dynamically configured debian control scripts
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/debian-control/${DEBIAN_POSTINST}
               ${CMAKE_CURRENT_BINARY_DIR}/debian-control/postinst)
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/debian-control/${DEBIAN_POSTRM}
               ${CMAKE_CURRENT_BINARY_DIR}/debian-control/postrm)

set(CPACK_DEBIAN_PACKAGE_CONTROL_EXTRA "${CMAKE_CURRENT_BINARY_DIR}/debian-control/postinst;${CMAKE_CURRENT_BINARY_DIR}/debian-control/postrm")

# dynamically configured rpm scripts (only works with cmake 2.8.1 or higher). 
# alternatively you can get CPackRPM.cmake from the cmake tip and copy it into
# your local cmake modules directory -- this is what we currently do
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/rpm-script/${RPM_POSTINST}
               ${CMAKE_CURRENT_BINARY_DIR}/rpm-script/postinst.sh)
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/rpm-script/${RPM_POSTRM}
               ${CMAKE_CURRENT_BINARY_DIR}/rpm-script/postrm.sh)

set(CPACK_RPM_POST_INSTALL_SCRIPT_FILE "${CMAKE_CURRENT_BINARY_DIR}/rpm-script/postinst.sh")
set(CPACK_RPM_POST_UNINSTALL_SCRIPT_FILE "${CMAKE_CURRENT_BINARY_DIR}/rpm-script/postrm.sh")


# package file name
set(CPACK_PACKAGE_FILE_NAME "${CPACK_PACKAGE_NAME}${RSTUDIO_PACKAGE_SUFFIX}${CPACK_PACKAGE_VERSION}-${PACKAGE_ARCHITECTURE}")
if(NOT ${CMAKE_BUILD_TYPE} STREQUAL "Release")
   set(CPACK_PACKAGE_FILE_NAME "${CPACK_PACKAGE_FILE_NAME}-${CMAKE_BUILD_TYPE}")
endif()
string(TOLOWER "${CPACK_PACKAGE_FILE_NAME}" CPACK_PACKAGE_FILE_NAME)

# variables to be re-used in package description fields
set(PACKAGE_LONG_DESCRIPTION "RStudio is a set of integrated tools designed to help you be more productive with R. It includes a console, syntax-highlighting editor that supports direct code execution, as well as tools for plotting, history, and workspace management.")

# debian-specific
set(CPACK_DEBIAN_PACKAGE_DESCRIPTION "${CPACK_PACKAGE_DESCRIPTION}\n ${PACKAGE_LONG_DESCRIPTION}")
set(CPACK_DEBIAN_PACKAGE_ARCHITECTURE "${PACKAGE_ARCHITECTURE}")
set(CPACK_DEBIAN_PACKAGE_DEPENDS "${RSTUDIO_DEBIAN_DEPENDS} libc6 (>= 2.7)")
set(CPACK_DEBIAN_PACKAGE_RECOMMENDS "r-base (>= 2.11.1)")

# rpm-specific
set(CPACK_RPM_PACKAGE_SUMMARY "${CPACK_PACKAGE_NAME}")
set(CPACK_RPM_PACKAGE_DESCRIPTION "${PACKAGE_LONG_DESCRIPTION}")
set(CPACK_RPM_PACKAGE_LICENSE "AGPL v.3.0")
set(CPACK_RPM_PACKAGE_GROUP "Development/Tools")
set(CPACK_RPM_PACKAGE_ARCHITECTURE "${PACKAGE_ARCHITECTURE}")
set(CPACK_RPM_PACKAGE_REQUIRES "${RSTUDIO_RPM_DEPENDS}")

# build package
include(CPack)





