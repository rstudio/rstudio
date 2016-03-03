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

#define kChunkDocId        "doc_id"
#define kChunkId           "chunk_id"
#define kChunkOutputPath   "chunk_output"
#define kChunkOutputType   "output_type"
#define kChunkOutputValue  "output_val"
#define kChunkOutputs      "chunk_outputs"
#define kChunkUrl          "url"
#define kChunkConsole      "console"

#define kChunkConsoleInput  0
#define kChunkConsoleOutput 1
#define kChunkConsoleError  3

#define kChunkOutputNone 0
#define kChunkOutputText 1
#define kChunkOutputPlot 2
#define kChunkOutputHtml 3

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

// for performance, we cache the ordinal and output type associated with each 
// chunk
struct OutputPair
{
   OutputPair() :
      outputType(kChunkOutputNone), ordinal(1)
   {}
   OutputPair(unsigned type, unsigned ord):
      outputType(type), ordinal(ord) 
   {}
   unsigned outputType;
   unsigned ordinal;
};
typedef std::map<std::string, OutputPair> LastChunkOutput;
LastChunkOutput s_lastChunkOutputs;

// A notebook .Rmd is accompanied by a sidecar .Rnb.cached folder, which has
// the following structure:
//
// - foo.Rmd
// + .foo.Rnd.cached
//   - chunks.json
//   - cwiaiw9i4f0.html
//   + cwiaiw9i4f0_files
//     - plot.png
//   - c0aj9vhk0cz.html
//   - cjz0958jgzh.csv
//   + lib
//     + htmlwidgets
//       - htmlwidget.js
// 
// That is:
// - each chunk has an ID and is represented by one of the following:
//   - an HTML file (.html) with accompanying dependencies indicating
//     chunk output, or 
//   - a CSV file (.csv) indicating console output
// - the special file "chunks.json" indicates the location of the chunks
//   in the source .Rmd
// - the special folder "lib" is used for shared libraries (e.g. scripts upon
//   which several htmlwidget chunks depend)


FilePath chunkOutputPath(
      const std::string& docPath, const std::string& docId,
      const std::string& chunkId, const std::string& contextId)

{
   return chunkCacheFolder(docPath, docId, contextId).childPath(chunkId);
}

FilePath chunkOutputPath(const std::string& docId, const std::string& chunkId)
{
   std::string docPath;
   Error error = source_database::getPath(docId, &docPath);
   if (error)
   {
      LOG_ERROR(error);
      return FilePath();
   }

   return chunkOutputPath(docPath, docId, chunkId, userSettings().contextId());
}

Error chunkConsoleContents(const FilePath& consoleFile, json::Array* pArray)
{
   std::string contents;
   Error error = readStringFromFile(consoleFile, &contents);
   if (error)
      return error;

   // parse each line of the CSV file
   std::pair<std::vector<std::string>, std::string::iterator> line;
   line = text::parseCsvLine(contents.begin(), contents.end());
   while (!line.first.empty())
   {
      if (line.first.size() > 1)
      {
         json::Array output;
         output.push_back(safe_convert::stringTo<int>(line.first[0], 
               module_context::ConsoleOutputNormal));
         output.push_back(line.first[1]);
         pArray->push_back(output);
      }
      // read next line
      line = text::parseCsvLine(line.second, contents.end());
   }

   return Success();
}

unsigned chunkOutputType(const FilePath& outputPath)
{
   int outputType = kChunkOutputNone;
   if (outputPath.extensionLowerCase() == ".csv")
      outputType = kChunkOutputText;
   else if (outputPath.extensionLowerCase() == ".png")
      outputType = kChunkOutputPlot;
   else if (outputPath.extensionLowerCase() == ".html")
      outputType = kChunkOutputHtml;
   return outputType;
}

