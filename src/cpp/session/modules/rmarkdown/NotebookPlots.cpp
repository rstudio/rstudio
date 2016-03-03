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

bool isPlotPath(const FilePath& path)
{
   std::cerr << "testing filter for " << path.absolutePath() << " " << 
      path.hasExtensionLowerCase(".png") <<  string_utils::isPrefixOf(path.stem(), kPlotPrefix) << std::endl;
   return path.hasExtensionLowerCase(".png") &&
          string_utils::isPrefixOf(path.stem(), kPlotPrefix);
}

bool plotFilter(const FileInfo& file)
{
   return file.isDirectory() || isPlotPath(FilePath(file.absolutePath()));
}

void removeGraphicsDevice(const FilePath& plotFolder, 
                          boost::function<void(const FilePath&)> plotCaptured)
{
   // turn off the graphics device -- this has the side effect of writing the
   // device's remaining output to files
   Error error = r::exec::RFunction("dev.off").call();
   if (error)
      LOG_ERROR(error);

   // clean up any stale plots from the folder
   std::vector<FilePath> folderContents;
   error = plotFolder.children(&folderContents);
   if (error)
      LOG_ERROR(error);

   BOOST_FOREACH(const FilePath& path, folderContents)
   {
      if (isPlotPath(path))
         plotCaptured(path);
   }
}

void unregisterMonitor(const std::string&,
                       const core::system::file_monitor::Handle& handle)
{
   std::cerr << "unregistering monitor " << handle.id << std::endl;
   module_context::events().onConsolePrompt.disconnect(
         boost::bind(unregisterMonitor, _1, handle));

   core::system::file_monitor::unregisterMonitor(handle);
}

void onMonitorRegistered(boost::function<void(const FilePath&)> plotCaptured,
      const core::system::file_monitor::Handle& handle,
      const tree<FileInfo>& files)
{
   std::cerr << "monitor registered" << std::endl;
   // we only want to listen until the next console prompt
   module_context::events().onConsolePrompt.connect(
         boost::bind(unregisterMonitor, _1, handle));

   // fire for any plots which were emitted during file monitor registration
   for (tree<FileInfo>::iterator it = files.begin(); it != files.end(); it++) 
   {
      std::cerr << "init contents:" << it->absolutePath() << std::endl;
      FilePath path(it->absolutePath());
      if (isPlotPath(path))
         plotCaptured(path);
   }

   // TODO: emit any plots which were created while we were registering
}

void onMonitorRegError(const FilePath& plotFolder, 
                       boost::function<void(const FilePath&)> plotCaptured,
                       const Error &error)
{
   LOG_ERROR(error);
   removeGraphicsDevice(plotFolder, plotCaptured);
}

void onMonitorUnregistered(const FilePath& plotFolder,
                           boost::function<void(const FilePath&)> plotCaptured,
                           const core::system::file_monitor::Handle& handle)
{
   // when the monitor is unregistered, disable the associated graphics
   // device
   std::cerr << "monitor unregistered " << std::endl;
   removeGraphicsDevice(plotFolder, plotCaptured);
}

void onPlotFilesChanged(
      boost::function<void(FilePath&)> plotCaptured,
      const std::vector<core::system::FileChangeEvent>& events)
{
   std::cerr << "notified of changes: " << events.size() << std::endl;
   BOOST_FOREACH(const core::system::FileChangeEvent& event, events)
   {
      std::cerr << "change notif " << event.type() << " for object " << event.fileInfo().absolutePath() << std::endl;
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
core::Error beginPlotCapture(
      const FilePath& plotFolder,
      boost::function<void(const FilePath&)> plotCaptured)
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
         std::cerr << "removing " << file.absolutePath() << std::endl;
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
                     "  width = 5, height = 5, "
                     "  units=\"in\", res = 96, type = \"cairo-png\", TRUE)"
                     "}");

   // create the PNG device
   error = r::exec::executeString(
         (fmt % plotFolder.absolutePath()).str());
   if (error)
      return error;

   // set up file monitor callbacks
   core::system::file_monitor::Callbacks callbacks;
   callbacks.onRegistered = boost::bind(onMonitorRegistered,
         plotCaptured, _1, _2);
   callbacks.onUnregistered = boost::bind(onMonitorUnregistered,
         plotFolder, plotCaptured, _1);
   callbacks.onRegistrationError = boost::bind(onMonitorRegError,
         plotFolder, plotCaptured, _1);
   callbacks.onFilesChanged = boost::bind(onPlotFilesChanged, plotCaptured, _1);

   // create the monitor
   core::system::file_monitor::registerMonitor(plotFolder, true, plotFilter,
         callbacks);

   return Success();
}


} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

