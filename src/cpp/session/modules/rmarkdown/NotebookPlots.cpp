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
#include <core/Exec.hpp>

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
   PlotState(const FilePath& folder):
      plotFolder(folder),
      hasPlots(false)
   {
   }

   FilePath plotFolder;
   bool hasPlots;
   FilePath snapshotFile;
   r::sexp::PreservedSEXP sexpMargins;
   boost::signals::connection onConsolePrompt;
   boost::signals::connection onBeforeNewPlot;
   boost::signals::connection onBeforeNewGridPage;
   boost::signals::connection onNewPlot;
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
   if (!pPlotState->plotFolder.exists())
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

         // it's possible for a PNG to get written that has no content 
         // (on Windows in particular closing the PNG device always generates
         // a PNG) so make sure we have something to write 
         if (pPlotState->hasPlots)
         {
            // emit the plot and the snapshot file
            events().onPlotOutput(path, pPlotState->snapshotFile);

            // we've consumed the snapshot file, so clear it
            pPlotState->snapshotFile = FilePath();
         }

         // clean up the plot so it isn't emitted twice
         error = path.removeIfExists();
         if (error)
            LOG_ERROR(error);
      }
   }
}

void saveSnapshot(boost::shared_ptr<PlotState> pPlotState)
{
   // if there's a plot on the device, write its display list before it's
   // cleared for the next page
   FilePath outputFile = pPlotState->plotFolder.complete(
         core::system::generateUuid(false) + kDisplayListExt);
   Error error = r::exec::RFunction(".rs.saveGraphics", 
         outputFile.absolutePath()).call();

   // if there's already an unconsumed display list, remove it, since this
   // display list replaces it
   if (!pPlotState->snapshotFile.empty())
   {
      error = pPlotState->snapshotFile.removeIfExists();
      if (error) 
         LOG_ERROR(error);
      else
         pPlotState->snapshotFile = FilePath();
   }

   if (error)
   {
      LOG_ERROR(error);
   }
   else
   {
      pPlotState->snapshotFile = outputFile;
   }
}

void removeGraphicsDevice(boost::shared_ptr<PlotState> pPlotState)
{
   // take a snapshot of the last plot's display list before we turn off the
   // device (if we haven't emitted it yet)
   if (pPlotState->hasPlots && pPlotState->snapshotFile.empty())
      saveSnapshot(pPlotState);

   // restore the figure margins
   Error error = r::exec::RFunction("par", pPlotState->sexpMargins).call();
   if (error)
      LOG_ERROR(error);

   // turn off the graphics device -- this has the side effect of writing the
   // device's remaining output to files
   error = r::exec::RFunction("dev.off").call();
   if (error)
      LOG_ERROR(error);

   processPlots(false, pPlotState);
}

void onBeforeNewPlot(boost::shared_ptr<PlotState> pPlotState)
{
   if (pPlotState->hasPlots)
   {
      saveSnapshot(pPlotState);
   }
   pPlotState->hasPlots = true;
}

void onNewPlot(boost::shared_ptr<PlotState> pPlotState)
{
   pPlotState->hasPlots = true;
   processPlots(true, pPlotState);
}

void onConsolePrompt(boost::shared_ptr<PlotState> pPlotState,
                     const std::string& )
{
   removeGraphicsDevice(pPlotState);
   pPlotState->onConsolePrompt.disconnect();
   pPlotState->onNewPlot.disconnect();
   pPlotState->onBeforeNewPlot.disconnect();
   pPlotState->onBeforeNewGridPage.disconnect();
}

} // anonymous namespace

// begins capturing plot output
core::Error beginPlotCapture(int pixelWidth, const FilePath& plotFolder)
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

   // marker for content; this is necessary because on Windows, turning off
   // the png device writes an empty PNG file even if nothing was plotted, and
   // we need to avoid treating that file as though it were an actual plot
   boost::shared_ptr<PlotState> pPlotState = boost::make_shared<PlotState>(
         plotFolder);

   // save old figure parameters
   r::exec::RFunction par("par");
   par.addParam("no.readonly", true);
   r::sexp::Protect protect;
   SEXP sexpMargins;
   error = par.call(&sexpMargins, &protect);
   if (error)
      LOG_ERROR(error);

   // preserve until chunk is finished executing
   pPlotState->sexpMargins.set(sexpMargins);

   // create the notebook graphics device
   error = r::exec::RFunction(".rs.createNotebookGraphicsDevice",
         plotFolder.absolutePath() + "/" kPlotPrefix "%03d.png",
         pixelWidth, 
         r::session::graphics::device::devicePixelRatio(),
         r::session::graphics::extraBitmapParams()).call();
   if (error)
   {
      // this is fatal; without a graphics device we can't capture plots
      return error;
   }

   // turn on display list recording so we can do intelligent resizing later
   r::exec::RFunction devControl("dev.control");
   devControl.addParam("displaylist", "enable");
   error = devControl.call();
   if (error)
   {
      // non-fatal since we'll do best-effort resizing in the absence of
      // display lists
      LOG_ERROR(error);
   }

   // set notebook-friendly figure margins 
   error = r::exec::RFunction(".rs.setNotebookMargins").call();
   if (error)
      LOG_ERROR(error);
   
   // complete the capture on the next console prompt
   pPlotState->onConsolePrompt = module_context::events().onConsolePrompt.connect(
         boost::bind(onConsolePrompt, pPlotState, _1));

   pPlotState->onBeforeNewPlot = plots::events().onBeforeNewPlot.connect(
         boost::bind(onBeforeNewPlot, pPlotState));
   
   pPlotState->onBeforeNewGridPage = plots::events().onBeforeNewGridPage.connect(
         boost::bind(onBeforeNewPlot, pPlotState));

   pPlotState->onNewPlot = plots::events().onNewPlot.connect(
         boost::bind(onNewPlot, pPlotState));

   return Success();
}

core::Error initPlots()
{
   ExecBlock initBlock;
   initBlock.addFunctions()
      (boost::bind(module_context::sourceModuleRFile, "NotebookPlots.R"));

   return initBlock.execute();
}


} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

