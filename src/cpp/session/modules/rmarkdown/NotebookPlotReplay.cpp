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
#include "NotebookPlotReplay.hpp"

#include <boost/algorithm/string.hpp>
#include <boost/foreach.hpp>

#include <core/StringUtils.hpp>
#include <core/Exec.hpp>

#include <r/RExec.hpp>
#include <r/RJSon.hpp>

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
   static boost::shared_ptr<ReplayPlots> create(const FilePath& cacheFolder)
   {
      // load the files which contain the R scripts needed to replay plots 
      std::vector<core::FilePath> sources;

      FilePath modulesPath = session::options().modulesRSourcePath();
      FilePath sourcesPath = session::options().coreRSourcePath();

      sources.push_back(sourcesPath.complete("Tools.R"));
      sources.push_back(modulesPath.complete("ModuleTools.R"));
      sources.push_back(modulesPath.complete("NotebookPlots.R"));

      // create path to cache folder
      std::string path = string_utils::utf8ToSystem(cacheFolder.absolutePath());
      std::string cmd(".rs.replayNotebokPlots(\"" + path + "\")");
      
      // invoke the asynchronous process
      boost::shared_ptr<ReplayPlots> pReplayer(new ReplayPlots());
      pReplayer->start(cmd.c_str(), FilePath(),
                       async_r::R_PROCESS_VANILLA,
                       sources);
      return pReplayer;
   }

private:
   void onStdout(const std::string& output)
   {
      r::sexp::Protect protect;
      Error error;

      std::vector<std::string> paths;
      boost::algorithm::split(paths, output,
                              boost::algorithm::is_any_of("\n\r"));
      BOOST_FOREACH(std::string& path, paths)
      {
         // TODO: parse & emit client event  
      }
   }

   void onCompleted(int exitStatus)
   {
      // TODO: let client know we've done all the plot resizing we're going
      // to do
   }
};


Error replayPlotOutput(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   std::string docId;
   int pixelWidth = 0;
   bool replace = false;
   Error error = json::readParams(request.params, &docId, &pixelWidth);
   if (error)
      return error;

   // TODO: create replayer
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

