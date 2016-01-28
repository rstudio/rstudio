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

#include <iostream>

#include <boost/foreach.hpp>

#include <r/RJson.hpp>
#include <r/RExec.hpp>

#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>

#include <session/SessionModuleContext.hpp>

#define kChunkDefs "chunk_definitions"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

namespace {

FilePath chunkCacheFolder(const FilePath& file)
{
   return file.parent().childPath(file.stem() + ".rnb.cached");
}

FilePath chunkDefinitionsPath(const FilePath& file) 
{
   return chunkCacheFolder(file).childPath("chunks.json");
}

FilePath chunkOutputPath(const FilePath& file, 
                         const std::string& chunkId)
{
   return chunkCacheFolder(file).childPath(chunkId).childPath("contents.html");
}


Error enqueueChunkOutput(const FilePath& file, 
                         const std::string& chunkId)
{
   // read the chunk HTML from the file
   std::string html;
   Error error = core::readStringFromFile(
         chunkOutputPath(file, chunkId), &html);
   if (error)
      return error;

   // and write it back out
   json::Object output;
   output["html"] = html;
   output["chunk_id"] = chunkId;
   output["file"] = module_context::createAliasedPath(FileInfo(file));
   ClientEvent event(client_events::kChunkOutput, output);
   module_context::enqueClientEvent(event);

   return Success();
}

Error executeInlineChunk(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse*)
{
   std::string file, chunkId, options, content;
   Error error = json::readParams(request.params, &file, &chunkId, &options, 
         &content);
   if (error)
      return error;
   FilePath path = module_context::resolveAliasedPath(file);

   // ensure we have a place to put the output
   FilePath chunkOutput = chunkOutputPath(path, chunkId);
   error = chunkOutput.parent().ensureDirectory();
   if (error)
      return error;

   // render the contents to the cached folder, then extract the contents
   error = r::exec::RFunction(".rs.executeSingleChunk", options, content,
         chunkOutput.absolutePath()).call();
   if (error)
      return error;

   error = enqueueChunkOutput(path, chunkId);

   return Success();
}

void replayChunkOutputs(const FilePath& file, const std::string& requestId,
      const json::Array& chunkOutputs) 
{
   // find all the chunks and play them back to the client
   BOOST_FOREACH(const json::Value& chunkOutput, chunkOutputs)
   {
      if (chunkOutput.type() != json::ObjectType)
         continue;
      std::string chunkId;
      std::cerr << "attempting to replay chunk ";
      json::write(chunkOutput, std::cerr);
      std::cerr << std::endl;
      if (json::readObject(chunkOutput.get_obj(), "chunk_id", &chunkId) ==
            Success()) 
      {
         // ignore errors here (it's okay if some chunks don't have output)
         enqueueChunkOutput(file, chunkId);
      }
   }

   std::cerr << "done with chunks for req " << requestId << std::endl;
   json::Object result;
   result["path"] = file.absolutePath();
   result["request_id"] = requestId;
   ClientEvent event(client_events::kChunkOutputFinished, result);
   module_context::enqueClientEvent(event);
}

Error refreshChunkOutput(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse*)
{
   // extract path to doc to be refreshed
   std::string docId, requestId;
   Error error = json::readParams(request.params, &docId, &requestId);
   if (error)
      return error;
   boost::shared_ptr<source_database::SourceDocument> pDoc(
         new source_database::SourceDocument());
   error = source_database::get(docId, pDoc);
   if (error)
      return error;
   if (!pDoc)
      return Error(json::errc::ParamInvalid, ERROR_LOCATION);
   FilePath path = module_context::resolveAliasedPath(pDoc->path());

   json::Object result;
   json::Value chunkDefs; 
   error = getChunkDefs(path, &chunkDefs);

   // schedule the work to play back the chunks (we don't do it synchronously
   // so the RPC can return immediately)
   if (!error && chunkDefs.type() == json::ArrayType) 
   {
      module_context::scheduleDelayedWork(boost::posix_time::milliseconds(10), 
            boost::bind(replayChunkOutputs, path, requestId, 
                        chunkDefs.get_array()));
   }

   return Success();
}

} // anonymous namespace

Error setChunkDefs(const FilePath& file, 
                   const json::Array& pDefs)
{
   // create JSON object wrapping 
   json::Object chunkDefs;
   chunkDefs[kChunkDefs] = pDefs;

   // ensure we have a place to write the sidecar file
   FilePath defFile = chunkDefinitionsPath(file);
   Error error = defFile.parent().ensureDirectory();
   if (error)
      return error;

   // write to the sidecar file
   std::ostringstream oss;
   json::write(chunkDefs, oss);
   std::cerr << "writing chunk defs to " << defFile.absolutePath() << ": " << oss.str() << std::endl;
   return writeStringToFile(defFile, oss.str());
}

Error getChunkDefs(const FilePath& file, core::json::Value* pDefs)
{
   Error error;
   FilePath defs = chunkDefinitionsPath(file);
   std::cerr << "checking for chunk defs @ " << defs.absolutePath() << std::endl;
   if (!defs.exists())
      return Success();

   // read the defs file 
   std::string contents;
   error = readStringFromFile(defs, &contents);
   if (error)
      return error;

   // pull out the contents
   json::Value defContents;
   if (!json::parse(contents, &defContents) || 
       defContents.type() != json::ObjectType)
      return Error(json::errc::ParseError, ERROR_LOCATION);
   
   // extract the chunk definitions
   json::Array chunkDefs;
   error = json::readObject(defContents.get_obj(), kChunkDefs, &chunkDefs);
   if (error)
      return error;

   // return to caller
   *pDefs = chunkDefs;
   return Success();
}

Error initialize()
{
   using boost::bind;
   using namespace module_context;
   
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "execute_inline_chunk", executeInlineChunk))
      (bind(registerRpcMethod, "refresh_chunk_output", refreshChunkOutput))
      (bind(module_context::sourceModuleRFile, "SessionRmdNotebook.R"));

   return initBlock.execute();
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

