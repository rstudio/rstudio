include(CMakeParseArguments)

function(download URL FILE)

   get_filename_component(FILENAME "${FILE}" NAME)
   if (EXISTS "${FILE}")
      message(STATUS "${FILENAME} already exists; skipping download")
      return()
   endif()

   # perform the download
   message(STATUS "Downloading ${FILENAME}")
   file(DOWNLOAD "${URL}" "${FILE}" STATUS STATUS)

   # check status
   list(GET STATUS 0 EC)
   list(GET STATUS 1 MSG)
   if(NOT EC EQUAL 0)
      message(FATAL_ERROR "Download failed: ${MSG}")
   endif()
   message(STATUS "Downloading ${FILENAME} - Success")

endfunction()

function(install_process)
   cmake_parse_arguments(install_process "" "" "COMMAND" ${ARGN})
   install(CODE "execute_process(COMMAND ${install_process_COMMAND} WORKING_DIRECTORY \"\$ENV{DESTDIR}\${CMAKE_INSTALL_PREFIX}\")")
endfunction()
