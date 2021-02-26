/*
 * Architecture.cpp
 *
 * Copyright (C) 2021 by RStudio, PBC
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

#include <core/system/Architecture.hpp>

#include <core/Algorithm.hpp>
#include <core/StringUtils.hpp>

namespace rstudio {
namespace core {
namespace system {

std::string supportedArchitecturesViaLipo(const core::FilePath& path)
{
   using namespace core::system;

   ProcessOptions options;
   ProcessResult result;

   std::vector<std::string> args = { "-archs", path.getAbsolutePath() };
   Error error = runProgram("/usr/bin/lipo", args, options, &result);
   if (error)
   {
      LOG_ERROR(error);
      return {};
   }

   return core::string_utils::trimWhitespace(result.stdOut);
}

std::string supportedArchitecturesViaUname()
{
   using namespace core::system;

   ProcessOptions options;
   ProcessResult result;

   Error error = runProgram("/usr/bin/uname", { "-m" }, options, &result);
   if (error)
   {
      LOG_ERROR(error);
      return {};
   }

   return core::string_utils::trimWhitespace(result.stdOut);
}

std::string supportedArchitectures(const core::FilePath& path)
{
#if defined(_WIN32)

   // assume x86_64 for now, but arm64 Windows builds with x86_64 emulation
   // may become a reality in the near future
   return "x86_64";

#elif defined(__APPLE__)

   // if lipo is available, use it to infer the architecture; otherwise,
   // fall back to default uname implementation
   FilePath lipoPath("/usr/bin/lipo");
   if (lipoPath.exists())
      return supportedArchitecturesViaLipo(path);
   else
      return supportedArchitecturesViaUname();

#else

   return supportedArchitecturesViaUname();

#endif

}

bool haveCompatibleArchitectures(const core::FilePath& lhs,
                                 const core::FilePath& rhs)
{
#ifndef __APPLE__
   return true;
#else
   std::string lhsArch = supportedArchitectures(lhs);
   std::string rhsArch = supportedArchitectures(rhs);

   for (auto&& arch : { "x86_64", "arm64" })
   {
      bool compatible =
            lhsArch.find(arch) != std::string::npos &&
            rhsArch.find(arch) != std::string::npos;

      if (compatible)
         return true;
   }

   return false;
#endif
}

} // end namespace system
} // end namespace core
} // end namespace rstudio
