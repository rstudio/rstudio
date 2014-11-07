/*
 * Definitions.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "Definitions.hpp"


#include <core/libclang/LibClang.hpp>

#include <session/IncrementalFileChangeHandler.hpp>

#include <session/projects/SessionProjects.hpp>


#include "RSourceIndex.hpp"

using namespace core;
using namespace core::libclang;

namespace session {
namespace modules { 
namespace clang {
namespace definitions {

namespace {

bool isTranslationUnit(const FileInfo& fileInfo)
{
   return SourceIndex::isTranslationUnit(fileInfo.absolutePath());
}

void fileChangeHandler(const core::system::FileChangeEvent& event)
{

}

} // anonymous namespace


Error initialize()
{
   using namespace projects;
   if (projectContext().config().buildType == r_util::kBuildTypePackage)
   {
      // create an incremental file change handler (on the heap so that it
      // survives the call to this function and is never deleted)
      IncrementalFileChangeHandler* pFileChangeHandler =
        new IncrementalFileChangeHandler(isTranslationUnit,
                                         fileChangeHandler,
                                         boost::posix_time::milliseconds(1000),
                                         boost::posix_time::milliseconds(500),
                                         true);
      pFileChangeHandler->subscribeToFileMonitor("Go to C/C++ Definition");
   }

   return Success();
}

} // namespace definitions
} // namespace clang
} // namespace modules
} // namesapce session

