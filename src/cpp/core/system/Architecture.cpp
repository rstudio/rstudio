/*
 * Architecture.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#include <set>

#include <core/system/Architecture.hpp>

#include <core/Algorithm.hpp>
#include <core/StringUtils.hpp>

namespace rstudio {
namespace core {
namespace system {

std::string supportedArchitecturesViaFile(const core::FilePath& path)
{
   using namespace core::system;

   ProcessOptions options;
   ProcessResult result;

   std::vector<std::string> args = { "--", path.getAbsolutePath() };
   Error error = runProgram("/usr/bin/file", args, options, &result);
   if (error)
   {
      LOG_ERROR(error);
      return {};
   }

   std::vector<std::string> lines =
         core::algorithm::split(result.stdOut, "\n");

   std::set<std::string> archs;

   for (std::string line : lines)
   {
      // trim leading filename if present
      auto idx = line.find(':');
      if (idx != std::string::npos)
         line = line.substr(idx + 1);

      // check for known architectures
      for (auto&& arch : { "x86_64", "arm64" })
         if (line.find(arch) != std::string::npos)
            archs.insert(arch);
   }

   return core::algorithm::join(
            std::vector<std::string>(archs.begin(), archs.end()),
            " ");
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

   return supportedArchitecturesViaFile(path);

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
