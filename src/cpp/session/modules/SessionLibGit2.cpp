/*
 * SessionLibGit2.cpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
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

#include <shared_core/Error.hpp>
#include <core/Log.hpp>

#include <git2.h>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace libgit2 {

Error initialize()
{
   git_libgit2_init();
   return Success();
}

Git::Git()
   : pRepo_(nullptr)
{
}

Git::~Git()
{
   if (pRepo_ != nullptr)
      git_repository_free(pRepo_);
}

Error Git::open(const FilePath& repoPath)
{
   int rc = git_repository_open(&pRepo_, repoPath.getAbsolutePath().c_str());
   if (rc != 0)
   {
      // not a git repo -- this is not an error condition
      pRepo_ = nullptr;
      LOG_DEBUG_MESSAGE("Git: path is not a git repository: " +
                        repoPath.getAbsolutePath());
   }

   return Success();
}

bool Git::isIgnored(const std::string& path) const
{
   if (pRepo_ == nullptr)
      return false;

   int ignored = 0;
   int rc = git_ignore_path_is_ignored(&ignored, pRepo_, path.c_str());
   if (rc != 0)
      return false;

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
