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

#include <iostream>

#include <boost/foreach.hpp>
#include <boost/format.hpp>

#include <r/RJson.hpp>
#include <r/RExec.hpp>

#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/Algorithm.hpp>
#include <core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/text/CsvParser.hpp>
#include <core/StringUtils.hpp>
#include <core/system/System.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionSourceDatabase.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionUserSettings.hpp>

#define kChunkConsoleInput  0
#define kChunkConsoleOutput 1
#define kChunkConsoleError  3

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

namespace {

// the ID of the doc / chunk currently executing console commands (if any)
std::string s_consoleChunkId, s_consoleDocId, s_activeConsole;

// whether we're currently connected to console events
bool s_consoleConnected = false;

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
   result["path"] = docPath;
   result["request_id"] = requestId;
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

void disconnectConsole();

void onConsolePrompt(const std::string& )
{
   if (s_consoleConnected)
      disconnectConsole();
}

void onConsoleText(const std::string& docId, const std::string& chunkId,
                   int type, const std::string& output, bool truncate)
{
   if (output.empty())
      return;

   FilePath outputCsv = chunkOutputFile(docId, chunkId, kChunkOutputText);

   std::vector<std::string> vals; 
   vals.push_back(safe_convert::numberToString(type));
   vals.push_back(output);
   Error error = core::writeStringToFile(outputCsv, 
         text::encodeCsvLine(vals) + "\n", 
         string_utils::LineEndingPassthrough, truncate);
   if (error)
   {
      LOG_ERROR(error);
   }

   events().onChunkConsoleOutput(docId, chunkId, type, output);
}

void onConsoleOutput(module_context::ConsoleOutputType type, 
      const std::string& output)
{
   if (type == module_context::ConsoleOutputNormal)
      onConsoleText(s_consoleDocId, s_consoleChunkId, kChunkConsoleOutput, 
                    output, false);
   else
      onConsoleText(s_consoleDocId, s_consoleChunkId, kChunkConsoleError, 
                    output, false);
}

void onConsoleInput(const std::string& input)
{
   onConsoleText(s_consoleDocId, s_consoleChunkId, kChunkConsoleInput, input, 
         false);
}

bool moveLibFile(const FilePath& from, const FilePath& to, 
      const FilePath& path, std::vector<FilePath> *pPaths)
{
   std::string relativePath = path.relativePath(from);
   FilePath target = to.complete(relativePath);

   pPaths->push_back(path);

   Error error = path.isDirectory() ?
                     target.ensureDirectory() :
                     path.move(target);
   if (error)
      LOG_ERROR(error);
   return true;
}

void writeLibDeps(const std::string& docId, const std::string& chunkId,
                  int ordinal, const std::vector<FilePath>& paths)
{
   // TODO: save dependency information to JSON
}

void onFileOutput(const FilePath& file, int outputType)
{
   OutputPair pair = lastChunkOutput(s_consoleDocId, s_consoleChunkId);
   pair.ordinal++;
   pair.outputType = outputType;
   FilePath target = chunkOutputFile(s_consoleDocId, s_consoleChunkId, pair);
   Error error = file.move(target);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // check to see if the file has an accompanying library folder; if so, move
   // it to the global library folder
   FilePath fileLib = file.parent().complete(kChunkLibDir);
   if (fileLib.exists())
   {
      std::vector<FilePath> paths;
      error = fileLib.childrenRecursive(boost::bind(moveLibFile, fileLib,
            chunkCacheFolder(s_consoleDocId, s_consoleChunkId)
                           .complete(kChunkLibDir), _2, &paths));
      if (error)
         LOG_ERROR(error);
      writeLibDeps(s_consoleDocId, s_consoleChunkId, pair.ordinal, paths);
      error = fileLib.remove();
      if (error)
         LOG_ERROR(error);
   }
   
   enqueueChunkOutput(s_consoleDocId, s_consoleChunkId, outputType, target);
   updateLastChunkOutput(s_consoleDocId, s_consoleChunkId, pair);
}

void disconnectConsole()
{
   module_context::events().onConsolePrompt.disconnect(onConsolePrompt);
   module_context::events().onConsoleOutput.disconnect(onConsoleOutput);
   module_context::events().onConsoleInput.disconnect(onConsoleInput);
   
   events().onPlotOutput.disconnect(
         boost::bind(onFileOutput, _1, kChunkOutputPlot));
   events().onHtmlOutput.disconnect(
         boost::bind(onFileOutput, _1, kChunkOutputHtml));

   s_consoleConnected = false;
}

void connectConsole()
{
   FilePath outputPath = chunkOutputPath(s_consoleDocId, s_consoleChunkId);
   Error error = outputPath.ensureDirectory();
   if (error)
   {
      // if we don't have a place to put the output, don't register any handlers
      // (will end in tears)
      LOG_ERROR(error);
      return;
   }

   // begin capturing console text
   module_context::events().onConsolePrompt.connect(onConsolePrompt);
   module_context::events().onConsoleOutput.connect(onConsoleOutput);
   module_context::events().onConsoleInput.connect(onConsoleInput);

   // begin capturing plots and HTML output
   events().onPlotOutput.connect(
         boost::bind(onFileOutput, _1, kChunkOutputPlot));
   events().onHtmlOutput.connect(
         boost::bind(onFileOutput, _1, kChunkOutputHtml));

   error = beginPlotCapture(outputPath);
   if (error)
      LOG_ERROR(error);

   error = beginWidgetCapture(outputPath, 
         outputPath.parent().complete(kChunkLibDir));
   if (error)
      LOG_ERROR(error);

   s_consoleConnected = true;
}

void onActiveConsoleChanged(const std::string& consoleId, 
                            const std::string& text)
{
   s_activeConsole = consoleId;
   if (consoleId == s_consoleChunkId)
   {
      if (s_consoleConnected) 
         return;
      connectConsole();
      onConsoleText(s_consoleDocId, s_consoleChunkId, kChunkConsoleInput, 
            text, false);
   }
   else if (s_consoleConnected)
   {
      // some other console is connected; disconnect ours
      disconnectConsole();
   }
}

void onChunkExecCompleted(const std::string& docId, 
                          const std::string& chunkId,
                          const std::string& contextId)
{
   // attempt to get the path of the doc (this may fail if the document does
   // not yet exist)
   std::string path;
   source_database::getPath(docId, &path);

   Error error = enqueueChunkOutput(path, docId, chunkId, contextId);
   if (error)
      LOG_ERROR(error);
}

// called by the client to set the active chunk console
Error setChunkConsole(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse*)
{

   std::string docId, chunkId;
   bool replace = false;
   Error error = json::readParams(request.params, &docId, &chunkId, &replace);
   if (error)
      return error;

   if (replace)
      cleanChunkOutput(docId, chunkId, true);

   s_consoleChunkId = chunkId;
   s_consoleDocId = docId;
   if (s_activeConsole == chunkId)
      connectConsole();

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

   module_context::events().onActiveConsoleChanged.connect(
         onActiveConsoleChanged);

   events().onChunkExecCompleted.connect(onChunkExecCompleted);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "refresh_chunk_output", refreshChunkOutput))
      (bind(registerRpcMethod, "set_chunk_console", setChunkConsole))
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

