/*
 * NotebookPlotReplay.cpp
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
#include "NotebookPlots.hpp"
#include "NotebookPlotReplay.hpp"
#include "NotebookChunkDefs.hpp"
#include "NotebookOutput.hpp"

#include <boost/algorithm/string.hpp>
#include <boost/foreach.hpp>

#include <core/StringUtils.hpp>
#include <core/Exec.hpp>

#include <r/RExec.hpp>
#include <r/RJson.hpp>
#include <r/session/RGraphics.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionAsyncRProcess.hpp>
#include <session/SessionOptions.hpp>


using namespace rstudio::core;

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
         int width,
         const std::vector<FilePath>& snapshotFiles)
   {
      // create the text to send to the process (it'll be read on stdin
      // inside R)
      std::string input;
      BOOST_FOREACH(const FilePath snapshot, snapshotFiles)
      {
         input.append(string_utils::utf8ToSystem(snapshot.absolutePath()));
         input.append("\n");
      }
      input.append("\n");
      
      // load the files which contain the R scripts needed to replay plots 
      std::vector<core::FilePath> sources;

      FilePath modulesPath = session::options().modulesRSourcePath();
      FilePath sourcesPath = session::options().coreRSourcePath();

      sources.push_back(sourcesPath.complete("Tools.R"));
      sources.push_back(modulesPath.complete("ModuleTools.R"));
      sources.push_back(modulesPath.complete("NotebookPlots.R"));

      // form extra bitmap params 
      std::string extraParams = r::session::graphics::extraBitmapParams();
      if (!extraParams.empty())
         extraParams = string_utils::singleQuotedStrEscape(extraParams);

      // form command to pass to R 
      std::string cmd(".rs.replayNotebookPlots(" + 
                      safe_convert::numberToString(width) + ", " + 
                      safe_convert::numberToString(
                         r::session::graphics::device::devicePixelRatio()) +
                         ", "
                      "'" + extraParams + "')");

      // invoke the asynchronous process
      boost::shared_ptr<ReplayPlots> pReplayer(new ReplayPlots());
      pReplayer->docId_ = docId;
      pReplayer->width_ = width;
      pReplayer->start(cmd.c_str(), FilePath(),
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
      BOOST_FOREACH(std::string& path, paths)
      {
         // ensure path exists
         FilePath png(string_utils::trimWhitespace(path));
         if (!png.exists())
            continue;

         // create the event and send to the client. consider: this makes some
         // assumptions about the way output URLs are formed and some
         // assumptions about cache structure that might be better localized.
         json::Object result;
         result["chunk_id"] = png.parent().filename();
         result["doc_id"] = docId_;
         result["plot_url"] = kChunkOutputPath "/" + 
            png.parent().parent().filename() + "/" + // context ID = folder name
            docId_ + "/" + 
            png.parent().filename() + "/" + 
            png.filename();

         ClientEvent event(client_events::kChunkPlotRefreshed, result);
         module_context::enqueClientEvent(event);
      }
   }

   void onCompleted(int exitStatus)
   {
      // let client know the process is completed (even if it failed)
      json::Object result;
      result["doc_id"] = docId_;
      result["width"] = width_;
      module_context::enqueClientEvent(ClientEvent(
               client_events::kChunkPlotRefreshFinished, result));
   }

   std::string docId_;
   int width_;
};

boost::shared_ptr<ReplayPlots> s_pPlotReplayer;

Error replayPlotOutput(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   std::string docId;
   std::string initialChunkId;
   int pixelWidth = 0;
   Error error = json::readParams(request.params, &docId, 
         &initialChunkId, &pixelWidth);
   if (error)
      return error;

   // do nothing if we're already replaying plots (consider: maybe better to
   // abort and restart with the new pixel width?)
   if (s_pPlotReplayer && s_pPlotReplayer->isRunning())
   {
      pResponse->setResult(false);
      return Success();
   }

   // extract the list of chunks to replay
   std::string docPath;
   source_database::getPath(docId, &docPath);
   core::json::Value chunkIdVals;
   error = getChunkDefs(docPath, docId, NULL, &chunkIdVals);
   if (error)
      return error;

   // very unlikely, but important to bail out if it happens so client doesn't
   // wait for replay to complete
   if (chunkIdVals.type() != json::ArrayType)
      return Error(json::errc::ParseError, ERROR_LOCATION);

   // convert to chunk IDs
   std::vector<std::string> chunkIds;
   extractChunkIds(chunkIdVals.get_array(), &chunkIds);

   // shuffle the chunk IDs so we re-render the visible ones first
   std::vector<std::string>::iterator it = std::find(
         chunkIds.begin(), chunkIds.end(), initialChunkId);
   if (it != chunkIds.end())
   {
      std::vector<std::string> shuffledChunkIds;
      std::copy(it, chunkIds.end(), std::back_inserter(shuffledChunkIds));
      std::copy(chunkIds.begin(), it, std::back_inserter(shuffledChunkIds));
      chunkIds = shuffledChunkIds;
   }

   // look for snapshot files
   std::vector<FilePath> snapshotFiles;
   BOOST_FOREACH(const std::string chunkId, chunkIds)
   {
      // find the storage location for this chunk output
      FilePath path = chunkOutputPath(docPath, docId, chunkId, notebookCtxId(),
            ContextSaved);
      if (!path.exists())
         continue;

      // look for snapshot files
      std::vector<FilePath> contents;
      error = path.children(&contents);
      if (error)
      {
         LOG_ERROR(error);
         continue;
      }
      BOOST_FOREACH(const FilePath content, contents)
      {
         if (content.hasExtensionLowerCase(kDisplayListExt))
            snapshotFiles.push_back(content);
      }
   }

   s_pPlotReplayer = ReplayPlots::create(docId, pixelWidth, snapshotFiles);
   pResponse->setResult(true);

   return Success();
}


} // anonymous namespace

core::Error initPlotReplay()
{
   using namespace module_context;

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "replay_notebook_plots", replayPlotOutput));

   return initBlock.execute();
}


} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

