/*
 * NotebookOutput.cpp
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

#include "NotebookCache.hpp"
#include "NotebookOutput.hpp"

#include <boost/foreach.hpp>
#include <boost/format.hpp>
#include <boost/algorithm/string.hpp>

#include <core/Algorithm.hpp>
#include <core/Exec.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/SafeConvert.hpp>
#include <session/SessionUserSettings.hpp>
#include <core/StringUtils.hpp>
#include <core/json/Json.hpp>
#include <core/text/CsvParser.hpp>

#include <session/SessionSourceDatabase.hpp>
#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>

#include <map>

#define kChunkOutputPath   "chunk_output"
#define kChunkOutputType   "output_type"
#define kChunkOutputValue  "output_val"
#define kChunkOutputs      "chunk_outputs"
#define kChunkUrl          "url"
#define kChunkId           "chunk_id"
#define kChunkDocId        "doc_id"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

namespace {

typedef std::map<std::string, OutputPair> LastChunkOutput;
LastChunkOutput s_lastChunkOutputs;

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

   // the chunks all share one library folder, so redirect requests for a 
   // chunk-specific folder to the shared folder
   if (parts.size() > 2 &&
       parts[1] == kChunkLibDir) 
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

core::Error cleanChunkOutput(const std::string& docId,
      const std::string& chunkId, bool preserveFolder)
{
   FilePath outputPath = chunkOutputPath(docId, chunkId);
   if (!outputPath.exists())
      return Success();

   Error error = outputPath.remove();
   if (error)
      return error;
   if (preserveFolder)
   {
      error = outputPath.ensureDirectory();
      if (error)
         return error;
   }
   updateLastChunkOutput(docId, chunkId, OutputPair());
   return Success();
}

Error initOutput()
{
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(module_context::registerUriHandler, "/" kChunkOutputPath, 
            handleChunkOutputRequest));
   return initBlock.execute();
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio
