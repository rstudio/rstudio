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
#include <session/SessionSourceDatabase.hpp>

#define kChunkDefs "chunk_definitions"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

namespace {

FilePath chunkCacheFolder(const std::string& docPath, const std::string& docId)
{
   FilePath folder;
   std::string stem;
   if (docPath.empty()) 
   {
      folder = module_context::userScratchPath().childPath("unsaved-notebooks");
      stem = docId;
   }
   else
   {
      FilePath path = module_context::resolveAliasedPath(docPath);
      stem = path.stem();
      folder = path.parent();
   }

   return folder.childPath(stem + ".rnb.cached");
}


FilePath chunkDefinitionsPath(
      const std::string& docPath, const std::string& docId)
{
   return chunkCacheFolder(docPath, docId).childPath("chunks.json");
}

FilePath chunkOutputPath(
      const std::string& docPath, const std::string& docId,
      const std::string& chunkId)
{
   return chunkCacheFolder(docPath, docId)
                          .childPath(chunkId)
                          .childPath("contents.html");
}


Error enqueueChunkOutput(
      const std::string& docPath, const std::string& docId,
      const std::string& chunkId)
{
   // read the chunk HTML from the file
   std::string html;
   Error error = core::readStringFromFile(
         chunkOutputPath(docPath, docId, chunkId), &html);
   if (error)
      return error;

   // and write it back out
   json::Object output;
   output["html"] = html;
   output["chunk_id"] = chunkId;
   output["doc_id"] = docId;
   ClientEvent event(client_events::kChunkOutput, output);
   module_context::enqueClientEvent(event);

   return Success();
}

Error executeInlineChunk(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse*)
{
   std::string docPath, docId, chunkId, options, content;
   Error error = json::readParams(request.params, &docPath, &docId, &chunkId, 
         &options, &content);
   if (error)
      return error;

   // ensure we have a place to put the output
   FilePath chunkOutput = chunkOutputPath(docPath, docId, chunkId);
   error = chunkOutput.parent().ensureDirectory();
   if (error)
      return error;

   // render the contents to the cached folder, then extract the contents
   error = r::exec::RFunction(".rs.executeSingleChunk", options, content,
         chunkOutput.absolutePath()).call();
   if (error)
      return error;

   error = enqueueChunkOutput(docPath, docId, chunkId);

   return Success();
}

void replayChunkOutputs(const std::string& docPath, const std::string& docId,
      const std::string& requestId, const json::Array& chunkOutputs) 
{
   // find all the chunks and play them back to the client
   BOOST_FOREACH(const json::Value& chunkOutput, chunkOutputs)
   {
      if (chunkOutput.type() != json::ObjectType)
         continue;
      std::string chunkId;
      if (json::readObject(chunkOutput.get_obj(), "chunk_id", &chunkId) ==
            Success()) 
      {
         // ignore errors here (it's okay if some chunks don't have output)
         enqueueChunkOutput(docPath, docId, chunkId);
      }
   }

   json::Object result;
   result["path"] = docPath;
   result["request_id"] = requestId;
   ClientEvent event(client_events::kChunkOutputFinished, result);
   module_context::enqueClientEvent(event);
}

Error refreshChunkOutput(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse*)
{
   // extract path to doc to be refreshed
   std::string docPath, docId, requestId;
   Error error = json::readParams(request.params, &docPath, &docId, &requestId);
   if (error)
      return error;

   json::Object result;
   json::Value chunkDefs; 
   error = getChunkDefs(docPath, docId, &chunkDefs);

   // schedule the work to play back the chunks (we don't do it synchronously
   // so the RPC can return immediately)
   if (!error && chunkDefs.type() == json::ArrayType) 
   {
      module_context::scheduleDelayedWork(boost::posix_time::milliseconds(10), 
            boost::bind(replayChunkOutputs, docPath, docId, requestId, 
                        chunkDefs.get_array()));
   }

   return Success();
}

bool copyCacheItem(const FilePath& from,
                   const FilePath& to,
                   const FilePath& path)
{

   std::string relativePath = path.relativePath(from);
   FilePath target = from.complete(relativePath);

   Error error = path.isDirectory() ?
                     target.ensureDirectory() :
                     path.copy(target);
   if (error)
      LOG_ERROR(error);

   return true;
}

Error copyCache(const FilePath& from, const FilePath& to)
{
   Error error = to.ensureDirectory();
   if (error)
      return error;

   return to.childrenRecursive(
             boost::bind(copyCacheItem, from, to, _2));
}

void onDocRenamed(const std::string& oldPath, 
                  boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   std::cerr << "doc rename: " << oldPath << " -> " << pDoc->path() << std::endl;
   // compute cache folders and ignore if we can't safely adjust them
   FilePath oldCacheDir = chunkCacheFolder(oldPath, pDoc->id());
   FilePath newCacheDir = chunkCacheFolder(pDoc->path(), pDoc->id());
   if (!oldCacheDir.exists() || newCacheDir.exists())
      return;


   std::cerr << "promote cache " << oldCacheDir << " -> " << newCacheDir << std::endl;

   // if the doc was previously unsaved, we can just move the whole folder 
   // to its newly saved location
   if (oldPath.empty())
   {
      oldCacheDir.move(newCacheDir);
      return;
   }

   Error error = copyCache(oldCacheDir, newCacheDir);
   if (error)
      LOG_ERROR(error);
}

} // anonymous namespace

Error setChunkDefs(const std::string& docPath, const std::string& docId,
                   const json::Array& pDefs)
{
   // create JSON object wrapping 
   json::Object chunkDefs;
   chunkDefs[kChunkDefs] = pDefs;

   // ensure we have a place to write the sidecar file
   FilePath defFile = chunkDefinitionsPath(docPath, docId);
   Error error = defFile.parent().ensureDirectory();
   if (error)
      return error;

   // write to the sidecar file
   std::ostringstream oss;
   json::write(chunkDefs, oss);
   return writeStringToFile(defFile, oss.str());
}

Error getChunkDefs(const std::string& docPath, const std::string& docId,
                   core::json::Value* pDefs)
{
   Error error;
   FilePath defs = chunkDefinitionsPath(docPath, docId);
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

   // TODO: when a doc is removed, we need to remove its cached chunks if 
   // it was never saved
   source_database::events().onDocRenamed.connect(onDocRenamed);
   
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

