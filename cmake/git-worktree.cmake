# git-worktree.cmake
#
# Resolves the primary git worktree (the main checkout) so that a build run from
# a secondary `git worktree` can locate downloaded dependencies. Those deps
# (node, gwtproject, quarto, etc.) are gitignored, so they are only ever
# populated in the primary checkout -- a freshly added worktree has empty
# dependency directories. Resolving the primary worktree lets the build borrow
# them instead of failing or requiring a manual re-install/symlink. This mirrors
# the ${git.main.worktree} resolution already done in src/gwt/build.xml.
#
# Sets RSTUDIO_GIT_MAIN_WORKTREE to the primary worktree root, or leaves it
# empty when not in a git checkout (or git is unavailable) -- callers must
# tolerate an empty value and fall back to their existing behavior.

if(DEFINED RSTUDIO_GIT_MAIN_WORKTREE)
   return()
endif()

set(RSTUDIO_GIT_MAIN_WORKTREE "" CACHE INTERNAL "Primary git worktree root (source of shared, gitignored dependencies)")

find_program(RSTUDIO_GIT_EXECUTABLE git)
if(RSTUDIO_GIT_EXECUTABLE)
   # --git-common-dir resolves to the *primary* checkout's .git directory even
   # from a linked worktree; its parent is that checkout's working tree.
   execute_process(
      COMMAND "${RSTUDIO_GIT_EXECUTABLE}" rev-parse --path-format=absolute --git-common-dir
      WORKING_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}"
      OUTPUT_VARIABLE _rstudio_git_common_dir
      OUTPUT_STRIP_TRAILING_WHITESPACE
      ERROR_QUIET
      RESULT_VARIABLE _rstudio_git_result)

   if(_rstudio_git_result EQUAL 0 AND _rstudio_git_common_dir)
      get_filename_component(_rstudio_main_worktree "${_rstudio_git_common_dir}" DIRECTORY)
      set(RSTUDIO_GIT_MAIN_WORKTREE "${_rstudio_main_worktree}" CACHE INTERNAL "Primary git worktree root (source of shared, gitignored dependencies)" FORCE)
      message(STATUS "Primary git worktree: ${RSTUDIO_GIT_MAIN_WORKTREE}")
   endif()
endif()
