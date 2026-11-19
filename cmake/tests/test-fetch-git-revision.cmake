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
# cannot check out (#19714). The pinned commit also records a submodule,
# with a submodule of its own, pinned the same way.
#
# Usage:
#   cmake [-DWORK_DIR=<dir>] -P cmake/tests/test-fetch-git-revision.cmake
#
# The test works in a new test-fetch-git-revision-<random> folder inside
# WORK_DIR (by default, the system's temporary directory), and removes it
# when every test passes.

cmake_minimum_required(VERSION 3.25)

get_filename_component(
   FETCH_SCRIPT
   "${CMAKE_CURRENT_LIST_DIR}/../fetch-git-revision.cmake"
   ABSOLUTE)

find_program(GIT_EXECUTABLE git REQUIRED)

# Not the current directory: a failed run leaves its repositories behind,
# which inside a checkout git would pick up as embedded repositories.
if("${WORK_DIR}" STREQUAL "")
   foreach(VAR TMPDIR TEMP TMP)
      if(NOT "$ENV{${VAR}}" STREQUAL "")
         file(TO_CMAKE_PATH "$ENV{${VAR}}" WORK_DIR)
         break()
      endif()
   endforeach()
endif()
if("${WORK_DIR}" STREQUAL "")
   set(WORK_DIR "/tmp")
endif()
get_filename_component(
   WORK_DIR
   "${WORK_DIR}"
   ABSOLUTE
   BASE_DIR "${CMAKE_CURRENT_BINARY_DIR}")
string(RANDOM LENGTH 8 ALPHABET "0123456789abcdef" TEST_SUFFIX)
set(TEST_DIR "${WORK_DIR}/test-fetch-git-revision-${TEST_SUFFIX}")
if(EXISTS "${TEST_DIR}")
   message(FATAL_ERROR "${TEST_DIR} already exists")
endif()
file(MAKE_DIRECTORY "${TEST_DIR}")
message(STATUS "Testing in ${TEST_DIR}")

# Keep the user's git config (commit signing, hooks, protocol version) out of
# the test, including on versions of git that predate GIT_CONFIG_GLOBAL.
# Allow file:// submodules, which git blocks by default. The local servers
# otherwise keep git's defaults, which send an unadvertised commit by id
# over protocol version 2 but not over version 0.
set(TEST_HOME "${TEST_DIR}/home")
file(WRITE "${TEST_HOME}/.gitconfig" [=[
[user]
   name = test
   email = test@example.com
[protocol]
   version = 2
[protocol "file"]
   allow = always
[core]
   longpaths = true
]=])
set(ENV{HOME} "${TEST_HOME}")
set(ENV{XDG_CONFIG_HOME} "${TEST_HOME}/.config")
set(ENV{GIT_CONFIG_GLOBAL} "${TEST_HOME}/.gitconfig")
set(ENV{GIT_CONFIG_NOSYSTEM} 1)

# Don't let variables git sets for its hooks send the fixture's commits to
# another repository.
foreach(VAR
   GIT_DIR GIT_WORK_TREE GIT_INDEX_FILE GIT_COMMON_DIR GIT_OBJECT_DIRECTORY
   GIT_ALTERNATE_OBJECT_DIRECTORIES GIT_QUARANTINE_PATH GIT_DEFAULT_HASH
   GIT_CONFIG GIT_CONFIG_PARAMETERS GIT_CONFIG_COUNT)
   unset(ENV{${VAR}})
endforeach()


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


# Runs fetch-git-revision.cmake, and stores its exit status in OUT_VAR and
# its output in OUT_VAR_OUTPUT. Further arguments are passed to the script,
# after (and so overriding) the defaults here.
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
         ${ARGN}
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


# Stops unless the fetch() that stored its status in STATUS_VAR failed with
# output matching PATTERN.
function(expect_fetch_failed STATUS_VAR PATTERN WHAT)
   if("${${STATUS_VAR}}" STREQUAL "0")
      message(FATAL_ERROR "${WHAT} succeeded")
   endif()
   if(NOT "${${STATUS_VAR}_OUTPUT}" MATCHES "${PATTERN}")
      message(FATAL_ERROR "${WHAT} did not report '${PATTERN}':\n${${STATUS_VAR}_OUTPUT}")
   endif()
endfunction()


function(expect_marker DIR EXPECTED)
   file(READ "${DIR}/marker.txt" _MARKER)
   expect_equal("${_MARKER}" "${EXPECTED}" "${DIR}/marker.txt")
endfunction()


# Stops if the repository at DIR has COMMIT.
function(expect_missing_commit DIR COMMIT WHAT)
   execute_process(
      COMMAND "${GIT_EXECUTABLE}" cat-file -e "${COMMIT}^{commit}"
      WORKING_DIRECTORY "${DIR}"
      RESULT_VARIABLE _RESULT
      OUTPUT_QUIET
      ERROR_QUIET)

   if(_RESULT EQUAL 0)
      message(FATAL_ERROR "${WHAT} (${COMMIT}) was downloaded to ${DIR}")
   endif()
