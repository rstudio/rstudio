#
# CMakeLists.txt
#
# Copyright (C) 2022 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
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
message(STATUS "Packaging RStudio for ${OS_RELEASE_PRETTY_NAME}")

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

# set libssl dependency
if(OS_RELEASE_ID STREQUAL "ubuntu")
  if(OS_RELEASE_VERSION_ID VERSION_GREATER_EQUAL "22.04")
    set(RSTUDIO_DEBIAN_DEPENDS_SSL "libssl-dev")
  else()
    set(RSTUDIO_DEBIAN_DEPENDS_SSL "libssl1.0.0 | libssl1.0.2 | libssl1.1")
  endif()
else()
  set(RSTUDIO_DEBIAN_DEPENDS_SSL "libssl-dev")
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
     list(APPEND RSTUDIO_RPM_DEPENDS "libsqlite3-0")
     if(RSTUDIO_PRO_BUILD)
       list(APPEND RSTUDIO_RPM_DEPENDS "libpq5")
     endif()
  else()
    list(APPEND RSTUDIO_RPM_DEPENDS "sqlite")
    if(RSTUDIO_PRO_BUILD)
      list(APPEND RSTUDIO_RPM_DEPENDS "postgresql-libs")
    endif()
  endif()
  list(APPEND RSTUDIO_RPM_DEPENDS "psmisc")

  # debian dependencies -- to install the .deb from the command line with
  # automatic dependency resolution use e.g.
  #   sudo apt-get install gdebi-core
  #   sudo gdebi rstudio-server-0.97.151-amd64.deb
  list(APPEND RSTUDIO_DEBIAN_DEPENDS
    "${RSTUDIO_DEBIAN_DEPENDS_SSL}"
    "psmisc"
    "libclang-dev"
    "libsqlite3-0"
    "lsb-release"
    "sudo")

  if(RSTUDIO_PRO_BUILD)
    list(APPEND RSTUDIO_DEBIAN_DEPENDS "libpq5")
  endif()

elseif(RSTUDIO_ELECTRON)

  # debian control files (Electron)
  set(DEBIAN_POSTINST postinst-electron.in)
  set(DEBIAN_POSTRM postrm-electron.in)

  # rpm scripts (Electron)
  set(RPM_POSTINST postinst-electron.sh.in)
  set(RPM_POSTRM postrm-electron.sh.in)

  # rpm dependencies
  string(FIND "${RSTUDIO_PACKAGE_OS}" "SUSE" SUSE_POS)
  if(SUSE_POS GREATER 0)
    # SuSE based systems
    list(
      APPEND RSTUDIO_RPM_DEPENDS
      "libxkbcommon-x11-0"
      "libsqlite3-0")
    if(RSTUDIO_PRO_BUILD)
      list(APPEND RSTUDIO_RPM_DEPENDS "libpq5")
    endif()
  else()
    list(APPEND RSTUDIO_RPM_DEPENDS
      "libxkbcommon-x11"
      "sqlite")
    if(RSTUDIO_PRO_BUILD)
      list(APPEND RSTUDIO_RPM_DEPENDS "postgresql-libs")
    endif()
  endif()

  # deb dependencies
  list(APPEND RSTUDIO_DEBIAN_DEPENDS
    "${RSTUDIO_DEBIAN_DEPENDS_SSL}"
    "libclang-dev"
    "libsqlite3-0"
    "libxkbcommon-x11-0")

  if(RSTUDIO_PRO_BUILD)
    list(APPEND RSTUDIO_DEBIAN_DEPENDS "libpq5")
  endif()

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

# Generate SELinux policy
# Note: Generating the manpages requires installing the policy, so
# they are stored as source files and should be regenerated using:
#    ./build_selinux_policy.sh --man
if(NOT DEFINED SELINUX_REFPOLICY_PATH)
   set(SELINUX_REFPOLICY_PATH /usr/share/selinux/devel)
endif()
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/selinux/rstudio.fc.in
               ${CMAKE_CURRENT_SOURCE_DIR}/selinux/rstudio.fc)
