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

#include <boost/format.hpp>
#include <boost/foreach.hpp>

#include <core/system/FileMonitor.hpp>
#include <core/StringUtils.hpp>

#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>

#define kPlotPrefix "_rs_chunk_plot_"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {
namespace {

bool plotFilter(const FileInfo& file)
{
   FilePath path(file.absolutePath());

   return path.hasExtensionLowerCase(".png") &&
          string_utils::isPrefixOf(path.stem(), kPlotPrefix);
}

void unregisterMonitor(const std::string&,
                       const core::system::file_monitor::Handle& handle)
{
   module_context::events().onConsolePrompt.disconnect(
         boost::bind(unregisterMonitor, _1, handle));

   core::system::file_monitor::unregisterMonitor(handle);
}

void onMonitorRegistered(const core::system::file_monitor::Handle& handle,
      const tree<FileInfo>&)
{
   // we only want to listen until the next console prompt
   module_context::events().onConsolePrompt.connect(
         boost::bind(unregisterMonitor, _1, handle));

   // TODO: emit any plots which were created while we were registering
}

void onMonitorUnregistered(const core::system::file_monitor::Handle& handle)
{
   // when the monitor is unregistered, disable the associated graphics
   // device
   Error error = r::exec::RFunction("dev.off").call();
   if (error)
      LOG_ERROR(error);
}

void onPlotFilesChanged(
      boost::function<void(FilePath&)> plotCaptured,
      const std::vector<core::system::FileChangeEvent>& events)
{
   BOOST_FOREACH(const core::system::FileChangeEvent& event, events)
   {
      // we only care about new plots
      if (event.type() != core::system::FileChangeEvent::FileAdded)
         continue;

      // notify caller
      FilePath path(event.fileInfo().absolutePath());
      plotCaptured(path);
   }
}

} // anonymous namespace

// begins capturing plot output
core::Error beginPlotCapture(const FilePath& plotFolder,
                             boost::function<void(FilePath&)> plotCaptured)
{
   // generate code for creating PNG device
   boost::format fmt("{ require(grDevices, quietly=TRUE); "
                     "  png(file = \"%1%" kPlotPrefix "%%03d\", "
                     "  width = 3, height = 3, "
                     "  units=\"in\", res = 96, type = \"cairo-png\", TRUE)"
                     "}");

   // create the PNG device
   Error error = r::exec::executeString(
         (fmt % plotFolder.absolutePath()).str());
   if (error)
      return error;

   // set up file monitor callbacks
   core::system::file_monitor::Callbacks callbacks;
   callbacks.onRegistered = onMonitorRegistered;
   callbacks.onUnregistered = onMonitorUnregistered;
   callbacks.onFilesChanged = boost::bind(onPlotFilesChanged, plotCaptured, _1);

   // create the monitor
   core::system::file_monitor::registerMonitor(plotFolder, false, plotFilter,
         callbacks);

   return Success();
}


} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

