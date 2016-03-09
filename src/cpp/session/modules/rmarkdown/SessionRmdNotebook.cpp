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
#include "SessionRnbParser.hpp"
#include "NotebookPlots.hpp"
#include "NotebookCache.hpp"
#include "NotebookChunkDefs.hpp"
#include "NotebookOutput.hpp"
#include "NotebookHtmlWidgets.hpp"
#include "NotebookExec.hpp"

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
#include <session/SessionSourceDatabase.hpp>
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

// the currently active console ID
std::string s_activeConsole;

void replayChunkOutputs(const std::string& docPath, const std::string& docId,
      const std::string& requestId, const json::Array& chunkOutputs) 
{
   std::vector<std::string> chunkIds;
   extractChunkIds(chunkOutputs, &chunkIds);

   // find all the chunks and play them back to the client
   BOOST_FOREACH(const std::string& chunkId, chunkIds)
   {
      enqueueChunkOutput(docPath, docId, chunkId, userSettings().contextId());
   }

   json::Object result;
   result["doc_id"] = docId;
   result["request_id"] = requestId;
   result["chunk_id"] = "";
   result["type"] = kFinishedReplay;
   ClientEvent event(client_events::kChunkOutputFinished, result);
   module_context::enqueClientEvent(event);
}

// called by the client to inject output into a recently opened document 
Error refreshChunkOutput(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   // extract path to doc to be refreshed
   std::string docPath, docId, contextId, requestId;
   Error error = json::readParams(request.params, &docPath, &docId, &contextId,
         &requestId);
   if (error)
      return error;

   // use our own context ID if none supplied
   if (contextId.empty())
      contextId = userSettings().contextId();

   json::Object result;
   json::Value chunkDefs; 
   error = getChunkDefs(docPath, docId, contextId, NULL, &chunkDefs);

   // schedule the work to play back the chunks
   if (!error && chunkDefs.type() == json::ArrayType) 
   {
      pResponse->setAfterResponse(
            boost::bind(replayChunkOutputs, docPath, docId, requestId, 
                        chunkDefs.get_array()));
   }

   return Success();
}

void emitOutputFinished(const std::string& docId, const std::string& chunkId)
{
   json::Object result;
   result["doc_id"] = docId;
   result["request_id"] = "";
   result["chunk_id"] = chunkId;
   result["type"] = kFinishedInteractive;
   ClientEvent event(client_events::kChunkOutputFinished, result);
   module_context::enqueClientEvent(event);
}

void onActiveConsoleChanged(boost::shared_ptr<ChunkExecContext> execContext,
                            const std::string& consoleId, 
                            const std::string& text)
{
   s_activeConsole = consoleId;
   if (!execContext)
      return;

   if (consoleId == execContext->chunkId())
   {
      if (execContext->consoleConnected()) 
         return;
      execContext->connect();
   }
   else if (execContext->consoleConnected())
   {
      execContext->disconnect();
   }
}

void onChunkExecCompleted(boost::shared_ptr<ChunkExecContext> execContext,
                          const std::string& docId, 
                          const std::string& chunkId,
                          const std::string& contextId)
{
   // if this event belongs to the current execution context, destroy it
   if (execContext &&
       execContext->docId() == docId &&
       execContext->chunkId() == chunkId)
   {
      execContext.reset();
   }

   emitOutputFinished(docId, chunkId);
}

// called by the client to set the active chunk console
Error setChunkConsole(boost::shared_ptr<ChunkExecContext> execContext,
                      const json::JsonRpcRequest& request,
                      json::JsonRpcResponse*)
{

   std::string docId, chunkId;
   bool replace = false;
   Error error = json::readParams(request.params, &docId, &chunkId, &replace);
   if (error)
      return error;

   cleanChunkOutput(docId, chunkId, true);

   // create the execution context and connect it immediately if necessary
   execContext.reset(new ChunkExecContext(docId, chunkId));
   if (s_activeConsole == chunkId)
      execContext->connect();

   return Success();
}

} // anonymous namespace

Events& events()
{
   static Events instance;
   return instance;
}

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   // the current execution context, if any
   boost::shared_ptr<ChunkExecContext> pExecContext;

   module_context::events().onActiveConsoleChanged.connect(
         bind(onActiveConsoleChanged, pExecContext, _1, _2));

   events().onChunkExecCompleted.connect(boost::bind(
            onChunkExecCompleted, pExecContext, _1, _2, _3));

   json::JsonRpcFunction setChunkConsoleCtx = bind(
         setChunkConsole, pExecContext, _1, _2);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "refresh_chunk_output", refreshChunkOutput))
      (bind(registerRpcMethod, "set_chunk_console", setChunkConsoleCtx))
      (bind(module_context::sourceModuleRFile, "SessionRmdNotebook.R"))
      (bind(initOutput))
      (bind(initCache))
      (bind(initHtmlWidgets));

   return initBlock.execute();
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

