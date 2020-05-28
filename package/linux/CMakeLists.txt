#
# CMakeLists.txt
#
# Copyright (C) 2020 by RStudio, PBC
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

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

# detect RedHat 5
set(IS_RHEL "OFF")
set(IS_RHEL_5 "OFF")
if(EXISTS "/etc/redhat-release")
   set(IS_RHEL "ON")
   file(READ "/etc/redhat-release" RHEL_RELEASE)
   string(FIND "${RHEL_RELEASE}" "release 5" RHEL_RELEASE_POS)
   if(RHEL_RELEASE_POS GREATER 0)
     set(IS_RHEL_5 "ON")
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

  # rpm dependencies
  string(FIND "${RSTUDIO_PACKAGE_OS}" "SUSE" SUSE_POS)
  if(SUSE_POS GREATER 0)
     # SuSE based systems
     set(RSTUDIO_RPM_DEPENDS "libsqlite3-0, libpq5, ")
  else()
     set(RSTUDIO_RPM_DEPENDS "sqlite, postgresql-libs, ")
  endif()
  set(RSTUDIO_RPM_DEPENDS "${RSTUDIO_RPM_DEPENDS} psmisc, ")

  # debian dependencies -- to install the .deb from the command line with
  # automatic dependency resolution use e.g.
  #   sudo apt-get install gdebi-core
  #   sudo gdebi rstudio-server-0.97.151-amd64.deb
  set(RSTUDIO_DEBIAN_DEPENDS "psmisc, libapparmor1, libedit2, sudo, lsb-release, libssl1.0.0 | libssl1.0.2 | libssl1.1, libclang-dev, libsqlite3-0, libpq5, ")

elseif(RSTUDIO_DESKTOP)
 
   # debian control files
  set(DEBIAN_POSTINST postinst-desktop.in)
  set(DEBIAN_POSTRM postrm-desktop.in)
  
  # rpm scripts
  set(RPM_POSTINST postinst-desktop.sh.in)
  set(RPM_POSTRM postrm-desktop.sh.in)

  # rpm dependencies
  string(FIND "${RSTUDIO_PACKAGE_OS}" "SUSE" SUSE_POS)
  if(SUSE_POS GREATER 0)
     # SuSE based systems
     set(RSTUDIO_RPM_DEPENDS "libxkbcommon-x11-0, libsqlite3-0, libpq5, ")
  else()
     set(RSTUDIO_RPM_DEPENDS "libxkbcommon-x11, sqlite, postgresql-libs, ")
  endif()

  # deb dependencies
  set(RSTUDIO_DEBIAN_DEPENDS "libedit2, libssl1.0.0 | libssl1.0.2 | libssl1.1, libclang-dev, libxkbcommon-x11-0, libsqlite3-0, libpq5, ")
  
endif()

# define package suffix
set(RSTUDIO_PACKAGE_SUFFIX "-")

if(RSTUDIO_SERVER)
   # add rhel to package name for redhat enterprise linux builds
   if(IS_RHEL) 
      set(RSTUDIO_PACKAGE_SUFFIX "${RSTUDIO_PACKAGE_SUFFIX}rhel-")
   endif()
endif()

# include overlay if it exists
if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/CMakeOverlay.txt")
   include(CMakeOverlay.txt)
endif()

# dynamically configured debian control scripts--process to temporary files
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/debian-control/${DEBIAN_POSTINST}
               ${CMAKE_CURRENT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/postinst)
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/debian-control/${DEBIAN_POSTRM}
               ${CMAKE_CURRENT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/postrm)

# Lintian insists on 0755 permissions on postinst and postrm files, but by
# default CMake just copies the permissions set on the original (*.in) files,
# which can change when those files are created/altered by source control. We
# set 0755 manually on these files to avoid any dependencies on particular
# permissions bits on the source files.
file(COPY ${CMAKE_CURRENT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/postinst
          ${CMAKE_CURRENT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/postrm
          DESTINATION ${CMAKE_CURRENT_BINARY_DIR}/debian-control
          FILE_PERMISSIONS OWNER_READ OWNER_WRITE OWNER_EXECUTE GROUP_READ
          GROUP_EXECUTE WORLD_READ WORLD_EXECUTE)

set(CPACK_DEBIAN_PACKAGE_CONTROL_EXTRA "${CMAKE_CURRENT_BINARY_DIR}/debian-control/postinst;${CMAKE_CURRENT_BINARY_DIR}/debian-control/postrm")

# dynamically configured rpm scripts (only works with cmake 2.8.1 or higher). 
# alternatively you can get CPackRPM.cmake from the cmake tip and copy it into
# your local cmake modules directory -- this is what we currently do
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/rpm-script/${RPM_POSTINST}
               ${CMAKE_CURRENT_BINARY_DIR}/rpm-script/postinst.sh)
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/rpm-script/${RPM_POSTRM}
               ${CMAKE_CURRENT_BINARY_DIR}/rpm-script/postrm.sh)

