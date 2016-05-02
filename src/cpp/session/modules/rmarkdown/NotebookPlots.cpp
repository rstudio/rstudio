/*
 * NotebookPlots.cpp
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
#include "../SessionPlots.hpp"

#include <boost/format.hpp>
#include <boost/foreach.hpp>
#include <boost/signals/connection.hpp>

#include <core/system/FileMonitor.hpp>
#include <core/StringUtils.hpp>

#include <session/SessionModuleContext.hpp>

#include <r/RExec.hpp>
#include <r/RSexp.hpp>
#include <r/session/RGraphics.hpp>

#define kPlotPrefix "_rs_chunk_plot_"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {
namespace {

class PlotState
{
public:
   PlotState(FilePath folder):
      plotFolder(folder),
      hasPlots(false)
   {
   }

   FilePath plotFolder;
   bool hasPlots;
   r::sexp::PreservedSEXP sexpMargins;
   boost::signals::connection onConsolePrompt;
   boost::signals::connection onNewPage;
};

bool isPlotPath(const FilePath& path)
{
   return path.hasExtensionLowerCase(".png") &&
          string_utils::isPrefixOf(path.stem(), kPlotPrefix);
}

void processPlots(bool ignoreEmpty,
                  boost::shared_ptr<PlotState> pPlotState)
{
   // ensure plot folder exists
   if (pPlotState->plotFolder.exists())
      return;

   // collect plots from the folder
   std::vector<FilePath> folderContents;
   Error error = pPlotState->plotFolder.children(&folderContents);
   if (error)
      LOG_ERROR(error);

   BOOST_FOREACH(const FilePath& path, folderContents)
   {
      if (isPlotPath(path))
      {
         // we might find an empty plot file if it hasn't been flushed to disk
         // yet--ignore these
         if (ignoreEmpty && path.size() == 0)
            continue;

#ifdef _WIN32
   // on Windows, turning off the PNG device writes an empty PNG file if no 
   // plot output occurs; we avoid treating that empty file as an actual plot
   // by only emitting an event if a plot occurred.
   //
   // TODO: not all plot libraries cause the new plot hooks to invoke, so this
   // heuristic may cause us to miss a plot on Windows; we may need some
   // mechanism by which we can determine whether the device or its output is
   // empty.
         if (pPlotState->hasPlots)
#endif
            events().onPlotOutput(path);

         // clean up the plot so it isn't emitted twice
         error = path.removeIfExists();
         if (error)
            LOG_ERROR(error);
      }
   }
}

void removeGraphicsDevice(const FilePath& plotFolder, 
                          boost::shared_ptr<PlotState> pPlotState)
{
   // restore the figure margins
   Error error = r::exec::RFunction("par", pPlotState->sexpMargins).call();
   if (error)
      LOG_ERROR(error);
}

void onNewPage(boost::shared_ptr<PlotState> pPlotState,
               SEXP previousPageSnapshot)
{
   pPlotState->hasPlots = true;

   // save the snapshot to a file for later replay
   FilePath snapshotFile = pPlotState->plotFolder.childPath(
         core::system::generateUuid());
   Error error = r::exec::RFunction(".rs.saveGraphicsSnapshot",
               previousPageSnapshot,
               string_utils::utf8ToSystem(snapshotFile.absolutePath())).call();
   if (error)
      LOG_ERROR(error);
}

void onConsolePrompt(boost::shared_ptr<PlotState> pPlotState,
                     const std::string& )
{
   removeGraphicsDevice(pPlotState->plotFolder, pPlotState);
   pPlotState->onConsolePrompt.disconnect();
   pPlotState->onNewPage.disconnect();
}

} // anonymous namespace

// begins capturing plot output
core::Error beginPlotCapture(const FilePath& plotFolder)
{
   // clean up any stale plots from the folder
   std::vector<FilePath> folderContents;
   Error error = plotFolder.children(&folderContents);
   if (error)
      return error;

   BOOST_FOREACH(const core::FilePath& file, folderContents)
   {
      // remove if it looks like a plot 
      if (isPlotPath(file)) 
      {
         error = file.remove();
         if (error)
         {
            // this is non-fatal 
            LOG_ERROR(error);
         }
      }
   }

   // initialize state needed inside plot accumulators
   boost::shared_ptr<PlotState> pPlotState = boost::make_shared<PlotState>(
         plotFolder);

   // save old plot figure margin parameters
   r::exec::RFunction par("par");
   par.addParam("no.readonly", true);
   r::sexp::Protect protect;
   SEXP sexpMargins;
   error = par.call(&sexpMargins, &protect);
   if (error)
      LOG_ERROR(error);

   // preserve until chunk is finished executing
   pPlotState->sexpMargins.set(sexpMargins);

   // set notebook-friendly figure margins
   //                                          bot  left top  right
   error = r::exec::executeString("par(mar = c(5.1, 4.1, 2.1, 2.1))");
   if (error)
      LOG_ERROR(error);
   
   // begin capturing new page events
   pPlotState->onNewPage = 
      r::session::graphics::device::events().onNewPage.connect(
         boost::bind(onNewPage, pPlotState, _1));

   // complete the capture on the next console prompt
   pPlotState->onConsolePrompt = 
      module_context::events().onConsolePrompt.connect(
         boost::bind(onConsolePrompt, pPlotState, _1));

   return Success();
}


} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

