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
# commit, and initializes its submodules (recursively) at depth 1.
#
# This replaces FetchContent's GIT_SHALLOW, which clones every branch tip and
# tag at depth 1 and then checks out the revision. That can't check out a
# pinned commit once it is neither a tag nor a branch tip, and it downloads a
# tree for every branch and tag to get one. Fetching the commit by id works
# for any commit the server can reach (GitHub allows it, as does any server
# speaking git protocol v2).
#
# Usage:
#   cmake -DREPOSITORY=<url> -DREVISION=<commit> -DSOURCE_DIR=<dir>
#         [-DGIT_EXECUTABLE=<git>] [-DRETRY_COUNT=<n>] [-DRETRY_DELAY=<seconds>]
#         [-DTLS_VERSION=<version>] [-DTLS_VERIFY=<bool>]
#         -P fetch-git-revision.cmake
#
# TLS_VERSION and TLS_VERIFY default to the CMAKE_TLS_VERSION and
# CMAKE_TLS_VERIFY environment variables, and are applied to git the way
# FetchContent applies them to its git clone.

cmake_minimum_required(VERSION 3.25)

foreach(_VAR REPOSITORY REVISION SOURCE_DIR)
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

# Check these before touching SOURCE_DIR. A count that isn't a number would
# never end the retry loop.
if(NOT RETRY_COUNT MATCHES "^[0-9]+$")
   message(FATAL_ERROR "RETRY_COUNT must be a whole number of retries, not '${RETRY_COUNT}'")
endif()
if(NOT RETRY_DELAY MATCHES "^([0-9]+(\\.[0-9]*)?|\\.[0-9]+)$")
   message(FATAL_ERROR "RETRY_DELAY must be a number of seconds, not '${RETRY_DELAY}'")
endif()

# The caller passes git along when it finds it, but doesn't require it: a
# build that supplies a dependency's sources with FETCHCONTENT_SOURCE_DIR_<NAME>
# never runs this script, and so needn't have git installed.
if(NOT GIT_EXECUTABLE)
   # Search into another variable: find_program() won't search when the
   # result variable was defined on the command line, even as empty.
   find_program(_FOUND_GIT git)
   if(NOT _FOUND_GIT)
      message(FATAL_ERROR "git is required to fetch ${REPOSITORY}")
   endif()
   set(GIT_EXECUTABLE "${_FOUND_GIT}")
endif()

# Variables that would point git at another repository, such as the ones
# git sets for its hooks, must not redirect the commands below.
foreach(_VAR
   GIT_DIR GIT_WORK_TREE GIT_INDEX_FILE GIT_COMMON_DIR GIT_OBJECT_DIRECTORY
   GIT_ALTERNATE_OBJECT_DIRECTORIES GIT_QUARANTINE_PATH)
   unset(ENV{${_VAR}})
endforeach()

# Options given to every git command
set(GIT_OPTIONS)
if("${TLS_VERSION}" STREQUAL "")
   set(TLS_VERSION "$ENV{CMAKE_TLS_VERSION}")
endif()
if(NOT "${TLS_VERSION}" STREQUAL "")
   # Git ignores a version it doesn't know, with only a warning, so reject
   # one here as ExternalProject does.
   if(NOT TLS_VERSION MATCHES "^1\\.[0-3]$")
      message(FATAL_ERROR "TLS_VERSION must be one of 1.0, 1.1, 1.2 or 1.3, not '${TLS_VERSION}'")
   endif()
   list(APPEND GIT_OPTIONS -c "http.sslVersion=tlsv${TLS_VERSION}")
endif()
if("${TLS_VERIFY}" STREQUAL "")
   set(TLS_VERIFY "$ENV{CMAKE_TLS_VERIFY}")
endif()
if(NOT "${TLS_VERIFY}" STREQUAL "")
   if(TLS_VERIFY)
      list(APPEND GIT_OPTIONS -c http.sslVerify=true)
   else()
      list(APPEND GIT_OPTIONS -c http.sslVerify=false)
   endif()
endif()

# Create the repository with the object format the revision is written in,
# whatever the user's default is. Versions of git that predate SHA-256
# ignore GIT_DEFAULT_HASH, and only create SHA-1 repositories anyway.
string(LENGTH "${REVISION}" _REVISION_LENGTH)
if(_REVISION_LENGTH EQUAL 64)
   set(ENV{GIT_DEFAULT_HASH} sha256)
else()
   set(ENV{GIT_DEFAULT_HASH} sha1)
endif()


# Runs git in SOURCE_DIR, and stops if it fails.
function(run_git)
   execute_process(
      COMMAND "${GIT_EXECUTABLE}" ${GIT_OPTIONS} ${ARGN}
      WORKING_DIRECTORY "${SOURCE_DIR}"
      RESULT_VARIABLE _RESULT)

   if(NOT _RESULT EQUAL 0)
      list(JOIN ARGN " " _COMMAND)
      message(FATAL_ERROR "'git ${_COMMAND}' failed in ${SOURCE_DIR}")
   endif()
endfunction()


# Runs git in SOURCE_DIR for commands that use the network: a failed attempt
# is retried RETRY_COUNT times, RETRY_DELAY seconds apart. Sets OUT_VAR to
# whether one of the attempts succeeded.
function(try_git_with_retries OUT_VAR)
   list(JOIN ARGN " " _COMMAND)

   set(_ATTEMPT 0)
   while(TRUE)
      execute_process(
         COMMAND "${GIT_EXECUTABLE}" ${GIT_OPTIONS} ${ARGN}
         WORKING_DIRECTORY "${SOURCE_DIR}"
         RESULT_VARIABLE _RESULT)
      if(_RESULT EQUAL 0)
         set(${OUT_VAR} TRUE PARENT_SCOPE)
         return()
      endif()

      math(EXPR _ATTEMPT "${_ATTEMPT} + 1")
      if(_ATTEMPT GREATER RETRY_COUNT)
         set(${OUT_VAR} FALSE PARENT_SCOPE)
         return()
      endif()

      message(STATUS "'git ${_COMMAND}' failed for ${REPOSITORY}; retrying in ${RETRY_DELAY} seconds")
      execute_process(COMMAND "${CMAKE_COMMAND}" -E sleep "${RETRY_DELAY}")
   endwhile()
endfunction()


# Like try_git_with_retries(), but stops if every attempt fails.
function(run_git_with_retries)
   try_git_with_retries(_SUCCEEDED ${ARGN})
   if(NOT _SUCCEEDED)
      list(JOIN ARGN " " _COMMAND)
      message(FATAL_ERROR "'git ${_COMMAND}' failed for ${REPOSITORY}")
   endif()
endfunction()


# Start from an empty directory, as FetchContent's git clone does, so files
# from an earlier revision or a partial fetch can't survive.
file(REMOVE_RECURSE "${SOURCE_DIR}")
if(EXISTS "${SOURCE_DIR}")
   # e.g. a file another process holds open on Windows
   message(FATAL_ERROR "Could not remove the previous checkout in ${SOURCE_DIR}")
endif()
file(MAKE_DIRECTORY "${SOURCE_DIR}")

run_git(init --quiet)
run_git(remote add origin "${REPOSITORY}")
run_git_with_retries(fetch --quiet --depth 1 --no-tags origin "${REVISION}")
run_git(checkout --quiet --detach FETCH_HEAD)

# FetchContent initializes every submodule, recursively; keep doing that, but
# shallowly. A shallow update fetches a submodule commit that isn't a branch
# tip by its id, which some servers refuse; FetchContent fetched submodules'
# full history, so fall back to that.
if(EXISTS "${SOURCE_DIR}/.gitmodules")
   try_git_with_retries(_SHALLOW_SUBMODULES submodule --quiet update --init --recursive --depth 1)
   if(NOT _SHALLOW_SUBMODULES)
      message(STATUS "Shallow submodule update failed for ${REPOSITORY}; fetching their full history")

      # Start the submodules over: an update would otherwise reuse their
      # shallow repositories in .git/modules.
      run_git(submodule --quiet deinit --all --force)
      file(REMOVE_RECURSE "${SOURCE_DIR}/.git/modules")
      run_git_with_retries(submodule --quiet update --init --recursive)
   endif()
endif()
