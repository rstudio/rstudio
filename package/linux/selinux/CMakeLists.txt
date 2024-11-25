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

cmake_minimum_required(VERSION 3.10)
project(selinux)

# configure cpack install location
set(CPACK_SET_DESTDIR "ON")
set(CPACK_INSTALL_PREFIX "${CMAKE_INSTALL_PREFIX}")

# package metadata
set(CPACK_PACKAGE_NAME "rstudio-server-selinux")
set(CPACK_PACKAGE_DESCRIPTION "SELinux policy module for rstudio-server")
set(CPACK_PACKAGE_VENDOR "Posit Software")
set(CPACK_PACKAGE_CONTACT "RStudio <info@posit.co>")

# read distro information from standard location
set(IS_RHEL "OFF")
set(DISTRO_NAME "generic Linux")
set(DISTRO_PRETTY_NAME "")
set(DISTRO_ID "")
set(DISTRO_VERSION "")
set(DISTRO_CODENAME "")
if(EXISTS "/etc/os-release")
  file(STRINGS "/etc/os-release" OS_RELEASE_LINES)
  foreach(LINE ${OS_RELEASE_LINES})
    string(REGEX MATCH "^[^=]+" KEY ${LINE})
    string(REPLACE "${KEY}=" "" VAL ${LINE})
    string(REPLACE "\"" "" VAL ${VAL})
    if(KEY STREQUAL "ID")
      set(DISTRO_ID ${VAL})
    elseif(KEY STREQUAL "ID_LIKE")
      string(FIND ${VAL} "rhel" FOUND)
      if(FOUND GREATER_EQUAL 0)
        set(IS_RHEL "ON")
      endif()
    elseif(KEY STREQUAL "VERSION_ID")
      string(REGEX MATCH "^[0-9]+" DISTRO_VERSION)
    elseif(KEY STREQUAL "VERSION_CODENAME")
      set(DISTRO_CODENAME ${VAL})
    elseif(KEY STREQUAL "NAME")
      set(DISTRO_NAME ${VAL})
    elseif(KEY STREQUAL "PRETTY_NAME")
      set(DISTRO_PRETTY_NAME ${VAL})
    endif()
  endforeach()
endif()
if(DISTRO_PRETTY_NAME STREQUAL "")
  set(DISTRO_PRETTY_NAME ${DISTRO_NAME})
endif()
if(IS_RHEL)
  set(PACKAGE_SUFFIX "-rhel${DISTRO_VERSION}")
elseif(NOT DISTRO_CODENAME STREQUAL "")
  set(PACKAGE_SUFFIX "-${DISTRO_CODENAME}")
elseif(NOT DISTRO_ID STREQUAL "")
  set(PACKAGE_SUFFIX "-${DISTRO_ID}${DISTRO_VERSION}")
else()
  set(PACKAGE_SUFFIX "")
endif()

message(STATUS "Packaging SELinux policy for ${DISTRO_PRETTY_NAME}")

# find the Makefile provided by the selinux policy
if(NOT DEFINED SELINUX_MAKEFILE)
  if(EXISTS "/usr/share/selinux/devel/Makefile")
    set(SELINUX_MAKEFILE "/usr/share/selinux/devel/Makefile")
  elseif(EXISTS "/usr/share/selinux/devel/include/Makefile")
    set(SELINUX_MAKEFILE "/usr/share/selinux/devel/include/Makefile")
  else()
    # Some distros use different paths, so search for the file
    file(GLOB_RECURSE SELINUX_MAKEFILE FOLLOW_SYMLINKS "/usr/share/selinux/*/Makefile")
    list(GET SELINUX_MAKEFILE 0 SELINUX_MAKEFILE)
  endif()
endif()

# Find where the distro stores its .pp files
if(NOT DEFINED SELINUX_PACKAGE_PATH)
  if(EXISTS "/usr/share/selinux/packages/")
    set(SELINUX_PACKAGE_PATH "/usr/share/selinux/packages/")
  elseif(EXISTS "/usr/share/selinux/default/")
    set(SELINUX_PACKAGE_PATH "/usr/share/selinux/default/")
  else()
    # Some distros use different paths, so search for an existing policy file and imitate it
    file(GLOB_RECURSE SELINUX_PACKAGE_PATH FOLLOW_SYMLINKS "/usr/share/selinux/*.pp*")
    list(GET SELINUX_PACKAGE_PATH 0 SELINUX_PACKAGE_PATH)
    get_filename_component(SELINUX_PACKAGE_PATH ${SELINUX_PACKAGE_PATH} DIRECTORY)
  endif()
endif()

# configure the paths in the file context list
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/rstudio.fc.in
               ${CMAKE_CURRENT_SOURCE_DIR}/rstudio.fc)

