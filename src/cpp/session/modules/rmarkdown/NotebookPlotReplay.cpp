/*
 * NotebookPlotReplay.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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
#include "NotebookChunkDefs.hpp"
#include "NotebookOutput.hpp"

#include <boost/algorithm/string.hpp>
#include <boost/bind/bind.hpp>

#include <core/StringUtils.hpp>
#include <core/Exec.hpp>

#include <r/RExec.hpp>
#include <r/RJson.hpp>
#include <r/session/RGraphics.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionAsyncRProcess.hpp>
#include <session/SessionOptions.hpp>


using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

namespace {

// this class supervises an asynchronous replay of all of a notebook's 
// plot display lists
class ReplayPlots : public async_r::AsyncRProcess
{
public:
   static boost::shared_ptr<ReplayPlots> create(
         const std::string& docId,
         const std::string& replayId,
         const std::vector<std::string>& chunkIds,
         const json::Array& chunkDefs,
         int width,
         int height,
         bool persistOutput)
   {
      Error error;
      
      // create the text to send to the process (it'll be read on stdin inside R)
      std::string input = core::algorithm::join(chunkIds, "\n");
      input.append("\n");
      
      // load the files which contain the R scripts needed to replay plots 
      std::vector<core::FilePath> sources;

      FilePath modulesPath = session::options().modulesRSourcePath();
      FilePath sourcesPath = session::options().coreRSourcePath();

      sources.push_back(sourcesPath.completePath("Tools.R"));
      sources.push_back(modulesPath.completePath("ModuleTools.R"));
      sources.push_back(modulesPath.completePath("NotebookPlots.R"));

      // form extra bitmap params 
      std::string extraParams = r::session::graphics::extraBitmapParams();
      if (!extraParams.empty())
         extraParams = string_utils::singleQuotedStrEscape(extraParams);
      
      // convert chunk definitions into object
      json::Object chunkDefsObject;
      for (std::size_t i = 0, n = chunkDefs.getSize(); i < n; i++)
      {
         json::Value chunkDef = chunkDefs.getValueAt(i);
         if (chunkDef.isObject() &&
             chunkDef.getObject().hasMember("chunk_id"))
         {
            std::string chunkId;
            Error error = core::json::readObject(chunkDef.getObject(), "chunk_id", chunkId);
            if (error)
            {
               LOG_ERROR(error);
               continue;
            }
            
            chunkDefsObject[chunkId] = chunkDef;
         }
      }
      
      // write chunk id array to JSON at a location for R to read
      FilePath chunkPath = module_context::tempFile("replay-plots-", "rds");
      std::string jsonChunks = chunkDefsObject.writeFormatted();
      error = core::writeStringToFile(chunkPath, jsonChunks);
      if (error)
         LOG_ERROR(error);
            
      // form command to pass to R
      std::string cmd = fmt::format(
               ".rs.replayNotebookPlots({}, {}, {}, {}, {}, {})",
               shell_utils::escape(chunkPath),
               width,
               height,
               r::session::graphics::device::devicePixelRatio(),
               persistOutput ? "TRUE" : "FALSE",
               "'" + extraParams + "'");
               
      // set up environment
      core::system::Options environment;
      core::system::environment(&environment);
      
      // pass along packages we might require for plotting
      std::vector<std::string> requiredPackages;
      error = r::exec::RFunction(".rs.replayNotebookPlotsPackages")
            .call(&requiredPackages);
      if (error)
         LOG_ERROR(error);
      
      environment.push_back({ "R_LIBS", module_context::libPathsString() });
      environment.push_back({ "RS_NOTEBOOK_PACKAGES", core::algorithm::join(requiredPackages, ",") });

      // invoke the asynchronous process
      boost::shared_ptr<ReplayPlots> pReplayer(new ReplayPlots());
      pReplayer->docId_ = docId;
      pReplayer->replayId_ = replayId;
      pReplayer->width_ = width;
      pReplayer->persistOutput_ = persistOutput;
      pReplayer->start(cmd.c_str(),
                       environment,
                       FilePath(),
                       async_r::R_PROCESS_VANILLA,
                       sources,
                       input);
      return pReplayer;
   }

private:
   void onStdout(const std::string& output)
   {
      r::sexp::Protect protect;
      Error error;

      // output is queued/buffered so multiple paths may be emitted
      std::vector<std::string> paths;
      boost::algorithm::split(paths, output,
                              boost::algorithm::is_any_of("\n\r"));
      for (const std::string& path : paths)
      {
         // ensure path exists
         FilePath png(string_utils::trimWhitespace(path));
         if (!png.exists())
            continue;

         FilePath chunkBase = png;
         if (!persistOutput_)
            chunkBase = png.getParent();

         // create the event and send to the client. consider: this makes some
         // assumptions about the way output URLs are formed and some
         // assumptions about cache structure that might be better localized.
         json::Object result;
         result["chunk_id"] = chunkBase.getParent().getFilename();
         result["doc_id"] = docId_;
         result["replay_id"] = replayId_;

         result["plot_url"] = kChunkOutputPath "/" + 
            chunkBase.getParent().getParent().getFilename() + "/" + // context ID = folder name
            docId_ + "/" + 
            chunkBase.getParent().getFilename() + 
            "/" + 
            (persistOutput_ ? "" : png.getParent().getFilename() + "/") +
            png.getFilename();

         ClientEvent event(client_events::kChunkPlotRefreshed, result);
         module_context::enqueClientEvent(event);
      }
   }
   
   void onStderr(const std::string& output)
   {
      stderr_.append(output);
   }

   void onCompleted(int exitStatus)
   {
      // let client know the process is completed (even if it failed)
      json::Object result;
      result["doc_id"] = docId_;
      result["width"] = width_;
      result["replay_id"] = replayId_;
      module_context::enqueClientEvent(ClientEvent(
               client_events::kChunkPlotRefreshFinished, result));
      
      // if some errors occurred, log those
      if (!stderr_.empty())
         WLOGF("Error replaying plots: {}", stderr_);

      // if we succeeded, write the new rendered width into the notebook chunk
      // file
      if (exitStatus == EXIT_SUCCESS && persistOutput_)
      {
         std::string docPath;
         source_database::getPath(docId_, &docPath);
         setChunkValue(docPath, docId_, "chunk_rendered_width", width_);
      }
   }
   
   std::string docId_;
   std::string replayId_;
   std::string stderr_;
   bool persistOutput_;
   int width_;
};

boost::shared_ptr<ReplayPlots> s_pPlotReplayer;

typedef std::map<std::string, boost::shared_ptr<ReplayPlots> > ReplayPlotsMap;
ReplayPlotsMap s_pPlotReplayerForChunkId;

Error replayNotebookPlots(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   std::string replayId = core::system::generateUuid();
   std::string docId;
   std::string initialChunkId;
   int pixelWidth = 0;
   int pixelHeight = 0;
   Error error = json::readParams(request.params, &docId, 
         &initialChunkId, &pixelWidth, &pixelHeight);
   if (error)
      return error;

   // do nothing if we're already replaying plots (consider: maybe better to
   // abort and restart with the new pixel width?)
   if (s_pPlotReplayer && s_pPlotReplayer->isRunning())
   {
      pResponse->setResult("");
      return Success();
   }

   // extract the list of chunks to replay
   std::string docPath;
   source_database::getPath(docId, &docPath);
   json::Array chunkDefs;
   error = getChunkValue(docPath, docId, kChunkDefs, &chunkDefs);
   if (error)
      return error;

   // convert to chunk IDs
   std::vector<std::string> chunkIds;
   extractChunkIds(chunkDefs, &chunkIds);

   // shuffle the chunk IDs so we re-render the visible ones first
   auto it = std::find(chunkIds.begin(), chunkIds.end(), initialChunkId);
   if (it != chunkIds.end())
   {
      std::vector<std::string> shuffledChunkIds;
      std::copy(it, chunkIds.end(), std::back_inserter(shuffledChunkIds));
      std::copy(chunkIds.begin(), it, std::back_inserter(shuffledChunkIds));
      chunkIds = shuffledChunkIds;
   }

   // look through out chunk definitions, and add in information
   // on the available snapshot files
   for (std::size_t i = 0, n = chunkDefs.getSize(); i < n; i++)
   {
      json::Value chunkDefValue = chunkDefs.getValueAt(i);
      if (!chunkDefValue.isObject())
         continue;
      
      json::Object chunkDefObject = chunkDefValue.getObject();
      
      std::string chunkId;
      Error error = core::json::readObject(chunkDefObject, "chunk_id", chunkId);
      if (error)
         continue;
    
      // find the storage location for this chunk output
      FilePath path = chunkOutputPath(docPath, docId, chunkId, notebookCtxId(), ContextSaved);
      if (!path.exists())
         continue;

      // look for snapshot files
      std::vector<FilePath> contents;
      error = path.getChildren(contents);
      if (error)
      {
         LOG_ERROR(error);
         continue;
      }
      
      json::Array snapshotFiles;
      for (const FilePath& content : contents)
      {
         if (content.hasExtensionLowerCase(kDisplayListExt))
            snapshotFiles.push_back(content.getAbsolutePath());
      }
      chunkDefObject["snapshot_files"] = snapshotFiles;
      
   }

   s_pPlotReplayer = ReplayPlots::create(
            docId,
            replayId,
            chunkIds,
            chunkDefs,
            pixelWidth,
            pixelHeight,
            true);
   
   pResponse->setResult(replayId);

   return Success();
}

Error replayChunkPlotOutput(const json::JsonRpcRequest& request,
                            json::JsonRpcResponse* pResponse)
{
   std::string replayId = core::system::generateUuid();
   std::string docId, chunkId;
   int pixelWidth, pixelHeight = 0;
   Error error = json::readParams(request.params, &docId,  &chunkId, &pixelWidth, &pixelHeight);
   if (error)
      return error;

   ReplayPlotsMap::iterator it = s_pPlotReplayerForChunkId.find(docId + chunkId);
   if (it != s_pPlotReplayerForChunkId.end())
   {
      boost::shared_ptr<ReplayPlots> pReplayPlots = it->second;
      
      // do nothing if we're already replaying plots (consider: maybe better to
      // abort and restart with the new pixel width?)
      if (pReplayPlots->isRunning())
      {
         pResponse->setResult("");
         return Success();
      }
   }
   
   // extract the list of chunks to replay
   std::string docPath;
   source_database::getPath(docId, &docPath);
   json::Array allChunkDefs;
   error = getChunkValue(docPath, docId, kChunkDefs, &allChunkDefs);
   if (error)
      return error;
   
   // keep only the chunk we're looking at
   json::Object chunkDef;
   for (std::size_t i = 0, n = allChunkDefs.getSize(); i < n; i++)
   {
      json::Value chunkDefValue = allChunkDefs.getValueAt(i);
      if (chunkDefValue.isObject())
      {
         json::Object chunkDefObject = chunkDefValue.getObject();
         if (chunkDefObject.hasMember("chunk_id") &&
             chunkDefObject["chunk_id"].isString() &&
             chunkDefObject["chunk_id"].getString() == chunkId)
         {
            chunkDef = chunkDefObject;
            break;
         }
      }
   }
   
   if (chunkDef.isNull())
      return Success();
   
   // find the storage location for this chunk output
   FilePath path = chunkOutputPath(docPath, docId, chunkId, notebookCtxId(), ContextSaved);
   if (!path.exists())
      return Success();

   // look for snapshot files
   std::vector<FilePath> contents;
   error = path.getChildren(contents);
   if (error)
   {
      LOG_ERROR(error);
      return Success();
   }

   json::Array snapshotFiles;
   for (const FilePath& content : contents)
   {
      if (content.hasExtensionLowerCase(kDisplayListExt))
         snapshotFiles.push_back(content.getAbsolutePath());
   }
   chunkDef["snapshot_files"] = snapshotFiles;
   
   std::vector<std::string> chunkIds = { chunkId };
   
   json::Array chunkDefs = json::Array();
   chunkDefs.push_back(chunkDef);
   
   s_pPlotReplayerForChunkId[docId + chunkId] = ReplayPlots::create(
            docId,
            replayId,
            chunkIds,
            chunkDefs,
            pixelWidth,
            pixelHeight,
            false);
   
   pResponse->setResult(replayId);

   return Success();
}

Error cleanReplayChunkPlotOutput(const json::JsonRpcRequest& request,
                                 json::JsonRpcResponse* pResponse)
{
   std::string docId;
   std::string chunkId;
   Error error = json::readParams(request.params, &docId, &chunkId);
   if (error)
      return error;

   // extract the list of chunks to replay
   std::string docPath;
   source_database::getPath(docId, &docPath);

   FilePath chunkFilePath = chunkOutputPath(docPath, docId, chunkId, notebookCtxId(),
      ContextSaved);

   FilePath tempFilePath = chunkFilePath.completePath("temp");

   error = tempFilePath.remove();
   if (error)
      LOG_ERROR(error);

   return Success();
}

} // anonymous namespace

core::Error initPlotReplay()
{
   using namespace module_context;
   using boost::bind;

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "replay_notebook_plots", replayNotebookPlots))
      (bind(registerRpcMethod, "replay_notebook_chunk_plots", replayChunkPlotOutput))
      (bind(registerRpcMethod, "clean_replay_notebook_chunk_plots", cleanReplayChunkPlotOutput));

   return initBlock.execute();
}


} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

