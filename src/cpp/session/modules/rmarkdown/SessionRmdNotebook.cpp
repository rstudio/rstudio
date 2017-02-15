/*
 * SessionRmdNotebook.cpp
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

#include "SessionRmdNotebook.hpp"
#include "NotebookPlots.hpp"
#include "NotebookPlotReplay.hpp"
#include "NotebookCache.hpp"
#include "NotebookChunkDefs.hpp"
#include "NotebookData.hpp"
#include "NotebookOutput.hpp"
#include "NotebookHtmlWidgets.hpp"
#include "NotebookExec.hpp"
#include "NotebookErrors.hpp"
#include "NotebookQueue.hpp"
#include "NotebookAlternateEngines.hpp"
#include "NotebookConditions.hpp"

#include <iostream>

#include <boost/foreach.hpp>
#include <boost/format.hpp>

#include <r/RJson.hpp>
#include <r/RExec.hpp>

#include <core/Exec.hpp>
#include <core/Algorithm.hpp>
#include <core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/StringUtils.hpp>
#include <core/system/System.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionUserSettings.hpp>

#define kFinishedReplay      0
#define kFinishedInteractive 1

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

namespace {

// the currently active console ID and chunk execution context
std::string s_activeConsole;
boost::shared_ptr<ChunkExecContext> s_execContext;

void replayChunkOutputs(const std::string& docPath, const std::string& docId,
      const std::string& requestId, const std::string& singleChunkId,
      const json::Array& chunkOutputs) 
{
   std::vector<std::string> chunkIds;
   extractChunkIds(chunkOutputs, &chunkIds);

   if (singleChunkId.empty())
   {
      // find all the chunks and play them back to the client
      BOOST_FOREACH(const std::string& chunkId, chunkIds)
      {
         enqueueChunkOutput(docPath, docId, chunkId, notebookCtxId(), requestId);
      }
   }
   else
   {
      // play back a specific chunk
      enqueueChunkOutput(docPath, docId, singleChunkId, notebookCtxId(), 
            requestId);
   }

   json::Object result;
   result["doc_id"] = docId;
   result["request_id"] = requestId;
   result["chunk_id"] = singleChunkId.empty() ? "" : singleChunkId;
   result["type"] = kFinishedReplay;
   ClientEvent event(client_events::kChunkOutputFinished, result);
   module_context::enqueClientEvent(event);
}

// called by the client to inject output into a recently opened document 
Error refreshChunkOutput(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   // extract path to doc to be refreshed
   std::string docPath, docId, nbCtxId, requestId, chunkId;
   Error error = json::readParams(request.params, &docPath, &docId, &nbCtxId,
         &requestId, &chunkId);
   if (error)
      return error;

   json::Object result;
   json::Array chunkDefs; 

   // use our own context ID if none supplied
   if (nbCtxId.empty())
      error = getChunkValue(docPath, docId, kChunkDefs, &chunkDefs);
   else
      error = getChunkValue(docPath, docId, nbCtxId, kChunkDefs, &chunkDefs);

   // schedule the work to play back the chunks
   if (!error) 
   {
      pResponse->setAfterResponse(
            boost::bind(replayChunkOutputs, docPath, docId, requestId, chunkId, 
                        chunkDefs));
   }

   // send back the execution queue, if any
   pResponse->setResult(getDocQueue(docId));

   return Success();
}

void emitOutputFinished(const std::string& docId, const std::string& chunkId,
      int scope)
{
   json::Object result;
   result["doc_id"]     = docId;
   result["request_id"] = "";
   result["chunk_id"]   = chunkId;
   result["type"]       = kFinishedInteractive;
   result["scope"]      = scope;
   ClientEvent event(client_events::kChunkOutputFinished, result);
   module_context::enqueClientEvent(event);
}

bool fixChunkFilename(int, const core::FilePath& path)
{
   std::string name = path.filename();
   if (name.empty())
      return true;
   
   // replace spaces at start of name with '0'
   std::string transformed = name;
   for (std::size_t i = 0, n = transformed.size(); i < n; ++i)
   {
      if (transformed[i] != ' ')
         break;
      
      transformed[i] = '0';
   }
   
   // rename file if we had to change it
   if (transformed != name)
   {
      FilePath target = path.parent().childPath(transformed);
      Error error = path.move(target);
      if (error)
         LOG_ERROR(error);
   }
   
   // return true to continue traversal
   return true;
}

void onChunkExecCompleted(const std::string& docId, 
                          const std::string& chunkId,
                          const std::string& nbCtxId)
{
   emitOutputFinished(docId, chunkId, ExecScopeChunk);
}

void onDeferredInit(bool)
{
   FilePath root = notebookCacheRoot();
   root.ensureDirectory();
   
   // Fix up chunk entries in the cache that were generated
   // with leading spaces on Windows
   FilePath patchPath = root.complete("patch-chunk-names");
   if (!patchPath.exists())
   {
      patchPath.ensureFile();
      Error error = root.childrenRecursive(fixChunkFilename);
      if (error)
         LOG_ERROR(error);
   }
}

} // anonymous namespace

Events& events()
{
   static Events instance;
   return instance;
}

// a notebook context is scoped to both a user and a session (which are only
// guaranteed unique per user); it must be unique since there are currently no
// concurrency mechanisms in place to guard multi-session writes to the file.
// the notebook context ID may be shared with other users/sessions for read 
// access during collaborative editing, but only a notebook context's own 
// session should write to it.
std::string notebookCtxId()
{
   return userSettings().contextId() + module_context::activeSession().id();
}

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   events().onChunkExecCompleted.connect(onChunkExecCompleted);
   
   module_context::events().onDeferredInit.connect(onDeferredInit);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "refresh_chunk_output", refreshChunkOutput))
      (bind(module_context::sourceModuleRFile, "SessionRmdNotebook.R"))
      (bind(initOutput))
      (bind(initCache))
      (bind(initData))
      (bind(initHtmlWidgets))
      (bind(initErrors))
      (bind(initPlots))
      (bind(initPlotReplay))
      (bind(initQueue))
      (bind(initAlternateEngines))
      (bind(initChunkDefs))
      (bind(initConditions));

   return initBlock.execute();
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

