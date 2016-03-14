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

#include <core/system/FileMonitor.hpp>
#include <core/StringUtils.hpp>

#include <session/SessionModuleContext.hpp>

#include <r/RExec.hpp>
#include <r/session/RGraphics.hpp>

#define kPlotPrefix "_rs_chunk_plot_"

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

void processPlots(const FilePath& plotFolder, bool fireEvents)
{
   // collect plots from the folder
   std::vector<FilePath> folderContents;
   Error error = plotFolder.children(&folderContents);
   if (error)
      LOG_ERROR(error);

   BOOST_FOREACH(const FilePath& path, folderContents)
   {
      if (isPlotPath(path))
      {
         if (fireEvents)
            events().onPlotOutput(path);

         // clean up the plot so it isn't emitted twice
         error = path.removeIfExists();
         if (error)
            LOG_ERROR(error);
      }
   }
}

void removeGraphicsDevice(const FilePath& plotFolder, bool hasPlots)
{
   // turn off the graphics device -- this has the side effect of writing the
   // device's remaining output to files
   Error error = r::exec::RFunction("dev.off").call();
   if (error)
      LOG_ERROR(error);

   processPlots(plotFolder, hasPlots);
}

void onNewPlot(const FilePath& plotFolder,
               boost::shared_ptr<bool> pHasPlots)
{
   *pHasPlots = true;
   processPlots(plotFolder, true);
}

void onConsolePrompt(const FilePath& plotFolder,
                     boost::shared_ptr<bool> pHasPlots,
                     const std::string& )
{
   removeGraphicsDevice(plotFolder, *pHasPlots);

   module_context::events().onConsolePrompt.disconnect(
         boost::bind(onConsolePrompt, plotFolder, pHasPlots, _1));

   plots::events().onNewPlot.disconnect(
         boost::bind(onNewPlot, pHasPlots, _1));
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
   
   // generate code for creating PNG device
   boost::format fmt("{ require(grDevices, quietly=TRUE); "
                     "  png(file = \"%1%/" kPlotPrefix "%%03d.png\", "
                     "  width = 8, height = 5, "
                     "  units=\"in\", res = 96 %2%)"
                     "}");

   // marker for content; this is necessary because on Windows, turning off
   // the png device writes an empty PNG file even if nothing was plotted, and
   // we need to avoid treating that file as though it were an actual plot
   boost::shared_ptr<bool> pHasPlots = boost::make_shared<bool>(false);

   // create the PNG device
   error = r::exec::executeString(
         (fmt % string_utils::utf8ToSystem(plotFolder.absolutePath())
              % r::session::graphics::extraBitmapParams()).str());
   if (error)
      return error;

   // complete the capture on the next console prompt
   module_context::events().onConsolePrompt.connect(
         boost::bind(onConsolePrompt, plotFolder, pHasPlots, _1));

   plots::events().onNewPlot.connect(
         boost::bind(onNewPlot, plotFolder, pHasPlots));

   return Success();
}


} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

