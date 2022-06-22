/*
 * RVersionInfo.cpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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

#include <r/RVersionInfo.hpp>
#include <r/RExec.hpp>

#include <shared_core/Error.hpp>

using namespace rstudio::core;
using namespace rstudio::core::r_util;

namespace rstudio {
namespace r {
namespace version_info {

namespace {

RVersionNumber readVersion()
{
   std::string currentRVersion;
   Error error = exec::RFunction(".rs.rVersionString").call(&currentRVersion);
   if (error)
      LOG_ERROR(error);
   
   return RVersionNumber::parse(currentRVersion);
}

} // end anonymous namespace

// Returns a representation of the current version of R
RVersionNumber currentRVersion()
{
   static RVersionNumber instance = readVersion();
   return instance;
}

} // namespace version_info
} // namespace r
} // namespace rstudio
