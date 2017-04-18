/*
 * RSourceIndex.cpp
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

#include "RSourceIndex.hpp"

#include <boost/bind.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/FileInfo.hpp>
#include <core/FilePath.hpp>

#include <session/SessionUserSettings.hpp>

#include <core/libclang/LibClang.hpp>

#include "RCompilationDatabase.hpp"

using namespace rstudio::core ;
using namespace rstudio::core::libclang;

namespace rstudio {
namespace session {
namespace modules { 
namespace clang {

namespace {

class RSourceIndex : public SourceIndex
{
public:
   RSourceIndex()
      : SourceIndex(rCompilationDatabase(), userSettings().clangVerbose())
   {
   }
};

// store as a pointer so that it's never destructrued during shutdown
// (we observed at least one instance of libclang crashing when calling
// clang_disposeTranslationUnit during shutdown)
RSourceIndex* s_pRSourceIndex = NULL;

} // anonymous namespace

SourceIndex& rSourceIndex()
{
   if (s_pRSourceIndex == NULL)
      s_pRSourceIndex = new RSourceIndex();
   return *s_pRSourceIndex;
}

bool isIndexableFile(const FileInfo& fileInfo,
                     const FilePath& pkgSrcDir,
                     const FilePath& pkgIncludeDir)
{
   FilePath filePath(fileInfo.absolutePath());

   if (pkgSrcDir.exists() &&
       filePath.isWithin(pkgSrcDir) &&
       SourceIndex::isSourceFile(filePath) &&
       !boost::algorithm::starts_with(filePath.stem(), kCompilationDbPrefix) &&
       (filePath.filename() != "RcppExports.cpp"))
   {
      return true;
   }
   else if (pkgIncludeDir.exists() &&
            filePath.isWithin(pkgIncludeDir) &&
            SourceIndex::isSourceFile(filePath) &&
            !boost::algorithm::ends_with(filePath.stem(), "_RcppExports"))
   {
      return true;
   }
   else
   {
      return false;
   }
}

} // namespace clang
} // namespace modules
} // namespace session
} // namespace rstudio

