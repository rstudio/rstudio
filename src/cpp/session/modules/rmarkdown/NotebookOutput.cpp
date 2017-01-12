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

#include "SessionRmdNotebook.hpp"
#include "NotebookCache.hpp"
#include "NotebookOutput.hpp"
#include "NotebookPlots.hpp"

#include <boost/foreach.hpp>
#include <boost/format.hpp>
#include <boost/algorithm/string.hpp>

#include <core/Algorithm.hpp>
#include <core/Base64.hpp>
#include <core/Exec.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/SafeConvert.hpp>
#include <session/SessionUserSettings.hpp>
#include <core/StringUtils.hpp>
#include <core/json/Json.hpp>
#include <core/text/CsvParser.hpp>

#include <r/RSexp.hpp>
#include <r/RJson.hpp>
#include <r/RExec.hpp>

#include <session/SessionSourceDatabase.hpp>
#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>

#include <map>

#define kRequestId    "request_id"
#define kChunkOutputs "chunk_outputs"

#define MAX_ORDINAL        16777215
#define OUTPUT_THRESHOLD   25

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

namespace {

typedef std::map<std::string, OutputPair> LastChunkOutput;
LastChunkOutput s_lastChunkOutputs;

ChunkOutputType chunkOutputType(const FilePath& outputPath)
{
   ChunkOutputType outputType = ChunkOutputNone;
   std::string ext = outputPath.extensionLowerCase();
   if (ext == ".csv")
      outputType = ChunkOutputText;
   else if (ext == ".png" || ext == ".jpg" || ext == ".jpeg")
      outputType = ChunkOutputPlot;
   else if (ext == ".html")
      outputType = ChunkOutputHtml;
   else if (ext == ".error")
      outputType = ChunkOutputError;
   else if (outputPath.extensionLowerCase() == ".rdf")
      outputType = ChunkOutputData;
   return outputType;
}

std::string chunkOutputExt(ChunkOutputType outputType)
{
   switch(outputType)
   {
      case ChunkOutputText:
         return ".csv";
      case ChunkOutputPlot:
         return ".png";
      case ChunkOutputHtml:
         return ".html";
      case ChunkOutputError:
         return ".error";
      case ChunkOutputData:
         return ".rdf";
      default: 
         return "";
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
         int outputType = safe_convert::stringTo<int>(line.first[0], 
               kChunkConsoleOutput);

         // don't emit input data to the client
         if (outputType != kChunkConsoleInput)
         {
            json::Array output;
            output.push_back(outputType);
            output.push_back(line.first[1]);
            pArray->push_back(output);
         }
      }
      // read next line
      line = text::parseCsvLine(line.second, contents.end());
   }

   return Success();
}

Error fillOutputObject(const std::string& docId, const std::string& chunkId,
      const std::string& nbCtxId, unsigned ordinal, ChunkOutputType outputType, 
      const FilePath& path, json::Object* pObj)
{
   (*pObj)[kChunkOutputType]    = static_cast<int>(outputType);
   (*pObj)[kChunkOutputOrdinal] = static_cast<int>(ordinal);

   if (outputType == ChunkOutputError)
   {
      // error outputs can be directly read from the file
      std::string fileContents;
      json::Value errorVal;
      Error error = core::readStringFromFile(path, &fileContents);
      if (error)
         return error;
      if (!json::parse(fileContents, &errorVal))
         return Error(json::errc::ParseError, ERROR_LOCATION);

     (*pObj)[kChunkOutputValue] = errorVal;
   } 
   else if (outputType == ChunkOutputText)
   {
      // deserialize console output
      json::Array consoleOutput;
      Error error = chunkConsoleContents(path, &consoleOutput);
      (*pObj)[kChunkOutputValue] = consoleOutput;
   }
   else if (outputType == ChunkOutputPlot || outputType == ChunkOutputHtml)
   {
      // plot/HTML outputs should be requested by the client, so pass the path
      std::string url(kChunkOutputPath "/" + nbCtxId + "/" + 
                         docId + "/" + chunkId + "/" + 
                         path.filename());

      // if this is a plot and it doesn't have a display list, hint to client
      // that plot can't be resized
      if (outputType == ChunkOutputPlot)
      {
         // form the path to where we'd expect the snapshot to be
         FilePath snapshotPath = path.parent().complete(
               path.stem() + kDisplayListExt);
         if (!snapshotPath.exists())
            url.append("?fixed_size=1");
      }

      (*pObj)[kChunkOutputValue] = url;
   }
   else if (outputType == ChunkOutputData)
   {
      SEXP argsSEXP;
      r::sexp::Protect rProtect;

      Error error = r::exec::RFunction(
         ".rs.readDataCapture",
         string_utils::utf8ToSystem(path.absolutePath())).call(
            &argsSEXP,
            &rProtect);

      if (error)
         return error;

      json::Value valJson;
      error = r::json::jsonValueFromList(argsSEXP, &valJson);
      if (error)
         return error;

      (*pObj)[kChunkOutputValue] = valJson;
   }

   return Success();
}