add_custom_command(OUTPUT ${CMAKE_CURRENT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/rstudio.pp
                   COMMAND make ARGS -C ${CMAKE_CURRENT_SOURCE_DIR}/selinux -f ${SELINUX_REFPOLICY_PATH}/include/Makefile NAME=devel rstudio.pp
                   COMMAND mv ARGS ${CMAKE_CURRENT_SOURCE_DIR}/selinux/rstudio.pp ${CMAKE_CURRENT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/rstudio.pp
                   MAIN_DEPENDENCY ${CMAKE_CURRENT_SOURCE_DIR}/selinux/rstudio.te
                   DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/selinux/rstudio.fc
                           ${CMAKE_CURRENT_SOURCE_DIR}/selinux/rstudio.te)
add_custom_target(selinux ALL
                  DEPENDS ${CMAKE_CURRENT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/rstudio.pp
                          ${CMAKE_CURRENT_SOURCE_DIR}/selinux/rstudio_selinux.8
                          ${CMAKE_CURRENT_SOURCE_DIR}/selinux/rstudio_server_selinux.8)
install(FILES ${CMAKE_CURRENT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/rstudio.pp
              ${CMAKE_CURRENT_SOURCE_DIR}/selinux/rstudio_selinux.8
              ${CMAKE_CURRENT_SOURCE_DIR}/selinux/rstudio_server_selinux.8
        DESTINATION ${CMAKE_INSTALL_DATAROOTDIR}/selinux/packages/)

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
# set _build_id_links none to avoid conflicts with other packages providing Electron bits
set(CPACK_RPM_SPEC_MORE_DEFINE [=[
%define ignore \#
%define _build_id_links none
]=])

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
if(DEFINED RSTUDIO_PACKAGE_FILE_NAME_OVERRIDE)
  set(RSTUDIO_PACKAGE_FILE_NAME "${RSTUDIO_PACKAGE_FILE_NAME_OVERRIDE}")
else()
  set(RSTUDIO_PACKAGE_FILE_NAME "${CPACK_PACKAGE_NAME}")
endif()

set(CPACK_PACKAGE_FILE_NAME "${RSTUDIO_PACKAGE_FILE_NAME}${RSTUDIO_PACKAGE_SUFFIX}${CPACK_PACKAGE_VERSION}-${PACKAGE_ARCHITECTURE}")
if(NOT ${CMAKE_BUILD_TYPE} STREQUAL "Release")
   set(CPACK_PACKAGE_FILE_NAME "${CPACK_PACKAGE_FILE_NAME}-${CMAKE_BUILD_TYPE}")
endif()

string(REPLACE "+" "-" CPACK_PACKAGE_FILE_NAME "${CPACK_PACKAGE_FILE_NAME}")
string(TOLOWER "${CPACK_PACKAGE_FILE_NAME}" CPACK_PACKAGE_FILE_NAME)

# variables to be re-used in package description fields
# PACKAGE_LONG_DESCRIPTION is based on the description in docs/user/rstudio/index.qmd
set(PACKAGE_LONG_DESCRIPTION "RStudio is an integrated development environment (IDE) designed to support multiple languages, including both R and Python. It includes a console, syntax-highlighting editor that supports direct code execution, and a variety of robust tools for plotting, viewing history, debugging and managing your workspace.")

# depend on a recent libc
list(APPEND RSTUDIO_DEBIAN_DEPENDS "libc6 (>= 2.7)")

# debian-specific
list(JOIN RSTUDIO_DEBIAN_DEPENDS ", " RSTUDIO_DEBIAN_DEPENDS)
set(CPACK_DEBIAN_PACKAGE_DESCRIPTION "${CPACK_PACKAGE_DESCRIPTION}\n ${PACKAGE_LONG_DESCRIPTION}")
set(CPACK_DEBIAN_PACKAGE_ARCHITECTURE "${PACKAGE_ARCHITECTURE}")
set(CPACK_DEBIAN_PACKAGE_DEPENDS "${RSTUDIO_DEBIAN_DEPENDS}")
set(CPACK_DEBIAN_PACKAGE_SUGGESTS "r-base (>= ${RSTUDIO_R_MAJOR_VERSION_REQUIRED}.${RSTUDIO_R_MINOR_VERSION_REQUIRED}.${RSTUDIO_R_PATCH_VERSION_REQUIRED})")
set(CPACK_DEBIAN_PACKAGE_CONTROL_STRICT_PERMISSION TRUE)
set(CPACK_DEBIAN_COMPRESSION_TYPE "xz")

# rpm-specific
list(JOIN RSTUDIO_RPM_DEPENDS ", " RSTUDIO_RPM_DEPENDS)
set(CPACK_RPM_PACKAGE_SUMMARY "${CPACK_PACKAGE_NAME}")
set(CPACK_RPM_PACKAGE_DESCRIPTION "${PACKAGE_LONG_DESCRIPTION}")
set(CPACK_RPM_PACKAGE_LICENSE "AGPL v.3.0")
set(CPACK_RPM_PACKAGE_GROUP "Development/Tools")
set(CPACK_RPM_PACKAGE_ARCHITECTURE "${PACKAGE_ARCHITECTURE}")
set(CPACK_RPM_PACKAGE_REQUIRES "${RSTUDIO_RPM_DEPENDS}")
set(CPACK_RPM_PACKAGE_AUTOREQPROV " no")


# build package
include(CPack)
