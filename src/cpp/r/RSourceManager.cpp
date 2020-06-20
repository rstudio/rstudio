/*
 * RSourceManager.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include <r/RSourceManager.hpp>

#include <algorithm>

#include <boost/bind.hpp>
#include <boost/algorithm/string/replace.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/Log.hpp>

#include <r/RExec.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace r {
   
   
SourceManager& sourceManager()
{
   static SourceManager instance;
   return instance;
}
   
Error SourceManager::sourceTools(const core::FilePath& filePath)
{
   Error error = sourceLocal(filePath);
   if (error)
      return error;

   toolsFilePaths_.push_back(filePath);

   return Success();
}


Error SourceManager::sourceLocal(const FilePath& filePath)
{
   return source(filePath, true);
}

void SourceManager::ensureToolsLoaded()
{
   // check whether tools:rstudio is still in the search patch
   bool toolsRStudioLoaded = true;
   Error error = r::exec::evaluateString("\"tools:rstudio\" %in% search()",
                                         &toolsRStudioLoaded);
   if (error)
      LOG_ERROR(error);

   // if not then source all of the tools files back in
   if (!toolsRStudioLoaded)
   {
      std::for_each(toolsFilePaths_.begin(),
                    toolsFilePaths_.end(),
                    boost::bind(&SourceManager::reSourceTools, this, _1));
   }
}
   
void SourceManager::reloadIfNecessary()
{
   if (autoReload_)
   {
      std::for_each(sourcedFiles_.begin(), 
                    sourcedFiles_.end(), 
                    boost::bind(&SourceManager::reloadSourceIfNecessary,
                                this,
                                _1));
   }
}


void SourceManager::reSourceTools(const core::FilePath& filePath)
{
   Error error = source(filePath, true);
   if (error)
      LOG_ERROR(error);
}
   
Error SourceManager::source(const FilePath& filePath, bool local)
{
   std::string localPrefix = local ? "local(" : "";
   std::string localParam = local ? "TRUE" : "FALSE";
   std::string localSuffix = local ? ")" : "";
      
   // do \ escaping (for windows)
   std::string path = filePath.getAbsolutePath();
   boost::algorithm::replace_all(path, "\\", "\\\\");

   // Build the code. If this build is targeted for debugging, keep the source
   // code around; otherwise, turn it off to conserve memory and expose fewer
   // internals to the user.
   std::string rCode = localPrefix + "source(\"" 
                           + path + "\", " +
                           "local=" + localParam + ", " + 
                           "echo=FALSE, " +
                           "verbose=FALSE, " + 
#ifdef NDEBUG
                           "keep.source=FALSE, " +
#else
                           "keep.source=TRUE, " +
#endif
                           "encoding='UTF-8')" + localSuffix;
      
   // record that we sourced the file. 
   recordSourcedFile(filePath, local);

   // source the file
   return r::exec::executeString(rCode);
}

void SourceManager::recordSourcedFile(const FilePath& filePath, bool local)
{
   SourcedFileInfo fileInfo(filePath.getLastWriteTime(), local);
   sourcedFiles_[filePath.getAbsolutePath()] = fileInfo;
}
   
void SourceManager::reloadSourceIfNecessary(
                                    const SourcedFileMap::value_type& value)
{
   // extract values
   FilePath sourcedFilePath(value.first);
   SourcedFileInfo fileInfo = value.second;
   
   // compare last write times and source again if necessary
   double diffTime = std::difftime(sourcedFilePath.getLastWriteTime(),
                                   fileInfo.lastWriteTime);
   if (diffTime > 0)
   {
      Error error = source(sourcedFilePath, fileInfo.local);
      if (error)
         LOG_ERROR(error);
   }
}
   
} // namespace r
} // namespace rstudio



