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

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

using namespace rstudio::core;

core::Error beginPlotCapture(const FilePath& plotFolder,
                             boost::function<void(FilePath&)> plotCaptured)
{
   // generate code for creating PNG device
   boost::format fmt("{ require(grDevices, quietly=TRUE); "
                     "  png(file = \"%1%\", width = 3, height = 3, "
                     "  units=\"in\", res = 96, type = \"cairo-png\", TRUE)");

   /*
    * the basic idea here is that we will plot to the cache folder 
    * 
    * we'll use an inotify based system to watch for the plots to be
    * produced -- this is okay even on NFS since this session is the one
    * responsible for producing the plots
    *
    */

   return Success();
}

void endPlotCapture()
{
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

