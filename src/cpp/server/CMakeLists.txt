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

project (SERVER)

add_subdirectory(pam)
add_subdirectory(crash-handler-proxy)

# include files
file(GLOB_RECURSE SERVER_HEADER_FILES "*.h*")

# source files
set(SERVER_SOURCE_FILES
   ServerBrowser.cpp
   ServerErrorCategory.cpp
   ServerEval.cpp
   ServerInit.cpp
   ServerLoginPages.cpp
   ServerMain.cpp
   ServerMainOverlay.cpp
   ServerMeta.cpp
   ServerOffline.cpp
   ServerOptions.cpp
   ServerOptionsOverlay.cpp
   ServerPAMAuth.cpp
   ServerPAMAuthOverlay.cpp
   ServerProcessSupervisor.cpp
   ServerREnvironment.cpp
   ServerSessionProxy.cpp
   ServerSessionProxyOverlay.cpp
   ServerSessionManager.cpp
   auth/ServerAuthHandler.cpp
   auth/ServerAuthCommon.cpp
   auth/ServerSecureUriHandler.cpp
   auth/ServerValidateUser.cpp
   ${CMAKE_CURRENT_BINARY_DIR}/ServerAddins.cpp
)

# define core include dirs
set(CORE_INCLUDE_DIRS
    ${CORE_SOURCE_DIR}/include
    ${SERVER_CORE_SOURCE_DIR}/include
)