#define kReHtmlWidgetContainerBegin     "<!-- htmlwidget-container-begin -->"
#define kReHtmlWidgetContainerEnd       "<!-- htmlwidget-container-end -->"
#define kReHtmlWidgetSizingPolicyBase64 "<!-- htmlwidget-sizing-policy-base64 (\\S+) -->"
#define kReHeadEnd                      "</head>"

class HtmlWidgetFilter : public boost::iostreams::regex_filter
{
public:
   HtmlWidgetFilter()
      : boost::iostreams::regex_filter(
           boost::regex(
              kReHtmlWidgetContainerBegin "|"
              kReHtmlWidgetContainerEnd "|"
              kReHtmlWidgetSizingPolicyBase64 "|"
              kReHeadEnd 
           ),
           boost::bind(&HtmlWidgetFilter::substitute, this, _1))
   {
   }
   
private:
   std::string substitute(const boost::cmatch& match)
   {
      // be paranoid
      std::size_t n = match.size();
      if (n < 1)
         return std::string();
      
      std::string matchString = match.str();
      if (matchString == kReHtmlWidgetContainerBegin)
         return "<div id=\"htmlwidget_container\">";
      else if (matchString == kReHtmlWidgetContainerEnd)
         return "</div>";
      else if (matchString == kReHeadEnd)
         return "<script type=\"text/javascript\">" + scrollScript() + 
                "</script>" + matchString;
      
      if (n < 2)
         return match[1].str();
      
      // decode htmlwidget sizing information
      std::string decoded;
      Error error = base64::decode(match[1].first, match[1].length(), &decoded);
      if (error)
         LOG_ERROR(error);
      return decoded;
   }

   std::string scrollScript()
   {
      return module_context::resourceFileAsString("propagate_scroll.js");
   }
};

