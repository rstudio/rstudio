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

#include <boost/foreach.hpp>

#include <r/RJson.hpp>
#include <r/RExec.hpp>

#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/json/JsonRpc.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

namespace {

FilePath chunkOutputPath(const FilePath& file, 
                         const std::string& chunkId)
{
   return file.parent().childPath(file.stem() + ".rnb.cached")
                       .childPath(chunkId)
                       .childPath("contents.html");
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
   output["file"] = file.absolutePath();
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

   // ensure we have a place to put the output
   FilePath chunkOutput = chunkOutputPath(FilePath(file), chunkId);
   error = chunkOutput.parent().ensureDirectory();
   if (error)
      return error;

   // render the contents to the cached folder, then extract the contents
   error = r::exec::RFunction(".rs.executeSingleChunk", options, content,
         chunkOutput.absolutePath()).call();
   if (error)
      return error;

   error = enqueueChunkOutput(FilePath(file), chunkId);

   return Success();
}

Error refreshChunkOutput(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse*)
{
   std::string docId, requestId;
   Error error = json::readParams(request.params, &docId, &requestId);
   if (error)
      return error;

   // get the source document 
   boost::shared_ptr<source_database::SourceDocument> pSourceDoc;
   error = source_database::get(docId, pSourceDoc);
   if (error)
      return error;
   if (!pSourceDoc)
      return Error(json::errc::ParamInvalid, ERROR_LOCATION);

   // find all the chunks and play them back to the client
   json::Array chunkOutputs = pSourceDoc->chunkOutput();
   BOOST_FOREACH(const json::Value& chunkOutput, chunkOutputs)
   {
      if (chunkOutput.type() != json::ObjectType)
         continue;
      std::string chunkId;
      if (json::readObject(chunkOutput.get_obj(), "chunk_id", &chunkId) ==
            Success()) 
      {
         // ignore errors here (it's okay if some chunks don't have output)
         enqueueChunkOutput(FilePath(pSourceDoc->path()), chunkId);
      }
   }

   json::Object result;
   result["doc_id"] = docId;
   result["request_id"] = requestId;
   ClientEvent event(client_events::kChunkOutputFinished, result);
   module_context::enqueClientEvent(event);

   return Success();
}

} // anonymous namespace


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

