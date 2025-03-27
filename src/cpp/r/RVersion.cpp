/*
 * RVersion.cpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <r/RVersion.hpp>

#include <r/RExec.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace r {

static core::Version versionImpl()
{
   std::string version;
   Error error = r::exec::evaluateString("format(getRversion())", R_BaseEnv, &version);
   if (error)
      LOG_ERROR(error);

   return Version(version);
}

core::Version version()
{
   static Version instance = versionImpl();
   return instance;
}

} // namespace r
} // namespace rstudio

