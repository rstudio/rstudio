/*
 * DiagnosticsMain.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
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

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/System.hpp>

#include "config.h"

using namespace core;

namespace {

FilePath homePath()
{
   return core::system::userHomePath("R_USER|HOME");
}

// NOTE: this code is duplicated in diagnostics as well (and also in
// SessionOptions.hpp although the code path isn't exactly the same)
FilePath userLogPath()
{
   FilePath logPath = core::system::userSettingsPath(
         homePath(),
#ifdef RSTUDIO_SERVER
         "RStudio"
#else
         "RStudio-Desktop"
#endif
         ).childPath("log");
   return logPath;
}

void writeLogFile(const std::string& logFileName, std::ostream& ostr)
{
   ostr << "Log file: " << logFileName << std::endl;
   ostr << "--------------------------------------------------" << std::endl;
   ostr << std::endl;

   FilePath logFilePath = userLogPath().childPath(logFileName);
   if (logFilePath.exists())
   {
      std::string contents;
      Error error = core::readStringFromFile(logFilePath, &contents);
      if (error)
         LOG_ERROR(error);
      if (contents.empty())
         ostr << "(Empty)" << std::endl << std::endl;
      else
         ostr << contents << std::endl;
   }
   else
   {
      ostr << "(Not Found)" << std::endl << std::endl;
   }
}


} // anonymous namespace


int main(int argc, char** argv)
{
  core::system::initializeStderrLog("rstudio-diagnostics",
                                    core::system::kLogLevelWarning);

  // ignore SIGPIPE
  Error error = core::system::ignoreSignal(core::system::SigPipe);
  if (error)
     LOG_ERROR(error);

  writeLogFile("rdesktop.log", std::cout);
  writeLogFile("rsession-" + core::system::username() + ".log", std::cout);

  return EXIT_SUCCESS;
}
