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
#define kGoldenRatio 1.618

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {
namespace {

bool isPlotPath(const FilePath& path)
{
   return path.hasExtensionLowerCase(".png") &&
          string_utils::isPrefixOf(path.stem(), kPlotPrefix);
}

} // anonymous namespace

PlotCapture::PlotCapture() :
   hasPlots_(false)
{
}

PlotCapture::~PlotCapture()
{
}

void PlotCapture::processPlots(bool ignoreEmpty)
{
   // ensure plot folder exists
   if (!plotFolder_.exists())
      return;

   // collect plots from the folder
   std::vector<FilePath> folderContents;
   Error error = plotFolder_.children(&folderContents);
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
         if (hasPlots_)
         {
            // emit the plot and the snapshot file
            events().onPlotOutput(path, snapshotFile_);

            // we've consumed the snapshot file, so clear it
            snapshotFile_ = FilePath();
         }

         // clean up the plot so it isn't emitted twice
         error = path.removeIfExists();
         if (error)
            LOG_ERROR(error);
      }
   }
}

void PlotCapture::saveSnapshot()
{
   // if there's a plot on the device, write its display list before it's
   // cleared for the next page
   FilePath outputFile = plotFolder_.complete(
         core::system::generateUuid(false) + kDisplayListExt);
   Error error = r::exec::RFunction(".rs.saveGraphics", 
         outputFile.absolutePath()).call();

   // if there's already an unconsumed display list, remove it, since this
   // display list replaces it
   if (snapshotFile_.empty())
   {
      error = snapshotFile_.removeIfExists();
      if (error) 
         LOG_ERROR(error);
      else
         snapshotFile_ = FilePath();
   }

   if (error)
   {
      LOG_ERROR(error);
   }
   else
   {
      snapshotFile_ = outputFile;
   }
}

void PlotCapture::onExprComplete()
{
   // nothing here yet; at one point we used this to flush the output device,
   // but that causes problems when output device needs to remain open for
   // the case wherein multiple expressions progressively draw on the device.
   // this leaves us with a problem wherein we don't know the expression
   // with which to associate the plot output; see case 5701
}

void PlotCapture::removeGraphicsDevice()
{
   // take a snapshot of the last plot's display list before we turn off the
   // device (if we haven't emitted it yet)
   if (hasPlots_ && 
       sizeBehavior_ == PlotSizeAutomatic &&
       snapshotFile_.empty())
      saveSnapshot();

   // turn off the graphics device -- this has the side effect of writing the
   // device's remaining output to files
   Error error = r::exec::RFunction("dev.off").call();
   if (error)
      LOG_ERROR(error);

   processPlots(false);
   hasPlots_ = false;
}

void PlotCapture::onBeforeNewPlot()
{
   if (hasPlots_ &&
       sizeBehavior_ == PlotSizeAutomatic)
   {
      saveSnapshot();
   }
   hasPlots_ = true;
}

void PlotCapture::onNewPlot()
{
   hasPlots_ = true;
   processPlots(true);
}

// begins capturing plot output
core::Error PlotCapture::connectPlots(double height, double width, 
                                      PlotSizeBehavior sizeBehavior,
                                      const FilePath& plotFolder)
{
   // clean up any stale plots from the folder
   plotFolder_ = plotFolder;
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

   // infer height/width if only one is given
   if (height == 0 && width > 0)
      height = width / kGoldenRatio;
   else if (height > 0 && width == 0)
      width = height * kGoldenRatio;
   width_ = width;
   height_ = height;
   sizeBehavior_ = sizeBehavior;

   // save old figure parameters
   r::exec::RFunction par("par");
   par.addParam("no.readonly", true);
   r::sexp::Protect protect;
   SEXP sexpMargins;
   error = par.call(&sexpMargins, &protect);
   if (error)
      LOG_ERROR(error);

   // preserve until chunk is finished executing
   sexpMargins_.set(sexpMargins);

   // create the graphics device (must succeed)
   error = createGraphicsDevice();
   if (error)
      return error;

   // set notebook-friendly figure margins 
   error = r::exec::RFunction(".rs.setNotebookMargins").call();
   if (error)
      LOG_ERROR(error);
   
   onBeforeNewPlot_ = plots::events().onBeforeNewPlot.connect(
         boost::bind(&PlotCapture::onBeforeNewPlot, this));
   
   onBeforeNewGridPage_ = plots::events().onBeforeNewGridPage.connect(
         boost::bind(&PlotCapture::onBeforeNewPlot, this));

   onNewPlot_ = plots::events().onNewPlot.connect(
         boost::bind(&PlotCapture::onNewPlot, this));

   NotebookCapture::connect();
   return Success();
}

void PlotCapture::disconnect()
{
   if (connected())
   {
      // remove the graphics device
      removeGraphicsDevice();

      // restore the figure margins
      Error error = r::exec::RFunction("par", sexpMargins_).call();
      if (error)
         LOG_ERROR(error);

      onNewPlot_.disconnect();
      onBeforeNewPlot_.disconnect();
      onBeforeNewGridPage_.disconnect();
   }
   NotebookCapture::disconnect();
}

core::Error PlotCapture::createGraphicsDevice()
{
   Error error;

   // create the notebook graphics device
   r::exec::RFunction createDevice(".rs.createNotebookGraphicsDevice");

   // the folder in which to place the rendered plots (this is a sibling of the
   // main chunk output folder)
   createDevice.addParam(
         plotFolder_.absolutePath() + "/" kPlotPrefix "%03d.png");

   // device dimensions
   createDevice.addParam(height_);
   createDevice.addParam(width_); 

   // sizing behavior drives units -- user specified units are in inches but
   // we use pixels when scaling automatically
   createDevice.addParam(sizeBehavior_ == PlotSizeManual ? "in" : "px");

   // devie parameters
   createDevice.addParam(r::session::graphics::device::devicePixelRatio());
   createDevice.addParam(r::session::graphics::extraBitmapParams());
   error = createDevice.call();
   if (error)
      return error;

   // if sizing automatically, turn on display list recording so we can do
   // intelligent resizing later
   if (sizeBehavior_ == PlotSizeAutomatic)
   {
      r::exec::RFunction devControl("dev.control");
      devControl.addParam("displaylist", "enable");
      error = devControl.call();
      if (error)
      {
         // non-fatal since we'll do best-effort (client side) resizing in the
         // absence of display lists
         LOG_ERROR(error);
      }
   }

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