Error handleChunkOutputRequest(const http::Request& request,
                               http::Response* pResponse)
{
   // uri format is: /chunk_output/<ctx-id>/<doc-id>/...
      
   // strip the querystring from the uri
   std::string uri = request.uri();
   size_t idx = uri.find_last_of("?");
   if (idx != std::string::npos)
      uri = uri.substr(0, idx);
   
   // split URI into pieces, extract the document ID, and remove that part of
   // the URI
   std::vector<std::string> parts = algorithm::split(uri, "/");
   if (parts.size() < 5) 
      return Success();

   std::string ctxId = parts[2];
   std::string docId = parts[3];
   for (int i = 0; i < 4; i++)
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

   FilePath target = chunkCacheFolder(path, docId, ctxId).complete(
         algorithm::join(parts, "/"));

   if (!target.exists())
   {
      pResponse->setNotFoundError(request.uri());
      return Success();
   }

   if (parts[0] == kChunkLibDir ||
       options().programMode() == kSessionProgramModeServer)
   {
      // in server mode, or if a reference to the chunk library folder, we can
      // reuse the contents (let the browser cache the file)
      pResponse->setCacheableFile(target, request, HtmlWidgetFilter());
   }
   else
   {
      // no cache necessary in desktop mode
      pResponse->setFile(target, request, HtmlWidgetFilter());
   }

   if (options().programMode() != kSessionProgramModeServer)
   {
       pResponse->setNoCacheHeaders();
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
                           const std::string& chunkId,
                           const std::string& nbCtxId)
{
   // check our cache 
   LastChunkOutput::iterator it = s_lastChunkOutputs.find(docId + chunkId);
   if (it != s_lastChunkOutputs.end())
   {
      return it->second;
   }
   
   FilePath outputPath = chunkOutputPath(docId, chunkId, nbCtxId, ContextExact);
   if (!outputPath.exists())
      return OutputPair();

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
      const std::string& chunkId, const std::string& nbCtxId, 
      ChunkOutputContext ctxType)
{
   // compute path to exact context
   FilePath path = chunkCacheFolder(docPath, docId, nbCtxId).childPath(chunkId);

   // fall back to saved context if permitted
   if (!path.exists() && ctxType == ContextSaved)
      path = chunkCacheFolder(docPath, docId, kSavedCtx).childPath(chunkId);

   return path;
}

FilePath chunkOutputPath(const std::string& docId, const std::string& chunkId,
      const std::string& nbCtxId, ChunkOutputContext ctxType)
{
   std::string docPath;
   source_database::getPath(docId, &docPath);

   return chunkOutputPath(docPath, docId, chunkId, nbCtxId, ctxType);
}

FilePath chunkOutputPath(const std::string& docId, const std::string& chunkId,
      ChunkOutputContext ctxType)
{
   return chunkOutputPath(docId, chunkId, notebookCtxId(), ctxType);
}

FilePath chunkOutputFile(const std::string& docId, 
                         const std::string& chunkId, 
                         const std::string& nbCtxId,
                         const OutputPair& output)
{
   return chunkOutputPath(docId, chunkId, nbCtxId, ContextExact).complete(
         (boost::format("%|1$06x|%2%") 
                     % (output.ordinal % MAX_ORDINAL)
                     % chunkOutputExt(output.outputType)).str());
}

FilePath chunkOutputFile(const std::string& docId, 
                         const std::string& chunkId, 
                         const std::string& nbCtxId, 
                         ChunkOutputType outputType)
{
   OutputPair output = lastChunkOutput(docId, chunkId, nbCtxId);
   if (output.outputType == outputType)
      return chunkOutputFile(docId, chunkId, nbCtxId, output);
   output.ordinal++;
   output.outputType = outputType;
   updateLastChunkOutput(docId, chunkId, output);
   return chunkOutputFile(docId, chunkId, nbCtxId, output);
}

void enqueueChunkOutput(const std::string& docId,
      const std::string& chunkId, const std::string& nbCtxId, unsigned ordinal, 
      ChunkOutputType outputType, const FilePath& path)
{
   enqueueChunkOutput(docId, chunkId, nbCtxId, ordinal, outputType, path, 
         json::Value());
}

void enqueueChunkOutput(const std::string& docId,
      const std::string& chunkId, const std::string& nbCtxId, unsigned ordinal, 
      ChunkOutputType outputType, const FilePath& path, 
      const core::json::Value& metadata)
{
   json::Object output;
   Error error = fillOutputObject(docId, chunkId, nbCtxId, ordinal, outputType, 
         path, &output);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   output[kChunkOutputMetadata] = metadata;

   json::Object result;
   result[kChunkId]         = chunkId;
   result[kChunkDocId]      = docId;
   result[kChunkOutputPath] = output;
   result[kRequestId]       = "";
   ClientEvent event(client_events::kChunkOutput, result);
   module_context::enqueClientEvent(event);
}

Error enqueueChunkOutput(
      const std::string& docPath, const std::string& docId,
      const std::string& chunkId, const std::string& nbCtxId,
      const std::string& requestId)
{
   FilePath outputDir = chunkOutputPath(docPath, docId, chunkId, nbCtxId,
         ContextSaved);

   std::string ctxId(outputDir.parent().filename());
   std::vector<FilePath> outputPaths;
   json::Array outputs;

   // if there's an output directory at the expected location (there may not be
   // for chunks which don't have any output at all), read it into a JSON
   // object for the client
   if (outputDir.exists())
   {
      Error error = outputDir.children(&outputPaths);
      if (error) 
         LOG_ERROR(error);

      // arrange by filename (use FilePath's < operator)
      std::sort(outputPaths.begin(), outputPaths.end());

      // loop through each and build an array of the outputs
      BOOST_FOREACH(const FilePath& outputPath, outputPaths)
      {
         json::Object output;

         // ascertain chunk output type from file extension; skip if extension 
         // unknown
         ChunkOutputType outputType = chunkOutputType(outputPath);
         if (outputType == ChunkOutputNone)
            continue;

         // extract ordinal from filename
         unsigned ordinal = ::strtol(outputPath.stem().c_str(), NULL, 16);

         // extract metadata if present
         json::Value meta;
         FilePath metadata = outputDir.complete(
               outputPath.stem() + ".metadata");
         if (metadata.exists())
         {
            std::string contents;
            error = readStringFromFile(metadata, &contents);
            if (!contents.empty())
               json::parse(contents, &meta);
         }
         output[kChunkOutputMetadata] = meta;
         
         // format/parse chunk output for client consumption
         error = fillOutputObject(docId, chunkId, ctxId, ordinal, outputType, 
               outputPath, &output);
         if (error)
            LOG_ERROR(error);
         else
            outputs.push_back(output);
      }
   }
   
   // note that if we find that this chunk has no output we can display, we
   // should still send it to the client, which will clean it up correctly, and
   // omit it in its next set of updated chunk definitions
   json::Object result;
   result[kChunkId]      = chunkId;
   result[kChunkDocId]   = docId;
   result[kChunkOutputs] = outputs;
   result[kRequestId]    = requestId;
   ClientEvent event(client_events::kChunkOutput, result);
   module_context::enqueClientEvent(event);

   return Success();
}

core::Error cleanChunkOutput(const std::string& docId,
      const std::string& chunkId, const std::string& nbCtxId, 
      bool preserveFolder)
{
   FilePath outputPath = chunkOutputPath(docId, chunkId, nbCtxId, ContextExact);
   if (!outputPath.exists())
      return Success();

   // reset counter if we're getting close to the end of our range (rare)
   OutputPair pair = lastChunkOutput(docId, chunkId, nbCtxId);
   if ((MAX_ORDINAL - pair.ordinal) < OUTPUT_THRESHOLD)
   {
      updateLastChunkOutput(docId, chunkId, OutputPair());
   }

   Error error = outputPath.remove();
   if (error)
      return error;
   if (preserveFolder)
   {
      error = outputPath.ensureDirectory();
      if (error)
         return error;
   }
   return Success();
}

core::Error appendConsoleOutput(int chunkConsoleType,
                                const std::string& output,
                                const core::FilePath& targetPath)
{
   Error error;
   
   error = targetPath.parent().ensureDirectory();
   if (error)
      return error;
   
   std::vector<std::string> data;
   data.push_back(safe_convert::numberToString(chunkConsoleType));
   data.push_back(output);
   
   std::string encoded = text::encodeCsvLine(data) + "\n";
   error = writeStringToFile(
            targetPath,
            encoded,
            string_utils::LineEndingPassthrough,
            false);
   return error;
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