endfunction()


# The submodule's own submodule: base, then the commit its parent pins, then
# a newer tip.
set(LEAF_DIR "${TEST_DIR}/leaf")
file(MAKE_DIRECTORY "${LEAF_DIR}")
run_git("${LEAF_DIR}" _IGNORED init --quiet)
commit_marker("${LEAF_DIR}" "base" LEAF_BASE)
commit_marker("${LEAF_DIR}" "pinned" LEAF_PINNED)
commit_marker("${LEAF_DIR}" "tip" _IGNORED)
file_url("${LEAF_DIR}" LEAF_URL)

# The submodule, shaped the same way. Its pinned commit records the leaf
# at the leaf's pinned commit.
set(SUB_DIR "${TEST_DIR}/sub")
file(MAKE_DIRECTORY "${SUB_DIR}")
run_git("${SUB_DIR}" _IGNORED init --quiet)
commit_marker("${SUB_DIR}" "base" SUB_BASE)
run_git("${SUB_DIR}" _IGNORED submodule --quiet add "${LEAF_URL}" leaf)
run_git("${SUB_DIR}/leaf" _IGNORED checkout --quiet "${LEAF_PINNED}")
commit_marker("${SUB_DIR}" "pinned" SUB_PINNED)
commit_marker("${SUB_DIR}" "tip" _IGNORED)
file_url("${SUB_DIR}" SUB_URL)

# The dependency itself, shaped the same way. Its pinned commit records the
# submodule at the submodule's pinned commit.
set(DEP_DIR "${TEST_DIR}/dep")
file(MAKE_DIRECTORY "${DEP_DIR}")
run_git("${DEP_DIR}" _IGNORED init --quiet)
commit_marker("${DEP_DIR}" "base" DEP_BASE)
run_git("${DEP_DIR}" _IGNORED submodule --quiet add "${SUB_URL}" sub)
run_git("${DEP_DIR}/sub" _IGNORED checkout --quiet "${SUB_PINNED}")
commit_marker("${DEP_DIR}" "pinned" DEP_PINNED)
commit_marker("${DEP_DIR}" "tip" DEP_TIP)
file_url("${DEP_DIR}" DEP_URL)

set(SOURCE_DIR "${TEST_DIR}/src")


# A commit that is neither a tag nor a branch tip, with its submodule.
fetch("${DEP_URL}" "${DEP_PINNED}" "${SOURCE_DIR}" RESULT)
expect_fetched(RESULT "fetching the pinned commit")

run_git("${SOURCE_DIR}" HEAD rev-parse HEAD)
expect_equal("${HEAD}" "${DEP_PINNED}" "checked-out commit")
expect_marker("${SOURCE_DIR}" "pinned")
expect_marker("${SOURCE_DIR}/sub" "pinned")
expect_marker("${SOURCE_DIR}/sub/leaf" "pinned")

# Only the pinned commit was downloaded: not its parent, nor the branch tip.
# Submodules are shallow, without the history before their pinned commits.
expect_missing_commit("${SOURCE_DIR}" "${DEP_BASE}" "the pinned commit's parent")
expect_missing_commit("${SOURCE_DIR}" "${DEP_TIP}" "the branch tip")
expect_missing_commit("${SOURCE_DIR}/sub" "${SUB_BASE}" "the submodule's pinned commit's parent")
expect_missing_commit("${SOURCE_DIR}/sub/leaf" "${LEAF_BASE}" "the nested submodule's pinned commit's parent")


# A user whose git creates SHA-256 repositories by default can still fetch
# a SHA-1 commit.
set(ENV{GIT_DEFAULT_HASH} sha256)
fetch("${DEP_URL}" "${DEP_PINNED}" "${SOURCE_DIR}" RESULT)
unset(ENV{GIT_DEFAULT_HASH})
expect_fetched(RESULT "fetching with GIT_DEFAULT_HASH=sha256")
expect_marker("${SOURCE_DIR}" "pinned")


# Moving the pin replaces the existing checkout.
fetch("${DEP_URL}" "${DEP_TIP}" "${SOURCE_DIR}" RESULT)
expect_fetched(RESULT "fetching a new pin over an old checkout")
expect_marker("${SOURCE_DIR}" "tip")


# Without GIT_EXECUTABLE, the script finds git itself. (A fractional retry
# delay and valid TLS settings are accepted too.)
fetch(
   "${DEP_URL}" "${DEP_PINNED}" "${SOURCE_DIR}" RESULT
   -DGIT_EXECUTABLE=
   -DRETRY_DELAY=.5
   -DTLS_VERSION=1.2
   -DTLS_VERIFY=ON)
expect_fetched(RESULT "fetching without GIT_EXECUTABLE")
expect_marker("${SOURCE_DIR}" "pinned")


