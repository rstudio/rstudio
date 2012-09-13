/*
 * DesktopWin32DetectRHome.cpp
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

#ifndef _WIN32
#error DesktopDetectRHome.cpp is Windows-specific
#endif

#include "DesktopDetectRHome.hpp"

#include <windows.h>

#include <boost/bind.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include "DesktopRVersion.hpp"
#include "DesktopUtils.hpp"

using namespace core;

namespace desktop {

namespace {

bool prepareEnvironment(Options&, bool forceUi, QWidget* parent)
{
   // save the previous RBinDir so we can strip it off the front
   // of the path if we set another R bin dir
   std::string s_previousRBinDir;

   RVersion rVersion = detectRVersion(forceUi, parent);
   if (!rVersion.isValid())
      return false;

   // get the short path version of the home dir
   std::string homePath =
         QDir::toNativeSeparators(rVersion.homeDir()).toStdString();
   DWORD len = ::GetShortPathName(homePath.c_str(), NULL, 0);
   std::vector<TCHAR> buffer(len, 0);
   if (::GetShortPathName(homePath.c_str(), &(buffer[0]), len) != 0)
   {
      // copy path to string and assign it we got one
      std::string shortHomePath(&(buffer[0]));
      if (!shortHomePath.empty())
         homePath = shortHomePath;
   }
   else
   {
      LOG_ERROR(systemError(::GetLastError(), ERROR_LOCATION));
   }


   // set R_HOME
   system::setenv("R_HOME", homePath);

   // get PATH and remove preiovus RBinDir if it's there
   std::string path = system::getenv("PATH");
   if (!s_previousRBinDir.empty())
   {
      std::string strip = s_previousRBinDir + ";";
      if (boost::algorithm::starts_with(path, strip))
         path = path.substr(strip.length());
   }

   // determine new R bin dir then prepend it to the path
   std::string rBinDir = QDir::toNativeSeparators(
                                       rVersion.binDir()).toStdString();
   s_previousRBinDir = rBinDir;
   path = rBinDir + ";" + path;
   system::setenv("PATH", path);

   return true;
}

} // anonymous namespace

bool prepareEnvironment(Options &options)
{
   return prepareEnvironment(options,
                             ::GetAsyncKeyState(VK_CONTROL) & ~1,
                             NULL);
}

bool chooseRHomeAndPrepareEnvironment(QWidget* parent)
{
   return prepareEnvironment(desktop::options(), true, parent);
}

} // namespace desktop
