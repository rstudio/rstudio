/*
 * SessionProjectFirstRun.cpp
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

#include "SessionProjectFirstRun.hpp"

#include <boost/foreach.hpp>

#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>

#include <session/SessionModuleContext.hpp>

#include "SessionProjectsInternal.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace projects {

namespace {

const char* const kFirstRunDocs = "first_run_docs";

} // anonymous namespace

void addFirstRunDoc(const FilePath& projectFile, const std::string& doc)
{
   FilePath scratchPath;
   Error error = computeScratchPaths(projectFile, &scratchPath, NULL);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   std::ostringstream ostr;
   ostr << doc << std::endl;
   error = core::appendToFile(scratchPath.childPath(kFirstRunDocs), ostr.str());
   if (error)
      LOG_ERROR(error);
}

std::vector<std::string> collectFirstRunDocs(const FilePath& projectFile)
{
   // docs to return
   std::vector<std::string> docs;

   // get the scratch path
   FilePath scratchPath;
   Error error = computeScratchPaths(projectFile, &scratchPath, NULL);
   if (error)
   {
      LOG_ERROR(error);
      return docs;
   }

   // check for first run file
   FilePath firstRunDocsPath = scratchPath.childPath(kFirstRunDocs);
   if (firstRunDocsPath.exists())
   {
      Error error = core::readStringVectorFromFile(firstRunDocsPath, &docs);
      if (error)
         LOG_ERROR(error);

      // remove since this is a one-time only thing
      firstRunDocsPath.remove();
   }

   return docs;
}

} // namespace projects
} // namespace session
} // namespace rstudio