# A retry count that isn't a number is rejected, and the checkout is kept.
fetch("${DEP_URL}" "${DEP_TIP}" "${SOURCE_DIR}" RESULT -DRETRY_COUNT=OFF)
expect_fetch_failed(RESULT "RETRY_COUNT must be a whole number" "fetching with RETRY_COUNT=OFF")
expect_marker("${SOURCE_DIR}" "pinned")

# So are a retry delay that isn't a number, and a TLS version that git would
# ignore.
fetch("${DEP_URL}" "${DEP_TIP}" "${SOURCE_DIR}" RESULT -DRETRY_DELAY=OFF)
expect_fetch_failed(RESULT "RETRY_DELAY must be a number" "fetching with RETRY_DELAY=OFF")
expect_marker("${SOURCE_DIR}" "pinned")

fetch("${DEP_URL}" "${DEP_TIP}" "${SOURCE_DIR}" RESULT -DTLS_VERSION=1.4)
expect_fetch_failed(RESULT "TLS_VERSION must be one of" "fetching with TLS_VERSION=1.4")
expect_marker("${SOURCE_DIR}" "pinned")


# A submodule whose server won't send its pinned commit by id is fetched
# with its full history instead, without retrying the shallow update. Over
# protocol version 0, only the dependency's server is set to send a commit
# by id, as GitHub's does.
run_git("${DEP_DIR}" _IGNORED config uploadpack.allowReachableSHA1InWant true)
run_git("${TEST_DIR}" _IGNORED config --global protocol.version 0)
fetch("${DEP_URL}" "${DEP_PINNED}" "${TEST_DIR}/src-full-submodules" RESULT)
run_git("${TEST_DIR}" _IGNORED config --global protocol.version 2)
run_git("${DEP_DIR}" _IGNORED config --unset uploadpack.allowReachableSHA1InWant)
expect_fetched(RESULT "fetching a submodule from a strict server")
if(NOT RESULT_OUTPUT MATCHES "Shallow submodule update failed")
   message(FATAL_ERROR "the shallow submodule update did not fall back:\n${RESULT_OUTPUT}")
endif()
if(RESULT_OUTPUT MATCHES "retrying")
   message(FATAL_ERROR "the shallow submodule update was retried:\n${RESULT_OUTPUT}")
endif()
expect_marker("${TEST_DIR}/src-full-submodules/sub" "pinned")
expect_marker("${TEST_DIR}/src-full-submodules/sub/leaf" "pinned")


# A submodule that can't be fetched fails the fetch, rather than leaving a
# checkout without it.
file(RENAME "${LEAF_DIR}" "${LEAF_DIR}-moved")
fetch("${DEP_URL}" "${DEP_PINNED}" "${TEST_DIR}/src-no-leaf" RESULT)
file(RENAME "${LEAF_DIR}-moved" "${LEAF_DIR}")
expect_fetch_failed(RESULT "submodule" "fetching with an unavailable nested submodule")


# A SHA-256 commit, for a user whose git creates SHA-1 repositories, if
# this git can create SHA-256 repositories at all.
set(SHA256_DIR "${TEST_DIR}/sha256")
file(MAKE_DIRECTORY "${SHA256_DIR}")
execute_process(
   COMMAND "${GIT_EXECUTABLE}" init --quiet --object-format=sha256
   WORKING_DIRECTORY "${SHA256_DIR}"
   RESULT_VARIABLE SHA256_SUPPORTED
   OUTPUT_QUIET
   ERROR_QUIET)
if(SHA256_SUPPORTED EQUAL 0)
   commit_marker("${SHA256_DIR}" "pinned" SHA256_PINNED)
   commit_marker("${SHA256_DIR}" "tip" _IGNORED)
   file_url("${SHA256_DIR}" SHA256_URL)

   set(ENV{GIT_DEFAULT_HASH} sha1)
   fetch("${SHA256_URL}" "${SHA256_PINNED}" "${TEST_DIR}/src-sha256" RESULT)
   unset(ENV{GIT_DEFAULT_HASH})
   expect_fetched(RESULT "fetching a SHA-256 commit")
   run_git("${TEST_DIR}/src-sha256" HEAD rev-parse HEAD)
   expect_equal("${HEAD}" "${SHA256_PINNED}" "checked-out SHA-256 commit")
   expect_marker("${TEST_DIR}/src-sha256" "pinned")
else()
   message(STATUS "This git can't create SHA-256 repositories; skipping that test")
endif()


# A commit the server doesn't have fails, after its retries.
fetch("${DEP_URL}" "0123456789abcdef0123456789abcdef01234567" "${SOURCE_DIR}" RESULT)
expect_fetch_failed(RESULT "retrying in 0 seconds" "fetching a missing commit")


file(REMOVE_RECURSE "${TEST_DIR}")
message(STATUS "fetch-git-revision.cmake: all tests passed")
