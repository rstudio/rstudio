/*
 * DiagnosticsMain.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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
#include <iostream>
#include <string>
#include <vector>

#include <boost/algorithm/string.hpp>

#include <core/Log.hpp>
#include <core/system/Xdg.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/System.hpp>
#include <core/system/Xdg.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include "config.h"

using namespace rstudio;
using namespace rstudio::core;

namespace {

// NOTE: this code is duplicated in diagnostics as well (and also in
// SessionOptions.hpp although the code path isn't exactly the same)
FilePath userLogPath()
{
   return core::system::xdg::userDataDir().completePath("log");
}

void writeFile(const std::string& description, const core::FilePath& path, std::ostream& ostr)
{
   ostr << description << ": " << path << std::endl;
   ostr << "--------------------------------------------------" << std::endl;
   ostr << std::endl;

   if (path.exists())
   {
      std::string contents;
      Error error = core::readStringFromFile(path, &contents);
      if (error)
         LOG_ERROR(error);
      if (contents.empty())
         ostr << "(Empty)" << std::endl << std::endl;
      else
         ostr << contents << std::endl << std::endl;
   }
   else
   {
      ostr << "(Not Found)" << std::endl << std::endl;
   }
}

void writeLogFile(const std::string& logFileName, std::ostream& ostr)
{
   writeFile("Log file", userLogPath().completeChildPath(logFileName), ostr);
}

void writeUserPrefs(std::ostream& ostr)
{
   writeFile("User prefs", core::system::xdg::userConfigDir().completePath("rstudio-prefs.json"),
         ostr);
   writeFile("System prefs", core::system::xdg::systemConfigFile("rstudio-prefs.json"),
         ostr);
   writeFile("User state", core::system::xdg::userDataDir().completePath("rstudio-desktop.json"),
         ostr);
}


} // anonymous namespace


int main(int argc, char** argv)
{
   core::log::setProgramId("rstudio-diagnostics");
   core::system::initializeStderrLog("rstudio-diagnostics",
                                    core::log::LogLevel::WARN);

   // ignore SIGPIPE
   Error error = core::system::ignoreSignal(core::system::SigPipe);
   if (error)
     LOG_ERROR(error);

   writeLogFile("rdesktop.log", std::cout);
   writeLogFile("rsession-" + core::system::username() + ".log", std::cout);
   writeUserPrefs(std::cout);

   return EXIT_SUCCESS;
}
