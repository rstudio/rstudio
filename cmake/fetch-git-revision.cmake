#
# fetch-git-revision.cmake
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

# Populates SOURCE_DIR with REPOSITORY at REVISION, downloading only that
# commit, plus the submodule commits it records, at depth 1.
#
# This replaces FetchContent's GIT_SHALLOW, which clones every branch tip and
# tag at depth 1 and then checks out the revision. That can't check out a
# pinned commit once it is neither a tag nor a branch tip, and it downloads a
# tree for every branch and tag to get one. Fetching the commit by id works
# for any commit the server can reach (GitHub allows it, as does any server
# speaking git protocol v2).
#
# Usage:
#   cmake -DGIT_EXECUTABLE=<git> -DREPOSITORY=<url> -DREVISION=<commit>
#         -DSOURCE_DIR=<dir> [-DRETRY_COUNT=<n>] [-DRETRY_DELAY=<seconds>]
#         -P fetch-git-revision.cmake

cmake_minimum_required(VERSION 3.25)

foreach(_VAR GIT_EXECUTABLE REPOSITORY REVISION SOURCE_DIR)
   if("${${_VAR}}" STREQUAL "")
      message(FATAL_ERROR "fetch-git-revision.cmake requires -D${_VAR}=<value>")
   endif()
endforeach()

# FetchContent's defaults for git clone retries
if("${RETRY_COUNT}" STREQUAL "")
   set(RETRY_COUNT 2)
endif()
if("${RETRY_DELAY}" STREQUAL "")
   set(RETRY_DELAY 0)
endif()


# Runs git in SOURCE_DIR, and stops if it fails.
function(run_git)
   execute_process(
      COMMAND "${GIT_EXECUTABLE}" ${ARGN}
      WORKING_DIRECTORY "${SOURCE_DIR}"
      RESULT_VARIABLE _RESULT)

   if(NOT _RESULT EQUAL 0)
      list(JOIN ARGN " " _COMMAND)
      message(FATAL_ERROR "'git ${_COMMAND}' failed in ${SOURCE_DIR}")
   endif()
endfunction()


# Like run_git(), for commands that use the network: a failed attempt is
# retried RETRY_COUNT times, RETRY_DELAY seconds apart.
function(run_git_with_retries)
   list(JOIN ARGN " " _COMMAND)

   set(_ATTEMPT 0)
   while(TRUE)
      execute_process(
         COMMAND "${GIT_EXECUTABLE}" ${ARGN}
         WORKING_DIRECTORY "${SOURCE_DIR}"
         RESULT_VARIABLE _RESULT)
      if(_RESULT EQUAL 0)
         return()
      endif()

      math(EXPR _ATTEMPT "${_ATTEMPT} + 1")
      if(_ATTEMPT GREATER RETRY_COUNT)
         message(FATAL_ERROR "'git ${_COMMAND}' failed for ${REPOSITORY}")
      endif()

      message(STATUS "'git ${_COMMAND}' failed for ${REPOSITORY}; retrying in ${RETRY_DELAY} seconds")
      execute_process(COMMAND "${CMAKE_COMMAND}" -E sleep "${RETRY_DELAY}")
   endwhile()
endfunction()


# Start from an empty directory, as FetchContent's git clone does, so files
# from an earlier revision or a partial fetch can't survive.
file(REMOVE_RECURSE "${SOURCE_DIR}")
file(MAKE_DIRECTORY "${SOURCE_DIR}")

run_git(init --quiet)
run_git(remote add origin "${REPOSITORY}")
run_git_with_retries(fetch --quiet --depth 1 --no-tags origin "${REVISION}")
run_git(checkout --quiet --detach FETCH_HEAD)

# FetchContent initializes every submodule, recursively; keep doing that, but
# shallowly.
if(EXISTS "${SOURCE_DIR}/.gitmodules")
   run_git_with_retries(submodule --quiet update --init --recursive --depth 1)
endif()
