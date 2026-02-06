/*
 * SessionLibGit2.hpp
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

#ifndef SESSION_LIB_GIT2_HPP
#define SESSION_LIB_GIT2_HPP

#include <string>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

typedef struct git_repository git_repository;

namespace rstudio {
namespace session {
namespace modules {
namespace libgit2 {

core::Error initialize();

class Git
{
public:
   Git();
   ~Git();

   Git(const Git&) = delete;
   Git& operator=(const Git&) = delete;

   // Open a git repository at the given path.
   // Returns success even if the path is not a git repo (isOpen() will be false).
   core::Error open(const core::FilePath& repoPath);

   // Check if a path (relative to repo root) is ignored by .gitignore rules.
   bool isIgnored(const std::string& path) const;

   // Whether a repository was successfully opened.
   bool isOpen() const;

private:
   git_repository* pRepo_;
};

} // namespace libgit2
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_LIB_GIT2_HPP
