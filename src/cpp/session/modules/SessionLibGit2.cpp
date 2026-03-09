/*
 * SessionLibGit2.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionLibGit2.hpp"

#include <git2.h>

#include <shared_core/Error.hpp>

#include <core/Log.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace libgit2 {

Error initialize()
{
   // NOTE: We do not call git_libgit2_shutdown(). The Git object's lifetime
   // is managed by shared_ptr in file monitor callbacks and may extend past
   // the shutdown signal. Calling git_repository_free() after
   // git_libgit2_shutdown() is undefined behavior per the libgit2 API.
   // Process exit reclaims all resources.
   int rc = git_libgit2_init();
   if (rc < 0)
   {
      const git_error* err = git_error_last();
      std::string msg = err ? err->message : "unknown error";
      LOG_ERROR_MESSAGE("git_libgit2_init() failed: " + msg);
      return systemError(boost::system::errc::io_error, msg, ERROR_LOCATION);
   }
   return Success();
}

Git::Git(const FilePath& repoPath)
   : pRepo_(nullptr)
{
   int rc = git_repository_open(&pRepo_, repoPath.getAbsolutePath().c_str());
   if (rc != 0)
   {
      pRepo_ = nullptr;
      if (rc == GIT_ENOTFOUND)
      {
         LOG_DEBUG_MESSAGE("Git: path is not a git repository: " +
                           repoPath.getAbsolutePath());
      }
      else
      {
         const git_error* err = git_error_last();
         std::string msg = err ? err->message : "unknown error";
         LOG_WARNING_MESSAGE("Git: failed to open repository at " +
                             repoPath.getAbsolutePath() + ": " + msg);
      }
   }
}

Git::~Git()
{
   if (pRepo_ != nullptr)
      git_repository_free(pRepo_);
}

bool Git::isIgnored(const std::string& path) const
{
   if (pRepo_ == nullptr)
      return false;

   int ignored = 0;
   int rc = git_ignore_path_is_ignored(&ignored, pRepo_, path.c_str());
   if (rc != 0)
   {
      const git_error* err = git_error_last();
      std::string msg = err ? err->message : "unknown error";
      LOG_DEBUG_MESSAGE("git_ignore_path_is_ignored() failed for '" + path + "': " + msg);
      return false;
   }

   return ignored != 0;
}

bool Git::isOpen() const
{
   return pRepo_ != nullptr;
}

} // namespace libgit2
} // namespace modules
} // namespace session
} // namespace rstudio
