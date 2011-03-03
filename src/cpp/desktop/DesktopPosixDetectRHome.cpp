/*
 * DesktopPosixDetectRHome.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "DesktopDetectRHome.hpp"

#include <core/FilePath.hpp>
#include <core/system/System.hpp>

using namespace core;

namespace desktop {

bool prepareEnvironment(Options& options)
{
   // determine path to r-ldpath script (try prod then dev path)
   FilePath supportingFilePath = options.supportingFilePath();
   FilePath scriptPath = supportingFilePath.complete("bin/r-ldpath");
   if (!scriptPath.exists())
      scriptPath = supportingFilePath.complete("session/r-ldpath");
   if (scriptPath.exists())
   {
      // run script
      std::string ldLibraryPath;
      Error error = system::captureCommand(scriptPath.absolutePath(),
                                           &ldLibraryPath);
      if (error)
      {
         LOG_ERROR(error);
         return true;
      }

      // set env var
      system::setenv("LD_LIBRARY_PATH", ldLibraryPath);

   }

   // always return true -- this function basically allows rJava to work
   // if it doesn't work for some reason we just log and don't prevent
   // the whole process from starting
   return true;
}

} // namespace desktop
