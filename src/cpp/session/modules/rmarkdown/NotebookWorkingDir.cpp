/*
 * NotebookWorkingDir.cpp
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

#include "SessionRmdNotebook.hpp"
#include "NotebookWorkingDir.hpp"

#include <r/RExec.hpp>

#include <shared_core/FilePath.hpp>

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

Error DirCapture::connectDir(const std::string& docId, 
                             const core::FilePath& workingDir)
{
   if (workingDir.exists())
   {
      // prefer manually specified working directory
      workingDir_ = workingDir;
   }
   else
   {
      // no manually specified dir; use working directory to doc path, if it
      // has one
      std::string docPath;
      source_database::getPath(docId, &docPath);
      if (!docPath.empty())
      {
         workingDir_ = module_context::resolveAliasedPath(docPath).getParent();
      }
   }

   if (!workingDir_.isEmpty())
   {
      // if we have a working directory, switch to it, and save directory we're
      // changing from (so we can detect changes)
      FilePath currentDir = FilePath::safeCurrentPath(workingDir_);
      if (currentDir != workingDir_)
      {
         Error error = FilePath::makeCurrent(workingDir_.getAbsolutePath());
         if (error)
            return error;
      }
      prevWorkingDir_ = currentDir.getAbsolutePath();
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
   if (!warned_ && !workingDir_.isEmpty())
   {
      // raise a warning when changing a working directory inside the notebook
      // (this leads to unexpected behavior)
      FilePath currentDir = FilePath::safeCurrentPath(workingDir_);
      if (!currentDir.isEquivalentTo(workingDir_))
      {
         r::exec::warning("The working directory was changed to " +
                             currentDir.getAbsolutePath() + " inside a notebook chunk. The "
               "working directory will be reset when the chunk is finished "
               "running. Use the knitr root.dir option in the setup chunk " 
               "to change the working directory for notebook chunks.\n");
         
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


