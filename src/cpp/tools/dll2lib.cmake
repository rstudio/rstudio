#
# dll2lib.cmake
#
# Copyright (C) 2026 by Posit Software, PBC
#
# This program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#
# The work of dll2lib.R without running R, for a cross-compiled build whose R
# can't run here: writes a .def and an import .lib for each DLL in R_BIN_DIR.
#
#   cmake -DR_BIN_DIR=<R bin folder> -DCOMPILER=<cl.exe> -DMACHINE=<ARM64> -P dll2lib.cmake

get_filename_component(_TOOLS_DIR "${COMPILER}" DIRECTORY)
file(GLOB _DLLS "${R_BIN_DIR}/*.dll")

message("Generating .lib files for DLLs in ${R_BIN_DIR}")

foreach(_DLL IN LISTS _DLLS)

   # skip DLLs whose exports were already generated
   string(REGEX REPLACE "dll$" "def" _DEF "${_DLL}")
   if(EXISTS "${_DEF}")
      continue()
   endif()

   set(_DUMP "${_DEF}.dumpbin")
   execute_process(
      COMMAND "${_TOOLS_DIR}/dumpbin.exe" /EXPORTS /NOLOGO "${_DLL}"
      OUTPUT_FILE "${_DUMP}"
      RESULT_VARIABLE _RESULT)
   if(NOT _RESULT EQUAL 0)
      message(FATAL_ERROR "dumpbin failed on ${_DLL}")
   endif()

   # export table rows are 'ordinal hint RVA name'; forwarded exports are skipped
   file(STRINGS "${_DUMP}" _ROWS REGEX "^ +[0-9]+ +[0-9A-Fa-f]+ +[0-9A-Fa-f]+ +[^ ]")
   file(REMOVE "${_DUMP}")

   # same layout as dll2lib.R writes: dumpbin's order (file(WRITE) writes CRLF here)
   set(_LINES "EXPORTS")
   foreach(_ROW IN LISTS _ROWS)
      if(_ROW MATCHES "forwarded to")
         continue()
      endif()
      if(_ROW MATCHES "^ +[0-9]+ +[0-9A-Fa-f]+ +[0-9A-Fa-f]+ +([^ =\r]+)")
         list(APPEND _LINES "\t${CMAKE_MATCH_1}")
      endif()
   endforeach()
   string(JOIN "\n" _CONTENT ${_LINES})
   file(WRITE "${_DEF}" "${_CONTENT}\n")

   string(REGEX REPLACE "dll$" "lib" _LIB "${_DLL}")
   execute_process(
      COMMAND "${_TOOLS_DIR}/lib.exe" "/def:${_DEF}" "/out:${_LIB}" "/machine:${MACHINE}"
      RESULT_VARIABLE _RESULT)
   if(NOT _RESULT EQUAL 0)
      message(FATAL_ERROR "lib.exe failed for ${_DLL}")
   endif()

endforeach()
