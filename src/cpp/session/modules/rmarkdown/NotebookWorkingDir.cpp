/*
 * NotebookWorkingDir.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#include "NotebookWorkingDir.hpp"

#include <r/RExec.hpp>

#include <core/FilePath.hpp>

#include <session/SessionSourceDatabase.hpp>
#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

DirCapture::DirCapture():
   warned_(false)
{
}


DirCapture::~DirCapture()
{
}

Error DirCapture::connectDir(const std::string& docId)
{
   // reset working directory to doc path, if it has one
   std::string docPath;
   source_database::getPath(docId, &docPath);
   if (!docPath.empty())
   {
      // save directory we're changing to (so we can detect changes)
      workingDir_ = module_context::resolveAliasedPath(docPath).parent();
      FilePath currentDir = FilePath::safeCurrentPath(workingDir_);
      if (currentDir != workingDir_)
      {
         Error error = FilePath::makeCurrent(workingDir_.absolutePath());
         if (error)
            return error;
      }
      prevWorkingDir_ = currentDir.absolutePath();
   }

   NotebookCapture::connect();

   return Success();
}

void DirCapture::disconnect()
{
   // restore working directory, if we saved one
   if (connected() && !prevWorkingDir_.empty())
   {
      Error error = FilePath::makeCurrent(prevWorkingDir_);
      if (error)
         LOG_ERROR(error);
   }
   NotebookCapture::disconnect();
}

void DirCapture::onExprComplete()
{
   if (!warned_ && !workingDir_.empty())
   {
      // raise a warning when changing a working directory inside the notebook
      // (this leads to unexpected behavior)
      FilePath currentDir = FilePath::safeCurrentPath(workingDir_);
      if (currentDir != workingDir_)
      {
         r::exec::warning("The working directory was changed to " + 
               currentDir.absolutePath() + " inside a notebook chunk. The "
               "working directory will be reset when the chunk is finished "
               "running.");
         
         // don't show warning more than once per chunk
         warned_ = true;
      }
   }
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio


