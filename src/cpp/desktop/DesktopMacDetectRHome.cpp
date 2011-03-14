/*
 * DesktopMacDetectRHome.cpp
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

#include <boost/algorithm/string/trim.hpp>

#include <QtCore>
#include <QMessageBox>

#include <core/system/System.hpp>

#include "config.h"

using namespace core;

namespace desktop {

namespace {

FilePath detectRHome()
{
   // run R to detect R home
   std::string output;
   Error error = core::system::captureCommand("R RHOME", &output);
   if (error)
   {
      LOG_ERROR(error);
      return FilePath();
   }
   boost::algorithm::trim(output);

   // return the home path if we got one
   if (!output.empty())
      return FilePath(output);
   else
      return FilePath();
}

void showRNotFoundError(const std::string& msg)
{
   QMessageBox::critical(NULL, "R Not Found", QString::fromStdString(msg));
}

} // anonymous namespace

bool prepareEnvironment(Options&)
{
   FilePath homePath = detectRHome();
   if (homePath.empty())
   {
      showRNotFoundError("Unable to detect a version of R on your system. "
                         "Is the R executable on your system path?");
      return false;
   }

   // verify set home path
   if (homePath.exists())
   {
      core::system::setenv("R_HOME", homePath.absolutePath());
   }
   else
   {
      showRNotFoundError("R home path (" + homePath.absolutePath() +
                         ") does not exist.");
      return false;
   }

   // verify and set doc dir
   FilePath rDocDir = homePath.complete("doc");
   if (rDocDir.exists())
   {
       core::system::setenv("R_DOC_DIR", rDocDir.absolutePath());
   }
   else
   {
      showRNotFoundError("R doc directory (" + rDocDir.absolutePath() +
                         ") does not exist.");
      return false;
   }

   // verify and set library path
   FilePath rLibPath = homePath.complete("lib");
   if (rLibPath.exists())
   {
      // make sure the R lib was built
      FilePath libRpath = rLibPath.complete("libR.dylib");
      if (libRpath.exists())
      {
         core::system::setenv("DYLD_LIBRARY_PATH", rLibPath.absolutePath());
      }
      else
      {
         showRNotFoundError(rLibPath.filename() + " not found in R library "
                            "path. If this is a custom build of R, was it "
                            "built with the --enable-R-shlib option?");
         return false;
      }
   }
   else
   {
      showRNotFoundError("R library directory (" + rLibPath.absolutePath() +
                         ") does not exist.");
      return false;
   }

   return true;
}

} // namespace desktop
