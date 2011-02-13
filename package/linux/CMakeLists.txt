
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

  # package name
  set(CPACK_PACKAGE_NAME "rstudio-server")

  # dynamically configured debian control files
  configure_file(${CMAKE_CURRENT_SOURCE_DIR}/debian-control/postinst.in
                 ${CMAKE_CURRENT_BINARY_DIR}/debian-control/postinst)
  set(CPACK_DEBIAN_PACKAGE_CONTROL_EXTRA "${CMAKE_CURRENT_BINARY_DIR}/debian-control/postinst;${CMAKE_CURRENT_SOURCE_DIR}/debian-control/postrm")

  # dynamically configured rpm scripts (only works with cmake 2.8.1 or higher). 
  # alternatively you can get CPackRPM.cmake from the cmake tip and copy it into
  # your local cmake modules directory -- this is what we currently do
  configure_file(${CMAKE_CURRENT_SOURCE_DIR}/rpm-script/postinst.sh.in
                 ${CMAKE_CURRENT_BINARY_DIR}/rpm-script/postinst.sh)
  set(CPACK_RPM_POST_INSTALL_SCRIPT_FILE "${CMAKE_CURRENT_BINARY_DIR}/rpm-script/postinst.sh")
  set(CPACK_RPM_POST_UNINSTALL_SCRIPT_FILE "${CMAKE_CURRENT_SOURCE_DIR}/rpm-script/postrm.sh")

endif()

# package file name
set(CPACK_PACKAGE_FILE_NAME "${CPACK_PACKAGE_NAME}-${CPACK_PACKAGE_VERSION}-${PACKAGE_ARCHITECTURE}")
string(TOLOWER "${CPACK_PACKAGE_FILE_NAME}" CPACK_PACKAGE_FILE_NAME)

# variables to be re-used in package description fields
set(PACKAGE_LONG_DESCRIPTION "RStudio is a set of integrated tools designed to help you be more productive with R. It includes a console, syntax-highlighting editor that supports direct code execution, as well as tools for plotting, history, and workspace management.")

# debian-specific
set(CPACK_DEBIAN_PACKAGE_DESCRIPTION "${CPACK_PACKAGE_DESCRIPTION}\n ${PACKAGE_LONG_DESCRIPTION}")
set(CPACK_DEBIAN_PACKAGE_ARCHITECTURE "${PACKAGE_ARCHITECTURE}")
set(CPACK_DEBIAN_PACKAGE_DEPENDS "${RSTUDIO_DEBIAN_DEPENDS} libpango1.0-0 (>= 1.20.0), r-base (>= 2.10.0)")

# rpm-specific
set(CPACK_RPM_PACKAGE_SUMMARY "${CPACK_PACKAGE_NAME}")
set(CPACK_RPM_PACKAGE_DESCRIPTION "${PACKAGE_LONG_DESCRIPTION}")
set(CPACK_RPM_PACKAGE_LICENSE "AGPL v.3.0")
set(CPACK_RPM_PACKAGE_GROUP "Development/Tools")
set(CPACK_RPM_PACKAGE_ARCHITECTURE "${PACKAGE_ARCHITECTURE}")
set(CPACK_RPM_PACKAGE_DEPENDS "${RSTUDIO_RPM_DEPENDS} pango (>= 1.14.0), R (>= 2.10.0)")

# build package
include(CPack)





