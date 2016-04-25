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
#include "NotebookErrors.hpp"

#include <iostream>

#include <boost/foreach.hpp>
#include <boost/format.hpp>

#include <r/RJson.hpp>
#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <core/Exec.hpp>
#include <core/Algorithm.hpp>
#include <core/FileSerializer.hpp>
#include <core/StringUtils.hpp>
#include <core/RegexUtils.hpp>
#include <core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/system/System.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionUserSettings.hpp>

#define kFinishedReplay      0
#define kFinishedInteractive 1

// symmetric with client
#define kExecModeSingle 0
#define kExecModeBatch  1

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
      const std::string& requestId, const json::Array& chunkOutputs) 
{
   std::vector<std::string> chunkIds;
   extractChunkIds(chunkOutputs, &chunkIds);

   // find all the chunks and play them back to the client
   BOOST_FOREACH(const std::string& chunkId, chunkIds)
   {
      enqueueChunkOutput(docPath, docId, chunkId, requestId, notebookCtxId());
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
   std::string docPath, docId, nbCtxId, requestId;
   Error error = json::readParams(request.params, &docPath, &docId, &nbCtxId,
         &requestId);
   if (error)
      return error;

   // use our own context ID if none supplied
   if (nbCtxId.empty())
      nbCtxId = notebookCtxId();

   json::Object result;
   json::Value chunkDefs; 
   error = getChunkDefs(docPath, docId, nbCtxId, NULL, &chunkDefs);

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

void onActiveConsoleChanged(const std::string& consoleId, 
                            const std::string& text)
{
   s_activeConsole = consoleId;
   if (!s_execContext)
      return;

   if (consoleId == s_execContext->chunkId())
   {
      if (s_execContext->connected()) 
         return;
      s_execContext->connect();
      s_execContext->onConsoleInput(text);
   }
   else if (s_execContext->connected())
   {
      s_execContext->disconnect();
      s_execContext.reset();
   }
}

void onChunkExecCompleted(const std::string& docId, 
                          const std::string& chunkId,
                          const std::string& nbCtxId)
{
   emitOutputFinished(docId, chunkId);

   // if this event belonged to the current execution context, destroy it
   if (s_execContext &&
       s_execContext->docId() == docId &&
       s_execContext->chunkId() == chunkId)
   {
      s_execContext.reset();
   }
}

// called by the client to set the active chunk console
Error setChunkConsole(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   std::string docId, chunkId, options;
   int pixelWidth = 0, charWidth = 0, execMode = 0;
   bool replace = false;
   Error error = json::readParams(request.params, &docId, &chunkId, &execMode,
         &options, &pixelWidth, &charWidth, &replace);
   if (error)
      return error;

   // evaluate this chunk's options
   r::sexp::Protect protect;
   SEXP sexpOptions = R_NilValue;
   error = r::exec::RFunction(".rs.evaluateChunkOptions", options)
                             .call(&sexpOptions, &protect);
   if (error)
      return error;

   // convert to JSON for client
   json::Value jsonOptions;
   error = r::json::jsonValueFromList(sexpOptions, &jsonOptions);
   if (error)
      return error;
   pResponse->setResult(jsonOptions);

   // if this chunk is going to be evaluated in batch mode, and the options
   // indicate that it shouldn't be evaluated, don't
   // evaluate it
   if (execMode == kExecModeBatch &&
       jsonOptions.type() == json::ObjectType)
   {
      bool eval = true;
      error = json::readObject(jsonOptions.get_obj(), "eval", &eval);
      if (!error && !eval) 
      {
         return Success();
      }
   }

   cleanChunkOutput(docId, chunkId, true);

   // clean up the old execution context if we still have one
   if (s_execContext)
      s_execContext->disconnect();

   // create the execution context and connect it immediately if necessary
   s_execContext.reset(new ChunkExecContext(docId, chunkId, options, 
            pixelWidth, charWidth));
   if (s_activeConsole == chunkId)
      s_execContext->connect();

   return Success();
}

Error createNotebookFromCache(const json::JsonRpcRequest& request,
                              json::JsonRpcResponse* pResponse)
{
   std::string rmdPath, outputPath;
   Error error = json::readParams(request.params, &rmdPath, &outputPath);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   r::exec::RFunction createNotebook(".rs.createNotebookFromCache");
   createNotebook.addParam(rmdPath);
   createNotebook.addParam(outputPath);
   error = createNotebook.call();
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   return Success();
}

SEXP rs_parseNotebook(SEXP nbPathSEXP)
{
   FilePath nbPath(r::sexp::asString(nbPathSEXP));
   
   std::string contents;
   Error error = core::readStringFromFile(nbPath, &contents);
   if (error)
      LOG_ERROR(error);
   
   boost::regex reHtmlCommentStart("^\\s*<!--\\s*");
   boost::regex reHtmlCommentEnd("\\s*-->\\s*$");
   boost::smatch match;
   std::string activeChunkId = "unknown";
   
   std::cout << "Size: " << contents.size() << std::endl;
   std::istringstream iss(contents);
   std::string line;
   while (std::getline(iss, line))
   {
      if (!regex_utils::search(reHtmlCommentStart, line, &match))
         continue;
      
      std::string::const_iterator begin = match[0].second;
      if (!regex_utils::search(reHtmlCommentEnd, line, &match))
         continue;
      
      std::string::const_iterator end = match[0].first;
      
      std::string metadata(begin, end);
   }
   
   return R_NilValue;
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
   
   RS_REGISTER_CALL_METHOD(rs_parseNotebook, 1);

   module_context::events().onActiveConsoleChanged.connect(
         onActiveConsoleChanged);

   events().onChunkExecCompleted.connect(onChunkExecCompleted);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "refresh_chunk_output", refreshChunkOutput))
      (bind(registerRpcMethod, "set_chunk_console", setChunkConsole))
      (bind(registerRpcMethod, "create_notebook_from_cache", createNotebookFromCache))
      (bind(module_context::sourceModuleRFile, "SessionRmdNotebook.R"))
      (bind(initOutput))
      (bind(initCache))
      (bind(initHtmlWidgets))
      (bind(initErrors));

   return initBlock.execute();
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