set(CPACK_RPM_SPEC_INSTALL_POST "/bin/true")
set(CPACK_RPM_POST_INSTALL_SCRIPT_FILE "${CMAKE_CURRENT_BINARY_DIR}/rpm-script/postinst.sh")
set(CPACK_RPM_POST_UNINSTALL_SCRIPT_FILE "${CMAKE_CURRENT_BINARY_DIR}/rpm-script/postrm.sh")

# /usr and /usr/lib are already present in CPACK_RPM_EXCLUDE_FROM_AUTO_FILELIST, 
# but some Linux distributions complain without this explicit suppression
set(CPACK_RPM_SPEC_MORE_DEFINE "%define ignore \#")
set(CPACK_RPM_USER_FILELIST 
  "%ignore /usr"
  "%ignore /usr/lib")

set(CPACK_RPM_EXCLUDE_FROM_AUTO_FILELIST_ADDITION 
  "/usr/share/applications"
  "/usr/share/icons"
  "/usr/share/pixmaps"
  "/usr/share/mime/packages"
  "/usr/share/mime"
  "/usr/share/icons/hicolor"
  "/usr/share/icons/hicolor/16x16"
  "/usr/share/icons/hicolor/16x16/apps"
  "/usr/share/icons/hicolor/24x24/apps"
  "/usr/share/icons/hicolor/256x256/apps"
  "/usr/share/icons/hicolor/32x32/apps"
  "/usr/share/icons/hicolor/48x48/apps"
  "/usr/share/icons/hicolor/24x24"
  "/usr/share/icons/hicolor/256x256"
  "/usr/share/icons/hicolor/32x32"
  "/usr/share/icons/hicolor/48x48"
  "/usr/share/icons/hicolor"
  "/usr/share/icons/hicolor/16x16"
  "/usr/share/icons/hicolor/16x16/apps"
  "/usr/share/icons/hicolor/24x24/apps"
  "/usr/share/icons/hicolor/256x256/apps"
  "/usr/share/icons/hicolor/32x32/apps"
  "/usr/share/icons/hicolor/48x48/apps"
  "/usr/share/icons/hicolor/16x16/mimetypes"
  "/usr/share/icons/hicolor/24x24/mimetypes"
  "/usr/share/icons/hicolor/256x256/mimetypes"
  "/usr/share/icons/hicolor/32x32/mimetypes"
  "/usr/share/icons/hicolor/48x48/mimetypes"
  "/usr/share/icons/hicolor/24x24"
  "/usr/share/icons/hicolor/256x256"
  "/usr/share/icons/hicolor/32x32"
  "/usr/share/icons/hicolor/48x48")

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
set(CPACK_DEBIAN_PACKAGE_RECOMMENDS "r-base (>= ${RSTUDIO_R_MAJOR_VERSION_REQUIRED}.${RSTUDIO_R_MINOR_VERSION_REQUIRED}.${RSTUDIO_R_PATCH_VERSION_REQUIRED})")

# rpm-specific
set(CPACK_RPM_PACKAGE_SUMMARY "${CPACK_PACKAGE_NAME}")
set(CPACK_RPM_PACKAGE_DESCRIPTION "${PACKAGE_LONG_DESCRIPTION}")
set(CPACK_RPM_PACKAGE_LICENSE "AGPL v.3.0")
set(CPACK_RPM_PACKAGE_GROUP "Development/Tools")
set(CPACK_RPM_PACKAGE_ARCHITECTURE "${PACKAGE_ARCHITECTURE}")
set(CPACK_RPM_PACKAGE_REQUIRES "${RSTUDIO_RPM_DEPENDS}")
set(CPACK_RPM_PACKAGE_AUTOREQPROV " no")


# build package
include(CPack)





