/*
 * NotebookCacheRenderer.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#include "NotebookCacheRenderer.hpp"

#include "NotebookCache.hpp"
#include "NotebookChunkDefs.hpp"

#include <core/Log.hpp>
#include <core/StringUtils.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/system/Environment.hpp>

#include <shared_core/FilePath.hpp>
#include <shared_core/json/Json.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>
#include <session/worker_safe/session/SessionClientEvent.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

using namespace rstudio::core;

// static member
boost::weak_ptr<NotebookCacheRenderer> NotebookCacheRenderer::s_running_;

NotebookCacheRenderer::NotebookCacheRenderer(const std::string& docId,
                                             const std::string& docPath,
                                             const std::string& outputPath)
   : docId_(docId),
     docPath_(docPath),
     outputPath_(outputPath)
{
}

bool NotebookCacheRenderer::isRunning()
{
   boost::shared_ptr<NotebookCacheRenderer> instance = s_running_.lock();
   return instance && instance->async_r::AsyncRProcess::isRunning();
}

void NotebookCacheRenderer::terminateRunning()
{
   boost::shared_ptr<NotebookCacheRenderer> instance = s_running_.lock();
   if (instance)
   {
      instance->cancelled_ = true;
      instance->terminate();
   }
}

void NotebookCacheRenderer::render(const std::string& rmdPath,
                                   const std::string& cachePath,
                                   const std::string& outputPath,
                                   const std::string& docId,
                                   const std::string& docPath,
                                   const std::string& encoding)
{
   // cancel any in-progress render so we always render the latest save
   terminateRunning();

   // create the renderer
   boost::shared_ptr<NotebookCacheRenderer> pRenderer(
      new NotebookCacheRenderer(docId, docPath, outputPath));

   // R source files to load:
   // 1. Tools.R is sourced automatically via R_PROCESS_AUGMENTED
   // 2. SessionRmdNotebook.R provides the .rs.rnb.* rendering functions
   // 3. SessionNotebookRender.R overrides .Call()-based functions and defines
   //    helpers not available in the child process (must be sourced AFTER
   //    SessionRmdNotebook.R so our overrides take precedence)
   FilePath modulesPath = session::options().modulesRSourcePath();
   std::vector<FilePath> sourceFiles = {
      modulesPath.completePath("SessionRmdNotebook.R"),
      modulesPath.completePath("SessionNotebookRender.R")
   };

   // set up environment variables for the script
   core::system::Options environment;
   core::system::environment(&environment);
   core::system::setenv(&environment, "RS_NB_RMD_PATH", rmdPath);
   core::system::setenv(&environment, "RS_NB_CACHE_PATH", cachePath);
   core::system::setenv(&environment, "RS_NB_OUTPUT_PATH", outputPath);
   core::system::setenv(&environment, "RS_NB_ENCODING", encoding);

   // store weak reference before starting (the process holds itself alive
   // via shared_from_this, but we want isRunning() to work immediately)
   s_running_ = pRenderer;

   // start the async R process in augmented mode (sources Tools.R first)
   pRenderer->start(
      ".rs.renderNotebookAsync()",
      environment,
      FilePath(),
      async_r::R_PROCESS_VANILLA | async_r::R_PROCESS_AUGMENTED,
      sourceFiles);
}

void NotebookCacheRenderer::onStdout(const std::string& output)
{
   stdOut_ << output;
}

void NotebookCacheRenderer::onStderr(const std::string& output)
{
   stdErr_ << output;
}

void NotebookCacheRenderer::onCompleted(int exitStatus)
{
   // if we were intentionally cancelled (e.g. a newer save triggered a
   // new render), silently discard the result
   if (cancelled_)
      return;

   // build the result object
   json::Object result;
   result["doc_id"] = docId_;
   result["doc_path"] = docPath_;

   std::string output = stdOut_.str();
   bool succeeded = (exitStatus == 0) &&
                    (output.find("__RENDER_SUCCESS__") != std::string::npos);

   result["succeeded"] = succeeded;

   if (!succeeded)
   {
      // try to extract error from the __RENDER_ERROR__: prefix
      std::string errorMessage;
      std::string::size_type errorPos = output.find("__RENDER_ERROR__:");
      if (errorPos != std::string::npos)
      {
         std::string errJson = output.substr(errorPos + strlen("__RENDER_ERROR__:"));
         // trim to first newline
         std::string::size_type newline = errJson.find('\n');
         if (newline != std::string::npos)
            errJson = errJson.substr(0, newline);

         // try to parse the JSON error
         json::Value errValue;
         if (!errValue.parse(errJson))
            json::readObject(errValue.getObject(), "error", errorMessage);

         if (errorMessage.empty())
            errorMessage = string_utils::trimWhitespace(errJson);
      }

      if (errorMessage.empty())
         errorMessage = stdErr_.str();

      if (errorMessage.empty())
         errorMessage = "Notebook rendering failed";

      result["error_message"] = errorMessage;

      // write the error to the R console so the user sees it
      module_context::consoleWriteError(
         "Error creating notebook: " + errorMessage + "\n");
   }
   else
   {
      result["error_message"] = std::string();
   }

   // fire client event
   ClientEvent event(client_events::kNotebookRenderCompleted, result);
   module_context::enqueClientEvent(event);

   // bump chunk defs timestamp to match output file to prevent re-render
   // (only on success -- failed renders should not suppress future attempts)
   if (succeeded)
   {
      FilePath rmdFile = module_context::resolveAliasedPath(docPath_);
      FilePath chunkDefsFile = chunkDefinitionsPath(rmdFile, kSavedCtx);
      FilePath outputFile(outputPath_);

      if (chunkDefsFile.exists() && outputFile.exists() &&
          chunkDefsFile.getLastWriteTime() < outputFile.getLastWriteTime())
      {
         chunkDefsFile.setLastWriteTime(outputFile.getLastWriteTime());
      }
   }
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio
