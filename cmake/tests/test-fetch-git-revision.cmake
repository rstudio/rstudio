#
# test-fetch-git-revision.cmake
#
# Copyright (C) 2026 by Posit Software, PBC
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

# Tests fetch-git-revision.cmake against local repositories whose pinned
# commits are neither tags nor branch tips, which FetchContent's GIT_SHALLOW
# cannot check out (#19714). The pinned commit also records a submodule
# pinned the same way.
#
# Usage:
#   cmake [-DWORK_DIR=<dir>] -P cmake/tests/test-fetch-git-revision.cmake
#
# WORK_DIR defaults to a folder in the current directory, and is removed
# when every test passes.

cmake_minimum_required(VERSION 3.25)

get_filename_component(
   FETCH_SCRIPT
   "${CMAKE_CURRENT_LIST_DIR}/../fetch-git-revision.cmake"
   ABSOLUTE)

find_program(GIT_EXECUTABLE git REQUIRED)

if("${WORK_DIR}" STREQUAL "")
   set(WORK_DIR "${CMAKE_CURRENT_BINARY_DIR}/test-fetch-git-revision")
endif()
file(REMOVE_RECURSE "${WORK_DIR}")
file(MAKE_DIRECTORY "${WORK_DIR}")

# Keep the user's git config (commit signing, hooks, protocol version) out of
# the test. Allow file:// submodules, which git blocks by default, and let
# the local server hand out unadvertised commits the way GitHub does.
file(WRITE "${WORK_DIR}/gitconfig" [=[
[user]
   name = test
   email = test@example.com
[protocol "file"]
   allow = always
[uploadpack]
   allowReachableSHA1InWant = true
]=])
set(ENV{GIT_CONFIG_GLOBAL} "${WORK_DIR}/gitconfig")
set(ENV{GIT_CONFIG_NOSYSTEM} 1)


# Runs git in DIR, stores its output in OUT_VAR, and stops if it fails.
function(run_git DIR OUT_VAR)
   execute_process(
      COMMAND "${GIT_EXECUTABLE}" ${ARGN}
      WORKING_DIRECTORY "${DIR}"
      RESULT_VARIABLE _RESULT
      OUTPUT_VARIABLE _OUTPUT
      ERROR_VARIABLE _ERROR
      OUTPUT_STRIP_TRAILING_WHITESPACE)

   if(NOT _RESULT EQUAL 0)
      list(JOIN ARGN " " _COMMAND)
      message(FATAL_ERROR "'git ${_COMMAND}' failed in ${DIR}: ${_ERROR}")
   endif()

   set(${OUT_VAR} "${_OUTPUT}" PARENT_SCOPE)
endfunction()


# Writes LABEL to marker.txt in the repository at DIR, commits everything,
# and stores the new commit in OUT_VAR.
function(commit_marker DIR LABEL OUT_VAR)
   file(WRITE "${DIR}/marker.txt" "${LABEL}")
   run_git("${DIR}" _IGNORED add --all)
   run_git("${DIR}" _IGNORED commit --quiet --message "${LABEL}")
   run_git("${DIR}" _COMMIT rev-parse HEAD)
   set(${OUT_VAR} "${_COMMIT}" PARENT_SCOPE)
endfunction()


# Returns a file:// URL for an absolute PATH, including Windows drive paths.
function(file_url PATH OUT_VAR)
   if(PATH MATCHES "^/")
      set(${OUT_VAR} "file://${PATH}" PARENT_SCOPE)
   else()
      set(${OUT_VAR} "file:///${PATH}" PARENT_SCOPE)
   endif()
endfunction()


# Runs fetch-git-revision.cmake, and stores its exit status in OUT_VAR.
function(fetch REPOSITORY REVISION SOURCE_DIR OUT_VAR)
   execute_process(
      COMMAND
         "${CMAKE_COMMAND}"
         "-DGIT_EXECUTABLE=${GIT_EXECUTABLE}"
         "-DREPOSITORY=${REPOSITORY}"
         "-DREVISION=${REVISION}"
         "-DSOURCE_DIR=${SOURCE_DIR}"
         -DRETRY_COUNT=1
         -DRETRY_DELAY=0
         -P "${FETCH_SCRIPT}"
      RESULT_VARIABLE _RESULT
      OUTPUT_VARIABLE _OUTPUT
      ERROR_VARIABLE _OUTPUT)

   set(${OUT_VAR} "${_RESULT}" PARENT_SCOPE)
   set(${OUT_VAR}_OUTPUT "${_OUTPUT}" PARENT_SCOPE)