# compile the policy module using the selinux Makefile
add_custom_command(OUTPUT ${CMAKE_CURRENT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/rstudio.pp
                   COMMAND make ARGS -C ${CMAKE_CURRENT_SOURCE_DIR} -f ${SELINUX_MAKEFILE} rstudio.pp
                   COMMAND mv ARGS ${CMAKE_CURRENT_SOURCE_DIR}/rstudio.pp ${CMAKE_CURRENT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/rstudio.pp
                   MAIN_DEPENDENCY ${CMAKE_CURRENT_SOURCE_DIR}/rstudio.te
                   DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/rstudio.fc
                           ${CMAKE_CURRENT_SOURCE_DIR}/rstudio.te)

# generate the man pages
add_custom_command(OUTPUT ${CMAKE_CURRENT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/rstudio_selinux.8
                          ${CMAKE_CURRENT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/rstudio_server_selinux.8
                   COMMAND sepolicy ARGS ${SEPOLICY_ARGS} manpage -p ${CMAKE_CURRENT_BINARY_DIR}${CMAKE_FILES_DIRECTORY} -d rstudio_t rstudio_server_t
                   MAIN_DEPENDENCY ${CMAKE_CURRENT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/rstudio.pp)

# install the module and the man pages to the appropriate paths
add_custom_target(selinux ALL
                  DEPENDS ${CMAKE_CURRENT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/rstudio.pp
                          ${CMAKE_CURRENT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/rstudio_selinux.8
                          ${CMAKE_CURRENT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/rstudio_server_selinux.8)
install(FILES ${CMAKE_CURRENT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/rstudio.pp
        DESTINATION ${SELINUX_PACKAGE_PATH})
install(FILES ${CMAKE_CURRENT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/rstudio_selinux.8
              ${CMAKE_CURRENT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/rstudio_server_selinux.8
        DESTINATION ${CMAKE_INSTALL_DATAROOTDIR}/man/man8)

# prepare the installation/uninstallation scripts
if(IS_RHEL)
  set(CHECK_UNINSTALL_CONDITION "[ $1 -eq 0 ]")
else()
  set(CHECK_UNINSTALL_CONDITION "/bin/true")
endif()
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/postinst.in
               ${CMAKE_CURRENT_BINARY_DIR}/postinst)
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/postrm.in
               ${CMAKE_CURRENT_BINARY_DIR}/postrm)

set(PACKAGE_ATOM "${CPACK_PACKAGE_NAME}${PACKAGE_SUFFIX}${CPACK_PACKAGE_VERSION}")

  # rpm-specific
set(CPACK_RPM_SPEC_INSTALL_POST "/bin/true")
set(CPACK_RPM_POST_INSTALL_SCRIPT_FILE "${CMAKE_CURRENT_BINARY_DIR}/postinst")
set(CPACK_RPM_POST_UNINSTALL_SCRIPT_FILE "${CMAKE_CURRENT_BINARY_DIR}/postrm")

set(CPACK_RPM_FILE_NAME "${PACKAGE_ATOM}-noarch")
set(CPACK_RPM_PACKAGE_SUMMARY "${CPACK_PACKAGE_DESCRIPTION}")
set(CPACK_RPM_PACKAGE_LICENSE "AGPL v.3.0")
set(CPACK_RPM_PACKAGE_GROUP "Development/Tools")
set(CPACK_RPM_PACKAGE_ARCHITECTURE "noarch")
set(CPACK_RPM_PACKAGE_REQUIRES "policycoreutils-python-utils, libselinux-utils")
set(CPACK_RPM_PACKAGE_REQUIRES_POST "selinux-policy-base, policycoreutils-python-utils")
set(CPACK_RPM_PACKAGE_REQUIRES_POSTUN "policycoreutils-python-utils")
set(CPACK_RPM_PACKAGE_SUGGESTS "rstudio-server")
set(CPACK_RPM_PACKAGE_AUTOREQPROV " no")

# debian-specific
file(COPY ${CMAKE_CURRENT_BINARY_DIR}/postinst
          ${CMAKE_CURRENT_BINARY_DIR}/postrm
          DESTINATION ${CMAKE_CURRENT_BINARY_DIR}/debian-control
          FILE_PERMISSIONS OWNER_READ OWNER_WRITE OWNER_EXECUTE GROUP_READ
          GROUP_EXECUTE WORLD_READ WORLD_EXECUTE)
set(CPACK_DEBIAN_PACKAGE_CONTROL_EXTRA "${CMAKE_CURRENT_BINARY_DIR}/debian-control/postinst;${CMAKE_CURRENT_BINARY_DIR}/debian-control/postrm")

set(CPACK_PACKAGE_FILE_NAME "${PACKAGE_ATOM}-all")
set(CPACK_DEBIAN_PACKAGE_DESCRIPTION "${CPACK_PACKAGE_DESCRIPTION}")
set(CPACK_DEBIAN_PACKAGE_ARCHITECTURE "all")
set(CPACK_DEBIAN_PACKAGE_DEPENDS "selinux-policy-default, policycoreutils, selinux-utils")
set(CPACK_DEBIAN_PACKAGE_SUGGESTS "rstudio-server")
set(CPACK_DEBIAN_PACKAGE_CONTROL_STRICT_PERMISSION TRUE)
set(CPACK_DEBIAN_COMPRESSION_TYPE "xz")

# build package
include(CPack)