# include addins
if(RSTUDIO_ADDINS_PATH)
   # search for addins (then remove special core directory)
   file(GLOB RSTUDIO_ADDINS ${RSTUDIO_ADDINS_PATH}/*)
   list(REMOVE_ITEM RSTUDIO_ADDINS "core")

   # incorporate all addins found
   foreach(RSTUDIO_ADDIN ${RSTUDIO_ADDINS})
      set(SERVER_ADDIN_PATH  ${RSTUDIO_ADDIN}/server)
      if(EXISTS ${SERVER_ADDIN_PATH})
          # glob the addin header, source, and template files
         file(GLOB_RECURSE ADDIN_HEADER_FILES "${SERVER_ADDIN_PATH}/*.h*")
         list(APPEND SERVER_HEADER_FILES ${ADDIN_HEADER_FILES})
         file(GLOB_RECURSE ADDIN_SOURCE_FILES "${SERVER_ADDIN_PATH}/*.c*")
         list(APPEND SERVER_SOURCE_FILES ${ADDIN_SOURCE_FILES})
         file(GLOB_RECURSE ADDIN_TEMPLATE_FILES "${SERVER_ADDIN_PATH}/templates/*")
         list(APPEND SERVER_ADDIN_TEMPLATE_FILES ${ADDIN_TEMPLATE_FILES})
         # generate an initialize call for the addin
         get_filename_component(ADDIN_NAME ${RSTUDIO_ADDIN} NAME)
         set(SERVER_ADDIN_DECLARATIONS
            "${SERVER_ADDIN_DECLARATIONS}namespace ${ADDIN_NAME} { Error initialize(); }\n" )
         set(SERVER_ADDIN_INITIALIZATIONS
            "${SERVER_ADDIN_INITIALIZATIONS}(${ADDIN_NAME}::initialize) ")
      endif()
   endforeach()

   # add to core include dirs if appropriate
   set(CORE_ADDINS_INCLUDE_DIR ${RSTUDIO_ADDINS_PATH}/core/include)
   if(EXISTS ${CORE_ADDINS_INCLUDE_DIR})
      list(APPEND CORE_INCLUDE_DIRS ${CORE_ADDINS_INCLUDE_DIR})
   endif()

endif()

# always configure the addins bootstrap file
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/ServerAddins.cpp.in
               ${CMAKE_CURRENT_BINARY_DIR}/ServerAddins.cpp)

# configure template files into the www directory
foreach(SERVER_ADDIN_TEMPLATE_FILE ${SERVER_ADDIN_TEMPLATE_FILES})
   get_filename_component(TEMPLATE_FILE_NAME ${SERVER_ADDIN_TEMPLATE_FILE} NAME)
   configure_file(${SERVER_ADDIN_TEMPLATE_FILE}
                  "${CMAKE_CURRENT_SOURCE_DIR}/../../gwt/www/templates/addins/${TEMPLATE_FILE_NAME}"
                  COPYONLY)
endforeach()


# generate config file
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/server-config.h.in
               ${CMAKE_CURRENT_BINARY_DIR}/server-config.h)

# required on Fedora for dynamic library-related code to compile
if(NOT APPLE)
   find_library(DL_LIBRARIES dl)
endif()

# include directories and libraries
set(SERVER_SYSTEM_LIBRARIES ${DL_LIBRARIES})

# handle El Capitan moving OpenSSL away
if(APPLE)
   if(${MACOSX_VERSION} VERSION_GREATER "10.10"
         AND EXISTS "/usr/local/opt/openssl")

      set(SERVER_SYSTEM_LIBRARIES
         ${SERVER_SYSTEM_LIBRARIES}
         -L/usr/local/opt/openssl/lib -lssl -lcrypto)

      set(CORE_INCLUDE_DIRS
         ${CORE_INCLUDE_DIRS}
         /usr/local/opt/openssl/include)

      if(NOT DEFINED OPENSSL_ROOT_DIR)
         set(OPENSSL_ROOT_DIR
            /usr/local/opt/openssl)
      endif()
   endif()

   find_package(OpenSSL REQUIRED QUIET)
   set(SERVER_SYSTEM_LIBRARIES
      ${SERVER_SYSTEM_LIBRARIES}
      ${OPENSSL_LIBRARIES}
   )
   set(CORE_INCLUDE_DIRS
      ${CORE_INCLUDE_DIRS}
      ${OPENSSL_INCLUDE_DIR}
   )
endif()

# set include directories
include_directories(
   include
   ${CMAKE_CURRENT_BINARY_DIR}
   ${Boost_INCLUDE_DIRS}
   ${SERVER_SYSTEM_INCLUDE_DIRS}
   ${CORE_INCLUDE_DIRS}
   ${MONITOR_SOURCE_DIR}/include
   ${EXT_SOURCE_DIR}
   ${SESSION_SOURCE_DIR}/include
   ${SHARED_CORE_SOURCE_DIR}/include
)

# define executable
add_stripped_executable(rserver ${SERVER_SOURCE_FILES} ${SERVER_HEADER_FILES})

# set link dependencies
target_link_libraries(rserver
   rstudio-core
   rstudio-server-core
   rstudio-monitor
   ${CRASHPAD_LIBRARIES}
   ${SOCI_LIBRARIES}
   ${SERVER_SYSTEM_LIBRARIES}
)

# install binary
install(TARGETS rserver DESTINATION ${RSTUDIO_INSTALL_BIN})

# set up to create a global theme directory for server installs.
set (RSERVER_THEME_README "themes/README.md")
configure_file("${CMAKE_CURRENT_SOURCE_DIR}/${RSERVER_THEME_README}"
               "${CMAKE_CURRENT_BINARY_DIR}/extras/${RSERVER_THEME_README}")
install(FILES "${CMAKE_CURRENT_BINARY_DIR}/extras/${RSERVER_THEME_README}"
       DESTINATION "extras/themes")

# do the same for fonts
set (RSERVER_FONT_README "fonts/README.md")
configure_file("${CMAKE_CURRENT_SOURCE_DIR}/${RSERVER_FONT_README}"
               "${CMAKE_CURRENT_BINARY_DIR}/extras/${RSERVER_FONT_README}")
install(FILES "${CMAKE_CURRENT_BINARY_DIR}/extras/${RSERVER_FONT_README}"
       DESTINATION "extras/fonts")

# install db migration scripts
file(GLOB DB_MIGRATION_FILES_SQL "db/*.sql")
file(GLOB DB_MIGRATION_FILES_SQLITE "db/*.sqlite")
file(GLOB DB_MIGRATION_FILES_POSTGRESQL "db/*.postgresql")
install(FILES ${DB_MIGRATION_FILES_SQL} ${DB_MIGRATION_FILES_SQLITE} ${DB_MIGRATION_FILES_POSTGRESQL}
        DESTINATION "db")

# install default database.conf file
set(RSERVER_CONF_DIR "extras/conf")
set(RSERVER_DATABASE_CONF_FILE "${RSERVER_CONF_DIR}/database.conf")
install(FILES ${CMAKE_CURRENT_SOURCE_DIR}/${RSERVER_DATABASE_CONF_FILE}
        DESTINATION ${RSERVER_CONF_DIR})

if (UNIX AND NOT APPLE)
   # install configured admin script
   set(RSERVER_ADMIN_SCRIPT "extras/admin/rstudio-server")
   configure_file(${CMAKE_CURRENT_SOURCE_DIR}/${RSERVER_ADMIN_SCRIPT}.in
                  ${CMAKE_CURRENT_BINARY_DIR}/${RSERVER_ADMIN_SCRIPT})
   install(PROGRAMS ${CMAKE_CURRENT_BINARY_DIR}/${RSERVER_ADMIN_SCRIPT}
                    DESTINATION ${RSTUDIO_INSTALL_BIN})

   # install configured debian init.d script
   set(RSERVER_INITD_DEBIAN_DIR "extras/init.d/debian")
   set(RSERVER_INITD_DEBIAN_SCRIPT "${RSERVER_INITD_DEBIAN_DIR}/rstudio-server")
   configure_file(${CMAKE_CURRENT_SOURCE_DIR}/${RSERVER_INITD_DEBIAN_SCRIPT}.in
                  ${CMAKE_CURRENT_BINARY_DIR}/${RSERVER_INITD_DEBIAN_SCRIPT})
   install(PROGRAMS ${CMAKE_CURRENT_BINARY_DIR}/${RSERVER_INITD_DEBIAN_SCRIPT}
           DESTINATION ${RSERVER_INITD_DEBIAN_DIR})

   # install configured redhat init.d script
   set(RSERVER_INITD_REDHAT_DIR "extras/init.d/redhat")
   set(RSERVER_INITD_REDHAT_SCRIPT "${RSERVER_INITD_REDHAT_DIR}/rstudio-server")
   configure_file(${CMAKE_CURRENT_SOURCE_DIR}/${RSERVER_INITD_REDHAT_SCRIPT}.in
                  ${CMAKE_CURRENT_BINARY_DIR}/${RSERVER_INITD_REDHAT_SCRIPT})
   install(PROGRAMS ${CMAKE_CURRENT_BINARY_DIR}/${RSERVER_INITD_REDHAT_SCRIPT}
           DESTINATION ${RSERVER_INITD_REDHAT_DIR})

   # install configured suse init.d script
   set(RSERVER_INITD_SUSE_DIR "extras/init.d/suse")
   set(RSERVER_INITD_SUSE_SCRIPT "${RSERVER_INITD_SUSE_DIR}/rstudio-server")
   configure_file(${CMAKE_CURRENT_SOURCE_DIR}/${RSERVER_INITD_SUSE_SCRIPT}.in
                  ${CMAKE_CURRENT_BINARY_DIR}/${RSERVER_INITD_SUSE_SCRIPT})
   install(PROGRAMS ${CMAKE_CURRENT_BINARY_DIR}/${RSERVER_INITD_SUSE_SCRIPT}
           DESTINATION ${RSERVER_INITD_SUSE_DIR})

   # install pam profile
   set(RSERVER_PAM_DIR "extras/pam")
   set(RSERVER_PAM_PROFILE "${RSERVER_PAM_DIR}/rstudio")
   install(FILES ${CMAKE_CURRENT_SOURCE_DIR}/${RSERVER_PAM_PROFILE}
           DESTINATION ${RSERVER_PAM_DIR})

   # install configured upstart profile
   set(RSERVER_UPSTART_DIR "extras/upstart")
   set(RSERVER_UPSTART_PROFILE "${RSERVER_UPSTART_DIR}/rstudio-server.conf")
   configure_file(${CMAKE_CURRENT_SOURCE_DIR}/${RSERVER_UPSTART_PROFILE}.in
                  ${CMAKE_CURRENT_BINARY_DIR}/${RSERVER_UPSTART_PROFILE})
   install(FILES ${CMAKE_CURRENT_BINARY_DIR}/${RSERVER_UPSTART_PROFILE}
           DESTINATION ${RSERVER_UPSTART_DIR})
   set(RSERVER_UPSTART_PROFILE_REDHAT "${RSERVER_UPSTART_DIR}/rstudio-server.redhat.conf")
   configure_file(${CMAKE_CURRENT_SOURCE_DIR}/${RSERVER_UPSTART_PROFILE_REDHAT}.in
                  ${CMAKE_CURRENT_BINARY_DIR}/${RSERVER_UPSTART_PROFILE_REDHAT})
   install(FILES ${CMAKE_CURRENT_BINARY_DIR}/${RSERVER_UPSTART_PROFILE_REDHAT}
           DESTINATION ${RSERVER_UPSTART_DIR})

    # install configured systemd profile
   set(RSERVER_SYSTEMD_DIR "extras/systemd")
   set(RSERVER_SYSTEMD_PROFILE "${RSERVER_SYSTEMD_DIR}/rstudio-server.service")
   configure_file(${CMAKE_CURRENT_SOURCE_DIR}/${RSERVER_SYSTEMD_PROFILE}.in
                  ${CMAKE_CURRENT_BINARY_DIR}/${RSERVER_SYSTEMD_PROFILE})
   install(FILES ${CMAKE_CURRENT_BINARY_DIR}/${RSERVER_SYSTEMD_PROFILE}
           DESTINATION ${RSERVER_SYSTEMD_DIR})
   set(RSERVER_SYSTEMD_PROFILE_REDHAT "${RSERVER_SYSTEMD_DIR}/rstudio-server.redhat.service")
   configure_file(${CMAKE_CURRENT_SOURCE_DIR}/${RSERVER_SYSTEMD_PROFILE_REDHAT}.in
                  ${CMAKE_CURRENT_BINARY_DIR}/${RSERVER_SYSTEMD_PROFILE_REDHAT})
   install(FILES ${CMAKE_CURRENT_BINARY_DIR}/${RSERVER_SYSTEMD_PROFILE_REDHAT}
           DESTINATION ${RSERVER_SYSTEMD_DIR})

elseif(APPLE)

   # install configured admin script
  set(RSERVER_ADMIN_SCRIPT "extras/admin/rstudio-server")
  configure_file(${CMAKE_CURRENT_SOURCE_DIR}/${RSERVER_ADMIN_SCRIPT}.mac.in
                 ${CMAKE_CURRENT_BINARY_DIR}/${RSERVER_ADMIN_SCRIPT})
  install(PROGRAMS ${CMAKE_CURRENT_BINARY_DIR}/${RSERVER_ADMIN_SCRIPT}
                   DESTINATION ${RSTUDIO_INSTALL_BIN})

  # install configured launchd plist for mac
  set(RSERVER_LAUNCHD_DIR "extras/launchd")
  set(RSERVER_LAUNCHD_PLIST "${RSERVER_LAUNCHD_DIR}/com.rstudio.launchd.rserver.plist")
  configure_file(${CMAKE_CURRENT_SOURCE_DIR}/${RSERVER_LAUNCHD_PLIST}.in
                 ${CMAKE_CURRENT_BINARY_DIR}/${RSERVER_LAUNCHD_PLIST})
  install(PROGRAMS ${CMAKE_CURRENT_BINARY_DIR}/${RSERVER_LAUNCHD_PLIST}
          DESTINATION ${RSERVER_LAUNCHD_DIR})

   # install pam profile
   set(RSERVER_PAM_DIR "extras/pam")
   set(RSERVER_PAM_PROFILE "${RSERVER_PAM_DIR}/mac/rstudio")
   install(FILES ${CMAKE_CURRENT_SOURCE_DIR}/${RSERVER_PAM_PROFILE}
           DESTINATION ${RSERVER_PAM_DIR})

endif()

# add overlay if it exists
if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/CMakeOverlay.txt")
   include(CMakeOverlay.txt)
endif()