std::string chunkOutputExt(unsigned outputType)
{
   switch(outputType)
   {
      case kChunkOutputText:
         return ".csv";
      case kChunkOutputPlot:
         return ".png";
      case kChunkOutputHtml:
         return ".html";
   }
   return "";
}

void updateLastChunkOutput(const std::string& docId, 
                           const std::string& chunkId,
                           const OutputPair& pair)
{
   s_lastChunkOutputs[docId + chunkId] = pair;
}

// given a document ID and a chunk ID, discover the last output the chunk had
OutputPair lastChunkOutput(const std::string& docId, 
                           const std::string& chunkId)
{
   // check our cache 
   LastChunkOutput::iterator it = s_lastChunkOutputs.find(docId + chunkId);
   if (it != s_lastChunkOutputs.end())
      return it->second;
   
   std::string docPath;
   source_database::getPath(docId, &docPath);
   FilePath outputPath = chunkOutputPath(docPath, docId, chunkId, 
         userSettings().contextId());

   // scan the directory for output
   std::vector<FilePath> outputPaths;
   Error error = outputPath.children(&outputPaths);
   if (error)
   {
      LOG_ERROR(error);
      return OutputPair();
   }

   OutputPair last;
   BOOST_FOREACH(const FilePath& path, outputPaths)
   {
      // extract ordinal and update if it's the most recent we've seen so far
      unsigned ordinal = static_cast<unsigned>(
            ::strtoul(path.stem().c_str(), NULL, 16));
      if (ordinal > last.ordinal)
      {
         last.ordinal = ordinal;
         last.outputType = chunkOutputType(path);
      }
   }

   // cache for future calls
   updateLastChunkOutput(docId, chunkId, last);
   return last;
}


FilePath chunkOutputFile(const std::string& docId, 
                         const std::string& chunkId, 
                         const OutputPair& output)
{
   return chunkOutputPath(docId, chunkId).complete(
         (boost::format("%|1$05x|%2%") 
                     % output.ordinal 
                     % chunkOutputExt(output.outputType)).str());
}

FilePath chunkOutputFile(const std::string& docId, 
                         const std::string& chunkId, 
                         unsigned outputType)
{
   OutputPair output = lastChunkOutput(docId, chunkId);
   if (output.outputType == outputType)
      return chunkOutputFile(docId, chunkId, output);
   output.ordinal++;
   output.outputType = outputType;
   updateLastChunkOutput(docId, chunkId, output);
   return chunkOutputFile(docId, chunkId, output);
}

void enqueueChunkOutput(const std::string& docId,
      const std::string& chunkId, int outputType, 
      const FilePath& path)
{
   json::Object output;
   output[kChunkOutputType]  = outputType;
   output[kChunkOutputValue] = kChunkOutputPath "/" + docId + "/" +
      chunkId + "/" + path.filename();

   json::Object result;
   result[kChunkId]         = chunkId;
   result[kChunkDocId]      = docId;
   result[kChunkOutputPath] = output;
   ClientEvent event(client_events::kChunkOutput, result);
   module_context::enqueClientEvent(event);
}