endfunction()


function(expect_equal ACTUAL EXPECTED WHAT)
   if(NOT "${ACTUAL}" STREQUAL "${EXPECTED}")
      message(FATAL_ERROR "${WHAT}: expected '${EXPECTED}', got '${ACTUAL}'")
   endif()
endfunction()


# Stops unless the fetch() that stored its status in STATUS_VAR succeeded.
function(expect_fetched STATUS_VAR WHAT)
   if(NOT "${${STATUS_VAR}}" STREQUAL "0")
      message(FATAL_ERROR "${WHAT} failed:\n${${STATUS_VAR}_OUTPUT}")
   endif()
endfunction()


function(expect_marker DIR EXPECTED)
   file(READ "${DIR}/marker.txt" _MARKER)
   expect_equal("${_MARKER}" "${EXPECTED}" "${DIR}/marker.txt")
endfunction()


# A submodule repository: base, then the commit the dependency pins, then
# a newer tip.
set(SUB_DIR "${WORK_DIR}/sub")
file(MAKE_DIRECTORY "${SUB_DIR}")
run_git("${SUB_DIR}" _IGNORED init --quiet)
commit_marker("${SUB_DIR}" "base" _IGNORED)
commit_marker("${SUB_DIR}" "pinned" SUB_PINNED)
commit_marker("${SUB_DIR}" "tip" _IGNORED)
file_url("${SUB_DIR}" SUB_URL)

# The dependency itself, shaped the same way. Its pinned commit records the
# submodule at the submodule's pinned commit.
set(DEP_DIR "${WORK_DIR}/dep")
file(MAKE_DIRECTORY "${DEP_DIR}")
run_git("${DEP_DIR}" _IGNORED init --quiet)
commit_marker("${DEP_DIR}" "base" _IGNORED)
run_git("${DEP_DIR}" _IGNORED submodule --quiet add "${SUB_URL}" sub)
run_git("${DEP_DIR}/sub" _IGNORED checkout --quiet "${SUB_PINNED}")
commit_marker("${DEP_DIR}" "pinned" DEP_PINNED)
commit_marker("${DEP_DIR}" "tip" DEP_TIP)
file_url("${DEP_DIR}" DEP_URL)

set(SOURCE_DIR "${WORK_DIR}/src")


# A commit that is neither a tag nor a branch tip, with its submodule.
fetch("${DEP_URL}" "${DEP_PINNED}" "${SOURCE_DIR}" RESULT)
expect_fetched(RESULT "fetching the pinned commit")

run_git("${SOURCE_DIR}" HEAD rev-parse HEAD)
expect_equal("${HEAD}" "${DEP_PINNED}" "checked-out commit")
expect_marker("${SOURCE_DIR}" "pinned")
expect_marker("${SOURCE_DIR}/sub" "pinned")

# Only the pinned commits were downloaded, not their history.
run_git("${SOURCE_DIR}" SHALLOW rev-parse --is-shallow-repository)
expect_equal("${SHALLOW}" "true" "dependency checkout is shallow")
run_git("${SOURCE_DIR}/sub" SHALLOW rev-parse --is-shallow-repository)
expect_equal("${SHALLOW}" "true" "submodule checkout is shallow")


# Moving the pin replaces the existing checkout.
fetch("${DEP_URL}" "${DEP_TIP}" "${SOURCE_DIR}" RESULT)
expect_fetched(RESULT "fetching a new pin over an old checkout")
expect_marker("${SOURCE_DIR}" "tip")


# A commit the server doesn't have fails, after its retries.
fetch("${DEP_URL}" "0123456789abcdef0123456789abcdef01234567" "${SOURCE_DIR}" RESULT)
if(RESULT EQUAL 0)
   message(FATAL_ERROR "fetching a missing commit succeeded")
endif()
if(NOT RESULT_OUTPUT MATCHES "retrying in 0 seconds")
   message(FATAL_ERROR "fetching a missing commit was not retried:\n${RESULT_OUTPUT}")
endif()


file(REMOVE_RECURSE "${WORK_DIR}")
message(STATUS "fetch-git-revision.cmake: all tests passed")
