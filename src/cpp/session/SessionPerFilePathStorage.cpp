/*
 * SessionPerFilePathStorage.cpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/FileSerializer.hpp>
#include <core/FileUtils.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace module_context {

Error perFilePathStorage(const std::string& scope, const FilePath& filePath, bool directory, FilePath* pStorage)
{
   // url escape path (so we can use key=value persistence)
   std::string escapedPath = http::util::urlEncode(filePath.getAbsolutePath());

   // read index
   FilePath dbPath = module_context::scopedScratchPath().completePath(scope);
   Error error = dbPath.ensureDirectory();
   if (error)
      return error;
   std::map<std::string,std::string> index;
   FilePath indexFile = dbPath.completePath("INDEX");
   if (indexFile.exists())
   {
      error = readStringMapFromFile(indexFile, &index);
      if (error)
         return error;
   }

   // use existing storage if it exists, otherwise create new
   std::string storage = index[escapedPath];
   if (storage.empty())
   {
      // create storage
      FilePath storageFile = file_utils::uniqueFilePath(dbPath);
      if (directory)
      {
         error = storageFile.ensureDirectory();
         if (error)
            return error;
      }
      else
      {
         error = core::writeStringToFile(storageFile, "");
         if (error)
            return error;
      }

      // update storage
      storage = storageFile.getFilename();

      //  update index
      index[escapedPath] = storage;
      Error error = writeStringMapToFile(indexFile, index);
      if (error)
         return error;
   }

   // return the path
   *pStorage = dbPath.completeChildPath(storage);
   return Success();
}


} // namespace module_context
} // namespace session
} // namespace rstudio