Error enqueueChunkOutput(
      const std::string& docPath, const std::string& docId,
      const std::string& chunkId, const std::string& contextId)
{
   FilePath outputPath = chunkOutputPath(docPath, docId, chunkId, contextId);

   // scan the directory for output
   std::vector<FilePath> outputPaths;
   Error error = outputPath.children(&outputPaths);

   // non-fatal: if we can't list we'll safely return an empty array
   if (error) 
      LOG_ERROR(error);

   // arrange by filename (use FilePath's < operator)
   std::sort(outputPaths.begin(), outputPaths.end());

   // loop through each and build an array of the outputs
   json::Array outputs;
   BOOST_FOREACH(const FilePath& outputPath, outputPaths)
   {
      json::Object output;

      // ascertain chunk output type from file extension; skip if extension 
      // unknown
      int outputType = chunkOutputType(outputPath);
      if (outputType == kChunkOutputNone)
         continue;

      // format/parse chunk output for client consumption
      output[kChunkOutputType] = outputType;
      if (outputType == kChunkOutputText)
      {
         json::Array consoleOutput;
         error = chunkConsoleContents(outputPath, &consoleOutput);
         output[kChunkOutputValue] = consoleOutput;
      }
      else if (outputType == kChunkOutputPlot ||
               outputType == kChunkOutputHtml)
      {
         output[kChunkOutputValue] = kChunkOutputPath "/" + docId + "/" +
            chunkId + "/" + outputPath.filename();
      }

      outputs.push_back(output);
   }
   
   // note that if we find that this chunk has no output we can display, we
   // should still send it to the client, which will clean it up correctly, and
   // omit it in its next set of updated chunk definitions
   json::Object result;
   result[kChunkId]      = chunkId;
   result[kChunkDocId]   = docId;
   result[kChunkOutputs] = outputs;
   ClientEvent event(client_events::kChunkOutput, result);
   module_context::enqueClientEvent(event);

   return Success();
}

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

void onPlotOutput(const FilePath& plot)
{
   OutputPair pair = lastChunkOutput(s_consoleDocId, s_consoleChunkId);
   pair.ordinal++;
   pair.outputType = kChunkOutputPlot;
   FilePath target = chunkOutputFile(s_consoleDocId, s_consoleChunkId, pair);
   Error error = plot.move(target);
   if (error)
   {
      LOG_ERROR(error);
   }
   else
   {
      enqueueChunkOutput(s_consoleDocId, s_consoleChunkId, kChunkOutputPlot,
            target);
      updateLastChunkOutput(s_consoleDocId, s_consoleChunkId, pair);
   }
}

void disconnectConsole()
{
   module_context::events().onConsolePrompt.disconnect(onConsolePrompt);
   module_context::events().onConsoleOutput.disconnect(onConsoleOutput);
   module_context::events().onConsoleInput.disconnect(onConsoleInput);
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

   module_context::events().onConsolePrompt.connect(onConsolePrompt);
   module_context::events().onConsoleOutput.connect(onConsoleOutput);
   module_context::events().onConsoleInput.connect(onConsoleInput);

   error = beginPlotCapture(outputPath, onPlotOutput);
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

Error handleChunkOutputRequest(const http::Request& request,
                               http::Response* pResponse)
{
   // uri format is: /chunk_output/<doc-id>/...
   
   // split URI into pieces, extract the document ID, and remove that part of
   // the URI
   std::vector<std::string> parts = algorithm::split(request.uri(), "/");
   if (parts.size() < 4) 
      return Success();
   std::string docId = parts[2];
   for (int i = 0; i < 3; i++)
      parts.erase(parts.begin());

   // attempt to get the path -- ignore failure (doc may be unsaved and
   // therefore won't have a path)
   std::string path;
   source_database::getPath(docId, &path);

   FilePath target = chunkCacheFolder(path, docId).complete(
         algorithm::join(parts, "/"));

   // ensure the target exists 
   if (!target.exists())
   {
      pResponse->setNotFoundError(request.uri());
      return Success();
   }

   if (parts[0] == kChunkLibDir)
   {
      // if a reference to the chunk library folder, we can reuse the contents
      // (let the browser cache the file)
      pResponse->setCacheableFile(target, request);
   }
   else
   {
      // otherwise, use ETag cache 
      pResponse->setCacheableBody(target, request);
   }

   return Success();
}

// called by the client to set the active chunk console
Error setChunkConsole(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse*)
{

   std::string docId, chunkId;
   Error error = json::readParams(request.params, &docId, &chunkId);
   if (error)
      return error;

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
      (bind(registerUriHandler, "/" kChunkOutputPath, 
            handleChunkOutputRequest))
      (bind(module_context::sourceModuleRFile, "SessionRmdNotebook.R"))
      (bind(initCache));

   return initBlock.execute();
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

