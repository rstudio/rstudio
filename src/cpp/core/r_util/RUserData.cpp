/*
 * RUserData.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include <core/r_util/RUserData.hpp>

#include <core/FilePath.hpp>

#include <core/system/System.hpp>


namespace rstudio {
namespace core {
namespace r_util {

namespace {


} // anonymous namespace


UserDirectories userDirectories(SessionType sessionType,
                                const std::string& homePath)
{
   UserDirectories dirs;
   if (!homePath.empty())
      dirs.homePath = homePath;
   else
      dirs.homePath = core::system::userHomePath("R_USER|HOME").absolutePath();

   // compute user scratch path
   std::string scratchPathName;
   if (sessionType == SessionTypeDesktop)
      scratchPathName = "RStudio-Desktop";
   else
      scratchPathName = "RStudio";

   dirs.scratchPath = core::system::userSettingsPath(
                                          FilePath(dirs.homePath),
                                          scratchPathName).absolutePath();

   return dirs;
}


} // namespace r_util
} // namespace core 
} // namespace rstudio



